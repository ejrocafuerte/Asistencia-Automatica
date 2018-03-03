#include <jni.h>
#include <string>
#include <android/log.h>
#include <opencv2/opencv.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/calib3d/calib3d.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <iostream>
#include <numeric>

using namespace std;
using namespace cv;

extern "C"
{
static vector<double> vTilt, vYaw, vRoll;
static double accumTilt = 1.0f, accumYaw = 1.0f, accumRoll = 1.0f;
enum MetodoBusqueda {LAPLACIAN, CANNY};

static void preprocesar(Mat& imagen_orig, Mat& imagen_prep, double thresh);
static void buscarCuadrados( const Mat& image, vector<vector<Point> >& cuadrados, MetodoBusqueda metodo, int thresholdLevel);
static void dibujarCuadrados( Mat& image, const vector<vector<Point> >& cuadrados, Scalar scalar);
static void ordenarVerticesCuadrado(vector<Point> &cuadrado);
static void ordenarCuadradosPorPosicionEspacial(vector<vector<Point> >& cuadrados, int direccion);
static bool alineadoEjeFocal(Mat& image, vector<vector<Point> >& cuadrados);
static void particionarCuadrados( Mat& image,
                                  const vector<vector<Point> >& cuadrados,
                                  vector<vector<vector<Point> > >& particiones);
static void unificarParticiones(const vector<vector<Point> >& cuadrados,
                                const vector<vector<Point> > & puntosVertices,
                                const vector<vector<vector<Point> > > & puntosFrontera,
                                const vector<vector<vector<Point> > > & puntosInternos,
                                vector<vector<vector<Point> > > & particiones);
static void decodificarParticiones( Mat& image,
                                    Mat& image_c,
                                    vector<vector<vector<Point> > >& particiones,
                                    string& mensajeBinario,
                                    double thresh);
static void traducirMensaje(string& mensajeBinario, string& mensaje, int& fase);
bool filtrarCuadrado(const vector<vector<Point> >& cuadrados, vector<Point>& approx);
double distanciaEntreDosPuntos(Point p1, Point p2);
int buscarPuntoMasCercano(vector<Point> puntos, Point punto);
double calcularAnguloEntreDosPuntos( Point pt1, Point pt2, Point pt0);
static void obtenerCuadradosCercanos(vector<vector<Point> >& cuadrados);
static void estimarAngulosEuler( Mat& image, vector<vector<Point> >& cuadrados,
                                 double f, double cx, double cy,
                                 double& tilt, double& yaw, double& roll);
void corregirPerspectiva(const Mat &input, Mat &output, double tilt, double yaw, double roll,
                         double dx, double dy, double dz, double f, double cx, double cy);
static void drawMarker(Mat& image, Scalar scalar);
static void resetAngles();
bool intersection(Point2f o1, Point2f p1, Point2f o2, Point2f p2, Point2f &r);
string IntToString (int a);
int binarioADecimal(int n);
static double rad2Deg(double rad);
static double deg2Rad(double deg);
double normalizeAngle(double ang);

int BUSQUEDA_TRESH = 15;
int GAUSSIAN_FACTOR = 5;
int MAX_WIDTH, MAX_HEIGHT;
int SEGMENTOS_FRONTERA = 4;
int PUNTOS_SEGMENTO_FRONTERA = 3;
int SEGMENTOS_INTERNOS = 3;
int PUNTOS_SEGMENTO_INTERNOS = 3;
int DIRECCION_ORDEN_C = 1; //RIGHT
int NIVEL_THRESHOLD = 50;
int NUM_MATRICES = 3;
int NUM_PARTICIONES = 16;
int PARTICION_OFFSET = 2;
int SQUARE_AREA = 150;
int INIT_THRESH_DECO = 120;
double TOLERANCIA_LED_ENCENDIDO = 6.0; //(%)
double DISTANCE_RATIO = 0.9;


JNIEXPORT jstring JNICALL
Java_com_app_house_asistenciaestudiante_CameraActivity_decodificar(JNIEnv *env,
                                                                 jobject,
                                                                 jlong imagenRGBA,
                                                                 jlong imagenResultado,
                                                                 jlong objectSize,
                                                                 jlong parameters) {


    Mat & mProcesado = *(Mat*)imagenRGBA;
    Mat mOriginalCopia = mProcesado.clone();
    Mat mOriginalCopiaB = mProcesado.clone();
    Mat & mResultado = *(Mat*)imagenResultado;
    Mat & mObjectSize = *(Mat*)objectSize;
    Mat & mParameters = *(Mat*)parameters;

    string mensajeBinario = "................................................";
    string mensajeDecimal = "";
    int faseDeco =  (int)mParameters.at<double>(0, 5);
    double threshold = mParameters.at<double>(0,7);
    double tilt = 0.0f, yaw = 0.0f, roll = 0.0f;

    MAX_WIDTH = mProcesado.cols;
    MAX_HEIGHT = mProcesado.rows;
    vector<vector<Point> > cuadrados, cuadradosImagenCorregida;
    vector<vector<vector<Point> > > particiones;

    buscarCuadrados(mProcesado, cuadrados, CANNY, BUSQUEDA_TRESH);
    if(cuadrados.size() == NUM_MATRICES){
        dibujarCuadrados(mProcesado, cuadrados, Scalar(255, 0, 0));
        drawMarker(mProcesado, Scalar(255, 0, 0));

        if(alineadoEjeFocal(mProcesado, cuadrados)) {

            dibujarCuadrados(mProcesado, cuadrados, Scalar(0, 255, 0));
            drawMarker(mProcesado, Scalar(0, 255, 0));

            double dist = distanciaEntreDosPuntos(cuadrados[0][0], cuadrados[2][1]);
            double ratiowidth = dist / MAX_WIDTH;

            threshold = INIT_THRESH_DECO * sqrt(ratiowidth);
            threshold = (threshold <= 0) ? 10 : ((threshold >= INIT_THRESH_DECO) ? INIT_THRESH_DECO : threshold);

            Mat mWarpImage;

            if (faseDeco < 4) {
                if(faseDeco == 0) {
                    resetAngles();
                }

                particionarCuadrados(mProcesado, cuadrados, particiones);

                decodificarParticiones(mProcesado, mOriginalCopia, particiones, mensajeBinario, threshold);

                traducirMensaje(mensajeBinario, mensajeDecimal, faseDeco);

                mResultado = mProcesado;

            } else if(faseDeco == 4){
                vector<Point2f> corners;

                Rect boundRect;
                vector<Point> brPoints;
                brPoints.push_back(cuadrados[0][0]);
                brPoints.push_back(cuadrados[2][1]);
                brPoints.push_back(cuadrados[2][2]);
                brPoints.push_back(cuadrados[0][3]);
                boundRect = boundingRect(brPoints);

                mObjectSize.at<double>(0, 0) = distanciaEntreDosPuntos(
                        Point(boundRect.tl().x, boundRect.tl().y + boundRect.height),
                        boundRect.br());
                mObjectSize.at<double>(0, 1) = distanciaEntreDosPuntos(boundRect.tl(),
                                                                       Point(boundRect.tl().x,
                                                                             boundRect.tl().y +
                                                                             boundRect.height));

                estimarAngulosEuler(mProcesado,
                                    cuadrados,
                                    mParameters.at<double>(0, 6),
                                    MAX_WIDTH / 2,
                                    MAX_HEIGHT / 2,
                                    tilt, yaw, roll);

                corregirPerspectiva(mOriginalCopiaB,
                          mWarpImage,
                          tilt, yaw, roll,
                          0, 0, 1.0 - (fabs(yaw) / CV_PI),
                          mParameters.at<double>(0, 6), //focal length in pixels
                          MAX_WIDTH / 2,
                          MAX_HEIGHT / 2);

                Mat croppedImage;
                int X = boundRect.tl().x - 50; X = (X > 0) ? X : 0;
                int Y = boundRect.tl().y - 40; Y = (Y > 0) ? Y : 0;
                int W = boundRect.width + 100; W = (W > 0) ? W : 0;
                int H = boundRect.height + 80; H = (H > 0) ? H : 0;

                try{
                    croppedImage = mWarpImage(Rect(X, Y, W, H));
                } catch(Exception e){
                    croppedImage = mWarpImage;
                }

                buscarCuadrados(croppedImage, cuadradosImagenCorregida, CANNY, BUSQUEDA_TRESH);

                if (cuadradosImagenCorregida.size() == NUM_MATRICES) {

                    Rect boundRect2;
                    vector<Point> brPoints2;
                    brPoints2.push_back(cuadradosImagenCorregida[0][0]);
                    brPoints2.push_back(cuadradosImagenCorregida[2][1]);
                    brPoints2.push_back(cuadradosImagenCorregida[2][2]);
                    brPoints2.push_back(cuadradosImagenCorregida[0][3]);
                    boundRect2 = boundingRect(brPoints2);

                    mObjectSize.at<double>(0, 2) = distanciaEntreDosPuntos(
                            Point(boundRect2.tl().x, boundRect2.tl().y + boundRect2.height),
                            boundRect2.br());

                    mObjectSize.at<double>(0, 3) = distanciaEntreDosPuntos(boundRect2.tl(),
                                                                           Point(boundRect2.tl().x,
                                                                                 boundRect2.tl().y +
                                                                                 boundRect2.height));
                    }
                mResultado = mProcesado;
            }
        }
        else {
            dibujarCuadrados(mProcesado, cuadrados, Scalar(255, 0, 0));
            drawMarker(mProcesado, Scalar(255, 0, 0));

            if(faseDeco == 4) {
                faseDeco = -2;
            } else {
                faseDeco = -1;
            }
            resetAngles();
            mResultado = mProcesado;
        }

    }
    else {
        dibujarCuadrados(mProcesado, cuadrados, Scalar(255, 0, 0));
        drawMarker(mProcesado, Scalar(255, 0, 0));
        mResultado = mProcesado;
    }
    mParameters.at<double>(0,0) = tilt;
    mParameters.at<double>(0,1) = yaw;
    mParameters.at<double>(0,2) = roll;
    mParameters.at<double>(0,5) = faseDeco;
    mParameters.at<double>(0,7) = threshold;

    return env->NewStringUTF(mensajeDecimal.c_str());
}
static void buscarCuadrados(const Mat& image, vector<vector<Point> >& cuadrados, MetodoBusqueda metodo, int thresholdFactor){
    cuadrados.clear();

    Mat gray0(image.size(), CV_8U);
    cvtColor(image, gray0, CV_BGR2GRAY);
    GaussianBlur( gray0, gray0, Size( GAUSSIAN_FACTOR, GAUSSIAN_FACTOR ), 0, 0 );
    Mat gray;
    vector<vector<Point> > contours1;

    for( int l = 0; l < thresholdFactor; l++ ){
        if(l == 0 ){
            if(metodo == LAPLACIAN) {
                Mat dst;
                bitwise_not(gray0, gray0);
                Laplacian(gray0, dst, 3, 3, 1, 0, BORDER_DEFAULT); //3 depth CV_16S
                convertScaleAbs(dst, gray);
            }
            else if (metodo == CANNY) {
                bitwise_not(gray0, gray0);
                Canny(gray0, gray, 0, 50, 3);//10,20,3);// 0 10 5

            }
            dilate(gray, gray, getStructuringElement(MORPH_RECT, Size(3, 3)));
        }
        else
        {
            threshold(gray0, gray, (l+1)*255/BUSQUEDA_TRESH, 255, CV_THRESH_BINARY);
        }
        // Encuentra los contornos (mas exteriores si existen otros dentro) y los enlista
        findContours(gray, contours1, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE, Point(-1, -1)); //Point(-1 ,-1)

        vector<Point> approx;

        for(size_t i = 0; i < contours1.size(); i++)
        {
            approxPolyDP(Mat(contours1[i]), approx, arcLength(Mat(contours1[i]), true)*0.015, true);

            if( approx.size() == 4 && fabs(contourArea(Mat(approx))) > SQUARE_AREA &&
                !filtrarCuadrado(cuadrados, approx) && isContourConvex(Mat(approx)))
            {
                double maxCosine = 0;

                for( int j = 2; j < 5; j++ )
                {
                    double cosine = fabs(calcularAnguloEntreDosPuntos(approx[j%4], approx[j-2], approx[j-1]));
                    maxCosine = MAX(maxCosine, cosine);
                }
                if( maxCosine < 0.35) {
                    cuadrados.push_back(approx);
                }
            }
        }
    }

    ordenarCuadradosPorPosicionEspacial(cuadrados, DIRECCION_ORDEN_C);
    obtenerCuadradosCercanos(cuadrados);
}

static void obtenerCuadradosCercanos(vector<vector<Point> >& cuadrados){

    if(cuadrados.size() <= 3){
        return;
    }

    vector<vector<Point> > cuadradosSeleccionados;

    double angSeleccionado = 0,
            angActual = 0,
            distanciaSeleccionada = 0,
            distanciaActual = 0;
    int indexMejorCuadrado[3] = {0,0,0};
    double mejorAngulo = MAXFLOAT;

        for (int n = 0; n < cuadrados.size() - 2; n++) {

            for (int p = n + 1; p < cuadrados.size() - 1; p++) {

                distanciaSeleccionada = distanciaEntreDosPuntos(cuadrados[n][0], cuadrados[p][0]);

                int dx = cuadrados[p][0].x - cuadrados[n][0].x;
                int dy = cuadrados[p][0].y - cuadrados[n][0].y;

                angSeleccionado = -atan2(dy, dx);

                for (int q = p + 1; q < cuadrados.size(); q++) {

                    distanciaActual = distanciaEntreDosPuntos(cuadrados[p][0], cuadrados[q][0]);
                    double razon = (distanciaSeleccionada >= distanciaActual) ?
                                   (distanciaActual / distanciaSeleccionada) :
                                   (distanciaSeleccionada / distanciaActual);

                    if (fabs(razon) > DISTANCE_RATIO) {

                        dx = cuadrados[q][0].x - cuadrados[p][0].x;
                        dy = cuadrados[q][0].y - cuadrados[p][0].y;

                        angActual = -atan2(dy, dx);

                        if(angSeleccionado == 0.0) angSeleccionado = 0.01;
                        if(angActual == 0.0)       angActual = 0.01;

                        angSeleccionado = normalizeAngle(angSeleccionado);
                        angActual = normalizeAngle(angActual);

                        double angDiff = fabs(angSeleccionado-angActual);

                        if (angDiff < mejorAngulo){
                            mejorAngulo = angDiff;
                            indexMejorCuadrado[0] = n;
                            indexMejorCuadrado[1] = p;
                            indexMejorCuadrado[2] = q;
                       }
                    }
                }
            }
        }
        cuadradosSeleccionados.push_back(cuadrados[indexMejorCuadrado[0]]);
        cuadradosSeleccionados.push_back(cuadrados[indexMejorCuadrado[1]]);
        cuadradosSeleccionados.push_back(cuadrados[indexMejorCuadrado[2]]);
        cuadrados = cuadradosSeleccionados;
}
double normalizeAngle(double ang){

    if(ang == -CV_PI) return CV_PI;
    else if(ang == CV_2PI) return 0;
    else if(ang > CV_PI) return ang - CV_2PI;
    else return ang;
}
static void estimarAngulosEuler( Mat& image, vector<vector<Point> >& cuadrados,
                                 double f, double cx, double cy,
                                 double& tilt, double& yaw, double& roll){
    if(cuadrados.size() <  3) return;

    try {
        Point2f a = cuadrados[0][0];
        Point2f b = cuadrados[2][1];
        Point2f c = cuadrados[2][2];
        Point2f d = cuadrados[0][3];

        Point2f p, q, r, s, vx, vy;

        tilt = 0;
        yaw = 0;
        roll = 0;

        double distance1 = distanciaEntreDosPuntos(a, d);
        double distance2 = distanciaEntreDosPuntos(b, c);
        double distance3 = distanciaEntreDosPuntos(a, b);
        double distance4 = distanciaEntreDosPuntos(d, c);
        double angTilt = 0;
        double angYaw = 0;
        double angRoll = 0;

        p.x = a.x + (a.x - d.x) / distance1 * 50000;
        p.y = a.y + (a.y - d.y) / distance1 * 50000;
        q.x = b.x + (b.x - c.x) / distance2 * 50000;
        q.y = b.y + (b.y - c.y) / distance2 * 50000;
        r.x = a.x + (a.x - b.x) / distance3 * 50000;
        r.y = a.y + (a.y - b.y) / distance3 * 50000;
        s.x = d.x + (d.x - c.x) / distance4 * 50000;
        s.y = d.y + (d.y - c.y) / distance4 * 50000;

        if(intersection(a, p, b, q, vx) && intersection(a, r, d, s, vy)) {

            Mat CI = Mat(3, 3, CV_64FC1), V = Mat(3, 1, CV_64FC1), R1, R2, R3;

            CI.at<double>(0, 0) = 1 / f;
            CI.at<double>(0, 1) = 0;
            CI.at<double>(0, 2) = -cx / f;
            CI.at<double>(1, 0) = 0;
            CI.at<double>(1, 1) = 1 / f;
            CI.at<double>(1, 2) = -cy / f;
            CI.at<double>(2, 0) = 0;
            CI.at<double>(2, 1) = 0;
            CI.at<double>(2, 2) = 1;

            V.at<double>(0, 0) = vx.x;
            V.at<double>(1, 0) = vx.y;
            V.at<double>(2, 0) = 1;

            R1 = CI * V;
            R1 = R1 / norm(R1, NORM_L2);

            V.at<double>(0, 0) = vy.x;
            V.at<double>(1, 0) = vy.y;
            V.at<double>(2, 0) = 1;

            R2 = CI * V;
            R2 = R2 / norm(R2, NORM_L2);
            R3 = R1.cross(R2);

            double ratio = 1;

            angTilt = -asin(R3.at<double>(1,0));
            angYaw = -atan(R3.at<double>(0,0) / R3.at<double>(2,0));
            angRoll = CV_PI - atan2(cuadrados[0][3].y - cuadrados[2][2].y, cuadrados[0][3].x - cuadrados[2][2].x);

                if(angTilt > 0 && accumTilt > 0){
                    if(angTilt >= accumTilt)
                        ratio = accumTilt / angTilt;
                    else
                        ratio = angTilt / accumTilt;
                }
                else if(angTilt < 0 && accumTilt < 0){
                    if(angTilt >= accumTilt)
                        ratio = angTilt / accumTilt;
                    else
                        ratio = accumTilt / angTilt;
                }
                else if(angTilt > 0 && accumTilt < 0){
                    ratio = fabs(accumTilt / angTilt);
                }
                else if(angTilt < 0 && accumTilt > 0){
                    ratio = fabs(angTilt / accumTilt);
                }

            if (ratio >= 0.2)
                vTilt.push_back(angTilt);


            if(angYaw > 0 && accumYaw > 0){
                if(angYaw >= accumYaw)
                    ratio = accumYaw / angYaw;
                else
                    ratio = angYaw / accumYaw;
            }
            else if(angYaw < 0 && accumYaw < 0){
                if(angYaw >= accumYaw)
                    ratio = angYaw / accumYaw;
                else
                    ratio = accumYaw / angYaw;
            }
            else if(angYaw > 0 && accumYaw < 0){
                ratio = fabs(accumYaw / angYaw);
            }
            else if(angYaw < 0 && accumYaw > 0){
                ratio = fabs(angYaw / accumYaw);
            }

            if (ratio >= 0.2)
                vYaw.push_back(angYaw);

        }

        accumTilt = accumulate(vTilt.begin(), vTilt.end(), 0.0f);
        tilt = (vTilt.size() == 0) ? 1 : (accumTilt / vTilt.size());

        accumYaw = accumulate(vYaw.begin(), vYaw.end(), 0.0f);
        yaw = (vYaw.size() == 0) ? 1 : (accumYaw / vYaw.size());

        roll = angRoll;
    }
    catch (Exception e){
    }
}

static void resetAngles(){
    vTilt.clear();
    vYaw.clear();
    vRoll.clear();
}
bool intersection(Point2f o1, Point2f p1, Point2f o2, Point2f p2,
                  Point2f &r)
{
    Point2f x = o2 - o1;
    Point2f d1 = p1 - o1;
    Point2f d2 = p2 - o2;

    float cross = d1.x*d2.y - d1.y*d2.x;
    if (abs(cross) < 1e-8)
        return false;

    double t1 = (x.x * d2.y - x.y * d2.x)/cross;
    r = o1 + d1 * t1;
    return true;
}
static void dibujarCuadrados( Mat& image, const vector<vector<Point> >& cuadrados , Scalar scalar) {
    if(cuadrados.size() <= 0) return;
    for (int i = 0; i < cuadrados.size(); i++) {

        const Point *p = &cuadrados[i][0];
        int n = (int) cuadrados[i].size();

        putText(image, IntToString(i + 1).c_str(), Point(cuadrados[i][0].x, cuadrados[i][0].y-10),
                FONT_HERSHEY_SIMPLEX, 0.75, Scalar(255, 0, 0), 1, LINE_AA, false);
        polylines(image, &p, &n, 1, true, scalar, 1, 16);
    }
}
static void particionarCuadrados( Mat& image, const vector<vector<Point> >& cuadrados , vector<vector<vector<Point> > >& particiones) {

    if(cuadrados.size() <= 0) return;

    vector<vector<vector<Point> > > puntosFrontera;
    vector<vector<vector<Point> > > puntosInternos;
    vector<vector<Point> > puntosVertices;

    vector<vector<Point> > puntosInternosPorCuadrado;
    vector<vector<Point> > puntosFronteraPorCuadrado;
    vector<Point> puntosVerticePorCuadrado;

    vector<Point> puntosFronteraPorSegmento;
    vector<Point> puntosInternosPorSegmento;

    particiones.clear();

    for (int i = 0; i < cuadrados.size(); i++) {
        //Buscando los puntos de las fronteras
        for (int n = 0; n < SEGMENTOS_FRONTERA; n++) {

            const Point puntoInicial = cuadrados[i][n];
            const Point puntoFinal = cuadrados[i][(n + 1) % SEGMENTOS_FRONTERA];

            LineIterator it(image, puntoInicial, puntoFinal, 8, false);

            int razon = (int) round(it.count / (PUNTOS_SEGMENTO_FRONTERA+1));

            puntosVerticePorCuadrado.push_back(puntoInicial);

            //Iterando cada segmento del cuadrado para hallar puntos medios y divirlos
            // en cuatro partes iguales
            for (int k = 0; k < it.count; k++, ++it) {

                if (k == 0 || k == it.count-1) { ; }
                else {
                    if ((k % razon) == 0 && puntosFronteraPorSegmento.size() < PUNTOS_SEGMENTO_FRONTERA) {
                        puntosFronteraPorSegmento.push_back(it.pos());

                        }
                    }
                }

            puntosFronteraPorCuadrado.push_back(puntosFronteraPorSegmento);
            puntosFronteraPorSegmento.clear();
        }

        puntosVertices.push_back(puntosVerticePorCuadrado);
        puntosVerticePorCuadrado.clear();

        puntosFrontera.push_back(puntosFronteraPorCuadrado);
        puntosFronteraPorCuadrado.clear();

        //Buscando los puntos internos del cuadrado
        for (int r = 0; r < SEGMENTOS_INTERNOS; r++) {
            //Punto del segmento A del cuadrado

            Point puntoInicial = puntosFrontera[i][3][SEGMENTOS_INTERNOS - r - 1];

            //Punto del segmento B del cuadrado opuesto a A
            Point puntoFinal = puntosFrontera[i][1][r];

            //Iterador que recorre el segmento de linea punto a punto
            LineIterator it(image, puntoInicial, puntoFinal, 8, true);
            int razon = (int) round(it.count / (PUNTOS_SEGMENTO_INTERNOS+1)); //+ 1

            //Recoriendo el segmento punto a punto
            for (int k = 0; k < it.count; k++, ++it) {

                if (k == 0 || k == it.count-1) { ; }
                else {
                    if ((k % razon) == 0 && puntosInternosPorSegmento.size() < PUNTOS_SEGMENTO_INTERNOS) {
                        puntosInternosPorSegmento.push_back(it.pos());
                    }
                }
            }
            puntosInternosPorCuadrado.push_back(puntosInternosPorSegmento);
            puntosInternosPorSegmento.clear();
        }
        puntosInternos.push_back(puntosInternosPorCuadrado);
        puntosInternosPorCuadrado.clear();
}

    unificarParticiones(cuadrados, puntosVertices, puntosFrontera, puntosInternos, particiones);

    //Dibujar particiones
    /*if (particiones.size() > 0) {
        for (int x = 0; x < cuadrados.size(); x++) {
            for (int r = 0; r < particiones[x].size(); r++) {
                const Point *p = &particiones[x][r][0];
                int n = (int) particiones[x][r].size();
                //__android_log_print(ANDROID_LOG_ERROR, "unificarParticiones1 ---> ", "%i, (%i %i)",i, r, n);
                polylines(image, &p, &n, 1, true, Scalar(255 * r / particiones[x].size(), 0, 200),
                          1, 16);
            }
        }
    }*/
    puntosVertices.clear();
    puntosFrontera.clear();
    puntosInternos.clear();

}
static void unificarParticiones(const vector<vector<Point> >& cuadrados,
                                const vector<vector<Point> > & puntosVertices,
                                const vector<vector<vector<Point> > >& puntosFrontera,
                                const vector<vector<vector<Point> > > & puntosInternos,
                                vector<vector<vector<Point> > > & particiones){
    if(cuadrados.size() <= 0) return;
    vector<vector<Point> > particionesPorCuadrado;
    vector<Point> particion;

    for(int indiceCuadrado = 0; indiceCuadrado < cuadrados.size(); indiceCuadrado++){
        //Particion 1
        particion.push_back(puntosVertices[indiceCuadrado][0]);
        particion.push_back(puntosFrontera[indiceCuadrado][0][0]);
        particion.push_back(puntosInternos[indiceCuadrado][0][0]);
        particion.push_back(puntosFrontera[indiceCuadrado][3][2]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();

        //Particion 2
        particion.push_back(puntosFrontera[indiceCuadrado][0][0]);
        particion.push_back(puntosFrontera[indiceCuadrado][0][1]);
        particion.push_back(puntosInternos[indiceCuadrado][0][1]);
        particion.push_back(puntosInternos[indiceCuadrado][0][0]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //Particion 3
        particion.push_back(puntosFrontera[indiceCuadrado][0][1]);
        particion.push_back(puntosFrontera[indiceCuadrado][0][2]);
        particion.push_back(puntosInternos[indiceCuadrado][0][2]);
        particion.push_back(puntosInternos[indiceCuadrado][0][1]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //Particion 4
        particion.push_back(puntosFrontera[indiceCuadrado][0][2]);
        particion.push_back(puntosVertices[indiceCuadrado][1]);
        particion.push_back(puntosFrontera[indiceCuadrado][1][0]);
        particion.push_back(puntosInternos[indiceCuadrado][0][2]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //Particion 5
        particion.push_back(puntosFrontera[indiceCuadrado][3][2]);
        particion.push_back(puntosInternos[indiceCuadrado][0][0]);
        particion.push_back(puntosInternos[indiceCuadrado][1][0]);
        particion.push_back(puntosFrontera[indiceCuadrado][3][1]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //Particion 6
        particion.push_back(puntosInternos[indiceCuadrado][0][0]);
        particion.push_back(puntosInternos[indiceCuadrado][0][1]);
        particion.push_back(puntosInternos[indiceCuadrado][1][1]);
        particion.push_back(puntosInternos[indiceCuadrado][1][0]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //Particion 7
        particion.push_back(puntosInternos[indiceCuadrado][0][1]);
        particion.push_back(puntosInternos[indiceCuadrado][0][2]);
        particion.push_back(puntosInternos[indiceCuadrado][1][2]);
        particion.push_back(puntosInternos[indiceCuadrado][1][1]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //Particion 8
        particion.push_back(puntosInternos[indiceCuadrado][0][2]);
        particion.push_back(puntosFrontera[indiceCuadrado][1][0]);
        particion.push_back(puntosFrontera[indiceCuadrado][1][1]);
        particion.push_back(puntosInternos[indiceCuadrado][1][2]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //Particion 9
        particion.push_back(puntosFrontera[indiceCuadrado][3][1]);
        particion.push_back(puntosInternos[indiceCuadrado][1][0]);
        particion.push_back(puntosInternos[indiceCuadrado][2][0]);
        particion.push_back(puntosFrontera[indiceCuadrado][3][0]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //Particion 10
        particion.push_back(puntosInternos[indiceCuadrado][1][0]);
        particion.push_back(puntosInternos[indiceCuadrado][1][1]);
        particion.push_back(puntosInternos[indiceCuadrado][2][1]);
        particion.push_back(puntosInternos[indiceCuadrado][2][0]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //Particion 11
        particion.push_back(puntosInternos[indiceCuadrado][1][1]);
        particion.push_back(puntosInternos[indiceCuadrado][1][2]);
        particion.push_back(puntosInternos[indiceCuadrado][2][2]);
        particion.push_back(puntosInternos[indiceCuadrado][2][1]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //Particion 12
        particion.push_back(puntosInternos[indiceCuadrado][1][2]);
        particion.push_back(puntosFrontera[indiceCuadrado][1][1]);
        particion.push_back(puntosFrontera[indiceCuadrado][1][2]);
        particion.push_back(puntosInternos[indiceCuadrado][2][2]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //Particion 13
        particion.push_back(puntosFrontera[indiceCuadrado][3][0]);
        particion.push_back(puntosInternos[indiceCuadrado][2][0]);
        particion.push_back(puntosFrontera[indiceCuadrado][2][2]);
        particion.push_back(puntosVertices[indiceCuadrado][3]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //Particion 14
        particion.push_back(puntosInternos[indiceCuadrado][2][0]);
        particion.push_back(puntosInternos[indiceCuadrado][2][1]);
        particion.push_back(puntosFrontera[indiceCuadrado][2][1]);
        particion.push_back(puntosFrontera[indiceCuadrado][2][2]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //Particion 15
        particion.push_back(puntosInternos[indiceCuadrado][2][1]);
        particion.push_back(puntosInternos[indiceCuadrado][2][2]);
        particion.push_back(puntosFrontera[indiceCuadrado][2][0]);
        particion.push_back(puntosFrontera[indiceCuadrado][2][1]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //Particion 16
        particion.push_back(puntosInternos[indiceCuadrado][2][2]);
        particion.push_back(puntosFrontera[indiceCuadrado][1][2]);
        particion.push_back(puntosVertices[indiceCuadrado][2]);
        particion.push_back(puntosFrontera[indiceCuadrado][2][0]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();

        particiones.push_back(particionesPorCuadrado);
        particionesPorCuadrado.clear();
    }


}
static void preprocesar( Mat& image, Mat& image_prep, double thresh){
    Mat tmp;
    cvtColor(image, image_prep, CV_BGR2GRAY);
    GaussianBlur( image_prep, image_prep, Size( GAUSSIAN_FACTOR, GAUSSIAN_FACTOR ), 0, 0 );
    threshold(image_prep, image_prep, thresh , 255, CV_THRESH_BINARY);//CV_TRESH_OTSU
}
static void decodificarParticiones( Mat& image,
                                    Mat& image_c,
                                    vector<vector<vector<Point> > >& particiones,
                                    string& mensajeBinario,
                                    double thresh){

    if(particiones.size() <= 0 ) return;
    if(particiones[0].size() <= 0 ) return;
    float porcentajeBlanco = 0;
    vector<Point> puntosBlancos;

    preprocesar(image_c, image_c, thresh);

    for(int i = 0; i < NUM_MATRICES; i++) {
        for (int k = 0; k < NUM_PARTICIONES; k++) {

            int X = particiones[i][k][0].x + PARTICION_OFFSET,
                Y = particiones[i][k][0].y + PARTICION_OFFSET,
                W = particiones[i][k][1].x - X - PARTICION_OFFSET,
                H = particiones[i][k][3].y - Y - PARTICION_OFFSET;
            //__android_log_print(ANDROID_LOG_ERROR, "decodificarParticiones", "(%i, %i) %i %i %i %i",i,k, X,Y,W,H);

            Mat mParticion;
            if(X>PARTICION_OFFSET && Y>PARTICION_OFFSET && W>PARTICION_OFFSET && H>PARTICION_OFFSET) {
                try{
                    mParticion = image_c(Rect(X, Y, W, H));
                }catch(Exception e){}

                float tamanio = W * H;

                if (tamanio > 36.0) {
                    PARTICION_OFFSET = 2;
                    dilate(mParticion, mParticion, getStructuringElement(MORPH_RECT, Size(3, 3)));
                }
                else PARTICION_OFFSET = 1;

                ////Busca pixeles blancos en imagen binarizada
                findNonZero(mParticion, puntosBlancos);

                //Calcula el procentaje de pixeles blancos contra el total de pixeles en la region
                porcentajeBlanco = float((puntosBlancos.size()) * 100.0) / tamanio;

                if (porcentajeBlanco >= TOLERANCIA_LED_ENCENDIDO) {
                    mensajeBinario[(i % NUM_MATRICES) * NUM_PARTICIONES + k] = '1';
                } else {
                    mensajeBinario[(i % NUM_MATRICES) * NUM_PARTICIONES + k] = '0';
                }
            }
        }
    }

    /*putText(image, string(mensajeBinario).substr(0,4).c_str(), Point(10,MAX_HEIGHT-135), FONT_HERSHEY_DUPLEX, 0.75,Scalar(255,0,0), 2, LINE_4, false);
    putText(image, string(mensajeBinario).substr(4,4).c_str(), Point(10,MAX_HEIGHT-105), FONT_HERSHEY_DUPLEX, 0.75,Scalar(255,0,0), 2, LINE_4, false);
    putText(image, string(mensajeBinario).substr(8,4).c_str(), Point(10,MAX_HEIGHT-75), FONT_HERSHEY_DUPLEX, 0.75,Scalar(255,0,0), 2, LINE_4, false);
    putText(image, string(mensajeBinario).substr(12,4).c_str(), Point(10,MAX_HEIGHT-45), FONT_HERSHEY_DUPLEX, 0.75,Scalar(255,0,0), 2, LINE_4, false);

    putText(image, string(mensajeBinario).substr(16,4).c_str(), Point(120,MAX_HEIGHT-135), FONT_HERSHEY_DUPLEX, 0.75,Scalar(255,0,0), 2, LINE_4, false);
    putText(image, string(mensajeBinario).substr(20,4).c_str(), Point(120,MAX_HEIGHT-105), FONT_HERSHEY_DUPLEX, 0.75,Scalar(255,0,0), 2, LINE_4, false);
    putText(image, string(mensajeBinario).substr(24,4).c_str(), Point(120,MAX_HEIGHT-75), FONT_HERSHEY_DUPLEX, 0.75,Scalar(255,0,0), 2, LINE_4, false);
    putText(image, string(mensajeBinario).substr(28,4).c_str(), Point(120,MAX_HEIGHT-45), FONT_HERSHEY_DUPLEX, 0.75,Scalar(255,0,0), 2, LINE_4, false);

    putText(image, string(mensajeBinario).substr(32,4).c_str(), Point(240,MAX_HEIGHT-135), FONT_HERSHEY_DUPLEX, 0.75,Scalar(255,0,0), 2, LINE_4, false);
    putText(image, string(mensajeBinario).substr(36,4).c_str(), Point(240,MAX_HEIGHT-105), FONT_HERSHEY_DUPLEX, 0.75,Scalar(255,0,0), 2, LINE_4, false);
    putText(image, string(mensajeBinario).substr(40,4).c_str(), Point(240,MAX_HEIGHT-75), FONT_HERSHEY_DUPLEX, 0.75,Scalar(255,0,0), 2, LINE_4, false);
    putText(image, string(mensajeBinario).substr(44,4).c_str(), Point(240,MAX_HEIGHT-45), FONT_HERSHEY_DUPLEX, 0.75,Scalar(255,0,0), 2, LINE_4, false);
*/
}
static void traducirMensaje(string& mensajeBinario, string& mensaje, int& fase){

    if(mensajeBinario == "................................................" ||
       mensajeBinario == "000000000000000000000000000000000000000000000000"){
        mensaje = "000000000000";
        return;
    }
    mensaje = "";

    string msg_seg = "";
    string msg_matriz_1 = "";
    string msg_matriz_2 = "";
    string msg_matriz_3 = "";
    int msg_seg1_bin = -1;
    string msg_dec = "";
    int bits_substr = 4;
    int seg_x_matrix = 4;



    if(mensajeBinario == "111111111111111111111111111111111111111111111111") {
        msg_matriz_1 = "9999";
        msg_matriz_2 = "9999";
        msg_matriz_3 = "9999";
        fase = 0;
    }

    //RECONOCER PATRON INICIO(TODOS LOS LEDS ENCENDIDOS)
    if(fase == 0){
        if(mensajeBinario == "111111111111111111111111111111111111111111111111") {
            //msg_matriz_1 = msg_matriz_2 = msg_matriz_3 = "9999";

            msg_matriz_1 = "9999";
            msg_matriz_2 = "9999";
            msg_matriz_3 = "9999";
            //__android_log_print(ANDROID_LOG_ERROR, "mensaje", "Fase 1");
        }
    }
    // RECONOCER PATRON de LECTURA DE IZQUIERDA A DERECHA y VICEVERSA
    else if(fase == 1 || fase == 2 || fase == 3){

        //__android_log_print(ANDROID_LOG_ERROR, "traduciendo fase", "%i", fase);

        for(int m = 0; m < NUM_MATRICES; m++) {

            for (int r = 0; r < seg_x_matrix; r++) {

                msg_seg = mensajeBinario.substr(m * seg_x_matrix * seg_x_matrix + r * seg_x_matrix, bits_substr);

                msg_seg1_bin = atoi(msg_seg.c_str());

                int dec = binarioADecimal(msg_seg1_bin);

                if(dec > 9){ dec=9; } //return

                if(fase == 1) {
                    if (m == 0)
                        msg_matriz_1 += IntToString(dec);
                    else if (m == 1)
                        msg_matriz_2 += IntToString(dec);
                    else if (m == 2)
                        msg_matriz_3 += IntToString(dec);
                }
                else if(fase == 2) {
                    if (m == 0)
                        msg_matriz_3 += IntToString(dec);
                    else if (m == 1)
                        msg_matriz_1 += IntToString(dec);
                    else if (m == 2)
                        msg_matriz_2 += IntToString(dec);
                }
                else if(fase == 3) {
                    if (m == 0)
                        msg_matriz_2 += IntToString(dec);
                    else if (m == 1)
                        msg_matriz_3 += IntToString(dec);
                    else if (m == 2)
                        msg_matriz_1 += IntToString(dec);
                }
                //__android_log_print(ANDROID_LOG_ERROR, "mensaje", "Fase %i, m = %i, msg1 = %s, msg2 = %s, msg3 = %s", fase, m, msg_matriz_1.c_str(), msg_matriz_2.c_str(), msg_matriz_3.c_str());
            }
        }
    }
    mensaje = msg_matriz_1  + msg_matriz_2  + msg_matriz_3;
    //__android_log_print(ANDROID_LOG_ERROR, "Decodificacion mensaje Fase ", "%d: %s", fase, mensaje.c_str());
}
static void ordenarCuadradosPorPosicionEspacial(vector<vector<Point> >& cuadrados, int direccion){
    if(cuadrados.size() <= 1) return;

    vector<vector<Point> >cuadradosOrdenados;
    vector<Point> puntosCentralesCuadrados;
    int coordenadaXReferencia;

    if(direccion == 1){
        coordenadaXReferencia = 0;
    }
    else{
        coordenadaXReferencia = MAX_WIDTH;
    }

    //Mientras existan cuadrados ordenarlos según posicion en la imagen
    // De derecha a izquierda (direccion = 1),de izquierda a derecha (direccion = -1)
    while(cuadrados.size() > 0) {

        int posicionMasCercana = 0;

        if (cuadrados.size() > 1) {
            for (int i = 0; i < cuadrados.size(); i++) {
                puntosCentralesCuadrados.push_back(cuadrados[i][0]);
            }

            posicionMasCercana = buscarPuntoMasCercano(puntosCentralesCuadrados,
                                                       Point(coordenadaXReferencia,
                                                             MAX_HEIGHT / 2));

        }
        cuadradosOrdenados.push_back(cuadrados[posicionMasCercana]);
        cuadrados[posicionMasCercana] = cuadrados.back();
        cuadrados.pop_back();
        puntosCentralesCuadrados.clear();
   }

    if(cuadradosOrdenados.size() > 0) {
        cuadrados = cuadradosOrdenados;
        cuadradosOrdenados.clear();
    }
}

double calcularAnguloEntreDosPuntos( Point pt1, Point pt2, Point pt0){
    double dx1 = pt1.x - pt0.x;
    double dy1 = pt1.y - pt0.y;
    double dx2 = pt2.x - pt0.x;
    double dy2 = pt2.y - pt0.y;
    return (dx1*dx2 + dy1*dy2)/sqrt((dx1*dx1 + dy1*dy1)*(dx2*dx2 + dy2*dy2) + 1e-10);
}

bool filtrarCuadrado(const vector<vector<Point> >& cuadrados, vector<Point>& approx){
    if(approx.size() <= 0) return true;

    int TOLERANCIA = 5;

    ordenarVerticesCuadrado(approx);

    const Point* q0 = &approx[0];
    const Point* q1 = &approx[1];
    const Point* q2 = &approx[2];
    const Point* q3 = &approx[3];



    if( q0->x < TOLERANCIA || q0->y < TOLERANCIA ||
        q1->x < TOLERANCIA || q1->y < TOLERANCIA ||
        q2->x < TOLERANCIA || q2->y < TOLERANCIA ||
        q3->x < TOLERANCIA || q3->y < TOLERANCIA ||
        q0->x > MAX_WIDTH-TOLERANCIA || q0->y < TOLERANCIA ||
        q1->x > MAX_WIDTH-TOLERANCIA || q1->y < TOLERANCIA ||
        q2->x > MAX_WIDTH-TOLERANCIA || q2->y < TOLERANCIA ||
        q3->x > MAX_WIDTH-TOLERANCIA || q3->y < TOLERANCIA ||
        q0->x > MAX_WIDTH-TOLERANCIA || q0->y > MAX_HEIGHT-TOLERANCIA ||
        q1->x > MAX_WIDTH-TOLERANCIA || q1->y > MAX_HEIGHT-TOLERANCIA ||
        q2->x > MAX_WIDTH-TOLERANCIA || q2->y > MAX_HEIGHT-TOLERANCIA ||
        q3->x > MAX_WIDTH-TOLERANCIA || q3->y > MAX_HEIGHT-TOLERANCIA ||
        q0->x < TOLERANCIA || q0->y > MAX_HEIGHT-TOLERANCIA ||
        q1->x < TOLERANCIA || q1->y > MAX_HEIGHT-TOLERANCIA ||
        q2->x < TOLERANCIA || q2->y > MAX_HEIGHT-TOLERANCIA ||
        q3->x < TOLERANCIA || q3->y > MAX_HEIGHT-TOLERANCIA)
    {
        return true;

    }

    //Discriminando cuadrados formados por el borde de la imagen
    if((q0->x < TOLERANCIA && q0->y < TOLERANCIA) ||
       (q1->x > MAX_WIDTH-TOLERANCIA && q1->y < TOLERANCIA) ||
       (q2->x > MAX_WIDTH - TOLERANCIA && q2->y > MAX_HEIGHT-TOLERANCIA) ||
       (q3->x < TOLERANCIA && q3->y > MAX_HEIGHT-TOLERANCIA)) {

        return true;
    }

    //Comparando con cuadrados ya existentes
    for( int i = 0; i < cuadrados.size(); i++ )
    {
        //Todos los vertices del cuadrado
        const Point* p0 = &cuadrados[i][0];
        const Point* p1 = &cuadrados[i][1];
        const Point* p2 = &cuadrados[i][2];
        const Point* p3 = &cuadrados[i][3];

        int d0 = (int)sqrt((p0->x - q0->x)*(p0->x - q0->x) + (p0->y - q0->y)*(p0->y - q0->y));//norm(p0-q0);
        int d1 = (int)sqrt((p1->x - q1->x)*(p1->x - q1->x) + (p1->y - q1->y)*(p1->y - q1->y));//norm(p1-q1);
        int d2 = (int)sqrt((p2->x - q2->x)*(p2->x - q2->x) + (p2->y - q2->y)*(p2->y - q2->y));//norm(p2-q2);
        int d3 = (int)sqrt((p3->x - q3->x)*(p3->x - q3->x) + (p3->y - q3->y)*(p3->y - q3->y));//norm(p3-q3);

        if((d0 <= TOLERANCIA && d1 <= TOLERANCIA && d2 <= TOLERANCIA && d3 <= TOLERANCIA) ||
                d0 <= TOLERANCIA || d1 <= TOLERANCIA || d2 <= TOLERANCIA || d3 <= TOLERANCIA){
            return true;
        }
    }
    return false;
}
static void ordenarVerticesCuadrado(vector<Point> &verticesCuadrado){
    int direccion = -1;

    if(buscarPuntoMasCercano(verticesCuadrado, Point(0,0)) == 0){
        direccion = 0;
    }
    else if(buscarPuntoMasCercano(verticesCuadrado, Point(MAX_WIDTH,0)) == 0){
        direccion = 1;
    }

    if(direccion == 0){
        Point pTemporal = Point(verticesCuadrado[3].x, verticesCuadrado[3].y);
        verticesCuadrado[3] = Point(verticesCuadrado[1].x, verticesCuadrado[1].y);
        verticesCuadrado[1] = pTemporal;
    }
    else /*if(direccion == 1)*/
    {
        //int posicionMasCercana = buscarPuntoMasCercano(verticesCuadrado, Point(0,0));
        Point p0 = Point(verticesCuadrado[0].x, verticesCuadrado[0].y);
        Point p1 = Point(verticesCuadrado[1].x, verticesCuadrado[1].y);
        Point p2 = Point(verticesCuadrado[2].x, verticesCuadrado[2].y);
        Point p3 = Point(verticesCuadrado[3].x, verticesCuadrado[3].y);


        verticesCuadrado[0] = p1;
        verticesCuadrado[1] = p0;
        verticesCuadrado[2] = p3;
        verticesCuadrado[3] = p2;
    }

}
int buscarPuntoMasCercano(vector<Point> puntos, Point punto){
    double distancia = MAXFLOAT, distanciaTemporal = 0;
    int posicionMasCercana = 0;

    //Encontrar punto mas cercano al origen Punto(0,0) que sera el punto de orden inicial
    for(int pos = 0; pos < puntos.size(); pos++ ){
        distanciaTemporal = distanciaEntreDosPuntos(punto, puntos[pos]);
        if(distanciaTemporal < distancia){
            distancia = distanciaTemporal;
            posicionMasCercana = pos;
        }
    }
    return posicionMasCercana;
}
double distanciaEntreDosPuntos(Point p1, Point p2){
    return sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
}

string IntToString (int a)
{
    ostringstream tmp;
    tmp << a;
    return tmp.str();
}

int binarioADecimal(int n)
{
    int factor = 1;
    int total = 0;

    while (n != 0)
    {
        total += (n%10) * factor;
        n /= 10;
        factor *= 2;
    }

    return total;
}

bool alineadoEjeFocal(Mat& image, vector<vector<Point> >& cuadrados) {
    if (cuadrados.size() == 1) {
        if(pointPolygonTest( cuadrados[0], Point2f(MAX_WIDTH/2, MAX_HEIGHT/2), false ) > 0){
            return true;
        }
    }
    else if(cuadrados.size() == 3) // MEJORAR ESTE ALGORITMO
    {
        if(pointPolygonTest( cuadrados[1], Point2f(MAX_WIDTH/2, MAX_HEIGHT/2), false ) > 0){
            return true;
        }
    }
    return false;
}

static double rad2Deg(double rad){return rad*(180/M_PI);}//Convert radians to degrees
static double deg2Rad(double deg){return deg*(M_PI/180);}//Convert degrees to radians

void corregirPerspectiva(const Mat &input, Mat &output, double tilt, double yaw, double roll,
                 double dx, double dy, double dz, double f, double cx, double cy){
    Mat Rx = Mat(4, 4, CV_64FC1);
    Mat Ry = Mat(4, 4, CV_64FC1);
    Mat Rz = Mat(4, 4, CV_64FC1);
    Mat T  = Mat(4, 4, CV_64FC1);
    Mat C  = Mat(3, 4, CV_64FC1);
    Mat CI = Mat(4, 3, CV_64FC1);

    //tilt = deg2Rad(tilt);
    //yaw = deg2Rad(yaw);
    //roll = deg2Rad(roll);

    // Camera Calibration Intrinsics Matrix
    C.at<double>(0,0) = f; C.at<double>(0,1) = 0; C.at<double>(0,2) = cx; C.at<double>(0,3) = 0;
    C.at<double>(1,0) = 0; C.at<double>(1,1) = f; C.at<double>(1,2) = cy; C.at<double>(1,3) = 0;
    C.at<double>(2,0) = 0; C.at<double>(2,1) = 0; C.at<double>(2,2) = 1;  C.at<double>(2,3) = 0; //??0

    // Inverted Camera Calibration Intrinsics Matrix
    CI.at<double>(0,0) = 1/f; CI.at<double>(0,1) = 0;   CI.at<double>(0,2) = -cx/f;
    CI.at<double>(1,0) = 0;   CI.at<double>(1,1) = 1/f; CI.at<double>(1,2) = -cy/f;
    CI.at<double>(2,0) = 0;   CI.at<double>(2,1) = 0;   CI.at<double>(2,2) = 0;
    CI.at<double>(3,0) = 0;   CI.at<double>(3,1) = 0;   CI.at<double>(3,2) = 1;//??0

    Rx.at<double>(0,0) = 1; Rx.at<double>(0,1) = 0;         Rx.at<double>(0,2) = 0;          Rx.at<double>(0,3) = 0;
    Rx.at<double>(1,0) = 0; Rx.at<double>(1,1) = cos(tilt); Rx.at<double>(1,2) = -sin(tilt); Rx.at<double>(1,3) = 0;
    Rx.at<double>(2,0) = 0; Rx.at<double>(2,1) = sin(tilt); Rx.at<double>(2,2) = cos(tilt);  Rx.at<double>(2,3) = 0;
    Rx.at<double>(3,0) = 0; Rx.at<double>(3,1) = 0;         Rx.at<double>(3,2) = 0;          Rx.at<double>(3,3) = 1;

    Ry.at<double>(0,0) = cos(yaw);  Ry.at<double>(0,1) = 0; Ry.at<double>(0,2) = sin(yaw); Ry.at<double>(0,3) = 0;
    Ry.at<double>(1,0) = 0;         Ry.at<double>(1,1) = 1; Ry.at<double>(1,2) = 0;        Ry.at<double>(1,3) = 0;
    Ry.at<double>(2,0) = -sin(yaw); Ry.at<double>(2,1) = 0; Ry.at<double>(2,2) = cos(yaw); Ry.at<double>(2,3) = 0;
    Ry.at<double>(3,0) = 0;         Ry.at<double>(3,1) = 0; Ry.at<double>(3,2) = 0;        Ry.at<double>(3,3) = 1;

    Rz.at<double>(0,0) = cos(roll); Rz.at<double>(0,1) = -sin(roll); Rz.at<double>(0,2) = 0; Rz.at<double>(0,3) = 0;
    Rz.at<double>(1,0) = sin(roll); Rz.at<double>(1,1) = cos(roll);  Rz.at<double>(1,2) = 0; Rz.at<double>(1,3) = 0;
    Rz.at<double>(2,0) = 0;         Rz.at<double>(2,1) = 0;          Rz.at<double>(2,2) = 1; Rz.at<double>(2,3) = 0;
    Rz.at<double>(3,0) = 0;         Rz.at<double>(3,1) = 0;          Rz.at<double>(3,2) = 0; Rz.at<double>(3,3) = 1;

    T.at<double>(0,0) = 1; T.at<double>(0,1) = 0; T.at<double>(0,2) = 0; T.at<double>(0,3) = dx;
    T.at<double>(1,0) = 0; T.at<double>(1,1) = 1; T.at<double>(1,2) = 0; T.at<double>(1,3) = dy;
    T.at<double>(2,0) = 0; T.at<double>(2,1) = 0; T.at<double>(2,2) = 1; T.at<double>(2,3) = dz;
    T.at<double>(3,0) = 0; T.at<double>(3,1) = 0; T.at<double>(3,2) = 0; T.at<double>(3,3) = 1;

    Mat R = Rz * Ry * Rx;

    Mat H = C * (T * (R * CI));

    warpPerspective(input, output, H, input.size(), INTER_LANCZOS4);
}

static void drawMarker(Mat& image, Scalar scalar){
    drawMarker(image, Point( MAX_WIDTH/2, MAX_HEIGHT/2), scalar, MARKER_CROSS, 20, 2);
}

}



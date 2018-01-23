#include <jni.h>
#include <string>
#include <android/log.h>
#include <opencv2/opencv.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/calib3d/calib3d.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <iostream>

using namespace std;
using namespace cv;

extern "C"
{
enum MetodoBusqueda {LAPLACIAN, CONVEXHULL, CANNY};
static void preprocesar(Mat& imagen_orig, Mat& imagen_prep);
static void buscarCuadrados( const Mat& image, vector<vector<Point> >& cuadrados, MetodoBusqueda metodo, int thresholdLevel);
static void dibujarCuadrados( Mat& image, const vector<vector<Point> >& cuadrados);
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
                                    string& mensajeBinario);
static void traducirMensaje(string& mensajeBinario, string& mensaje, int& fase);
bool filtrarCuadrado(const vector<vector<Point> >& cuadrados, vector<Point>& approx);
float distanciaEntreDosPuntos(Point p1, Point p2);
int buscarPuntoMasCercano(vector<Point> puntos, Point punto);
double calcularAnguloEntreDosPuntos( Point pt1, Point pt2, Point pt0);
float getWidthObjectImage(vector<vector<Point> >& cuadrados);
float getHeightObjectImage(vector<vector<Point> >& cuadrados);
string IntToString (int a);
int binarioADecimal(int n);
static double rad2Deg(double rad);
Vec3f rotationMatrixToEulerAngles(Mat &R);
bool isRotationMatrix(Mat &R);
float angle(Point A, Point B);
static double deg2Rad(double deg);
float cal_angle (  float current_x , float current_y , float tar_x , float tar_y );
void cameraPoseFromHomography(const Mat& H, Mat& pose);
void warpImage(const Mat &src,
               double    theta,
               double    phi,
               double    gamma,
               double    scale,
               double    fovy,
               Mat&      dst,
               Mat&      M,
               vector<Point2f> &corners);
void warpMatrix(Size   sz,
                double theta,
                double phi,
                double gamma,
                double scale,
                double fovy,
                Mat&   M,
                vector<Point2f>* corners);
void warpFrame(const Mat &input, Mat &output, double tilt, double yaw, double roll,
                 double dx, double dy, double dz, double f, double cx, double cy);

int N = 15; //11
int GAUSSIAN_FACTOR = 5;
int MAX_WIDTH, MAX_HEIGHT;
int SEGMENTOS_FRONTERA = 4;
int PUNTOS_SEGMENTO_FRONTERA = 3;
int SEGMENTOS_INTERNOS = 3;
int PUNTOS_SEGMENTO_INTERNOS = 3;
int DIRECCION_ORDEN_C = 1;
int NIVEL_THRESHOLD = 50;
float TOLERANCIA_LED_ENCENDIDO = 5.0; //(%)
int NUM_MATRICES = 3;
int NUM_PARTICIONES = 16;
int PARTICION_OFFSET = 1;

JNIEXPORT jstring JNICALL
Java_com_app_house_asistenciaestudiante_CameraActivity_decodificar(JNIEnv *env,
                                                                 jobject,
                                                                 jlong imagenRGBA,
                                                                 jlong imagenResultado,
                                                                 jlong objectSize,
                                                                 jlong parameters) {

    //TO DO LIST
    // RESIZE CORRECTED IMAGE BEFORE SEARCH
    Mat & mProcesado = *(Mat*)imagenRGBA;
    Mat mOriginalCopia = mProcesado.clone();
    Mat mOriginalCopiaB = mProcesado.clone();
    Mat & mResultado = *(Mat*)imagenResultado;
    Mat & mObjectSize = *(Mat*)objectSize;
    Mat & mParameters = *(Mat*)parameters;

    string mensajeBinario = "................................................";
    string mensajeDecimal = "";
    int faseDeco =  mParameters.at<double>(0, 5);

    MAX_WIDTH = mProcesado.cols;
    MAX_HEIGHT = mProcesado.rows;
    vector<vector<Point> > cuadrados, cuadradosImagenCorregida;
    vector<vector<vector<Point> > > particiones;
    cuadrados.clear();

    buscarCuadrados(mProcesado, cuadrados, LAPLACIAN, N);
    dibujarCuadrados(mProcesado, cuadrados);
    drawMarker(mProcesado, Point( MAX_WIDTH/2, MAX_HEIGHT/2),  Scalar(255, 0, 0), MARKER_CROSS, 20, 2);

    if(cuadrados.size() == NUM_MATRICES && alineadoEjeFocal(mProcesado, cuadrados)) {

        Mat mWarpImage, mLambda;

        /*if(faseDeco < 4) {
            particionarCuadrados(mProcesado, cuadrados, particiones);
            decodificarParticiones(mProcesado, mOriginalCopia, particiones, mensajeBinario);
            traducirMensaje(mensajeBinario, mensajeDecimal, faseDeco);
            mResultado = mProcesado;
        }
        else {*/
            vector<Point2f> corners;

            Rect boundRect;
            vector<Point> brPoints;
            brPoints.push_back(cuadrados[0][0]);
            brPoints.push_back(cuadrados[2][1]);
            brPoints.push_back(cuadrados[2][2]);
            brPoints.push_back(cuadrados[0][3]);

            boundRect = boundingRect(brPoints);

            //mObjectSize.at<double>(0, 0) = distanciaEntreDosPuntos(cuadrados[0][3], cuadrados[2][2]); //getWidthObjectImage(cuadrados); //
            //mObjectSize.at<double>(0, 1) = distanciaEntreDosPuntos(cuadrados[0][0], cuadrados[0][3]);
            mObjectSize.at<double>(0, 0) = distanciaEntreDosPuntos(Point(boundRect.tl().x, boundRect.tl().y + boundRect.height), boundRect.br()); //getWidthObjectImage(cuadrados); //
            mObjectSize.at<double>(0, 1) = distanciaEntreDosPuntos(boundRect.tl(), Point(boundRect.tl().x, boundRect.tl().y +  boundRect.height)); ////getHeightObjectImage(cuadrados);
            warpFrame(mOriginalCopiaB,
                        mWarpImage,
                        mParameters.at<double>(0, 0), //tilt,
                        mParameters.at<double>(0, 1), //yaw,
                        -mParameters.at<double>(0, 2), //roll,
                        0,
                        0,
                        1,
                        mParameters.at<double>(0, 6), //focal length in pixels
                        MAX_WIDTH/2,
                        MAX_HEIGHT/2);
            //mWarpImage = mWarpImage(Rect((mWarpImage.cols / 2) - (MAX_WIDTH / 2), (mWarpImage.rows / 2) - (MAX_HEIGHT / 2),MAX_WIDTH, MAX_HEIGHT));

            //Mat croppedImage;
            //croppedImage = mWarpImage(Rect(boundRect.tl().x - 50, boundRect.tl().y - 40, boundRect.width + 100, boundRect.height + 80)) ;

            buscarCuadrados(mWarpImage, cuadradosImagenCorregida, LAPLACIAN, N);
            //dibujarCuadrados(croppedImage, cuadradosImagenCorregida);

            //__android_log_print(ANDROID_LOG_ERROR, "cuadradosImagenCorregida", "%i", cuadradosImagenCorregida.size());
            if (cuadradosImagenCorregida.size() == NUM_MATRICES) {

                Rect boundRect2;
                vector<Point> brPoints2;
                brPoints2.push_back(cuadradosImagenCorregida[0][0]);
                brPoints2.push_back(cuadradosImagenCorregida[2][1]); //2
                brPoints2.push_back(cuadradosImagenCorregida[2][2]); //2
                brPoints2.push_back(cuadradosImagenCorregida[0][3]);
                boundRect2 = boundingRect(brPoints2);
                rectangle(mWarpImage, boundRect2.tl(), boundRect2.br(), Scalar(255, 200, 0), 1, 8, 0);
                rectangle(mWarpImage, boundRect.tl(), boundRect.br(), Scalar(0, 255, 200), 1, 8, 0);

                circle(mWarpImage, cuadradosImagenCorregida[0][3], 2, Scalar(255, 0, 255), 2, 8);
                circle(mWarpImage, cuadradosImagenCorregida[2][2], 2, Scalar(255, 0, 255), 2, 8);
                circle(mWarpImage, Point(boundRect2.tl().x, boundRect2.tl().y + boundRect2.height), 2, Scalar(0, 0, 255), 2, 8);
                circle(mWarpImage, boundRect2.br(), 2, Scalar(0, 0, 255), 2, 8);

                //mObjectSize.at<double>(0, 2) = distanciaEntreDosPuntos(cuadradosImagenCorregida[0][3], cuadradosImagenCorregida[2][2]); //getWidthObjectImage(cuadrados); //
                //__android_log_print(ANDROID_LOG_ERROR, "cuadradosImagenCorregida sin", "(%i %i) (%i %i)", cuadradosImagenCorregida[0][3].x, cuadradosImagenCorregida[0][3].y,cuadradosImagenCorregida[2][2].x,cuadradosImagenCorregida[2][2].y );
                //mObjectSize.at<double>(0, 3) = distanciaEntreDosPuntos(cuadradosImagenCorregida[0][0], cuadradosImagenCorregida[0][3]);
                mObjectSize.at<double>(0, 2) = distanciaEntreDosPuntos(Point(boundRect2.tl().x, boundRect2.tl().y + boundRect2.height), boundRect2.br()); //getWidthObjectImage(cuadrados); //
                //__android_log_print(ANDROID_LOG_ERROR, "cuadradosImagenCorregida corr", "(%i %i) (%i %i)",Point(boundRect2.tl().x, boundRect2.tl().y + boundRect2.height).x,
                  //                                                                                        Point(boundRect2.tl().x, boundRect2.tl().y + boundRect2.height).y,
                    //                boundRect2.br().x, boundRect2.br().y);
                mObjectSize.at<double>(0, 3) = distanciaEntreDosPuntos(boundRect2.tl(), Point(boundRect2.tl().x, boundRect2.tl().y +  boundRect2.height)); ////getHeightObjectImage(cuadrados);

            }
            mResultado = mProcesado;//mWarpImage;
       //}
    }
    else mResultado = mProcesado;
    return env->NewStringUTF(mensajeDecimal.c_str());
}
static void buscarCuadrados( const Mat& image, vector<vector<Point> >& cuadrados, MetodoBusqueda metodo, int thresholdFactor){
    cuadrados.clear();

    Mat gray0(image.size(), CV_8U);
    cvtColor(image, gray0, CV_BGR2GRAY);
    GaussianBlur( gray0, gray0, Size( GAUSSIAN_FACTOR, GAUSSIAN_FACTOR ), 0, 0 );
    Mat gray, gray2;
    vector<vector<Point> > contours1;

    for( int l = 0; l < thresholdFactor; l++ )
    {
        // hack: use Canny instead of zero threshold level.
        // Canny helps to catch squares with gradient shading
        if( l == 0 )
        {
            // apply Canny. Take the upper threshold from slider
            // and set the lower to 0 (which forces edges merging)
            if(metodo == LAPLACIAN) {
                /// Apply Laplace function
                Mat dst;
                bitwise_not(gray0, gray0);
                Laplacian(gray0, dst, 3, 3, 1, 0, BORDER_DEFAULT); //3 depth CV_16S
                convertScaleAbs(dst, gray);
                /*if(1){

                    string path = "/storage/3034-3465/DCIM/";
                    string filename = IntToString(1);
                    string ext = ".jpg";
                    imwrite(path+filename+ext, gray);
                }*/
            }
            else if (metodo == CANNY) {
                bitwise_not(gray0, gray0);
                Canny(gray0, gray, 0, 15, 3, false);//10,20,3);// 0 10 5
                /*if(1){

                    string path = "/storage/3034-3465/DCIM/";
                    string filename = IntToString(1);
                    string ext = ".jpg";
                    imwrite(path+filename+ext, gray);
                }*/
            }
                // dilate canny output to remove potential
                // holes between edge segments
            ///dilate(gray, gray, getStructuringElement(MORPH_RECT, Size(3, 3)));
            /*if(1){

                string path = "/storage/3034-3465/DCIM/";
                string filename = IntToString(2);
                string ext = ".jpg";
                imwrite(path+filename+ext, gray);
            }*/

        }
        else
        {
            // apply threshold if l!=0:
            //     tgray(x,y) = gray(x,y) < (l+1)*255/N ? 255 : 0
            gray = gray0 >= ((l + 1) * 255 / thresholdFactor); //255 -

        }
        // Encuentra los contornos (mas exteriores si existen otros dentro) y los enlista
        findContours(gray, contours1, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE, Point(-1, -1)); //Point(-1 ,-1)
        //findContours(gray2, contours2, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE, Point(-1 ,-1));
        //printf("contours1: %i, contours2: %i\n",contours1.size(), contours2.size());
        //contours1.insert(contours1.end(), contours2.begin(), contours2.end());
        vector<Point> approx, _approx;

        // test each contour
        for( size_t i = 0; i < contours1.size(); i++ )
        {
            //convexHull(Mat(contours1[i]), _approx, true, true); //PROVOCA DESORDEN EN LOS VERTICES
            try {
                approxPolyDP(Mat(contours1[i]), approx, arcLength(Mat(contours1[i]), true)*0.02, true);
            }catch(Exception e){
            }
            // square contours should have 4 vertices after approximation
            // relatively large area (to filter out noisy contours)
            // and be convex.
            // Note: absolute value of an area is used because
            // area may be positive or negative - in accordance with the
            // contour orientation
            if( approx.size() == 4 &&
                !filtrarCuadrado(cuadrados, approx) &&
                    fabs(contourArea(Mat(approx))) > 100 &&
                isContourConvex(Mat(approx)))
            {
                double maxCosine = 0;
                //__android_log_print(ANDROID_LOG_ERROR, "decodificar", "%.3f", 1.3);
                for( int j = 2; j < 5; j++ )
                {
                    // find the maximum cosine of the angle between joint edges
                    double cosine = fabs(calcularAnguloEntreDosPuntos(approx[j%4], approx[j-2], approx[j-1]));
                    maxCosine = MAX(maxCosine, cosine);
                }

                // if cosines of all angles are small
                // (all angles are ~90 degree) then write quandrange
                // vertices to resultant sequence
                if( maxCosine < 0.3 ) {
                    cuadrados.push_back(approx);
                    //__android_log_print(ANDROID_LOG_ERROR, "APPROX SELECCIONADO ---> ", "(%i %i) (%i %i) (%i %i) (%i %i) ", approx[0].x, approx[0].y,approx[1].x,approx[1].y, approx[2].x, approx[2].y, approx[3].x, approx[3].y);
                }
                //__android_log_print(ANDROID_LOG_ERROR, "decodificar", "%.3f", 1.4);
            }
        }
    }

    ordenarCuadradosPorPosicionEspacial(cuadrados, DIRECCION_ORDEN_C);
}
static void dibujarCuadrados( Mat& image, const vector<vector<Point> >& cuadrados ) {

    for (int i = 0; i < cuadrados.size(); i++) {

        const Point *p = &cuadrados[i][0];
        int n = (int) cuadrados[i].size();

        putText(image, IntToString(i + 1).c_str(), Point(cuadrados[i][0].x, cuadrados[i][0].y-10),
                FONT_HERSHEY_SIMPLEX, 0.8, Scalar(255, 0, 0), 2, LINE_AA, false);
        //if (p->x > 3 && p->y > 3 && p->x < MAX_WIDTH -3 && p->y < MAX_HEIGHT-3) {
        //Rect boundRect;
        //boundRect = boundingRect(cuadrados[i]);
        //rectangle(image, boundRect.tl(), boundRect.br(), Scalar(100, 100, 100), 2, 8, 0);
        polylines(image, &p, &n, 1, true, Scalar(0, 255, 0), 1, 16);
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

    //__android_log_print(ANDROID_LOG_ERROR, "particionarCuadrados", "%0.2f",3.0);

    for (int i = 0; i < cuadrados.size(); i++) {
        //__android_log_print(ANDROID_LOG_ERROR, "particionarCuadrados", "%f", 3.1);
        //Buscando los puntos de las fronteras


        for (int n = 0; n < SEGMENTOS_FRONTERA; n++) {
            //__android_log_print(ANDROID_LOG_ERROR, "particionarCuadrados", "%i", 3);
            const Point puntoInicial = cuadrados[i][n];
            const Point puntoFinal = cuadrados[i][(n + 1) % SEGMENTOS_FRONTERA];

            //__android_log_print(ANDROID_LOG_ERROR, "puntos vertices ptos ---> ", " i(%i %i) f(%i %i)", puntoInicial.x, puntoInicial.y, puntoFinal.x, puntoFinal.y);

            LineIterator it(image, puntoInicial, puntoFinal, 8, false);

            int razon = (int) round(it.count / (PUNTOS_SEGMENTO_FRONTERA+1));

            puntosVerticePorCuadrado.push_back(puntoInicial);

            //circle(image, puntoInicial, 2, Scalar(0, 0, 255 * n / (SEGMENTOS_FRONTERA-1)), 2, 8);

            //Iterando cada segmento del cuadrado para hallar puntos medios y divirlos
            // en cuatro partes iguales
            for (int k = 0; k < it.count; k++, ++it) {

                if (k == 0 || k == it.count-1) { ; }
                else {
                    if ((k % razon) == 0 && puntosFronteraPorSegmento.size() < PUNTOS_SEGMENTO_FRONTERA) {
                        puntosFronteraPorSegmento.push_back(it.pos());

                        //__android_log_print(ANDROID_LOG_ERROR, "puntos FRONTERA ptos ---> ", "PTO INI PTO FIN %i %i %i %i %i (%i %i)", i, n, razon, it.count, puntosFronteraPorSegmento.size(), it.pos().x, it.pos().y);
                        //circle(image, it.pos(), 2, Scalar((255 * k / it.count), 0, 0), 2, 8);
                        }
                    }
                }

            puntosFronteraPorCuadrado.push_back(puntosFronteraPorSegmento);
            puntosFronteraPorSegmento.clear();
        }
        //__android_log_print(ANDROID_LOG_ERROR, "particionarCuadrados", "%f", 3.2);
        //__android_log_print(ANDROID_LOG_ERROR, "puntos Vertice ---> ", "%i, (%i %i) (%i %i) (%i %i) (%i %i)",i+1,
        //                    puntosFronteraPorCuadrado[0][0].x, puntosFronteraPorCuadrado[0][0].y,
        //                    puntosFronteraPorCuadrado[1][0].x, puntosFronteraPorCuadrado[1][0].y,
        //                   puntosFronteraPorCuadrado[2][0].x, puntosFronteraPorCuadrado[2][0].y,
        //                   puntosFronteraPorCuadrado[3][0].x, puntosFronteraPorCuadrado[3][0].y);
        
        puntosVertices.push_back(puntosVerticePorCuadrado);
        puntosVerticePorCuadrado.clear();

        puntosFrontera.push_back(puntosFronteraPorCuadrado);
        puntosFronteraPorCuadrado.clear();

        //Buscando los puntos internos del cuadrado
        for (int r = 0; r < SEGMENTOS_INTERNOS; r++) {
            //__android_log_print(ANDROID_LOG_ERROR, "particionarCuadrados", "%i", 4);
            //Punto del segmento A del cuadrado
            //__android_log_print(ANDROID_LOG_ERROR, "particionarCuadrados", "%f %i %i %i", 3.3, i, SEGMENTOS_INTERNOS - r - 1, (int)puntosFrontera[i].size());
            Point puntoInicial = puntosFrontera[i][3][SEGMENTOS_INTERNOS - r - 1];

            //__android_log_print(ANDROID_LOG_ERROR, "particionarCuadrados", "%f", 3.31);
            //Punto del segmento B del cuadrado opuesto a A
            Point puntoFinal = puntosFrontera[i][1][r];
            //__android_log_print(ANDROID_LOG_ERROR, "puntos internos ptos ---> ", "PTO INI PTO FIN (%i %i) (%i %i)",  puntoInicial.x, puntoInicial.y, puntoFinal.x, puntoFinal.y);
            //Iterador que recorre el segmento de linea punto a punto
            LineIterator it(image, puntoInicial, puntoFinal, 8, true);
            int razon = (int) round(it.count / (PUNTOS_SEGMENTO_INTERNOS+1)); //+ 1

            //Recoriendo el segmento punto a punto
            for (int k = 0; k < it.count; k++, ++it) {
                //__android_log_print(ANDROID_LOG_ERROR, "particionarCuadrados", "%i", 5);
                if (k == 0 || k == it.count-1) { ; }
                else {
                    if ((k % razon) == 0 && puntosInternosPorSegmento.size() < PUNTOS_SEGMENTO_INTERNOS) {
                        puntosInternosPorSegmento.push_back(it.pos());
                        //__android_log_print(ANDROID_LOG_ERROR, "Punto Interno ---> ", "(%i %i) %i %i %i %i", it.pos().x, it.pos().y, r, k, puntosInternosPorSegmento.size(), it.count);
                        //circle(image, it.pos(), 2, Scalar(0, 0, 255 * k / it.count), 2, 8);
                        //puntos internos ptos --->: 0 1 0 (20 1529) - (111 317)

                    }
                }
            }
            //__android_log_print(ANDROID_LOG_ERROR, "particionarCuadrados", "%f", 4.0);
            //__android_log_print(ANDROID_LOG_ERROR, "puntos Vertice ---> ", "%i %i, (%i %i) (%i %i) (%i %i)",i+1, r+1,
            //                    puntosInternosPorSegmento[0].x, puntosInternosPorSegmento[0].y,
            //                    puntosInternosPorSegmento[1].x, puntosInternosPorSegmento[1].y,
            //                    puntosInternosPorSegmento[2].x, puntosInternosPorSegmento[2].y);

            //__android_log_print(ANDROID_LOG_ERROR, "puntos frontera ---> ", "%i %i",r+1, puntosInternosPorSegmento.size());
            puntosInternosPorCuadrado.push_back(puntosInternosPorSegmento);
            puntosInternosPorSegmento.clear();
        }
        //__android_log_print(ANDROID_LOG_ERROR, "particionarCuadrados", "%i", 6);

        puntosInternos.push_back(puntosInternosPorCuadrado);
        puntosInternosPorCuadrado.clear();

        //__android_log_print(ANDROID_LOG_ERROR, "size ---> ", "%i %i %i", puntosVertices[i].size(), puntosFrontera[i].size(), puntosInternos[i].size());
    }

    unificarParticiones(cuadrados, puntosVertices, puntosFrontera, puntosInternos, particiones);

    //Dibujar particiones
    if (particiones.size() > 0) {
        for (int x = 0; x < cuadrados.size(); x++) {
            for (int r = 0; r < particiones[x].size(); r++) {
                const Point *p = &particiones[x][r][0];
                int n = (int) particiones[x][r].size();
                //__android_log_print(ANDROID_LOG_ERROR, "unificarParticiones1 ---> ", "%i, (%i %i)",i, r, n);
                polylines(image, &p, &n, 1, true, Scalar(255 * r / particiones[x].size(), 0, 200),
                          1, 16);
            }
        }
    }
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
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.11, puntosVertices[indiceCuadrado][0].x, puntosVertices[indiceCuadrado][0].y);
        particion.push_back(puntosVertices[indiceCuadrado][0]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.12, puntosFrontera[indiceCuadrado][0][0].x, puntosFrontera[indiceCuadrado][0][0].y);
        particion.push_back(puntosFrontera[indiceCuadrado][0][0]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.13, puntosInternos[indiceCuadrado][0][0].x, puntosInternos[indiceCuadrado][0][0].y);
        particion.push_back(puntosInternos[indiceCuadrado][0][0]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.14, puntosFrontera[indiceCuadrado][3][2].x, puntosFrontera[indiceCuadrado][3][2].y);
        particion.push_back(puntosFrontera[indiceCuadrado][3][2]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();

        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%f", 7.2);
        //Particion 2
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.21, puntosFrontera[indiceCuadrado][0][0].x, puntosFrontera[indiceCuadrado][0][0].y);
        particion.push_back(puntosFrontera[indiceCuadrado][0][0]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.22, puntosFrontera[indiceCuadrado][0][1].x, puntosFrontera[indiceCuadrado][0][1].y);
        particion.push_back(puntosFrontera[indiceCuadrado][0][1]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.23, puntosInternos[indiceCuadrado][0][1].x, puntosInternos[indiceCuadrado][0][1].y);
        particion.push_back(puntosInternos[indiceCuadrado][0][1]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.24, puntosInternos[indiceCuadrado][0][0].x, puntosInternos[indiceCuadrado][0][0].y);
        particion.push_back(puntosInternos[indiceCuadrado][0][0]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%f", 7.3);
        //Particion 3
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.31, puntosFrontera[indiceCuadrado][0][1].x, puntosFrontera[indiceCuadrado][0][1].y);
        particion.push_back(puntosFrontera[indiceCuadrado][0][1]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.32, puntosFrontera[indiceCuadrado][0][2].x, puntosFrontera[indiceCuadrado][0][2].y);
        particion.push_back(puntosFrontera[indiceCuadrado][0][2]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.33, puntosInternos[indiceCuadrado][0][2].x, puntosInternos[indiceCuadrado][0][2].y);
        particion.push_back(puntosInternos[indiceCuadrado][0][2]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.34, puntosInternos[indiceCuadrado][0][1].x, puntosInternos[indiceCuadrado][0][1].y);
        particion.push_back(puntosInternos[indiceCuadrado][0][1]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%f", 7.4);
        //Particion 4
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.41, puntosFrontera[indiceCuadrado][0][2].x, puntosFrontera[indiceCuadrado][0][2].y);
        particion.push_back(puntosFrontera[indiceCuadrado][0][2]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.42, puntosVertices[indiceCuadrado][1].x, puntosVertices[indiceCuadrado][1].y);
        particion.push_back(puntosVertices[indiceCuadrado][1]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.43, puntosFrontera[indiceCuadrado][1][0].x, puntosFrontera[indiceCuadrado][1][0].y);
        particion.push_back(puntosFrontera[indiceCuadrado][1][0]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.44, puntosInternos[indiceCuadrado][0][2].x, puntosInternos[indiceCuadrado][0][2].y);
        particion.push_back(puntosInternos[indiceCuadrado][0][2]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%f", 7.5);
        //Particion 5
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.51, puntosFrontera[indiceCuadrado][3][2].x, puntosFrontera[indiceCuadrado][3][2].y);
        particion.push_back(puntosFrontera[indiceCuadrado][3][2]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.52, puntosInternos[indiceCuadrado][0][0].x, puntosInternos[indiceCuadrado][0][0].y);
        particion.push_back(puntosInternos[indiceCuadrado][0][0]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.53, puntosInternos[indiceCuadrado][1][0].x, puntosInternos[indiceCuadrado][1][0].y);
        particion.push_back(puntosInternos[indiceCuadrado][1][0]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.54, puntosFrontera[indiceCuadrado][3][1].x, puntosFrontera[indiceCuadrado][3][1].y);
        particion.push_back(puntosFrontera[indiceCuadrado][3][1]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%f", 7.6);
        //Particion 6
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.61, puntosInternos[indiceCuadrado][0][0].x, puntosInternos[indiceCuadrado][0][0].y);
        particion.push_back(puntosInternos[indiceCuadrado][0][0]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.62, puntosInternos[indiceCuadrado][0][1].x, puntosInternos[indiceCuadrado][0][1].y);
        particion.push_back(puntosInternos[indiceCuadrado][0][1]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.63, puntosVertices[indiceCuadrado][0].x, puntosVertices[indiceCuadrado][0].y);
        particion.push_back(puntosInternos[indiceCuadrado][1][1]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.64, puntosInternos[indiceCuadrado][1][1].x, puntosInternos[indiceCuadrado][1][1].y);
        particion.push_back(puntosInternos[indiceCuadrado][1][0]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%f", 7.7);
        //Particion 7
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.71, puntosInternos[indiceCuadrado][0][1].x, puntosInternos[indiceCuadrado][0][1].y);
        particion.push_back(puntosInternos[indiceCuadrado][0][1]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.72, puntosInternos[indiceCuadrado][0][2].x, puntosInternos[indiceCuadrado][0][2].y);
        particion.push_back(puntosInternos[indiceCuadrado][0][2]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.73, puntosInternos[indiceCuadrado][1][2].x, puntosInternos[indiceCuadrado][1][2].y);
        particion.push_back(puntosInternos[indiceCuadrado][1][2]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.74, puntosInternos[indiceCuadrado][1][1].x, puntosInternos[indiceCuadrado][1][1].y);
        particion.push_back(puntosInternos[indiceCuadrado][1][1]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%f", 7.8);
        //Particion 8
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.81, puntosVertices[indiceCuadrado][0].x, puntosVertices[indiceCuadrado][0].y);
        particion.push_back(puntosInternos[indiceCuadrado][0][2]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.82, puntosVertices[indiceCuadrado][0].x, puntosVertices[indiceCuadrado][0].y);
        particion.push_back(puntosFrontera[indiceCuadrado][1][0]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.83, puntosVertices[indiceCuadrado][0].x, puntosVertices[indiceCuadrado][0].y);
        particion.push_back(puntosFrontera[indiceCuadrado][1][1]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.84, puntosVertices[indiceCuadrado][0].x, puntosVertices[indiceCuadrado][0].y);
        particion.push_back(puntosInternos[indiceCuadrado][1][2]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%f", 7.9);
        //Particion 9
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.91, puntosFrontera[indiceCuadrado][3][1].x, puntosFrontera[indiceCuadrado][3][1].y);
        particion.push_back(puntosFrontera[indiceCuadrado][3][1]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.92, puntosInternos[indiceCuadrado][1][0].x, puntosInternos[indiceCuadrado][1][0].y);
        particion.push_back(puntosInternos[indiceCuadrado][1][0]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.93, puntosInternos[indiceCuadrado][2][0].x, puntosInternos[indiceCuadrado][2][0].y);
        particion.push_back(puntosInternos[indiceCuadrado][2][0]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.94, puntosFrontera[indiceCuadrado][3][0].x, puntosFrontera[indiceCuadrado][3][0].y);
        particion.push_back(puntosFrontera[indiceCuadrado][3][0]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%f", 7.10);
        //Particion 10
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.101, puntosInternos[indiceCuadrado][1][0].x, puntosVertices[indiceCuadrado][0].y);
        particion.push_back(puntosInternos[indiceCuadrado][1][0]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.102, puntosVertices[indiceCuadrado][0].x, puntosInternos[indiceCuadrado][1][1].y);
        particion.push_back(puntosInternos[indiceCuadrado][1][1]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.103, puntosInternos[indiceCuadrado][2][1].x, puntosInternos[indiceCuadrado][2][1].y);
        particion.push_back(puntosInternos[indiceCuadrado][2][1]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.104, puntosInternos[indiceCuadrado][2][0].x, puntosInternos[indiceCuadrado][2][0].y);
        particion.push_back(puntosInternos[indiceCuadrado][2][0]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%f", 7.11);
        //Particion 11
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.111, puntosInternos[indiceCuadrado][1][1].x, puntosInternos[indiceCuadrado][1][1].y);
        particion.push_back(puntosInternos[indiceCuadrado][1][1]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.112, puntosInternos[indiceCuadrado][1][2].x, puntosInternos[indiceCuadrado][1][2].y);
        particion.push_back(puntosInternos[indiceCuadrado][1][2]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.113, puntosInternos[indiceCuadrado][2][2].x, puntosInternos[indiceCuadrado][2][2].y);
        particion.push_back(puntosInternos[indiceCuadrado][2][2]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.114, puntosInternos[indiceCuadrado][2][1].x, puntosInternos[indiceCuadrado][2][1].y);
        particion.push_back(puntosInternos[indiceCuadrado][2][1]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%f", 7.12);
        //Particion 12
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.121, puntosInternos[indiceCuadrado][1][2].x, puntosInternos[indiceCuadrado][1][2].y);
        particion.push_back(puntosInternos[indiceCuadrado][1][2]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.122, puntosFrontera[indiceCuadrado][1][1].x, puntosFrontera[indiceCuadrado][1][1].y);
        particion.push_back(puntosFrontera[indiceCuadrado][1][1]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.123, puntosFrontera[indiceCuadrado][1][2].x, puntosFrontera[indiceCuadrado][1][2].y);
        particion.push_back(puntosFrontera[indiceCuadrado][1][2]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.124, puntosInternos[indiceCuadrado][2][2].x, puntosInternos[indiceCuadrado][2][2].y);
        particion.push_back(puntosInternos[indiceCuadrado][2][2]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%f", 7.13);
        //Particion 13
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.131, puntosFrontera[indiceCuadrado][3][0].x, puntosFrontera[indiceCuadrado][3][0].y);
        particion.push_back(puntosFrontera[indiceCuadrado][3][0]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.132, puntosInternos[indiceCuadrado][2][0].x, puntosInternos[indiceCuadrado][2][0].y);
        particion.push_back(puntosInternos[indiceCuadrado][2][0]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.133, puntosFrontera[indiceCuadrado][2][2].x, puntosFrontera[indiceCuadrado][2][2].y);
        particion.push_back(puntosFrontera[indiceCuadrado][2][2]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.134, puntosVertices[indiceCuadrado][3].x, puntosVertices[indiceCuadrado][3].y);
        particion.push_back(puntosVertices[indiceCuadrado][3]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%f", 7.14);
        //Particion 14
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.141, puntosInternos[indiceCuadrado][2][0].x, puntosInternos[indiceCuadrado][2][0].y);
        particion.push_back(puntosInternos[indiceCuadrado][2][0]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.142, puntosInternos[indiceCuadrado][2][1].x, puntosInternos[indiceCuadrado][2][1].y);
        particion.push_back(puntosInternos[indiceCuadrado][2][1]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.143, puntosFrontera[indiceCuadrado][2][1].x, puntosFrontera[indiceCuadrado][2][1].y);
        particion.push_back(puntosFrontera[indiceCuadrado][2][1]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.144, puntosFrontera[indiceCuadrado][2][2].x, puntosFrontera[indiceCuadrado][2][2].y);
        particion.push_back(puntosFrontera[indiceCuadrado][2][2]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%f", 7.15);
        //Particion 15
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.151, puntosInternos[indiceCuadrado][2][1].x, puntosInternos[indiceCuadrado][2][1].y);
        particion.push_back(puntosInternos[indiceCuadrado][2][1]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.152, puntosInternos[indiceCuadrado][2][2].x, puntosInternos[indiceCuadrado][2][2].y);
        particion.push_back(puntosInternos[indiceCuadrado][2][2]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.153, puntosFrontera[indiceCuadrado][2][0].x, puntosFrontera[indiceCuadrado][2][0].y);
        particion.push_back(puntosFrontera[indiceCuadrado][2][0]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.154, puntosFrontera[indiceCuadrado][2][1].x, puntosFrontera[indiceCuadrado][2][1].y);
        particion.push_back(puntosFrontera[indiceCuadrado][2][1]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%f", 7.16);
        //Particion 16
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.161, puntosInternos[indiceCuadrado][2][2].x, puntosInternos[indiceCuadrado][2][2].y);
        particion.push_back(puntosInternos[indiceCuadrado][2][2]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.162, puntosFrontera[indiceCuadrado][1][2].x, puntosFrontera[indiceCuadrado][1][2].y);
        particion.push_back(puntosFrontera[indiceCuadrado][1][2]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.163, puntosVertices[indiceCuadrado][2].x, puntosVertices[indiceCuadrado][2].y);
        particion.push_back(puntosVertices[indiceCuadrado][2]);
        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%.2f (%i %i)", 7.164, puntosFrontera[indiceCuadrado][2][0].x, puntosFrontera[indiceCuadrado][2][0].y);
        particion.push_back(puntosFrontera[indiceCuadrado][2][0]);
        particionesPorCuadrado.push_back(particion);

        particion.clear();

        particiones.push_back(particionesPorCuadrado);
        particionesPorCuadrado.clear();

        //__android_log_print(ANDROID_LOG_ERROR, "unificarCuadrados", "%i", 0);
    }


}
static void preprocesar( Mat& image, Mat& image_prep){
    Mat tmp;
    cvtColor(image, image_prep, CV_BGR2GRAY);

    //imwrite("/storage/3034-3465/DCIM/prep0.jpg", image_prep);
    GaussianBlur( image_prep, image_prep, Size( GAUSSIAN_FACTOR, GAUSSIAN_FACTOR ), 0, 0 );
    //imwrite("/storage/3034-3465/DCIM/prep1.jpg", image_prep);
    //Laplacian( image_prep, tmp, CV_16S, 1, 1, 0, BORDER_DEFAULT );
    //imwrite("/storage/3034-3465/DCIM/prep2.jpg", tmp);
    //convertScaleAbs( tmp, image_prep );
    //Mat m = tmp.clone();
    //__android_log_print(ANDROID_LOG_ERROR, "decodificarParticiones", "%i %i", m.rows, m.cols);
    //imwrite("/storage/3034-3465/DCIM/prep3.jpg", image_prep);
    //bitwise_not(image_prep, image_prep);
    //imwrite("/storage/3034-3465/DCIM/prep4.jpg", image_prep);
    threshold(image_prep, image_prep, NIVEL_THRESHOLD, 255, CV_THRESH_OTSU);

    imwrite("/storage/3034-3465/DCIM/prep4.jpg", image_prep);
    //erode(image_prep, image_prep, getStructuringElement(MORPH_RECT, Size(3, 3)));
    //imwrite("/storage/3034-3465/DCIM/prep5.jpg", image_prep);
    //dilate(image_prep, image_prep, getStructuringElement(MORPH_RECT, Size(3, 3)));

    //int morph_size =1;
    //Mat element = getStructuringElement( MORPH_RECT, Size( 2*morph_size + 1, 2*morph_size+1 ), Point( morph_size, morph_size ) );
    //morphologyEx( image_prep, image_prep, MORPH_CLOSE, element, Point(-1,-1));

    //imwrite("/storage/3034-3465/DCIM/prep6.jpg", image_prep);
    //LAPLACIAN
    /*imwrite("/storage/3034-3465/DCIM/prep0.jpg", image_prep);
    GaussianBlur( image_prep, image_prep, Size( GAUSSIAN_FACTOR, GAUSSIAN_FACTOR ), 0, 0 );
    imwrite("/storage/3034-3465/DCIM/prep1.jpg", image_prep);
    Laplacian( image_prep, tmp, CV_16S, 1, 1, 0, BORDER_DEFAULT );
    imwrite("/storage/3034-3465/DCIM/prep2.jpg", tmp);
    convertScaleAbs( tmp, image_prep );
    //Mat m = tmp.clone();
    //__android_log_print(ANDROID_LOG_ERROR, "decodificarParticiones", "%i %i", m.rows, m.cols);
    imwrite("/storage/3034-3465/DCIM/prep3.jpg", image_prep);
    //bitwise_not(image_prep, image_prep);
    //imwrite("/storage/3034-3465/DCIM/prep4.jpg", image_prep);
    threshold(image_prep, image_prep, 60, 255, CV_THRESH_OTSU);
    imwrite("/storage/3034-3465/DCIM/prep5.jpg", image_prep);*/

}
static void decodificarParticiones( Mat& image,
                                    Mat& image_c,
                                    vector<vector<vector<Point> > >& particiones,
                                    string& mensajeBinario){

    if(particiones.size() <= 0 ) return;
    if(particiones[0].size() <= 0 ) return;
    int NUM_MATRICES_ACTUALES = (int)particiones.size();
    int NUM_PARTICIONES_ACTUALES = (int)particiones[0].size();
    float porcentajeBlanco = 0;
    vector<Point> puntosBlancos;
    Point2f origen[4];

    preprocesar(image_c, image_c);

    //__android_log_print(ANDROID_LOG_ERROR, "decodificarParticiones", "%.3f", 2.0);

    for(int i = 0; i < NUM_MATRICES; i++) {
        for (int k = 0; k < NUM_PARTICIONES; k++) {
            origen[0] = particiones[i][k][0]; //oint(particiones[i][k][0].x + PARTICION_OFFSET, particiones[i][k][0].y + PARTICION_OFFSET); //particiones[i][k][0];
            origen[1] = particiones[i][k][1]; //Point(particiones[i][k][1].x - PARTICION_OFFSET, particiones[i][k][1].y + PARTICION_OFFSET); //particiones[i][k][1];
            origen[2] = particiones[i][k][2]; //Point(particiones[i][k][2].x - PARTICION_OFFSET, particiones[i][k][2].y - PARTICION_OFFSET); //particiones[i][k][2];
            origen[3] = particiones[i][k][3]; // Point(particiones[i][k][3].x + PARTICION_OFFSET, particiones[i][k][3].y - PARTICION_OFFSET); //particiones[i][k][3];

            Mat mParticion = image_c(
                    Rect(particiones[i][k][0].x+PARTICION_OFFSET,
                         particiones[i][k][0].y+PARTICION_OFFSET,
                         particiones[i][k][1].x - particiones[i][k][0].x-PARTICION_OFFSET,
                         particiones[i][k][3].y - particiones[i][k][0].y-PARTICION_OFFSET));
            if(i == 1){
            string path = "/storage/3034-3465/DCIM/";
            string filename = IntToString(k+1);
            string ext = ".jpg";
            imwrite(path+filename+ext, mParticion);
        }
            dilate(mParticion, mParticion, getStructuringElement(MORPH_RECT, Size(3, 3)));
            //dilate(mParticion, mParticion, getStructuringElement(MORPH_RECT, Size(3, 3)));
            //int morph_size =1;
            //Mat element = getStructuringElement( MORPH_RECT, Size( 2*morph_size + 1, 2*morph_size+1 ), Point( morph_size, morph_size ) );
            //morphologyEx( mParticion, mParticion, MORPH_OPEN, element, Point(-1,-1));
            /*if(i == 1){
                string path = "/storage/3034-3465/DCIM/";
                string filename = IntToString(k+1);
                string ext = "_.jpg";
                imwrite(path+filename+ext, mParticion);
            }*/

            ////Busca pixeles blancos en imagen binarizada
            findNonZero(mParticion, puntosBlancos);
            //__android_log_print(ANDROID_LOG_ERROR, "decodificarParticiones", "%.3f", 4.0);
            float tamanio = ((particiones[i][k][1].x - particiones[i][k][0].x) * (particiones[i][k][3].y - particiones[i][k][0].y));

            //Calcula el procentaje de pixeles blancos contra el total de pixeles en la region
            porcentajeBlanco = float((puntosBlancos.size()) * 100.0) / tamanio;

            if (porcentajeBlanco >= TOLERANCIA_LED_ENCENDIDO) {
                mensajeBinario[(i % NUM_MATRICES) * NUM_PARTICIONES + k] = '1';
            }
            else{
                mensajeBinario[(i % NUM_MATRICES) * NUM_PARTICIONES + k] = '0';
            }
        }
    }

    putText(image, string(mensajeBinario).substr(0,4).c_str(), Point(10,MAX_HEIGHT-135), FONT_HERSHEY_DUPLEX, 0.75,Scalar(255,0,0), 2, LINE_4, false);
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

}
static void traducirMensaje(string& mensajeBinario, string& mensaje, int& fase){
    if(mensajeBinario == "................................................") return;
    if(mensajeBinario == "000000000000000000000000000000000000000000000000") return;

    mensaje = "";

    string msg_seg = "";
    string msg_matriz_1 = "";
    string msg_matriz_2 = "";
    string msg_matriz_3 = "";
    int msg_seg1_bin = -1;
    string msg_dec = "";
    int bits_substr = 4;
    int seg_x_matrix = 4;



    //RECONOCER PATRON INICIO(TODOS LOS LEDS ENCENDIDOS)
    if(fase == 0){
        if(mensajeBinario == "111111111111111111111111111111111111111111111111") {
            msg_matriz_1 = msg_matriz_2 = msg_matriz_3 = "9999";
            //__android_log_print(ANDROID_LOG_ERROR, "mensaje", "Fase 1");
        }
    }
    // RECONOCER PATRON de LECTURA DE IZQUIERDA A DERECHA y VICEVERSA
    else if(fase == 1 || fase == 2 || fase == 3){

        __android_log_print(ANDROID_LOG_ERROR, "traduciendo fase", "%i", fase);

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

        if(msg_matriz_1.length() + msg_matriz_2.length() + msg_matriz_3.length() == 12 ){
            //__android_log_print(ANDROID_LOG_ERROR, "mensaje", "Fase %i", fase);
        }
        //else  __android_log_print(ANDROID_LOG_ERROR, "mensaje", "Fase %i", -1);

    }
    if(fase == 0 || fase == 1)
        mensaje = msg_matriz_1  + msg_matriz_2  + msg_matriz_3;
    else if(fase == 2)
        mensaje = msg_matriz_1  + msg_matriz_3  + msg_matriz_2;
    else if(fase == 3)
        mensaje = msg_matriz_3  + msg_matriz_2  + msg_matriz_1;

 //   mensaje = msg_matriz_1  + msg_matriz_2  + msg_matriz_3;
    //__android_log_print(ANDROID_LOG_ERROR, "mensaje", "%s", mensaje.c_str());
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

        //__android_log_print(ANDROID_LOG_ERROR, "1 cuadra size --> ", "%i", cuadrados.size() );
        //__android_log_print(ANDROID_LOG_ERROR, "2 ptos centr size --> ", "%i", puntosCentralesCuadrados.size());
        ///__android_log_print(ANDROID_LOG_ERROR, "3 pos cercana --> ", "%i", posicionMasCercana);

        cuadradosOrdenados.push_back(cuadrados[posicionMasCercana]);
        //__android_log_print(ANDROID_LOG_ERROR, "4 cuadra pto movido --> ", "(%i %i) ", cuadrados[posicionMasCercana][0].x, cuadrados[posicionMasCercana][0].y);
        //__android_log_print(ANDROID_LOG_ERROR, "5 ordenados cuadrado--> ", "%i", cuadradosOrdenados.size());
        cuadrados[posicionMasCercana] = cuadrados.back();
        cuadrados.pop_back();
        //__android_log_print(ANDROID_LOG_ERROR, "6 cuadra size    --> ", "%i", cuadrados.size() );
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

    int TOLERANCIA = 20;

    ordenarVerticesCuadrado(approx);

    const Point* q0 = &approx[0];
    const Point* q1 = &approx[1];
    const Point* q2 = &approx[2];
    const Point* q3 = &approx[3];  //(-1796805904 0)(1077542912 0)(1079148544 0)(1078165504 0)



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

        //__android_log_print(ANDROID_LOG_ERROR, "dist entre 2 ptos ---> ", "%i %i %i %i ", d0,d1,d2,d3);

        if((d0 <= TOLERANCIA && d1 <= TOLERANCIA && d2 <= TOLERANCIA && d3 <= TOLERANCIA) ||
                d0 <= TOLERANCIA || d1 <= TOLERANCIA || d2 <= TOLERANCIA || d3 <= TOLERANCIA){
            return true;
        }
    }
    //__android_log_print(ANDROID_LOG_ERROR, "APPROX SELECCIONADO ---> ", "(%i %i) (%i %i) (%i %i) (%i %i) ",
    //                    q0->x, q0->y,q1->x,q1->y, q2->x, q2->y, q3->x, q3->y);
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
    float distancia = MAXFLOAT, distanciaTemporal = 0;
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
float distanciaEntreDosPuntos(Point p1, Point p2){
    return (float)sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
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
        /*if (Rect(cuadrados[0][0], cuadrados[0][2]).contains(Point(MAX_WIDTH/2, MAX_HEIGHT/2))){
            drawMarker(image, Point( MAX_WIDTH/2, MAX_HEIGHT/2),  Scalar(0, 255, 0), MARKER_CROSS, 20, 2);
            return true;
        }*/
    }
    else if(cuadrados.size() == 3)
    {
        if(pointPolygonTest( cuadrados[1], Point2f(MAX_WIDTH/2, MAX_HEIGHT/2), false ) > 0){
            return true;
        }
        /*if (Rect(cuadrados[1][0], cuadrados[1][2]).contains(Point(MAX_WIDTH/2, MAX_HEIGHT/2))){
            //if (Rect(cuadrados[1][0], cuadrados[1][2]).contains(Point(MAX_WIDTH/2, MAX_HEIGHT/2))){
            drawMarker(image, Point( MAX_WIDTH/2, MAX_HEIGHT/2),  Scalar(0, 255, 0), MARKER_CROSS, 20, 2);
            return true;
        }*/
    }
    return false;
}

float getWidthObjectImage(vector<vector<Point> >& cuadrados){
    if(cuadrados.size() != 1 && cuadrados.size() != 3) return 0.0f;
    if(NUM_MATRICES == 1)
        return distanciaEntreDosPuntos(cuadrados[0][3], cuadrados[0][2]);
    else if(NUM_MATRICES == 3)
        return distanciaEntreDosPuntos(cuadrados[0][3], cuadrados[2][2]);

}

float getHeightObjectImage(vector<vector<Point> >& cuadrados){
    if(cuadrados.size() != 1 && cuadrados.size() != 3) return 0.0f;
        return distanciaEntreDosPuntos(cuadrados[0][0], cuadrados[0][3]);

}

static double rad2Deg(double rad){return rad*(180/M_PI);}//Convert radians to degrees
static double deg2Rad(double deg){return deg*(M_PI/180);}//Convert degrees to radians




void warpMatrix(Size   sz,
                double theta,
                double phi,
                double gamma,
                double scale,
                double fovy,
                Mat&   M,
                vector<Point2f>* corners){
    double st=sin(deg2Rad(theta));
    double ct=cos(deg2Rad(theta));
    double sp=sin(deg2Rad(phi));
    double cp=cos(deg2Rad(phi));
    double sg=sin(deg2Rad(gamma));
    double cg=cos(deg2Rad(gamma));

    double halfFovy=fovy*0.5;
    double d=hypot(sz.width,sz.height);
    double sideLength=scale*d/cos(deg2Rad(halfFovy));
    double h=d/(2.0*sin(deg2Rad(halfFovy)));
    double n=h-(d/2.0);
    double f=h+(d/2.0);

    Mat C=Mat(3,4,CV_64FC1);
    Mat CI=Mat(4,3,CV_64FC1);
    Mat F=Mat(4,4,CV_64FC1);//Allocate 4x4 transformation matrix F
    Mat R=Mat::eye(4,4,CV_64FC1);
    Mat Rtheta=Mat::eye(4,4,CV_64FC1);//Allocate 4x4 rotation matrix around Z-axis by theta degrees
    Mat Rphi=Mat::eye(4,4,CV_64FC1);//Allocate 4x4 rotation matrix around X-axis by phi degrees
    Mat Rgamma=Mat::eye(4,4,CV_64FC1);//Allocate 4x4 rotation matrix around Y-axis by gamma degrees

    Mat T=Mat::eye(4,4,CV_64FC1);//Allocate 4x4 translation matrix along Z-axis by -h units
    Mat P=Mat::zeros(4,4,CV_64FC1);//Allocate 4x4 projection matrix

    //Rtheta
    Rtheta.at<double>(0,0)=Rtheta.at<double>(1,1)=ct;
    Rtheta.at<double>(0,1)=st;Rtheta.at<double>(1,0)=-st;
    //Rphi
    Rphi.at<double>(1,1)=Rphi.at<double>(2,2)=cp;
    Rphi.at<double>(1,2)=sp;Rphi.at<double>(2,1)=-sp;
    //Rgamma
    Rgamma.at<double>(0,0)=Rgamma.at<double>(2,2)=cg;
    Rgamma.at<double>(0,2)=-sg;Rgamma.at<double>(2,0)=sg; ///CAMBIO !!!!!!!!

    //T
    T.at<double>(2,3)=-h;//*cos(deg2Rad(gamma));
    //P
    P.at<double>(0,0)=P.at<double>(1,1)=1.0/tan(deg2Rad(halfFovy));
    P.at<double>(2,2)=-(f+n)/(f-n);
    P.at<double>(2,3)=-(2.0*f*n)/(f-n);
    P.at<double>(3,2)=-1.0;
    //Compose transformations
    //F=P*T*Rphi*Rtheta*Rgamma;//Matrix-multiply to produce master matrix
    //__android_log_print(ANDROID_LOG_ERROR, "cameraPoseFromHomography -> ", "%s", "2");
    R=(Rtheta*(Rphi*Rgamma));
    F=P*(T*R);//Matrix-multiply to produce master matrix
    //__android_log_print(ANDROID_LOG_ERROR, "cameraPoseFromHomography -> ", "%s", "3");
    //Transform 4x4 points
    double ptsIn [4*3];
    double ptsOut[4*3];
    double halfW=sz.width/2, halfH=sz.height/2;

    ptsIn[0]=-halfW;ptsIn[ 1]= halfH;
    ptsIn[3]= halfW;ptsIn[ 4]= halfH;
    ptsIn[6]= halfW;ptsIn[ 7]=-halfH;
    ptsIn[9]=-halfW;ptsIn[10]=-halfH;
    ptsIn[2]=ptsIn[5]=ptsIn[8]=ptsIn[11]=0;//Set Z component to zero for all 4 components

    Mat ptsInMat(1,4,CV_64FC3,ptsIn);
    Mat ptsOutMat(1,4,CV_64FC3,ptsOut);

    perspectiveTransform(ptsInMat,ptsOutMat,F);//Transform points

    //Get 3x3 transform and warp image
    Point2f ptsInPt2f[4];
    Point2f ptsOutPt2f[4];

    for(int i=0;i<4;i++){
        Point2f ptIn (ptsIn [i*3+0], ptsIn [i*3+1]);
        Point2f ptOut(ptsOut[i*3+0], ptsOut[i*3+1]);
        ptsInPt2f[i]  = ptIn+Point2f(halfW,halfH);
        ptsOutPt2f[i] = (ptOut+Point2f(1,1))*(sideLength*0.5);
    }

    M=getPerspectiveTransform(ptsInPt2f,ptsOutPt2f);

    //Load corners vector
    if(corners){
        corners->clear();
        corners->push_back(ptsOutPt2f[0]);//Push Top Left corner
        corners->push_back(ptsOutPt2f[1]);//Push Top Right corner
        corners->push_back(ptsOutPt2f[2]);//Push Bottom Right corner
        corners->push_back(ptsOutPt2f[3]);//Push Bottom Left corner
    }
}

void warpImage(const Mat &src,
               double    phi, //roll
               double    gamma,   //tilt
               double    theta, //theta
               double    scale,
               double    fovy,
               Mat&      dst,
               Mat&      M,
               vector<Point2f> &corners){

    double halfFovy=fovy*0.5;
    double d=hypot(src.cols,src.rows);
    double sideLength=scale*d/cos(deg2Rad(halfFovy));

    warpMatrix(src.size(),theta,phi,gamma, scale,fovy,M,&corners);//Compute warp matrix
    warpPerspective(src,dst,M,Size(sideLength,sideLength));//Do actual image warp
    //warpPerspective(src, dst, M, src.size());
}

void warpFrame(const Mat &input, Mat &output, double tilt, double yaw, double roll,
                 double dx, double dy, double dz, double f, double cx, double cy){
    Mat Rx = Mat(4, 4, CV_64FC1);
    Mat Ry = Mat(4, 4, CV_64FC1);
    Mat Rz = Mat(4, 4, CV_64FC1);
    Mat T  = Mat(4, 4, CV_64FC1);
    Mat C  = Mat(3, 4, CV_64FC1);
    Mat CI = Mat(4, 3, CV_64FC1);

    tilt = deg2Rad(tilt);
    yaw = deg2Rad(yaw);
    roll = deg2Rad(roll);

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

    // Compose rotation matrix with (RX, RY, RZ)
    Mat R = Rz * Ry * Rx;

    // Final transformation matrix
    Mat H = C * (T * (R * CI));

    // Apply matrix transformation
    warpPerspective(input, output, H, input.size(), INTER_LANCZOS4);
}

}



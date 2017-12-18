#include <jni.h>
#include <string>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>

using namespace std;
using namespace cv;

extern "C"
{
static void test( Mat& image);
static double calcularAnguloEntreDosPuntos( Point pt1, Point pt2, Point pt0);
static void buscarCuadrados( const Mat& image, vector<vector<Point> >& cuadrados);
static void dibujarCuadrados( Mat& image, const vector<vector<Point> >& cuadrados);
static void ordenarPuntosCuadrado(vector<Point> &cuadrado);
static void ordenarCuadradosPorPosicionEspacial(vector<vector<Point> >& cuadrados, int direccion);
static void particionarCuadrados( Mat& image,
                                  const vector<vector<Point> >& cuadrados,
                                  vector<vector<vector<Point> > >& particiones);
static void unificarParticiones(const int indiceCuadrado,
                               const vector<vector<Point> > & puntosVertices,
                               const vector<vector<vector<Point> > > & puntosFrontera,
                               const vector<vector<vector<Point> > > & puntosInternos,
                               vector<vector<vector<Point> > > & particiones);
static void decodificarParticiones( Mat& image,
                                    Mat& image_c,
                                    vector<vector<vector<Point> > >& particiones);
bool existeCuadradoSemejante(const vector<vector<Point> >& cuadrados, vector<Point>& approx);
float distanciaEntreDosPuntos(Point p1, Point p2);
int buscarPuntoMasCercano(vector<Point> puntos, Point punto);
string IntToString (int a);



int N = 5; //11
int CANALES = 3;
int GAUSSIAN_FACTOR = 7;
int MAX_WIDTH, MAX_HEIGHT;
int TECNICA_BORDES = 2;
int SEGMENTOS = 4;
int DIRECCION_ORDEN_C = 1;
int NIVEL_THRESHOLD = 40;
float TOLERANCIA_LED_ENCENDIDO = 3.0; //(%)


JNIEXPORT jint JNICALL
Java_com_app_house_asistenciaestudiante_MainActivity_decodificar(JNIEnv *env, jobject, jlong addrRgba, jlong addrCuadrados) {
    Mat & mRgba = *(Mat*)addrRgba;
    Mat mRgba_copia = mRgba.clone();
    Mat & mCuadrados = *(Mat*)addrCuadrados;

    MAX_WIDTH = mRgba.cols;
    MAX_HEIGHT = mRgba.rows;

    vector<vector<Point> > cuadrados;
    vector<vector<vector<Point> > > particiones;

    //test(mRgba);
    buscarCuadrados(mRgba, cuadrados);
    dibujarCuadrados(mRgba, cuadrados);
    particionarCuadrados(mRgba, cuadrados, particiones);
    decodificarParticiones(mRgba, mRgba_copia, particiones);


    /*cvtColor(mRgba, mRgba, CV_BGR2GRAY);
    bitwise_not(mRgba, mRgba);
    threshold(mRgba, mRgba, NIVEL_THRESHOLD, 255, CV_THRESH_BINARY);*/

    //mRgba = mRgba_original;
    mCuadrados = mRgba;

    int conv = 1;
    jint retVal;

    //conv = toGray(mRgba, mcuadrados);
    retVal = (jint)conv;

    return retVal;
}
static void test( Mat& image){
    cvtColor(image, image, CV_BGR2GRAY);
    GaussianBlur( image, image, Size( GAUSSIAN_FACTOR, GAUSSIAN_FACTOR ), 0, 0 );
    threshold( image, image, 70, 255, CV_THRESH_BINARY );
}
static void buscarCuadrados( const Mat& image, vector<vector<Point> >& cuadrados ){
    cuadrados.clear();

    // blur will enhance edge detection
    Mat timg(image);
    GaussianBlur( image, timg, Size( GAUSSIAN_FACTOR, GAUSSIAN_FACTOR ), 0, 0 );

    Mat gray0(timg.size(), CV_8U), gray;
    //Mat gray0_not(gray0.size(), CV_8U);

    vector<vector<Point> > contours;

    // find cuadrados in every color plane of the image
    for( int c = 0; c < CANALES; c++ ) //ORIGINAL 3 para canales
    {
        int ch[] = {c, 0};
        mixChannels(&timg, 1, &gray0, 1, ch, 1);

        // try several threshold levels
        for( int l = 0; l < N; l++ )
        {
            // hack: use Canny instead of zero threshold level.
            // Canny helps to catch cuadrados with gradient shading
            if( l == 0 )
            {
                // apply Canny. Take the upper threshold from slider
                // and set the lower to 0 (which forces edges merging)
                if(TECNICA_BORDES == 1){
                    /// Generate grad_x and grad_y
                    int scale = 1;
                    int delta = 0;
                    int ddepth = CV_16S;
                    Mat grad_x, grad_y;
                    Mat abs_grad_x, abs_grad_y;

                    /// Gradient X
                    //Scharr( src_gray, grad_x, ddepth, 1, 0, scale, delta, BORDER_DEFAULT );
                    Sobel( gray0, grad_x, ddepth, 1, 0, 3, scale, delta, BORDER_DEFAULT );
                    convertScaleAbs( grad_x, abs_grad_x );

                    /// Gradient Y
                    //Scharr( src_gray, grad_y, ddepth, 0, 1, scale, delta, BORDER_DEFAULT );
                    Sobel( gray0, grad_y, ddepth, 0, 1, 3, scale, delta, BORDER_DEFAULT );
                    convertScaleAbs( grad_y, abs_grad_y );

                    /// Total Gradient (approximate)
                    addWeighted( abs_grad_x, 0.5, abs_grad_y, 0.5, 0, gray );

                }
                else {
                    bitwise_not(gray0, gray0);
                    Canny(gray0, gray, 10, 20, 3, false);//10,20,3);// 0 10 5
                }
                //bitwise_not(gray, gray);

                // dilate canny output to remove potential
                // holes between edge segments
                dilate(gray, gray, getStructuringElement( MORPH_RECT, Size(3,3))); //Mat(), Point(-1,-1));

                //erode( gray, gray, getStructuringElement( MORPH_RECT, Size(3,3)) );
            }
            else
            {
                // apply threshold if l!=0:
                //     tgray(x,y) = gray(x,y) < (l+1)*255/N ? 255 : 0
                gray = gray0 >= (l+1)*255/N;
            }

            // Encuentra los contornos (mas exteriores si existen otros dentro) y los enlista
            findContours(gray, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE, Point(-1 ,-1));

            vector<Point> approx;

            // test each contour
            for( size_t i = 0; i < contours.size(); i++ )
            {
                // approximate contour with accuracy proportional
                // to the contour perimeter
                approxPolyDP(Mat(contours[i]), approx, arcLength(Mat(contours[i]), true)*0.02, true);
                //convexHull( Mat(contours[i]), approx, true );

                // square contours should have 4 vertices after approximation
                // relatively large area (to filter out noisy contours)
                // and be convex.
                // Note: absolute value of an area is used because
                // area may be positive or negative - in accordance with the
                // contour orientation
                if( approx.size() == 4 && !existeCuadradoSemejante(cuadrados, approx) &&
                    fabs(contourArea(Mat(approx))) > 1000 &&
                    isContourConvex(Mat(approx)) )
                {
                    double maxCosine = 0;

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
                    }
                }
            }
        }
    }
    ordenarCuadradosPorPosicionEspacial(cuadrados, DIRECCION_ORDEN_C);
}
static void dibujarCuadrados( Mat& image, const vector<vector<Point> >& cuadrados ) {

  // __android_log_print(ANDROID_LOG_ERROR, "cuadrados.size --> ", "%i", cuadrados.size());
    //char txt[] = "Cuadrados: ";
    //putText(image, IntToString((int)cuadrados.size()).c_str(), Point(10,MAX_HEIGHT-20), FONT_HERSHEY_SIMPLEX, 1,Scalar(0,0,0),1, LINE_AA, false);

    for (int i = 0; i < cuadrados.size(); i++) {

        const Point *p = &cuadrados[i][0];
        int n = (int) cuadrados[i].size();

        putText(image, IntToString(i+1).c_str(), Point(cuadrados[i][0].x,cuadrados[i][0].y), FONT_HERSHEY_SIMPLEX, 1,Scalar(0,0,0),2 , LINE_AA, false);
        //if (p->x > 3 && p->y > 3 && p->x < MAX_WIDTH -3 && p->y < MAX_HEIGHT-3) {
        //Rect boundRect;
        //boundRect = boundingRect(cuadrados[i]);
        //rectangle(image, boundRect.tl(), boundRect.br(), Scalar(100, 100, 100), 2, 8, 0);
        polylines(image, &p, &n, 1, true, Scalar(0, 255, 0), 2, 16);
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
        for (int n = 0; n < SEGMENTOS; n++) {

            const Point puntoInicial = cuadrados[i][n];
            const Point puntoFinal = cuadrados[i][(n + 1) % SEGMENTOS];
            LineIterator it(image, puntoInicial, puntoFinal, 8, false);
            int razon = (int) round(it.count / SEGMENTOS);

            puntosVerticePorCuadrado.push_back(puntoInicial);

            //Iterando cada segmento del cuadrado para hallar puntos medios y divirlos
            // en cuatro partes iguales
            for (int k = 1; k < it.count; k++, ++it) {
                if (k == 1 || k == it.count /*|| (it.count - k) < 3*/) {;}
                else {
                    if (((k - 1) % razon) == 0) {
                        puntosFronteraPorSegmento.push_back(it.pos());

                        /*__android_log_print(ANDROID_LOG_ERROR, "puntos FRONTERA ptos ---> ", "PTO INI PTO FIN %i %i %i, %i  (%i %i)",
                                            i,n,razon, it.count,it.pos().x, it.pos().y);*/
                        //circle(image, it.pos(), 2, Scalar((255 * k / it.count), 0, 0), 2, 8);
                    }
                }
            }
            puntosFronteraPorCuadrado.push_back(puntosFronteraPorSegmento);
            puntosFronteraPorSegmento.clear();
        }
        /*__android_log_print(ANDROID_LOG_ERROR, "puntos Vertice ---> ", "%i, (%i %i) (%i %i) (%i %i) (%i %i)",i+1,
                            puntosFronteraPorCuadrado[0][0].x, puntosFronteraPorCuadrado[0][0].y,
                            puntosFronteraPorCuadrado[1][0].x, puntosFronteraPorCuadrado[1][0].y,
                            puntosFronteraPorCuadrado[2][0].x, puntosFronteraPorCuadrado[2][0].y,
                            puntosFronteraPorCuadrado[3][0].x, puntosFronteraPorCuadrado[3][0].y);*/
        
        puntosVertices.push_back(puntosVerticePorCuadrado);
        puntosVerticePorCuadrado.clear();

        puntosFrontera.push_back(puntosFronteraPorCuadrado);
        puntosFronteraPorCuadrado.clear();

        //int NPUNTOSFRONTERA = (int)puntosFrontera[0].size();

        //Buscando los puntos internos del cuadrado
        for (int r = 0; r < SEGMENTOS - 1; r++) {
            //Punto del segmento A del cuadrado
            Point puntoInicial = puntosFrontera[i][3][SEGMENTOS - r - 2];

            //Punto del segmento B del cuadrado opuesto a A
            Point puntoFinal = puntosFrontera[i][1][r];
            /*__android_log_print(ANDROID_LOG_ERROR, "puntos internos ptos ---> ", "PTO INI PTO FIN(%i %i) - (%i %i)",
                                puntoInicial.x, puntoInicial.y, puntoFinal.x, puntoFinal.y);*/
            //Iterador que recorre el segmento de linea punto a punto
            LineIterator it(image, puntoInicial, puntoFinal, 8, true);

            int razon = (int) round(it.count / (SEGMENTOS)); //+ 1

            //Recoriendo el segmento punto a punto
            for (int k = 1; k <= it.count; k++, ++it) {
                if (k == 1 || k == it.count /*|| (it.count - k) < 3*/) { ; }
                else {
                    if (((k - 1) % razon) == 0) {
                        puntosInternosPorSegmento.push_back(it.pos());
                        //circle(image, it.pos(), 2, Scalar(0, 0, 255 * k / it.count), 2, 8);
                        //puntos internos ptos --->: 0 1 0 (20 1529) - (111 317)

                    }
                }
            }
            /*__android_log_print(ANDROID_LOG_ERROR, "puntos Vertice ---> ", "%i %i, (%i %i) (%i %i) (%i %i)",i+1, r+1,
                                puntosInternosPorSegmento[0].x, puntosInternosPorSegmento[0].y,
                                puntosInternosPorSegmento[1].x, puntosInternosPorSegmento[1].y,
                                puntosInternosPorSegmento[2].x, puntosInternosPorSegmento[2].y);*/

            //__android_log_print(ANDROID_LOG_ERROR, "puntos frontera ---> ", "%i %i",r+1, puntosInternosPorSegmento.size());
            puntosInternosPorCuadrado.push_back(puntosInternosPorSegmento);
            puntosInternosPorSegmento.clear();
        }
        puntosInternos.push_back(puntosInternosPorCuadrado);
        //__android_log_print(ANDROID_LOG_ERROR, "puntos sq ---> ", "%i %i",i+1, puntosInternos.size());

        puntosInternosPorCuadrado.clear();

        unificarParticiones(i, puntosVertices, puntosFrontera, puntosInternos, particiones);

        //
        //__android_log_print(ANDROID_LOG_ERROR, "particiones ---> ", "%i -> (%i %i)",i, particiones[i].size());

        if (particiones.size() > 0) {
            for (int r = 0; r < particiones[i].size(); r++) {
                const Point *p = &particiones[i][r][0];
                int n = (int) particiones[i][r].size();
                //__android_log_print(ANDROID_LOG_ERROR, "unificarParticiones1 ---> ", "%i, (%i %i)",i, r, n);
                polylines(image, &p, &n, 1, true, Scalar(255 * r / particiones[i].size(), 0, 200),
                          1, 16);
            }
        }


    }

    puntosVertices.clear();
    puntosFrontera.clear();
    puntosInternos.clear();
}
static void unificarParticiones(const int indiceCuadrado,
                               const vector<vector<Point> > & puntosVertices,
                               const vector<vector<vector<Point> > >& puntosFrontera,
                               const vector<vector<vector<Point> > > & puntosInternos,
                               vector<vector<vector<Point> > > & particiones){
    //puntosFrontera MAX 0-2
    //puntosVertices MAX 0-3
    //puntosInternos MAX 0-3
    vector<vector<Point> > particionesPorCuadrado;
    vector<Point> particion; // = puntosFrontera[3];
    //__android_log_print(ANDROID_LOG_ERROR, "unificarParticiones1 ---> ", "(%i %i) (%i %i) (%i %i)", p1[0].x, p1[0].y, p1[1].x, p1[1].y, p1[2].x, p1[2].y);
    /*__android_log_print(ANDROID_LOG_ERROR, "unificarParticiones1 ---> ", "%i, (v-> %i f-> %i i-> %i)",indiceCuadrado,
                        puntosVertices[indiceCuadrado].size(),
                        puntosFrontera[indiceCuadrado].size(),
                        puntosInternos[indiceCuadrado].size());*/
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



    //__android_log_print(ANDROID_LOG_ERROR, "unificarParticiones1 ---> ", "%i, (%i)",indiceCuadrado+1,particiones[indiceCuadrado].size());
}
static void decodificarParticiones( Mat& image, Mat& image_c, vector<vector<vector<Point> > >& particiones){

    if(particiones.size() <= 0 ) return;
    if(particiones[0].size() <= 0 ) return; //!!

    int ANCHO_TRANSF = 50, ALTO_TRANSF = 50, NUM_PARTICIONES = (int)particiones[0].size();
    float porcentajeBlanco = 0;
    vector<Point> puntosBlancos;
    Mat mParticionTransfomada(ANCHO_TRANSF, ALTO_TRANSF, CV_8UC1);
    Point2f origen[4], destino[4] = {Point(0, 0),
                                     Point(ANCHO_TRANSF, 0),
                                     Point(ANCHO_TRANSF, ALTO_TRANSF),
                                     Point(0, ALTO_TRANSF)};
    char mensaje[3][16] = {{'.','.','.','.','.','.','.','.','.','.','.','.','.','.','.','.'},
                           {'.','.','.','.','.','.','.','.','.','.','.','.','.','.','.','.'},
                           {'.','.','.','.','.','.','.','.','.','.','.','.','.','.','.','.'}};

    //strcpy(mensaje, "");

    //Convirtiendo particion a escala de grises
    cvtColor(image_c, image_c, CV_BGR2GRAY);

    //Convirtiendo particion a binaria
    threshold(image_c, image_c, NIVEL_THRESHOLD, 255, CV_THRESH_BINARY);

    for(int i = 0; i < particiones.size(); i++) {
        for (int k = 0; k < NUM_PARTICIONES; k++) {
            origen[0] = particiones[i][k][0];
            origen[1] = particiones[i][k][1];
            origen[2] = particiones[i][k][2];
            origen[3] = particiones[i][k][3];

            Mat mLambda = getPerspectiveTransform(origen, destino);
            warpPerspective(image_c, mParticionTransfomada, mLambda, mParticionTransfomada.size());

            /*Mat mLambda = getPerspectiveTransform(origen, destino);
            warpPerspective(image_c, mParticionTransfomada, mLambda, mParticionTransfomada.size());

            //Convirtiendo particion a escala de grises
            cvtColor(mParticionTransfomada, mParticionTransfomada, CV_BGR2GRAY);

            //Convirtiendo particion a binaria
            threshold(mParticionTransfomada, mParticionTransfomada, NIVEL_THRESHOLD, 255, CV_THRESH_BINARY);*/

            //Busca pixeles blancos en imagen binarizada
            findNonZero(mParticionTransfomada, puntosBlancos);

            float tamanio = (ANCHO_TRANSF * ALTO_TRANSF);

            //Calcula el procentaje de pixeles blancos contra el total de pixeles en la region
            porcentajeBlanco = float((puntosBlancos.size()) * 100.0) / tamanio;

            /*if(i == 1){
                __android_log_print(ANDROID_LOG_ERROR, "findNonZero", "%f %f", (puntosBlancos.size()), porcentajeBlanco);
            }*/

            //mensaje[i % 3][k] = x;

            if (porcentajeBlanco >= TOLERANCIA_LED_ENCENDIDO) {
                mensaje[i % 3][k] = '1'; //[k / 4][k % 4] = '1';
            }
            else{
                mensaje[i % 3][k] = '0'; //[k / 4][k % 4] = '0';
            }
        }
        //__android_log_print(ANDROID_LOG_ERROR, "findNonZero", "%s", string(mensaje[0]).substr(0,4).c_str());
    }
    //cvtColor(image, image, CV_BGR2GRAY);
    //bitwise_not(mRgba, mRgba);
    //threshold(image, image, NIVEL_THRESHOLD, 255, CV_THRESH_BINARY);
    //image = image_c;

    putText(image, string(mensaje[0]).substr(0,4).c_str(), Point(10,MAX_HEIGHT-135), FONT_HERSHEY_DUPLEX, 1,Scalar(0,0,0), 1, LINE_4, false);
    putText(image, string(mensaje[0]).substr(4,4).c_str(), Point(10,MAX_HEIGHT-105), FONT_HERSHEY_DUPLEX, 1,Scalar(0,0,0), 1, LINE_4, false);
    putText(image, string(mensaje[0]).substr(8,4).c_str(), Point(10,MAX_HEIGHT-75), FONT_HERSHEY_DUPLEX, 1,Scalar(0,0,0), 1, LINE_4, false);
    putText(image, string(mensaje[0]).substr(12,4).c_str(), Point(10,MAX_HEIGHT-45), FONT_HERSHEY_DUPLEX, 1,Scalar(0,0,0), 1, LINE_4, false);

    putText(image, string(mensaje[1]).substr(0,4).c_str(), Point(120,MAX_HEIGHT-135), FONT_HERSHEY_DUPLEX, 1,Scalar(0,0,0), 1, LINE_4, false);
    putText(image, string(mensaje[1]).substr(4,4).c_str(), Point(120,MAX_HEIGHT-105), FONT_HERSHEY_DUPLEX, 1,Scalar(0,0,0), 1, LINE_4, false);
    putText(image, string(mensaje[1]).substr(8,4).c_str(), Point(120,MAX_HEIGHT-75), FONT_HERSHEY_DUPLEX, 1,Scalar(0,0,0), 1, LINE_4, false);
    putText(image, string(mensaje[1]).substr(12,4).c_str(), Point(120,MAX_HEIGHT-45), FONT_HERSHEY_DUPLEX, 1,Scalar(0,0,0), 1, LINE_4, false);

    putText(image, string(mensaje[2]).substr(0,4).c_str(), Point(240,MAX_HEIGHT-135), FONT_HERSHEY_DUPLEX, 1,Scalar(0,0,0), 1, LINE_4, false);
    putText(image, string(mensaje[2]).substr(4,4).c_str(), Point(240,MAX_HEIGHT-105), FONT_HERSHEY_DUPLEX, 1,Scalar(0,0,0), 1, LINE_4, false);
    putText(image, string(mensaje[2]).substr(8,4).c_str(), Point(240,MAX_HEIGHT-75), FONT_HERSHEY_DUPLEX, 1,Scalar(0,0,0), 1, LINE_4, false);
    putText(image, string(mensaje[2]).substr(12,4).c_str(), Point(240,MAX_HEIGHT-45), FONT_HERSHEY_DUPLEX, 1,Scalar(0,0,0), 1, LINE_4, false);

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
    }
    cuadradosOrdenados.clear();

    //return;
}


static double calcularAnguloEntreDosPuntos( Point pt1, Point pt2, Point pt0){
    double dx1 = pt1.x - pt0.x;
    double dy1 = pt1.y - pt0.y;
    double dx2 = pt2.x - pt0.x;
    double dy2 = pt2.y - pt0.y;
    return (dx1*dx2 + dy1*dy2)/sqrt((dx1*dx1 + dy1*dy1)*(dx2*dx2 + dy2*dy2) + 1e-10);
}

bool existeCuadradoSemejante(const vector<vector<Point> >& cuadrados, vector<Point>& approx){
    if(approx.size() <= 0) return true;

    int TOLERANCIA = 20;

    ordenarPuntosCuadrado(approx);

    const Point* q0 = &approx[0];
    const Point* q1 = &approx[1];
    const Point* q2 = &approx[2];
    const Point* q3 = &approx[3];

    //Discriminando cuadrados formados por el borde de la imagen
    if((q0->x <= TOLERANCIA && q0->y <= TOLERANCIA) ||
       (q1->x >= MAX_WIDTH-TOLERANCIA && q1->y <= TOLERANCIA) ||
       (q2->x >= MAX_WIDTH - TOLERANCIA && q2->y >= MAX_HEIGHT-TOLERANCIA) ||
       (q3->x <= TOLERANCIA && q3->y >= MAX_HEIGHT-TOLERANCIA)) {

        return true;
    }

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
    return false;
}
static void ordenarPuntosCuadrado(vector<Point> &verticesCuadrado){
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

}

string IntToString (int a)
{
    ostringstream tmp;
    tmp << a;
    return tmp.str();
}

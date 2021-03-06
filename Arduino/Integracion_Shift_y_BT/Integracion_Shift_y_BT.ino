#include <SoftwareSerial.h>
SoftwareSerial BT1(4,2); // RX | TX
char c[5];
String s=" ";
int latchPin=8;
int clockPin=12;
int dataPin=11;
int espacio=256;
void setup() {
  pinMode(latchPin,OUTPUT);
  pinMode(clockPin,OUTPUT);
  pinMode(dataPin,OUTPUT);
  // put your setup code here, to run once:
  Serial.begin(9600);
  Serial.println("Arduino is ready");
  BT1.begin(9600);
  Serial.println("BTserial started at 9600");
}
void loop() {
  if(!BT1.available()){
      pintarVacio(1000);
    }
    else{
      /*************PARPADEO 1***********/
      pintarLleno(5000);
      pintarVacio(2000);
      s= GetLineBT();
      c=procesarCadena(s);
      Serial.println(s);
      s.toCharArray(c,5);
      /*************PARPADEO 2**********/
      digitalWrite(latchPin, 0) ; // Latch a LOW para que no
      /////M3
      shiftOut(dataPin, clockPin, LSBFIRST, espacio*0+c[5]);
      shiftOut(dataPin, clockPin, LSBFIRST, espacio*1+c[4]);
      /////M2
      shiftOut(dataPin, clockPin, LSBFIRST, espacio*2+c[3]);
      shiftOut(dataPin, clockPin, LSBFIRST, espacio*3+c[2]);
      /////M1
      shiftOut(dataPin, clockPin, LSBFIRST, espacio*4+c[1]);
      shiftOut(dataPin, clockPin, LSBFIRST, espacio*5+c[0]);
      digitalWrite(latchPin, 1);
      delay(5000);
      pintarVacio(2000);
      /*************PARPADEO 3**********/
      digitalWrite(latchPin, 0) ; // Latch a LOW para que no
      /////M3
      shiftOut(dataPin, clockPin, LSBFIRST, espacio*0+c[3]);
      shiftOut(dataPin, clockPin, LSBFIRST, espacio*1+c[2]);
      /////M2
      shiftOut(dataPin, clockPin, LSBFIRST, espacio*2+c[1]);
      shiftOut(dataPin, clockPin, LSBFIRST, espacio*3+c[0]);
      /////M1
      shiftOut(dataPin, clockPin, LSBFIRST, espacio*4+c[5]);
      shiftOut(dataPin, clockPin, LSBFIRST, espacio*5+c[4]);
      digitalWrite(latchPin, 1);
      delay(5000);
      pintarVacio(2000);
      /*************PARPADEO 4**********/
      digitalWrite(latchPin, 0) ; // Latch a LOW para que no
      /////M3
      shiftOut(dataPin, clockPin, LSBFIRST, espacio*0+c[1]);
      shiftOut(dataPin, clockPin, LSBFIRST, espacio*1+c[0]);
      /////M2
      shiftOut(dataPin, clockPin, LSBFIRST, espacio*2+c[5]);
      shiftOut(dataPin, clockPin, LSBFIRST, espacio*3+c[4]);
      /////M1
      shiftOut(dataPin, clockPin, LSBFIRST, espacio*4+c[3]);
      shiftOut(dataPin, clockPin, LSBFIRST, espacio*5+c[2]);
      digitalWrite(latchPin, 1);
      delay(5000);
      pintarVacio(2000);
    }
    if (Serial.available()){
      String S = GetLine();
      BT1.print("BT: "+S);
      Serial.println("---> " + S);
      }
}
String GetLine(){
  String S = "" ;
  if(Serial.available()){
    char c = Serial.read();
    while ( c != '\n'){//Hasta que el caracter sea intro     
      S = S + c;
      delay(25);
      c = Serial.read();
    }
    return(S + '\n');
  }
}
String GetLineBT(){
  String S = "" ;
  if (BT1.available()){
    char c = BT1.read();
    while ( c != '\n'){           //Hasta que el caracter sea intro
      S = S + c;
      delay(25);
      c = BT1.read();
    }
  return(S) ;
  }
}
void pintarVacio(int tiempo){
  digitalWrite(latchPin, 0);
  shiftOut(dataPin, clockPin, LSBFIRST,espacio*0);
  shiftOut(dataPin, clockPin, LSBFIRST,espacio*1);
  shiftOut(dataPin, clockPin, LSBFIRST,espacio*2);
  shiftOut(dataPin, clockPin, LSBFIRST,espacio*3);
  digitalWrite(latchPin, 1);
  delay(tiempo);
}
void pintarLleno(int tiempo){
  digitalWrite(latchPin, 0);
  shiftOut(dataPin, clockPin, LSBFIRST,espacio*0-1);
  shiftOut(dataPin, clockPin, LSBFIRST,espacio*1-1);
  shiftOut(dataPin, clockPin, LSBFIRST,espacio*2-1);
  shiftOut(dataPin, clockPin, LSBFIRST,espacio*3-1);
  digitalWrite(latchPin, 1);
  delay(tiempo);
}
int[6] procesarInfo(String datos){
  ///es string siempre enviara 12 bytes 
  //los 4 primeros son id_materia que se convertiran en 2 bytes
  //los 4 siguientes son id_profesor que se convertiran en 2 bytes
  //los 2 siguientes son id_paralelo  que se convertira en 1 byte
  //los 2 siguientes son aula  que se convertira en 1 byte
  char[6] resultado;
  resultado[0]=datos.charAt(0)<<4+datos.charAt(1);
  resultado[1]=datos.charAt(2)<<4+datos.charAt(3);
  resultado[2]=datos.charAt(4)<<4+datos.charAt(5);
  resultado[3]=datos.charAt(6)<<4+datos.charAt(7);
  resultado[4]=datos.charAt(8)<<4+datos.charAt(9);
  resultado[5]=datos.charAt(10)<<4+datos.charAt(11);
  return resultado;
}

int calculoDesplazamiento(int num1EnChar){
  int resultado=0;
  if(num1EnChar>=10 && num1EnChar<=99){
    int decena=num1EnChar/10;
    int unidad=num1EnChar%10;
    resultado=decena<<4+unidad;
    return resultado;
  }
}


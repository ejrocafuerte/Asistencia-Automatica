#include <SoftwareSerial.h>
SoftwareSerial BT1(4, 2); // RX | TX
char c[9];
byte m[4];
//byte m[6];
String s = " ";
int latchPin = 8;
int clockPin = 12;
int dataPin = 11;
int espacio = 256;
void setup() {
  pinMode(latchPin, OUTPUT);
  pinMode(clockPin, OUTPUT);
  pinMode(dataPin, OUTPUT);
  // put your setup code here, to run once:
  Serial.begin(9600);
  Serial.println("Arduino is ready");
  BT1.begin(9600);
  Serial.println("BTserial started at 9600");
}
void loop() {
  //
  //pintarVacio(1000);
  if (!BT1.available()) {
  }
  else {
    /*************PARPADEO 1***********/
    BT1.println("OK");
    s = GetLineBT();
    ///es string siempre enviara 12 bytes
    //los 4 primeros son id_materia que se convertiran en 2 bytes
    //los 4 siguientes son id_profesor que se convertiran en 2 bytes
    //byte(c[0]<<4)+(byte(c[1])-48) codigo apto para mostrarse en matriz sin desbordamiento
    Serial.println(s);
    s.toCharArray(c, 9);
    m[0] = byte(c[0] << 4) + (byte(c[1]) - 48);
    m[1] = byte(c[2] << 4) + (byte(c[3]) - 48);
    m[2] = byte(c[4] << 4) + (byte(c[5]) - 48);
    m[3] = byte(c[6] << 4) + (byte(c[7]) - 48);
    Serial.println(m[0]);
    Serial.println(m[1]);
    Serial.println(m[2]);
    Serial.println(m[3]);
    for(int i=0;i<50;i++){
      pintarLleno(1000);
    pintarVacio(1000);

    /*************PARPADEO 2*********/
    digitalWrite(latchPin, 0) ; // Latch a LOW para que no
    /////M2
    shiftOut(dataPin, clockPin, LSBFIRST, espacio*0+m[3]);
    shiftOut(dataPin, clockPin, LSBFIRST, espacio*1+m[2]);
    /////M1
    shiftOut(dataPin, clockPin, LSBFIRST, espacio * 2 + m[1]);
    shiftOut(dataPin, clockPin, LSBFIRST, espacio * 3 + m[0]);
    digitalWrite(latchPin, 1);
    delay(1000);
    pintarVacio(1000);
    /*************PARPADEO 3**********/
    digitalWrite(latchPin, 0) ; // Latch a LOW para que no
    /////M2
    shiftOut(dataPin, clockPin, LSBFIRST, espacio * 0 + m[1]);
    shiftOut(dataPin, clockPin, LSBFIRST, espacio * 1 + m[0]);
    /////M1
    shiftOut(dataPin, clockPin, LSBFIRST, espacio * 2 + m[3]);
    shiftOut(dataPin, clockPin, LSBFIRST, espacio * 3 + m[2]);
    digitalWrite(latchPin, 1);
    delay(1000);
    pintarVacio(1000);
    }
    BT1.println("OK");
  }
  
  if (Serial.available()) {
    String S = GetLineBT();
    //BT1.print("BT: " + S);
    BT1.print("OK");
  }
}
String GetLine() {
  String S = "" ;
  if (Serial.available()) {
    char c = Serial.read();
    while ( c != '\n') { //Hasta que el caracter sea intro
      S = S + c;
      delay(25);
      c = Serial.read();
    }
    return (S + '\n');
  }
}
String GetLineBT() {
  String S = "" ;
  if (BT1.available()) {
    char c = BT1.read();
    while ( c != '\n') {          //Hasta que el caracter sea intro
      S = S + c;
      delay(25);
      c = BT1.read();
    }
    return (S) ;
  }
}
void pintarVacio(int tiempo) {
  digitalWrite(latchPin, 0);
  shiftOut(dataPin, clockPin, LSBFIRST, espacio * 0);
  shiftOut(dataPin, clockPin, LSBFIRST, espacio * 1);
  shiftOut(dataPin, clockPin, LSBFIRST, espacio * 2);
  shiftOut(dataPin, clockPin, LSBFIRST, espacio * 3);
  digitalWrite(latchPin, 1);
  delay(tiempo);
}
void pintarLleno(int tiempo) {
  digitalWrite(latchPin, 0);
  shiftOut(dataPin, clockPin, LSBFIRST, espacio * 0 - 1);
  shiftOut(dataPin, clockPin, LSBFIRST, espacio * 1 - 1);
  shiftOut(dataPin, clockPin, LSBFIRST, espacio * 2 - 1);
  shiftOut(dataPin, clockPin, LSBFIRST, espacio * 3 - 1);
  digitalWrite(latchPin, 1);
  delay(tiempo);
}/*
  void procesarInfo(String datos){  ///es string siempre enviara 12 bytes
  //los 4 primeros son id_materia que se convertiran en 2 bytes
  //los 4 siguientes son id_profesor que se convertiran en 2 bytes
  char[4] r;
  r[0]=datos.charAt(0)<<4+datos.charAt(1);
  r[1]=datos.charAt(2)<<4+datos.charAt(3);
  r[2]=datos.charAt(4)<<4+datos.charAt(5);
  r[3]=datos.charAt(6)<<4+datos.charAt(7);
  resultado=r;
  //return resultado;
  }*/
 

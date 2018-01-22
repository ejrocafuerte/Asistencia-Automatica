#include <SoftwareSerial.h>
SoftwareSerial BT1(4,2); // RX | TX
char c[5];
int encendido=2000;
int apagado=1000;
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
	/*************PARPADEO 1***********/
    s = GetLineBT();
    ///es string siempre enviara 12 bytes
    //los 4 primeros son id_materia que se convertiran en 2 bytes
    //los 4 siguientes son id_profesor que se convertiran en 2 bytes
    //byte(c[0]<<4)+(byte(c[1])-48) codigo apto para mostrarse en matriz sin desbordamiento
    Serial.println(s);
	BT1..println("Mensaje recibido, mostrando en matriz.\n");
    s.toCharArray(c, 9);
    m[0] = byte(c[0] << 4) + (byte(c[1]) - 48);
    m[1] = byte(c[2] << 4) + (byte(c[3]) - 48);
    m[2] = byte(c[4] << 4) + (byte(c[5]) - 48);
    m[3] = byte(c[6] << 4) + (byte(c[7]) - 48);
    Serial.println(m[0]);
    Serial.println(m[1]);
    Serial.println(m[2]);
    Serial.println(m[3]);
    for(int i=0;i<10;i++){
      pintarLleno(encendido);
    pintarVacio(apagado);
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
      delay(encendido);
      pintarVacio(apagado);
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
      delay(encendido);
      pintarVacio(apagado);
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
	  delay(encendido);
      pintarVacio(apagado);
    }
    BT1.println("OK\n");
      delay(encendido);
      pintarVacio(apagado);
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


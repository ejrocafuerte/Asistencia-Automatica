//formato de paso de mensaje a bluetooth
//mmmmppppaauu\n
//mmmm materia 4 digitos
//pppp profesor 4 digitos
//aa paralelo de esa materia que dicta el profesor
//uu aula donde se dicta esa clase
#include <SoftwareSerial.h>
SoftwareSerial BT1(4, 2); // RX | TX
byte m[6];
int encendido = 600;
int apagado = 100;
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
void pintarVacio(int tiempo) {
  digitalWrite(latchPin, 0);
  shiftOut(dataPin, clockPin, LSBFIRST, espacio * 0);
  shiftOut(dataPin, clockPin, LSBFIRST, espacio * 1);
  shiftOut(dataPin, clockPin, LSBFIRST, espacio * 2);
  shiftOut(dataPin, clockPin, LSBFIRST, espacio * 3);
  shiftOut(dataPin, clockPin, LSBFIRST, espacio * 4);
  shiftOut(dataPin, clockPin, LSBFIRST, espacio * 5);
  digitalWrite(latchPin, 1);
  delay(tiempo);
}
void pintarLleno(int tiempo) {
  digitalWrite(latchPin, 0);
  shiftOut(dataPin, clockPin, LSBFIRST, espacio * 0 - 1);
  shiftOut(dataPin, clockPin, LSBFIRST, espacio * 1 - 1);
  shiftOut(dataPin, clockPin, LSBFIRST, espacio * 2 - 1);
  shiftOut(dataPin, clockPin, LSBFIRST, espacio * 3 - 1);
  shiftOut(dataPin, clockPin, LSBFIRST, espacio * 4 - 1);
  shiftOut(dataPin, clockPin, LSBFIRST, espacio * 5 - 1);
  digitalWrite(latchPin, 1);
  delay(tiempo);
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
void procesarInfo(String datos) {
  ///es string siempre enviara 12 bytes
  //los 4 primeros son id_materia que se convertiran en 2 bytes
  //los 4 siguientes son id_profesor que se convertiran en 2 bytes
  //los 2 siguientes son id_paralelo  que se convertira en 1 byte
  //los 2 siguientes son aula  que se convertira en 1 byte
  //la primera parte datos.charAt(10) << 4 tomo el caracter inicial y lo desplazo 4 espacios ()bits
  //la segunda parte se resta 48 00110000 para que queden solo los 4 ultimos bits
  //se suman (3) o 0011000 con 4 o 00000100
  char c[13];
  s.toCharArray(c, 13);
  m[0] = byte(c[0] << 4) + (byte(c[1]) - 48);
  m[1] = byte(c[2] << 4) + (byte(c[3]) - 48);
  m[2] = byte(c[4] << 4) + (byte(c[5]) - 48);
  m[3] = byte(c[6] << 4) + (byte(c[7]) - 48);
  m[4] = byte(c[8] << 4) + (byte(c[9]) - 48);
  m[5] = byte(c[10] << 4) + (byte(c[11]) - 48);
  //return resultado;
}

void loop() {
  if (!BT1.available()) {
    pintarVacio(1000);
  } else {
    /*************PARPADEO 1***********/
    s = GetLineBT();
    ///es string siempre enviara 12 bytes
    //los 4 primeros son id_materia que se convertiran en 2 bytes
    //los 4 siguientes son id_profesor que se convertiran en 2 bytes
    //byte(c[0]<<4)+(byte(c[1])-48) codigo apto para mostrarse en matriz sin desbordamiento
    //s="012345678912\n";
    Serial.println(s);
    BT1.println("Mensaje recibido, mostrando en matriz.\n");
    procesarInfo(s);
    Serial.println(m[0]);
    Serial.println(m[1]);
    Serial.println(m[2]);
    Serial.println(m[3]);
    Serial.println(m[4]);
    Serial.println(m[5]);
    for (int i = 0; i < 1; i++) {
      pintarLleno(encendido);
      pintarVacio(apagado);
      /*************PARPADEO 2**********/
      digitalWrite(latchPin, 0) ; // Latch a LOW para que no
      /////M3
      shiftOut(dataPin, clockPin, LSBFIRST, espacio * 0 + m[5]);
      shiftOut(dataPin, clockPin, LSBFIRST, espacio * 1 + m[4]);
      /////M2
      shiftOut(dataPin, clockPin, LSBFIRST, espacio * 2 + m[3]);
      shiftOut(dataPin, clockPin, LSBFIRST, espacio * 3 + m[2]);
      /////M1
      shiftOut(dataPin, clockPin, LSBFIRST, espacio * 4 + m[1]);
      shiftOut(dataPin, clockPin, LSBFIRST, espacio * 5 + m[0]);
      digitalWrite(latchPin, 1);
      delay(encendido);
      pintarVacio(apagado);
      /*************PARPADEO 3**********/
      digitalWrite(latchPin, 0) ; // Latch a LOW para que no
      /////M3
      shiftOut(dataPin, clockPin, LSBFIRST, espacio * 0 + m[3]);
      shiftOut(dataPin, clockPin, LSBFIRST, espacio * 1 + m[2]);
      /////M2
      shiftOut(dataPin, clockPin, LSBFIRST, espacio * 2 + m[1]);
      shiftOut(dataPin, clockPin, LSBFIRST, espacio * 3 + m[0]);
      /////M1
      shiftOut(dataPin, clockPin, LSBFIRST, espacio * 4 + m[5]);
      shiftOut(dataPin, clockPin, LSBFIRST, espacio * 5 + m[4]);
      digitalWrite(latchPin, 1);
      delay(encendido);
      pintarVacio(apagado);
      /*************PARPADEO 4**********/
      digitalWrite(latchPin, 0) ; // Latch a LOW para que no
      /////M3
      shiftOut(dataPin, clockPin, LSBFIRST, espacio * 0 + m[1]);
      shiftOut(dataPin, clockPin, LSBFIRST, espacio * 1 + m[0]);
      /////M2
      shiftOut(dataPin, clockPin, LSBFIRST, espacio * 2 + m[5]);
      shiftOut(dataPin, clockPin, LSBFIRST, espacio * 3 + m[4]);
      /////M1
      shiftOut(dataPin, clockPin, LSBFIRST, espacio * 4 + m[3]);
      shiftOut(dataPin, clockPin, LSBFIRST, espacio * 5 + m[2]);
      digitalWrite(latchPin, 1);
      delay(encendido);
      pintarVacio(apagado);
    }
    BT1.println("OK\n");
    delay(encendido);
    pintarVacio(apagado);
    if (Serial.available()) {
      String S = GetLine();
      BT1.print("BT: " + S);
      Serial.println("---> " + S);
    }
  }
}

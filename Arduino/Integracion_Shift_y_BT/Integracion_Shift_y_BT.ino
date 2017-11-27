#include <SoftwareSerial.h>
SoftwareSerial BT1(4,2); // RX | TX
char c[8];
String s=" ";
int latchPin=8;
int clockPin=12;
int dataPin=11;
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
    if(BT1.available()){
      s= GetLineBT();
      Serial.println(s);
      s.toCharArray(c,8);
      digitalWrite(latchPin, LOW) ; // Latch a LOW para que no
      shiftOut(dataPin, clockPin, LSBFIRST, c[0]);  // Aqui va Num
      shiftOut(dataPin, clockPin, LSBFIRST, c[1]);  // Aqui va Num
      shiftOut(dataPin, clockPin, LSBFIRST, c[2]);  // Aqui va Num
      digitalWrite(latchPin, HIGH) ; // Latch a HIGH fija valores en la salida
      delay(1000);
      digitalWrite(latchPin, LOW) ; // Latch a LOW para que no
      shiftOut(dataPin, clockPin, LSBFIRST, c[3]);  // Aqui va Num
      shiftOut(dataPin, clockPin, LSBFIRST, c[4]);  // Aqui va Num
      shiftOut(dataPin, clockPin, LSBFIRST, c[5]);  // Aqui va Num
      digitalWrite(latchPin, HIGH) ; // Latch a HIGH fija valores en la salida
      delay(1000);
      digitalWrite(latchPin, LOW) ; // Latch a LOW para que no
      shiftOut(dataPin, clockPin, LSBFIRST, c[6]);  // Aqui va Num
      shiftOut(dataPin, clockPin, LSBFIRST, c[7]);  // Aqui va Num
      shiftOut(dataPin, clockPin, LSBFIRST, c[8]);  // Aqui va Num
      digitalWrite(latchPin, HIGH) ; // Latch a HIGH fija valores en la salida
    }
    if (Serial.available()){
      String S = GetLine();
      BT1.print("BT: "+S);
      Serial.println("---> " + S);
      }
}
String GetLine()
   {   String S = "" ;
       if (Serial.available())
          {    char c = Serial.read(); ;
                while ( c != '\n')            //Hasta que el caracter sea intro
                  {     S = S + c ;
                        delay(25) ;
                        c = Serial.read();
                  }
                return( S + '\n') ;
          }
   }
String GetLineBT()
   {   String S = "" ;
       if (BT1.available())
          {    char c = BT1.read();
                while ( c != '\n')           //Hasta que el caracter sea intro
                  {     S = S + c ;
                        delay(25) ;
                        c = BT1.read();
                  }
                return( S) ;
          }
   }

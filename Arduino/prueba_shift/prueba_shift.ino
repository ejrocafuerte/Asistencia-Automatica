#include <SoftwareSerial.h>
SoftwareSerial BT1(4,2); // RX | TX
char c = ' ';
int latchPin=8;
int clockPin=12;
int dataPin=11;
 
void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  Serial.println("Arduino is ready");
  BT1.begin(9600);
  Serial.println("BTserial started at 9600");
  pinMode(latchPin,OUTPUT);
  pinMode(clockPin,OUTPUT);
  pinMode(dataPin,OUTPUT);
  
}

void loop() {
  // put your main code here, to run repeatedly:
  if (BT1.available())
  Serial.write(BT1.read());
  if (Serial.available())
  {
    String S = GetLine();
    BT1.print(S);
    Serial.println("---> " + S);
    digitalWrite(latchPin, LOW) ; // Latch a LOW para que no varÃ­e la salida
    shiftOut(dataPin, clockPin, LSBFIRST, S);  // Aqui va Num
    digitalWrite(latchPin, HIGH) ; // Latch a HIGH fija valores en la salida
    delay(500);
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



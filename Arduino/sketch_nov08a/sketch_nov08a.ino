#include <SoftwareSerial.h>
SoftwareSerial BT1(2, 3); // RX | TX
void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  Serial.println("Enter AT commands:");
  BT1.begin(9600);
}

void loop() {
  // put your main code here, to run repeatedly:
  if (BT1.available()){
    Serial.write(BT1.read());
    }
  if (Serial.available()){
    String S = GetLine();
    BT1.print(S);
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

#include <SoftwareSerial.h>
SoftwareSerial BT1(4,2); // RX | TX
char c = ' ';
 
void setup() {
    Serial.begin(9600);
    Serial.println("Arduino is ready");
    BT1.begin(9600);  
    Serial.println("BTserial started at 9600");
}
 
void loop(){
    if (BT1.available()){
      c=BT1.read();
           //Serial.println(BT1.read(),BIN);
           Serial.println(c,BIN);
    }    
       if (Serial.available())
          {  String S = GetLine();
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

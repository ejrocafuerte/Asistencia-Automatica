#include <Servo.h>
Servo srvo;
int const potPin = A0;
int potVal;
int angle;
void setup() {
  // put your setup code here, to run once:
  srvo.attach(9);
  Serial.begin(9600);
}

void loop() {
  // put your main code here, to run repeatedly:
  potVal=analogRead(potPin);
  Serial.print("potVal: ");
  Serial.print(potVal);
  angle = map(potVal,0,1023,0,179);
  Serial.print(" ,angulo: ");
  Serial.println(angle);
  srvo.write(angle);
  delay(150);
}

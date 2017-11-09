const int sensorPin = A0;
const float baseline=28.0;
void setup() {
  Serial.begin(9600);

  for(int pinNumber=4; pinNumber<7; pinNumber++){
    pinMode(pinNumber,OUTPUT);
    digitalWrite(pinNumber,LOW);
  }
  // put your setup code here, to run once:

}

void loop() {
  // put your main code here, to run repeatedly:
  int sensorVal = analogRead(sensorPin);
  Serial.print("Sensor Value: ");
  Serial.print(sensorVal);
  float voltage = (sensorVal/1024.0)*5.0;
  Serial.print(", Voltage: ");
  Serial.print(voltage);

  Serial.print(", Grados: ");
  float t = (voltage- .5)*100;
  Serial.println(t);

  if(t<baseline+.5){
    digitalWrite(4,LOW);
    digitalWrite(5,LOW);
    digitalWrite(6,LOW);
  }else if(t>=baseline+.5 && t<baseline+1){
    digitalWrite(4,HIGH);
    digitalWrite(5,LOW);
    digitalWrite(6,LOW);
  }else if(t>=baseline+1 && t<baseline+1.5){
    digitalWrite(4,HIGH);
    digitalWrite(5,HIGH);
    digitalWrite(6,LOW);
  }else if(t>=baseline+1.5){
    digitalWrite(4,HIGH);
    digitalWrite(5,HIGH);
    digitalWrite(6,HIGH);
  }
  delay(250);
}

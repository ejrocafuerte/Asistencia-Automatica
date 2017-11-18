void setup() {
  // put your setup code here, to run once:
  pinMode(6,OUTPUT);
  pinMode(7,OUTPUT);

}

void loop() {
  // put your main code here, to run repeatedly:
  digitalWrite(6,HIGH);
  digitalWrite(7,LOW);
  delay(1500);
  digitalWrite(6,LOW);
  digitalWrite(7,HIGH);
  delay(1500);
  digitalWrite(6,HIGH);
  digitalWrite(7,HIGH);
  delay(1500);
}

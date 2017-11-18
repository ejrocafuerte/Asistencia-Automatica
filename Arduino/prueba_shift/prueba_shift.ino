int latchPin=8;
int clockPin=12;
int dataPin=11;
 
void setup() {
  // put your setup code here, to run once:
  pinMode(latchPin,OUTPUT);
  pinMode(clockPin,OUTPUT);
  pinMode(dataPin,OUTPUT);
}

void loop() {
  // put your main code here, to run repeatedly:
  for (int Num = 0; Num < 256; Num++){
    digitalWrite(latchPin, LOW) ; // Latch a LOW para que no varÃ­e la salida
    shiftOut(dataPin, clockPin, LSBFIRST, Num);  // Aqui va Num
    digitalWrite(latchPin, HIGH) ; // Latch a HIGH fija valores en la salida
    delay(500);
    }
}



#include <SoftwareSerial.h>
#include <Thread.h>
#include <OneWire.h>
#include <DallasTemperature.h>

float			valueCurrentF;
float			valueVoltageF;
unsigned short	valueCurrent = 0;
unsigned short	valueVoltage = 0;
unsigned short	valueBatteryState = 0;
unsigned short	valueTemperature = 1234;
unsigned short	valueLedPwm = 0;
unsigned short	valueButtonState = 0;

unsigned char	dataTmpBuzzerMode = 0;
unsigned char	dataTmpBuzzerIndex = 1;
boolean			dataTmpBuzzerIsRun = false;
unsigned short	dataTmpSystemState = 0;
unsigned short	dataTmpLedMode = 0;
unsigned short	dataTmpLedModeOffIndex = 0;
boolean			dataTmpLedModeBack = false;
boolean			dataTmpLedIsOff = false;
boolean			dataTmpButtonIsTouch = false;
boolean			dataTmpWelcome = true;
unsigned short	dataTmpCapacityTime = 0;
boolean			dataTmpBleOff = false;

const char		pinCurrent = A0;
const char		pinVoltage = A1;
const char		pinBatteryTemp = 2;
const char		pinBatteryState = 4;
const char		pinSystemStateG = 3;
const char		pinSystemStateR = A2;
const char		pinBleState = 5;
const char		pinBleTx = 6;
const char		pinBleRx = 7;
const char		pinBleEn = 8;
const char		pinLedPwm = 9;
const char		pinButton = 10;
const char		pinBuzzer = 12;

const float		ADC_RESOLUTION = 5.0f / 1024.0f;
const float		ADC_RESISTIVE = (100000.0f + 47000.0f) / 47000.0f;

SoftwareSerial SerialBle(pinBleTx, pinBleRx);

OneWire oneWire(pinBatteryTemp);
DallasTemperature sensors(&oneWire);

Thread threadSetSerialMOSI = Thread();
Thread threadSetBuzzer = Thread();
Thread threadSetBleOff = Thread();
Thread threadCapacity = Thread();

void setup() {

	Serial.begin(9600);
	SerialBle.begin(9600);

	pinMode(pinBleEn, OUTPUT);
	pinMode(pinBatteryState, INPUT_PULLUP);
	pinMode(pinSystemStateG, OUTPUT);
	pinMode(pinSystemStateR, OUTPUT);
	pinMode(pinBleState, INPUT_PULLUP);
	pinMode(pinBleEn, OUTPUT);
	pinMode(pinLedPwm, OUTPUT);
	pinMode(pinButton, INPUT_PULLUP);
	pinMode(pinBuzzer, OUTPUT);

	digitalWrite(pinSystemStateG, HIGH);
	digitalWrite(pinSystemStateR, HIGH);

	/* Temperature */
	sensors.begin();

	/* Thread */
	threadSetSerialMOSI.onRun(setSerialMOSI);
	threadSetSerialMOSI.setInterval(500);
	threadSetBuzzer.onRun(setBuzzerMode);

	threadCapacity.setInterval(500);
	threadCapacity.onRun(setCapacity);
}

void loop() {

	if (dataTmpWelcome) {
		dataTmpWelcome = false;
		tone(pinBuzzer, 2400, 50);
	}

	getDigitalReadValue();
	updataSerialMISO();
	updataSerialMOSI();
	updataBuzzer();
	updataCapacity();
}

int bufferIndex = 0;
const int bufferSize = 16;
int bufferContainer[bufferSize];
void updataSerialMISO() {
	// 4D 49 00 08 6D

	// Read Packet
	while (SerialBle.available() > 0)
		bufferContainer[bufferIndex++] = SerialBle.read();

	// Reast Buffer Index
	if (bufferIndex > (sizeof(bufferContainer) - 1))
		bufferIndex = 0;

	// Analysis Packet
	for (int i = 0; i < sizeof(bufferContainer); i++) {
		if (bufferContainer[i] == 0x4D && bufferContainer[i + 1] == 0x49 && bufferContainer[i + 4] == 0x6D) {

			setLedPwm(bufferContainer[i + 2]);

			//Serial.print(" >> ");
			//Serial.println(+ bufferContainer[i + 2]);
			//
			//Serial.println("--");
			//for (int j = i; j < (i + 5); j++) {
			//	Serial.print(" ");
			//	Serial.print(bufferContainer[j], HEX);
			//}

			// Debug
			setSystemState(1);
			// setBuzzer(0);

			// Reast Buffer Container
			memset(bufferContainer, 0x00, sizeof(bufferContainer));
			bufferIndex = 0;
			// break;
		}
	}
}

void updataSerialMOSI() {
	if (threadSetSerialMOSI.shouldRun() && !dataTmpBleOff)
		threadSetSerialMOSI.run();
}

void updataBuzzer() {
	if (dataTmpBuzzerIsRun) {
		if (threadSetBuzzer.shouldRun())
			threadSetBuzzer.run();
		if (dataTmpBuzzerIndex == 0)
			dataTmpBuzzerIsRun = false;
	}
}

void updataCapacity() {
	if (threadCapacity.shouldRun())
		threadCapacity.run();
}

/* Analog Read Value */
void getAnalogReadValue() {
	valueCurrent = analogRead(pinCurrent);
	valueVoltage = analogRead(pinVoltage);
}

/* Digital Read Value */
void getDigitalReadValue() {

	// Serial.println(digitalRead(pinBleState));

	if (valueBatteryState != digitalRead(pinBatteryState)) 
		valueBatteryState = digitalRead(pinBatteryState);

	if (digitalRead(pinButton) == HIGH) {
		valueButtonState = 1;

		if (!dataTmpButtonIsTouch) {
			dataTmpButtonIsTouch = true;
		}
		else {
			dataTmpLedModeOffIndex++;
			if (dataTmpLedModeOffIndex > 8000) {
				dataTmpLedModeOffIndex = 0;
				dataTmpLedIsOff = true;
				setLedMode();
			}
		}
	}

	if (digitalRead(pinButton) == LOW) {
		valueButtonState = 0;

		if (dataTmpButtonIsTouch) {
			dataTmpButtonIsTouch = false;
			if (dataTmpLedIsOff) {
				dataTmpLedIsOff = false;
			}
			else {
				dataTmpLedModeOffIndex = 0;
				setLedMode();
			}
		}
	}
}

/* System State Led */
void setSystemState(int value) {
	dataTmpSystemState = value;
	if (dataTmpSystemState == 11)
		digitalWrite(pinSystemStateR, LOW);
	if (dataTmpSystemState == 10)
		digitalWrite(pinSystemStateR, HIGH);
}

/* LED PWM 設定 */
void setLedPwm(int value) {
	valueLedPwm = value;
	analogWrite(pinLedPwm, valueLedPwm);
}

void setLedMode() {

	if (dataTmpLedIsOff) {
		dataTmpLedMode = 0;
		dataTmpLedModeOffIndex = 0;
		dataTmpLedModeBack = false;
		setLedPwm(0);
		setBuzzer(2);
	}
	else {

		// BZ
		if (dataTmpLedMode == 3)
			setBuzzer(1);
		else
			setBuzzer(0);

		// LED
		switch (dataTmpLedMode) {
		case 0:
			setLedPwm(4);
			break;
		case 1:
			setLedPwm(64);
			break;
		case 2:
			setLedPwm(128);
			break;
		case 3:
			setLedPwm(255);
			break;
		}

		// Mode Index Add
		if (!dataTmpLedModeBack) {
			if (dataTmpLedMode < 3)
				dataTmpLedMode++;
			else
				dataTmpLedModeBack = true;
		}

		// Mode Index Less
		if (dataTmpLedModeBack && dataTmpLedMode > 1) {
			dataTmpLedMode--;
			if (dataTmpLedMode == 1)
				dataTmpLedModeBack = false;
		}

	}
}

/* BLE Data MOSI */
void setSerialMOSI() {
	getAnalogReadValue();
	setSystemState(0);

	sensors.requestTemperatures();
	valueTemperature = sensors.getTempCByIndex(0);

	char dataArray[13];

	dataArray[0] = 0x4D;							//   PacketStart
	dataArray[1] = 0x4F;							//	 PacketMOSI
	dataArray[2] = (valueCurrent >> 8) & 0xFF;		// H Current
	dataArray[3] = valueCurrent & 0xFF;				// L Current
	dataArray[4] = (valueVoltage >> 8) & 0xFF;		// H Voltage
	dataArray[5] = valueVoltage & 0xFF;				// L Voltage
	dataArray[6] = valueBatteryState & 0xFF;		//	 BatteryState
	dataArray[7] = (valueTemperature >> 8) & 0xFF;	// H Temperature
	dataArray[8] = valueTemperature & 0xFF;			// L Temperature
	dataArray[9] = valueLedPwm & 0xFF;				//   LedPwm
	dataArray[10] = valueButtonState & 0xFF;		// L LedPwmTemperature
	unsigned short valueSum = 0;
	for (int i = 2; i < 13; i++)
		valueSum += dataArray[i];
	dataArray[11] = valueSum & 0xFF;				//   PacketSum
	dataArray[12] = 0x6D;							//   PacketEnd

	for (int i = 0; i < sizeof(dataArray); i++)
		SerialBle.print(dataArray[i]);
}

/* Buzzer Mode Setting */
void setBuzzer(int mode) {
	if (!dataTmpBuzzerIsRun) {
		dataTmpBuzzerIsRun = true;
		dataTmpBuzzerMode = mode;
		switch (dataTmpBuzzerMode) {
		case 0:
			threadSetBuzzer.setInterval(100);
			dataTmpBuzzerIndex = 0;
			break;
		case 1:
			threadSetBuzzer.setInterval(100);
			dataTmpBuzzerIndex = 0;
			break;
		case 2:
			threadSetBuzzer.setInterval(100);
			dataTmpBuzzerIndex = 2;
			break;
		case 3:
			threadSetBuzzer.setInterval(500);
			dataTmpBuzzerIndex = 3;
			break;
		}
	}
}

/* Buzzer Mode Database */
void setBuzzerMode() {
	if (dataTmpBuzzerIndex > 0)
		dataTmpBuzzerIndex--;
	switch (dataTmpBuzzerMode) {
	case 0:
		tone(pinBuzzer, 2400, 100);
		break;
	case 1:
		tone(pinBuzzer, 4000, 50);
		break;
	case 2:
		tone(pinBuzzer, 3600, 50);
		break;
	case 3:
		if (dataTmpBuzzerIndex == 2)
			tone(pinBuzzer, 2000, 200);
		if (dataTmpBuzzerIndex == 1)
			tone(pinBuzzer, 1000, 100);
		if (dataTmpBuzzerIndex == 0)
			tone(pinBuzzer, 1000, 100);
		break;
	}
}

/*
>Vcc	>%		>Time	>Led	>BZ
10.8V	10		1s		R		ON
11.0V	20		10s		R		ON
11.2V	40		2s		R
11.4V	50		5s		R
11.6V	60		5s		G/R
11.8V	80		5s		G
12.6V	100		10s		G
*/

void setCapacity() {

	if (valueVoltageF > 11.8) {
		// Serial.println("11.8");
		if ((dataTmpCapacityTime % 5) == 4) {
			digitalWrite(pinSystemStateG, LOW);
			digitalWrite(pinSystemStateR, HIGH);
		}
		else {
			digitalWrite(pinSystemStateG, HIGH);
			digitalWrite(pinSystemStateR, HIGH);
		}
	}
	else if (valueVoltageF > 11.6) {
		// Serial.println("11.6");
		if ((dataTmpCapacityTime % 2) == 1) {
			digitalWrite(pinSystemStateG, LOW);
			digitalWrite(pinSystemStateR, HIGH);
		}
		else {
			digitalWrite(pinSystemStateG, HIGH);
			digitalWrite(pinSystemStateR, HIGH);
		}
	}
	else if (valueVoltageF > 11.4) {
		// Serial.println("11.4");
		if ((dataTmpCapacityTime % 5) == 4) {
			digitalWrite(pinSystemStateG, LOW);
			digitalWrite(pinSystemStateR, LOW);
		}
		else {
			digitalWrite(pinSystemStateG, HIGH);
			digitalWrite(pinSystemStateR, HIGH);
		}
	}
	else if (valueVoltageF > 11.2) {
		// Serial.println("11.2");
		if ((dataTmpCapacityTime % 2) == 1) {
			digitalWrite(pinSystemStateG, LOW);
			digitalWrite(pinSystemStateR, HIGH);
		}
		else {
			digitalWrite(pinSystemStateG, HIGH);
			digitalWrite(pinSystemStateR, LOW);
		}
	}
	else if (valueVoltageF > 11.0) {
		// Serial.println("11.0");
		if ((dataTmpCapacityTime % 5) == 4) {
			digitalWrite(pinSystemStateG, HIGH);
			digitalWrite(pinSystemStateR, LOW);
		}
		else {
			digitalWrite(pinSystemStateG, HIGH);
			digitalWrite(pinSystemStateR, HIGH);
		}
	}
	else if (valueVoltageF > 10.8) {
		// Serial.println("10.8");
		if ((dataTmpCapacityTime % 2) == 1) {
			digitalWrite(pinSystemStateG, HIGH);
			digitalWrite(pinSystemStateR, LOW);
		}
		else {
			digitalWrite(pinSystemStateG, HIGH);
			digitalWrite(pinSystemStateR, HIGH);
		}
		if ((dataTmpCapacityTime % 5) == 4) {
			setBuzzer(0);
		}
	}

	float valueCurrentF = ADC_RESOLUTION * valueCurrent;
	float valueVoltageF = ADC_RESOLUTION * ADC_RESISTIVE * valueVoltage;

	// Serial.print("Current: ");
	// Serial.println(valueCurrentF, 3);
	// Serial.print(", ");
	// Serial.print("Voltage: ");
	// Serial.print(valueVoltageF, 3);
	// Serial.println("");

	if (dataTmpCapacityTime < 99)
		dataTmpCapacityTime++;
	else
		dataTmpCapacityTime = 0;
}

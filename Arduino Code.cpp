#include <SPI.h>
#include <MFRC522.h>
#include <SoftwareSerial.h>
#include <TinyGPS++.h>


#define SS_PIN 10
#define RST_PIN 9
MFRC522 rfid(SS_PIN, RST_PIN);

SoftwareSerial gpsSerial(4, 3);  // RX, TX for GPS
TinyGPSPlus gps;

SoftwareSerial espSerial(2, 3);  // RX, TX for ESP8266
const char* ssid = "your_SSID";
const char* password = "your_PASSWORD";
const char* server = "http://your-server.com/data.php";  // Change to your API endpoint

void setup() {
    Serial.begin(115200);
    SPI.begin();
    rfid.PCD_Init();
    gpsSerial.begin(9600);
    espSerial.begin(115200);
    
    connectWiFi();
}

void loop() {
    if (rfid.PICC_IsNewCardPresent() && rfid.PICC_ReadCardSerial()) {
        String tagID = "";
        for (byte i = 0; i < rfid.uid.size; i++) {
            tagID += String(rfid.uid.uidByte[i], HEX);
        }
        Serial.println("RFID Tag: " + tagID);

        String gpsData = getGPS();
        sendData(tagID, gpsData);
    }
}

String getGPS() {
    while (gpsSerial.available()) {
        gps.encode(gpsSerial.read());
    }
    if (gps.location.isUpdated()) {
        return String(gps.location.lat(), 6) + "," + String(gps.location.lng(), 6);
    }
    return "0,0"; // Default if no GPS data
}

void connectWiFi() {
    espSerial.println("AT+CWMODE=1");
    delay(1000);
    espSerial.println("AT+CWJAP=\"" + String(ssid) + "\",\"" + String(password) + "\"");
    delay(5000);
}

void sendData(String rfid, String gps) {
    String request = "GET /data.php?rfid=" + rfid + "&gps=" + gps + " HTTP/1.1\r\nHost: your-server.com\r\n\r\n";
    espSerial.println("AT+CIPSTART=\"TCP\",\"your-server.com\",80");
    delay(2000);
    espSerial.println("AT+CIPSEND=" + String(request.length() + 2));
    delay(2000);
    espSerial.println(request);
    delay(2000);
    espSerial.println("AT+CIPCLOSE");
}


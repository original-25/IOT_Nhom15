#include "globals.h"
#include "storage.h"
#include "app_network.h"
#include "mqtt_logic.h"
#include "device_logic.h"

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("\n\n=== ESP32 Starting ===");
  
  EEPROM.begin(EEPROM_SIZE);
  Serial.println("EEPROM initialized");
  
  mqttClient.setBufferSize(512);
  Serial.println("MQTT buffer set");
  
  loadFromEEPROM();
  Serial.println("Loaded from EEPROM");
  Serial.print("WiFi SSID: '");
  Serial.print(wifiSSID);
  Serial.println("'");
  Serial.print("WiFi Pass: '");
  Serial.print(wifiPassword);
  Serial.println("'");
  
  loadDevicesFromEEPROM();
  Serial.println("Devices loaded");

  if (wifiSSID == "" || wifiPassword == "") {
    Serial.println("No WiFi credentials, entering AP mode");
    setupAP();
  } else {
    Serial.println("WiFi credentials found, attempting connection");
    if (!connectWiFi()) {
      Serial.println("Connection failed, clearing EEPROM and entering AP mode");
      clearEEPROM();
      setupAP();
    }
  }
  Serial.println("=== Setup Complete ===");
}

void loop() {
  static unsigned long lastDebug = 0;
  if (millis() - lastDebug > 5000) {
    Serial.println("--- Loop Running ---");
    Serial.print("WiFi Mode: ");
    Serial.print(WiFi.getMode());
    Serial.print(" Status: ");
    Serial.println(WiFi.status());
    lastDebug = millis();
  }
  
  if (WiFi.getMode() == WIFI_MODE_AP || WiFi.getMode() == WIFI_MODE_APSTA) {
    server.handleClient();
  } else if (WiFi.status() == WL_CONNECTED) {
    if (!mqttClient.connected()) {
      Serial.println("MQTT not connected, attempting to connect...");
      connectMQTT();
    }
    mqttClient.loop();

    // Send sensor data every 10 seconds if sensor exists
    static unsigned long lastSend = 0;
    if (millis() - lastSend > 10000) {
      sendSensorData();
      lastSend = millis();
    }
  }
}


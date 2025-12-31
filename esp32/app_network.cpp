#include "app_network.h"
#include "mqtt_logic.h"

void setupAP() {
  WiFi.softAP(apSSID, apPassword);
  Serial.println("AP mode started");
  Serial.print("IP address: ");
  Serial.println(WiFi.softAPIP());

  server.on("/config", HTTP_POST, handleConfig);
  server.begin();
}

void handleConfig() {
  if (server.hasArg("plain")) {
    String body = server.arg("plain");
    DynamicJsonDocument doc(1024);
    deserializeJson(doc, body);

    wifiSSID = doc["ssid"].as<String>();
    wifiPassword = doc["pass"].as<String>();
    claimToken = doc["claimToken"].as<String>();
    espDeviceId = doc["espDeviceId"].as<String>();
    
    Serial.println("Received configuration:");
    Serial.print("WiFi SSID: ");
    Serial.println(wifiSSID);
    Serial.print("WiFi Password: ");
    Serial.println(wifiPassword);
    
    // Receive MQTT credentials from app
    mqttHost = doc["mqtt"]["host"].as<String>();
    mqttPort = doc["mqtt"]["port"];
    mqttUsername = doc["mqtt"]["username"].as<String>();
    baseTopic = doc["mqtt"]["baseTopic"].as<String>();

    

    // Extract homeId and espId from baseTopic
    // baseTopic: iot_nhom15/home/{HOME_ID}/esp32/{ESP_ID}
    int homeStart = baseTopic.indexOf("/home/") + 6;
    int homeEnd = baseTopic.indexOf("/esp32");
    homeId = baseTopic.substring(homeStart, homeEnd);

    int espStart = baseTopic.indexOf("/esp32/") + 7;
    espId = baseTopic.substring(espStart);

    saveToEEPROM();

    server.send(200, "application/json", "{\"success\":true}");

    // Wait a bit before restarting to ensure response is sent
    delay(1000);
    ESP.restart();
  } else {
    server.send(400, "application/json", "{\"error\":\"Invalid request\"}");
  }
}

bool connectWiFi() {
  Serial.println("\n--- Starting WiFi Connection ---");
  Serial.print("SSID: '");
  Serial.print(wifiSSID);
  Serial.println("'");
  Serial.print("Password length: ");
  Serial.println(wifiPassword.length());
  
  WiFi.begin(wifiSSID.c_str(), wifiPassword.c_str());
  Serial.print("Connecting to WiFi");
  unsigned long startTime = millis();

  while (WiFi.status() != WL_CONNECTED && millis() - startTime < 30000) { // 30 seconds timeout
    delay(500);
    Serial.print(".");
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\n*** WiFi Connected! ***");
    Serial.print("IP Address: ");
    Serial.println(WiFi.localIP());
    
    Serial.println("Configuring time...");
    configTime(0, 0, "pool.ntp.org");
    delay(2000);
    
    Serial.println("Connecting to MQTT...");
    connectMQTT();
    return true;
  } else {
    Serial.println("\n*** WiFi Connection Failed ***");
    Serial.print("WiFi Status: ");
    Serial.println(WiFi.status());
    return false;
  }
}

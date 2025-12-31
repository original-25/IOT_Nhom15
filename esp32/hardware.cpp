#include <WiFi.h>
#include <WebServer.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <EEPROM.h>
#include <time.h>

// AP mode settings
const char* apSSID = "ESP32_Config";
const char* apPassword = "12345678";

// MQTT settings
String mqttHost = "";
int mqttPort = 1883;
String mqttUsername = "";
String baseTopic = "";

// Device info
String espDeviceId = "";
String claimToken = "";

// WiFi credentials
String wifiSSID = "";
String wifiPassword = "";

// Home and ESP IDs (from MQTT config)
String homeId = "";
String espId = "";

// WebServer
WebServer server(80);

// MQTT client
WiFiClient espClient;
PubSubClient mqttClient(espClient);

// EEPROM addresses
#define EEPROM_SIZE 1024
#define WIFI_SSID_ADDR 0
#define WIFI_PASS_ADDR 32
#define MQTT_HOST_ADDR 64
#define MQTT_PORT_ADDR 96
#define MQTT_USER_ADDR 100
#define MQTT_PASS_ADDR 164
#define BASE_TOPIC_ADDR 196
#define ESP_DEVICE_ID_ADDR 228
#define CLAIM_TOKEN_ADDR 260
#define HOME_ID_ADDR 292
#define ESP_ID_ADDR 324
#define DEVICES_ADDR 400

// Device types
#define TYPE_OUTPUT 0
#define TYPE_SENSOR 1

// Device list
struct Device {
  char id[25];    // deviceId
  uint8_t pin;    // GPIO
  uint8_t type;   // 0: Output, 1: Sensor
  bool state;
};
Device devices[10]; // Max 10 devices
int deviceCount = 0;

// Sensor data topic
String dataTopic = "";

// Function prototypes
void setupAP();
void handleConfig();
bool connectWiFi();
void connectMQTT();
void sendMqttClaimRequest();
void mqttCallback(char* topic, byte* payload, unsigned int length);
void saveToEEPROM();
void loadFromEEPROM();
void clearEEPROM();
void handleCreate(String payload);
void handleDelete(String payload);
void handleState(String payload);
void sendAck(String action, String cid, String deviceId, bool success);
void sendSensorData();
String getISOTimestamp();
void saveDevicesToEEPROM();
void loadDevicesFromEEPROM();

void setup() {
  Serial.begin(115200);
  EEPROM.begin(EEPROM_SIZE);
  mqttClient.setBufferSize(512);
  loadFromEEPROM();
  loadDevicesFromEEPROM();

  if (wifiSSID == "" || wifiPassword == "") {
    // No WiFi credentials, enter AP mode
    setupAP();
  } else {
    // Try to connect to WiFi
    if (!connectWiFi()) {
      // Failed to connect within 3 minutes, clear EEPROM and enter AP mode
      clearEEPROM();
      setupAP();
    }
  }
}

void loop() {
  if (WiFi.getMode() == WIFI_AP) {
    server.handleClient();
  } else {
    if (!mqttClient.connected()) {
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
    wifiPassword = doc["wifiPassword"].as<String>();
    claimToken = doc["claimToken"].as<String>();
    espDeviceId = doc["espDeviceId"].as<String>();
    
    // Receive MQTT credentials from app
    mqttHost = doc["mqtt"]["host"].as<String>();
    mqttPort = doc["mqtt"]["port"];
    mqttUsername = doc["mqtt"]["password"].as<String>();
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
  WiFi.begin(wifiSSID.c_str(), wifiPassword.c_str());
  Serial.print("Connecting to WiFi");
  unsigned long startTime = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - startTime < 180000) { // 3 minutes = 180000 ms
    delay(500);
    Serial.print(".");
  }
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\nConnected to WiFi");
    configTime(0, 0, "pool.ntp.org"); // Configure NTP
    delay(2000); // Wait for time sync
    connectMQTT();
    return true;
  } else {
    Serial.println("\nFailed to connect to WiFi within 3 minutes");
    return false;
  }
}

void connectMQTT() {
  mqttClient.setServer(mqttHost.c_str(), mqttPort);
  mqttClient.setCallback(mqttCallback);

  while (!mqttClient.connected()) {
    Serial.print("Connecting to MQTT...");
    
    if (mqttClient.connect(espDeviceId.c_str(), mqttUsername.c_str(), "")) {
      Serial.println("connected");
    

      // Check if we need to claim (claimToken is set)
      if (claimToken.length() > 0 && homeId.length() > 0 && espId.length() > 0) {
        // Send claim request
        sendMqttClaimRequest();
        // Clear claim token after sending
        claimToken = "";
        saveToEEPROM();
      } else {
        // Already claimed: Subscribe to cmd topic
        String cmdTopic = "iot_nhom15/home/" + homeId + "/esp32/" + espId + "/cmd";
        mqttClient.subscribe(cmdTopic.c_str());

        // Set data topic for publishing sensor data
        dataTopic = "iot_nhom15/home/" + homeId + "/esp32/" + espId + "/data";
      }
    } else {
      Serial.print("failed, rc=");
      Serial.print(mqttClient.state());
      //test xóa nhanh
      clearEEPROM();
      setupAP();

      delay(5000);
    }
  }
}

void sendMqttClaimRequest() {
  if (!mqttClient.connected()) return;
  
  String ackTopic = "iot_nhom15/home/" + homeId + "/esp32/" + espId + "/ack";
  
  DynamicJsonDocument doc(256);
  doc["action"] = "create esp32";
  doc["claimToken"] = claimToken;
  doc["ts"] = getISOTimestamp();

  String payload;
  serializeJson(doc, payload);

  mqttClient.publish(ackTopic.c_str(), payload.c_str());
  Serial.println("Sent claim request to " + ackTopic + ": " + payload);
}

void mqttCallback(char* topic, byte* payload, unsigned int length) {
  String message;
  for (int i = 0; i < length; i++) {
    message += (char)payload[i];
  }

  DynamicJsonDocument doc(1024);
  deserializeJson(doc, message);

  String action = doc["payload"]["action"].as<String>();
  String cid = doc["payload"]["cid"].as<String>();

  if (action == "create") {
    handleCreate(message);
  } else if (action == "delete") {
    handleDelete(message);
  } else if (action == "state") {
    handleState(message);
  }
}

void handleCreate(String payload) {
  DynamicJsonDocument doc(1024);
  deserializeJson(doc, payload);
Str = doc["payload"]["device"]["type"].as<String>();
  int pin = doc["payload"]["device"]["config"]["pin"] | 0;
  String cid = doc["payload"]["cid"].as<String>();

  // Check duplicate
  for(int i=0; i<deviceCount; i++) {
      if(String(devices[i].id) == deviceId) {
          sendAck("create", cid, deviceId, false);
          return;
      }
  }

  // Add device
  if (deviceCount < 10) {
    deviceId.toCharArray(devices[deviceCount].id, 25);
    devices[deviceCount].pin = pin;
    
    if (typeStr == "sensor") {
        devices[deviceCount].type = TYPE_SENSOR;
    } else {
        devices[deviceCount].type = TYPE_OUTPUT;
    }
    
    devices[deviceCount].state = false;
    
    // Init pin
    if (devices[deviceCount].type == TYPE_OUTPUT) {
        pinMode(devices[deviceCount].pin, OUTPUT);
        digitalWrite(devices[deviceCount].pin, LOW);
    }

    deviceCount++;
    saveDevicesToEEPROM();

    // If sensor, subscribe to data topic
    if (devices[deviceCount-1].type == TYPE_SENSOR) {
      mqttClient.subscribe(dataTopic.c_str());
    }

    sendAck("create", cid, deviceId, true);
  } else {
    sendAck("create", cid, deviceId, false);
  }
}

void handleDelete(String payload) {
  DynamicJsonDocument doc(1024);
  deserializeJson(doc, payload);

  String deviceId = doc["payload"]["deviceId"].as<String>();
  String cid = doc["payload"]["cid"].as<String>();

  // Remove device
  for (int i = 0; i < deviceCount; i++) {
    if (String(devices[i].id) == deviceId) {
      // Reset pin
      if (devices[i].type == TYPE_OUTPUT) {
          digitalWrite(devices[i].pin, LOW);
      }

      for (int j = i; j < deviceCount - 1; j++) {
        devices[j] = devices[j + 1];
      }
      deviceCount--;
      saveDevicesToEEPROM();
      sendAck("delete", cid, deviceId, true);
      return;
    }
  }
  sendAck("delete", cid, deviceId, false);
}

void handleState(String payload) {
  DynamicJsonDocument doc(1024);
  deserializeJson(doc, payload);

  String deviceId = doc["payload"]["deviceId"].as<String>();
  String value = doc["payload"]["value"].as<String>();

  // Find device and set state
  for (int i = 0; i < deviceCount; i++) {
    if (String(devices[i].id) == deviceId) {
      devices[i].state = (value == "on");
      
      if (devices[i].type == TYPE_OUTPUT) {
          pinMode(devices[i].pin, OUTPUT);
          digitalWrite(devices[i].pin, devices[i].state ? HIGH : LOW);
      }
      TYPE_SENSOR
      Serial.println("Device " + deviceId + " set to " + value);
      saveDevicesToEEPROM()
      devices[i].state = (value == "on");
      // Here you would control the actual hardware pin
      Serial.println("Device " + deviceId + " set to " + value);
      break;
    }
  }
}

void sendAck(String action, String cid, String deviceId, bool success) {
  String ackTopic = "iot_nhom15/home/" + homeId + "/esp32/" + espId + "/ack";

  DynamicJsonDocument doc(256);
  doc["action"] = action;
  doc["cid"] = cid;
  doc["deviceId"] = deviceId;
  doc["success"] = success;
  doc["ts"] = getISOTimestamp();

  String ackMessage;
  serializeJson(doc, ackMessage);

  mqttClient.publish(ackTopic.c_str(), ackMessage.c_str());
}

void sendSensorData() {
  if (dataTopic == "") return;

  // Simulate sensor data
  float temperature = 25.0 + random(-5, 5);
  float humidity = 60.0 + random(-10, 10);

  for (int i = 0; i < deviceCount; i++) {
    if (devices[i].type == "sensor") {
      DynamicJsonDocument doc(256);
      doc["type"] = "sensor";
      doc["deviceId"] = devices[i].id;
      doc["data"]["temperature"] = temperature;
      doc["data"]["humidity"] = humidity;
      doc["timestamp"] = getISOTimestamp();

      String dataMessage;
      serializeJson(doc, dataMessage);

      mqttClient.publish(dataTopic.c_str(), dataMessage.c_str());
    }
  }
}

String getISOTimestamp() {
  time_t now = time(nullptr);
  struct tm* timeinfo = localtime(&now);
  char buffer[25];
  strftime(buffer, sizeof(buffer), "%Y-%m-%dT%H:%M:%SZ", timeinfo);
  return String(buffer);
}

void saveDevicesToEEPROM() {
    EEPROM.put(DEVICES_ADDR, deviceCount);
    for(int i=0; i<deviceCount; i++) {
        EEPROM.put(DEVICES_ADDR + sizeof(int) + i*sizeof(Device), devices[i]);
    }
    EEPROM.commit();
}

void loadDevicesFromEEPROM() {
    EEPROM.get(DEVICES_ADDR, deviceCount);
    if (deviceCount < 0 || deviceCount > 10) {
        deviceCount = 0;
        return;
    }
    for(int i=0; i<deviceCount; i++) {
        EEPROM.get(DEVICES_ADDR + sizeof(int) + i*sizeof(Device), devices[i]);
        // Init pin
        if (devices[i].type == TYPE_OUTPUT) {
            pinMode(devices[i].pin, OUTPUT);
            digitalWrite(devices[i].pin, devices[i].state ? HIGH : LOW);
        }
    }
}

void saveToEEPROM() {
  EEPROM.put(WIFI_SSID_ADDR, wifiSSID);
  EEPROM.put(WIFI_PASS_ADDR, wifiPassword);
  EEPROM.put(MQTT_HOST_ADDR, mqttHost);
  EEPROM.put(MQTT_PORT_ADDR, mqttPort);
  EEPROM.put(MQTT_USER_ADDR, mqttUsername);
  EEPROM.put(BASE_TOPIC_ADDR, baseTopic);
  EEPROM.put(ESP_DEVICE_ID_ADDR, espDeviceId);
  EEPROM.put(CLAIM_TOKEN_ADDR, claimToken);
  EEPROM.put(HOME_ID_ADDR, homeId);
  EEPROM.put(ESP_ID_ADDR, espId);
  EEPROM.commit();
}

void loadFromEEPROM() {
  EEPROM.get(WIFI_SSID_ADDR, wifiSSID);
  EEPROM.get(WIFI_PASS_ADDR, wifiPassword);
  EEPROM.get(MQTT_HOST_ADDR, mqttHost);
  EEPROM.get(MQTT_PORT_ADDR, mqttPort);
  EEPROM.get(MQTT_USER_ADDR, mqttUsername);
  EEPROM.get(BASE_TOPIC_ADDR, baseTopic);
  EEPROM.get(ESP_DEVICE_ID_ADDR, espDeviceId);
  EEPROM.get(CLAIM_TOKEN_ADDR, claimToken);
  EEPROM.get(HOME_ID_ADDR, homeId);
  EEPROM.get(ESP_ID_ADDR, espId);
}

void clearEEPROM() {
  for (int i = 0; i < EEPROM_SIZE; i++) {
    EEPROM.write(i, 0);
  }
  EEPROM.commit();
  Serial.println("EEPROM cleared");
}
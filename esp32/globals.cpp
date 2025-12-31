#include "globals.h"

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

// Device list
Device devices[10];
int deviceCount = 0;

// Sensor data topic
String dataTopic = "";

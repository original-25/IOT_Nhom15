#ifndef GLOBALS_H
#define GLOBALS_H

#include <WiFi.h>
#include <WebServer.h>
#include <PubSubClient.h>
#include "definitions.h"

// AP mode settings
extern const char* apSSID;
extern const char* apPassword;

// MQTT settings
extern String mqttHost;
extern int mqttPort;
extern String mqttUsername;
extern String baseTopic;

// Device info
extern String espDeviceId;
extern String claimToken;

// WiFi credentials
extern String wifiSSID;
extern String wifiPassword;

// Home and ESP IDs (from MQTT config)
extern String homeId;
extern String espId;

// WebServer
extern WebServer server;

// MQTT client
extern WiFiClient espClient;
extern PubSubClient mqttClient;

// Device list
extern Device devices[10];
extern int deviceCount;

// Sensor data topic
extern String dataTopic;

#endif

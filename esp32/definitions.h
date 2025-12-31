#ifndef DEFINITIONS_H
#define DEFINITIONS_H

#include <Arduino.h>

// EEPROM addresses
#define EEPROM_SIZE 1024
#define WIFI_SSID_ADDR 0
#define WIFI_PASS_ADDR 32
#define MQTT_HOST_ADDR 64
#define MQTT_PORT_ADDR 96
#define MQTT_USER_ADDR 100
// #define MQTT_PASS_ADDR 164 // Removed
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

#endif

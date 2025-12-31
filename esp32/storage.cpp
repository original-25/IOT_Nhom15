#include "storage.h"

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

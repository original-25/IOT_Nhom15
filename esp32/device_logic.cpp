#include "device_logic.h"
#include "mqtt_logic.h"

void handleCreate(String payload) {
  DynamicJsonDocument doc(1024);
  deserializeJson(doc, payload);
  String typeStr = doc["payload"]["device"]["type"].as<String>();
  String deviceId = doc["payload"]["device"]["deviceId"].as<String>();
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
      
      Serial.println("Device " + deviceId + " set to " + value);
      saveDevicesToEEPROM();
      break;
    }
  }
}

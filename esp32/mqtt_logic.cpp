#include "mqtt_logic.h"
#include "device_logic.h"
#include "app_network.h"

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
    if (devices[i].type == TYPE_SENSOR) {
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

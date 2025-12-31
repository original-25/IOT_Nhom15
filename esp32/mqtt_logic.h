#ifndef MQTT_LOGIC_H
#define MQTT_LOGIC_H

#include <ArduinoJson.h>
#include "globals.h"
#include "utils.h"
#include "storage.h"

void connectMQTT();
void sendMqttClaimRequest();
void mqttCallback(char* topic, byte* payload, unsigned int length);
void sendAck(String action, String cid, String deviceId, bool success);
void sendSensorData();

#endif

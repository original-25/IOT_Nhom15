#ifndef APP_NETWORK_H
#define APP_NETWORK_H

#include <ArduinoJson.h>
#include "globals.h"
#include "storage.h"

void setupAP();
void handleConfig();
bool connectWiFi();

#endif

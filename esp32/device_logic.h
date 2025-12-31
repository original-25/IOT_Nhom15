#ifndef DEVICE_LOGIC_H
#define DEVICE_LOGIC_H

#include <ArduinoJson.h>
#include "globals.h"
#include "storage.h"

void handleCreate(String payload);
void handleDelete(String payload);
void handleState(String payload);

#endif

#ifndef STORAGE_H
#define STORAGE_H

#include <EEPROM.h>
#include "globals.h"

void saveToEEPROM();
void loadFromEEPROM();
void clearEEPROM();
void saveDevicesToEEPROM();
void loadDevicesFromEEPROM();

#endif

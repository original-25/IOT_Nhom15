// ================================================================
// MERGED HARDWARE.CPP - SINGLE FILE PROJECT
// ================================================================

// 1. INCLUDES (External Libraries)
// QUAN TRỌNG: Include NimBLE TRƯỚC tất cả các file khác
#include <NimBLEDevice.h>
#include <Arduino.h>
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <Preferences.h>
#include <DHT.h>

// ================================================================
// 2. DEFINES & STRUCTS (From app_config.h)
// ================================================================

// --- BLE Settings ---
#define BLE_SERVICE_UUID        "12345678-1234-1234-1234-123456789abc"
#define BLE_CHARACTERISTIC_UUID "87654321-4321-4321-4321-cba987654321"
#define BLE_DEVICE_NAME         "ESP32_SmartHome"

// --- Device Types ---
#define TYPE_OUTPUT 0
#define TYPE_SENSOR 1
#define TYPE_MOTOR  2 

// --- Device Structure ---
struct Device {
  char id[25];    
  uint8_t pin;    // GPIO A
  uint8_t pin2;   // GPIO B (cho Motor)
  uint8_t type;   // 0:Output, 1:Sensor, 2:Motor
  bool state;     
};

// ================================================================
// 3. GLOBAL VARIABLES (From app_config.cpp)
// ================================================================

// MQTT settings
String mqttHost = "";
int mqttPort = 1883;
String mqttUsername = "";
String baseTopic = "";

// Device info
String espDeviceId = "";
String claimToken = "";
String wifiSSID = "";
String wifiPassword = "";

// IDs
String homeId = "";
String espId = "";

// Clients & Objects
WiFiClient espClient;
PubSubClient mqttClient(espClient);

// Device list
Device devices[10];
int deviceCount = 0;

// Config Buffer
bool shouldSaveConfig = false;
String configJsonBuffer = "";

// Storage Object (From storage.cpp)
Preferences prefs;

// ================================================================
// 4. FORWARD DECLARATIONS (Prototypes)
// Giúp các hàm gọi chéo nhau mà không bị lỗi undeclared
// ================================================================

// Storage
void saveConfigToNVS();
bool loadConfigFromNVS();
void clearNVS();
void saveDevicesToNVS();
void loadDevicesFromNVS();

// Device Logic
void handleCreate(String payload);
void handleDelete(String payload);
void handleState(String payload);
void loopReadSensors();
void controlMotor(int pinA, int pinB, bool state);

// MQTT Logic
void connectMQTT();
void sendMqttClaimRequest();
void sendAck(String action, String cid, String deviceId, bool success);
void mqttCallback(char* topic, byte* payload, unsigned int length);

// Hardware Helpers
void checkButtonReset();

// ================================================================
// 5. IMPLEMENTATION: STORAGE (From storage.cpp)
// ================================================================

void saveConfigToNVS() {
  prefs.begin("config", false); // RW mode
  
  prefs.putString("ssid", wifiSSID);
  prefs.putString("pass", wifiPassword);
  prefs.putString("mq_host", mqttHost);
  prefs.putInt("mq_port", mqttPort);
  prefs.putString("mq_user", mqttUsername);
  prefs.putString("base_t", baseTopic);
  prefs.putString("dev_id", espDeviceId);
  prefs.putString("token", claimToken);
  prefs.putString("home_id", homeId);
  prefs.putString("esp_id", espId);
  
  prefs.end();
  Serial.println(">> Config saved to NVS");
}

bool loadConfigFromNVS() {
  prefs.begin("config", true); // Read-only
  
  wifiSSID = prefs.getString("ssid", "");
  if (wifiSSID == "") {
    prefs.end();
    return false; // Chưa có config
  }

  wifiPassword = prefs.getString("pass", "");
  mqttHost = prefs.getString("mq_host", "");
  mqttPort = prefs.getInt("mq_port", 1883);
  mqttUsername = prefs.getString("mq_user", "");
  baseTopic = prefs.getString("base_t", "");
  espDeviceId = prefs.getString("dev_id", "");
  claimToken = prefs.getString("token", "");
  homeId = prefs.getString("home_id", "");
  espId = prefs.getString("esp_id", "");
  
  prefs.end();
  return true;
}

void clearNVS() {
  prefs.begin("config", false);
  prefs.clear();
  prefs.end();
  prefs.begin("devices", false);
  prefs.clear();
  prefs.end();
  Serial.println(">> NVS Cleared!");
}

void saveDevicesToNVS() {
  prefs.begin("devices", false);
  prefs.putInt("count", deviceCount);
  if (deviceCount > 0) {
      prefs.putBytes("list", devices, sizeof(devices));
  }
  prefs.end();
}

void loadDevicesFromNVS() {
  prefs.begin("devices", true);
  deviceCount = prefs.getInt("count", 0);
  if (deviceCount > 10) deviceCount = 0;

  if (deviceCount > 0) {
      prefs.getBytes("list", devices, sizeof(devices));
  }
  prefs.end();

  // Khôi phục trạng thái chân GPIO
  for(int i=0; i<deviceCount; i++) {
      pinMode(devices[i].pin, OUTPUT);
      
      if (devices[i].type == TYPE_MOTOR) {
          pinMode(devices[i].pin2, OUTPUT); 
          digitalWrite(devices[i].pin, LOW);
          digitalWrite(devices[i].pin2, LOW);
      } else if (devices[i].type == TYPE_OUTPUT) {
          digitalWrite(devices[i].pin, devices[i].state ? HIGH : LOW);
      }
  }
}

// ================================================================
// 6. IMPLEMENTATION: DEVICE LOGIC (From device_logic.cpp)
// ================================================================

// Thời gian giữa các lần gửi (ms)
#define SENSOR_INTERVAL 10000 
unsigned long lastSensorTime = 0;

void loopReadSensors() {
  if (millis() - lastSensorTime < SENSOR_INTERVAL) return; // Chưa đến giờ đọc
  lastSensorTime = millis();

  // Duyệt qua tất cả thiết bị
  for (int i = 0; i < deviceCount; i++) {
    if (devices[i].type == TYPE_SENSOR) {
        // Khởi tạo DHT động trên chân GPIO của thiết bị đó
        // Giả sử dùng DHT11. Nếu dùng DHT22 thì đổi thành DHT22
        DHT dht(devices[i].pin, DHT11); 
        dht.begin();
        
        // Đọc dữ liệu
        float t = dht.readTemperature();
        float h = dht.readHumidity();

        // Kiểm tra lỗi đọc
        if (isnan(t) || isnan(h)) {
            Serial.print("Failed to read from sensor ID: ");
            Serial.println(devices[i].id);
            continue;
        }

        // Tạo gói tin JSON
        DynamicJsonDocument doc(256);
        doc["deviceId"] = devices[i].id;
        doc["temp"] = t;
        doc["humid"] = h;
        
        String payload;
        serializeJson(doc, payload);

        // Tạo Topic: baseTopic/data
        String dataTopic = baseTopic + "/data";
        
        // Gửi MQTT
        if (mqttClient.connected()) {
            mqttClient.publish(dataTopic.c_str(), payload.c_str());
            Serial.print("Sent sensor data: ");
            Serial.println(payload);
        }
    }
  }
}

// Điều khiển L9110S
void controlMotor(int pinA, int pinB, bool state) {
    if (state) {
        digitalWrite(pinA, HIGH);
        digitalWrite(pinB, LOW);
    } else {
        digitalWrite(pinA, LOW);
        digitalWrite(pinB, LOW);
    }
}

void handleCreate(String jsonString) {
  Serial.println("\n╔════════════════════════════════════╗");
  Serial.println("║       HANDLE CREATE STARTED        ║");
  Serial.println("╚════════════════════════════════════╝");

  // 1. In ra chuỗi JSON gốc nhận được từ MQTT để kiểm tra format
  Serial.print(">> [Raw JSON]: ");
  Serial.println(jsonString);

  DynamicJsonDocument doc(1024);
  DeserializationError error = deserializeJson(doc, jsonString);

  if (error) {
    Serial.print(">> [ERROR] JSON Deserialize failed: ");
    Serial.println(error.c_str());
    return;
  }

  // 2. Parse dữ liệu và in ra các giá trị đọc được
  String cid = doc["cid"].as<String>();
  String deviceId = doc["device"]["id"].as<String>(); 
  String typeStr = doc["device"]["type"].as<String>();
  
  // Lấy Pin. Lưu ý cú pháp | 0 để default về 0 nếu không có
  int pin1 = doc["device"]["config"]["pin"] | 0; 
  int pin2 = doc["device"]["config"]["pin2"] | 0;

  Serial.println(">> [Parsed Data]:");
  Serial.printf("   - CID: %s\n", cid.c_str());
  Serial.printf("   - Device ID: %s\n", deviceId.c_str());
  Serial.printf("   - Type String: %s\n", typeStr.c_str());
  Serial.printf("   - Pin 1: %d\n", pin1);
  Serial.printf("   - Pin 2: %d\n", pin2);

  // 3. Kiểm tra trùng lặp
  for(int i=0; i<deviceCount; i++) {
      if(String(devices[i].id) == deviceId) {
          Serial.println(">> [Info] Device ID exists. Resending ACK...");
          sendAck("create", cid, deviceId, true); 
          return;
      }
      if (devices[i].pin == pin1 && pin1 != 0) {
          Serial.printf(">> [ERROR] Pin %d is already used by another device!\n", pin1);
          sendAck("create", cid, deviceId, false);
          return;
      }
  }

  // 4. Bắt đầu tạo thiết bị và Mapping Type
  if (deviceCount < 10) {
    deviceId.toCharArray(devices[deviceCount].id, 25);
    devices[deviceCount].pin = pin1;
    devices[deviceCount].pin2 = pin2;
    devices[deviceCount].state = false;

    // --- LOGIC MAPPING (Theo cập nhật Light/Fan/Sensor) ---
    Serial.print(">> [Mapping Logic]: ");
    
    if (typeStr == "sensor") {
        devices[deviceCount].type = TYPE_SENSOR;
        Serial.println("Mapped to TYPE_SENSOR (1)");
    } 
    else if (typeStr == "fan") {
        devices[deviceCount].type = TYPE_MOTOR;
        Serial.println("Mapped to TYPE_MOTOR (2) -> Using 2 Pins");
    } 
    else if (typeStr == "light") {
        devices[deviceCount].type = TYPE_OUTPUT;
        Serial.println("Mapped to TYPE_OUTPUT (0)");
    } 
    else {
        devices[deviceCount].type = TYPE_OUTPUT;
        Serial.printf("Unknown type '%s' -> Defaulting to TYPE_OUTPUT (0)\n", typeStr.c_str());
    }

    // 5. Cấu hình GPIO thực tế
    Serial.println(">> [Hardware Setup]:");
    pinMode(devices[deviceCount].pin, OUTPUT);
    digitalWrite(devices[deviceCount].pin, LOW);
    Serial.printf("   - Pin %d set to OUTPUT/LOW\n", devices[deviceCount].pin);
    
    if (devices[deviceCount].type == TYPE_MOTOR) {
        pinMode(devices[deviceCount].pin2, OUTPUT);
        digitalWrite(devices[deviceCount].pin2, LOW);
        Serial.printf("   - Pin %d (Pin2) set to OUTPUT/LOW\n", devices[deviceCount].pin2);
    }

    deviceCount++;
    saveDevicesToNVS(); 
    
    Serial.printf(">> [Success] Device Created! Total devices: %d\n", deviceCount);
    sendAck("create", cid, deviceId, true);
  } else {
    Serial.println(">> [ERROR] Device list full (Max 10)");
    sendAck("create", cid, deviceId, false);
  }
  
  Serial.println("--------------------------------------\n");
}

void handleDelete(String jsonString) {
    DynamicJsonDocument doc(1024);
    deserializeJson(doc, jsonString);
    
    // Server gửi: { action: 'delete', cid: '...', deviceId: '...' }
    // Lưu ý: Có thể server gửi "deviceId" hoặc "device": {"id": ...} tùy code delete của bạn
    // Tôi giả định delete gửi payload đơn giản hơn create
    
    String cid = doc["cid"].as<String>();
    
    // Cần kiểm tra code deleteDevice ở server gửi key là gì. 
    // Nếu server gửi: payload = { action, cid, deviceId }
    String targetId = doc["deviceId"].as<String>(); 

    bool found = false;
    for (int i = 0; i < deviceCount; i++) {
        if (String(devices[i].id) == targetId) {
            // Reset pin về an toàn trước khi xóa
            pinMode(devices[i].pin, INPUT); 
            if (devices[i].type == TYPE_MOTOR) pinMode(devices[i].pin2, INPUT);

            // Xóa khỏi mảng (dồn mảng)
            for (int j = i; j < deviceCount - 1; j++) {
                devices[j] = devices[j + 1];
            }
            deviceCount--;
            saveDevicesToNVS();
            found = true;
            Serial.printf(">> Deleted device: %s\n", targetId.c_str());
            break;
        }
    }
    
    sendAck("delete", cid, targetId, found);
}

void handleState(String payload) {
  Serial.println("\n╔════════════════════════════════════╗");
  Serial.println("║        HANDLE STATE TRIGGERED      ║");
  Serial.println("╚════════════════════════════════════╝");

  // 1. In gói tin gốc nhận được
  Serial.print(">> [Raw Payload]: ");
  Serial.println(payload);

  DynamicJsonDocument doc(1024);
  DeserializationError error = deserializeJson(doc, payload);

  if (error) {
    Serial.print(">> [ERROR] JSON Parsing failed: ");
    Serial.println(error.c_str());
    return;
  }

  // 2. Parse dữ liệu (LƯU Ý: Đọc thẳng từ root, không qua ["payload"])
  // Nếu backend gửi: {"action":"state", "deviceId":"...", "value":"on"}
  String deviceId = doc["deviceId"].as<String>();
  String value = doc["value"].as<String>();

  Serial.println(">> [Parsed Data]:");
  Serial.printf("   - Target ID: %s\n", deviceId.c_str());
  Serial.printf("   - Command Value: %s\n", value.c_str());

  bool found = false;

  // 3. Duyệt danh sách thiết bị
  for (int i = 0; i < deviceCount; i++) {
    // Debug nhẹ để xem đang so sánh với thiết bị nào
    // Serial.printf("   ...Checking against stored ID: %s\n", devices[i].id);

    if (String(devices[i].id) == deviceId) {
      found = true;
      Serial.printf(">> [MATCH FOUND] at index %d\n", i);

      // Chuyển đổi value string sang boolean
      bool newState = (value == "on" || value == "1" || value == "true");
      devices[i].state = newState;
      
      Serial.printf("   - New State (Bool): %s\n", newState ? "TRUE (ON)" : "FALSE (OFF)");

      // 4. Thực hiện điều khiển phần cứng và Log Pin
      if (devices[i].type == TYPE_OUTPUT) {
          Serial.printf("   - Action: DigitalWrite\n");
          Serial.printf("   - Type: OUTPUT/LIGHT/RELAY (0)\n");
          Serial.printf("   - PIN: %d\n", devices[i].pin);
          Serial.printf("   - Logic: %s\n", newState ? "HIGH" : "LOW");
          
          digitalWrite(devices[i].pin, newState ? HIGH : LOW);
      } 
      else if (devices[i].type == TYPE_MOTOR) {
          Serial.printf("   - Action: ControlMotor\n");
          Serial.printf("   - Type: MOTOR/FAN (2)\n");
          Serial.printf("   - PIN A: %d\n", devices[i].pin);
          Serial.printf("   - PIN B: %d\n", devices[i].pin2);
          Serial.printf("   - Logic: %s\n", newState ? "HIGH" : "LOW");
          
          controlMotor(devices[i].pin, devices[i].pin2, newState);
      }
      else {
           Serial.printf("   - [WARNING] Unknown device type: %d\n", devices[i].type);
      }
      
      saveDevicesToNVS();
      Serial.println(">> [Success] State updated & Saved to NVS.");
      break;
    }
  }

  if (!found) {
    Serial.printf(">> [ERROR] Device ID %s NOT FOUND in local list!\n", deviceId.c_str());
    Serial.printf("   (Total devices in memory: %d)\n", deviceCount);
  }
  Serial.println("--------------------------------------\n");
}

// ================================================================
// 7. IMPLEMENTATION: MQTT LOGIC (From mqtt_logic.cpp)
// ================================================================

void mqttCallback(char* topic, byte* payload, unsigned int length) {
  // 1. Parse JSON
  DynamicJsonDocument doc(1024);
  DeserializationError error = deserializeJson(doc, payload, length);

  if (error) {
    Serial.print("JSON Error: ");
    Serial.println(error.c_str());
    return;
  }

  // 2. Lấy Action (Nằm ngay root)
  const char* actionPtr = doc["action"]; 
  if (!actionPtr) {
    Serial.println("Error: No 'action' field in JSON");
    return;
  }
  String action = String(actionPtr);

  Serial.printf(">> MQTT Recv Action: %s\n", action.c_str());

  // 3. Điều hướng
  // Lưu ý: Truyền thẳng chuỗi JSON gốc vào hàm xử lý để tiết kiệm bước serialize lại
  // Nhưng vì hàm xử lý của bạn đang nhận String, ta ép kiểu (char*)payload
  if (action == "create") {
      handleCreate(String((char*)payload)); 
  }
  else if (action == "delete") {
      handleDelete(String((char*)payload));
  }
  else if (action == "state") {
      handleState(String((char*)payload));
  }
}

void sendMqttClaimRequest() {
  if (!mqttClient.connected()) return;
  String ackTopic = "iot_nhom15/home/" + homeId + "/esp32/" + espId + "/ack";
  DynamicJsonDocument doc(256);
  doc["action"] = "create esp32";
  doc["claimToken"] = claimToken;
  String payload;
  serializeJson(doc, payload);
  mqttClient.publish(ackTopic.c_str(), payload.c_str());
}

void sendAck(String action, String cid, String deviceId, bool success) {
  String ackTopic = "iot_nhom15/home/" + homeId + "/esp32/" + espId + "/ack";
  DynamicJsonDocument doc(256);
  doc["action"] = action;
  doc["cid"] = cid;
  doc["deviceId"] = deviceId;
  doc["success"] = success;
  String ackMessage;
  serializeJson(doc, ackMessage);
  mqttClient.publish(ackTopic.c_str(), ackMessage.c_str());
}

// Thay thế hàm connectMQTT cũ bằng hàm này
void connectMQTT() {
  // 1. Nếu đã kết nối rồi thì thôi, không làm gì cả
  if (mqttClient.connected()) return;

  // 2. Cấu hình Server
  mqttClient.setServer(mqttHost.c_str(), mqttPort);
  mqttClient.setCallback(mqttCallback);

  Serial.print("Attempting MQTT connection to ");
  Serial.print(mqttHost);
  Serial.print("...");

  // 3. Thử kết nối
  if (mqttClient.connect(espDeviceId.c_str(), mqttUsername.c_str(), "")) {
      Serial.println("CONNECTED!");

      // --- LOGIC QUAN TRỌNG ĐÃ SỬA ---
      
      // TRƯỜNG HỢP A: Đang trong quá trình Claim (Lần đầu gặp Home)
      if (claimToken.length() > 0) {
          Serial.println(">> Mode: CLAIMING (Sending claim request...)");
          sendMqttClaimRequest();
          
          // Xóa token và lưu config để lần sau không vào đây nữa
          claimToken = ""; 
          saveConfigToNVS(); 
          
          Serial.println(">> Claim sent & Config saved.");
          Serial.println(">> RESTARTING in 2s to apply Normal Mode...");
          
          // QUAN TRỌNG: Chờ tin đi hết rồi Reset để nạp lại từ đầu
          delay(2000); 
          ESP.restart(); 
      } 
      // TRƯỜNG HỢP B: Chạy bình thường (Đã có Home ID, ESP ID)
      else {
          String cmdTopic = "iot_nhom15/home/" + homeId + "/esp32/" + espId + "/cmd";
          
          Serial.print(">> Subscribing to Topic: ");
          Serial.println(cmdTopic);
          
          if (mqttClient.subscribe(cmdTopic.c_str())) {
             Serial.println(">> Subscribe SUCCESS!");
          } else {
             Serial.print(">> Subscribe FAILED! Error code=");
             Serial.println(mqttClient.state());
          }
      }
  } else {
      Serial.print("Failed rc=");
      Serial.println(mqttClient.state());
      delay(2000); // Chờ chút rồi thử lại ở vòng loop sau
  }
}

// ================================================================
// 8. IMPLEMENTATION: HARDWARE / BLE / MAIN (From hardware.cpp)
// ================================================================

// --- BLE CALLBACK CLASSES ---

class MyServerCallbacks: public NimBLEServerCallbacks {
public:
    void onConnect(NimBLEServer* pServer, NimBLEConnInfo& connInfo) {
        Serial.println("\n╔═══════════════════════╗");
        Serial.println("║  BLE CLIENT CONNECTED  ║");
        Serial.println("╚═══════════════════════╝");
        Serial.printf("MTU: %d bytes\n", pServer->getPeerMTU(connInfo.getConnHandle()));
        Serial.printf("Connection ID: %d\n\n", connInfo.getConnHandle());
    }

    void onDisconnect(NimBLEServer* pServer, NimBLEConnInfo& connInfo) {
        Serial.println("\n╔═══════════════════════╗");
        Serial.println("║ BLE CLIENT DISCONNECT  ║");
        Serial.println("╚═══════════════════════╝\n");
        NimBLEDevice::startAdvertising(); 
    }
};

class MyCallbacks: public NimBLECharacteristicCallbacks {
public:
    // QUAN TRỌNG: Phải thêm tham số "NimBLEConnInfo& connInfo"
    void onWrite(NimBLECharacteristic *pCharacteristic, NimBLEConnInfo& connInfo) override {
        Serial.println("\n██████████████████████████████████");
        Serial.println("█  onWrite() TRIGGERED!          █");
        Serial.println("██████████████████████████████████");
        
        std::string value = pCharacteristic->getValue();
        
        Serial.printf("Data Length: %d bytes\n", value.length());

        if (value.length() > 0) {
            String dataStr = String(value.c_str());
            
            // In ra log để kiểm tra
            Serial.println("\n--- DATA RECEIVED ---");
            Serial.println(dataStr); 
            Serial.println("---------------------\n");
            
            // Đẩy vào buffer xử lý
            configJsonBuffer = dataStr; 
            shouldSaveConfig = true;
            
        } else {
            Serial.println("⚠ WARNING: Empty data received!");
        }
    }
};

// Tạo instance global để tránh bị garbage collector xóa
MyServerCallbacks serverCallbacks;
MyCallbacks characteristicCallbacks;

// --- SETUP BLE ---

void setupBLE() {
    Serial.println("\n╔════════════════════════════════════╗");
    Serial.println("║      INITIALIZING BLE...           ║");
    Serial.println("╚════════════════════════════════════╝\n");
    
    // Init BLE
    NimBLEDevice::init(BLE_DEVICE_NAME);
    Serial.println("✓ BLE Device initialized");
    
    // Set MTU cao để nhận gói lớn
    NimBLEDevice::setMTU(517);
    Serial.println("✓ MTU set to 517 bytes");

    // Tạo Server
    NimBLEServer *pServer = NimBLEDevice::createServer();
    pServer->setCallbacks(&serverCallbacks); // Dùng con trỏ tới instance global
    Serial.println("✓ Server created with callbacks");

    // Tạo Service
    NimBLEService *pService = pServer->createService(BLE_SERVICE_UUID);
    Serial.println("✓ Service created");

    // Tạo Characteristic - CHỈ DÙNG WRITE (không dùng WRITE_NR)
    NimBLECharacteristic *pCharacteristic = pService->createCharacteristic(
        BLE_CHARACTERISTIC_UUID,
        NIMBLE_PROPERTY::WRITE
    );
    Serial.println("✓ Characteristic created (WRITE only)");

    // Gán callback
    pCharacteristic->setCallbacks(&characteristicCallbacks); // Dùng con trỏ global
    Serial.println("✓ Characteristic callbacks set");
    
    // Start service
    pService->start();
    Serial.println("✓ Service started");

    // Setup advertising
    NimBLEAdvertising *pAdvertising = NimBLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(BLE_SERVICE_UUID);
    // pAdvertising->setScanResponse(true);
    pAdvertising->setMinInterval(32);  // 20ms
    pAdvertising->setMaxInterval(48);  // 30ms
    
    // Start advertising
    NimBLEDevice::startAdvertising();
    Serial.println("✓ Advertising started");
    
    Serial.println("\n╔═════════════════════════════════════════╗");
    Serial.println("║   BLE READY - WAITING FOR CONNECTION    ║");
    Serial.println("╠═════════════════════════════════════════╣");
    Serial.printf("║ Device Name: %-24s║\n", BLE_DEVICE_NAME);
    Serial.println("║                                         ║");
    Serial.println("║ Service UUID:                           ║");
    Serial.println("║ " BLE_SERVICE_UUID "  ║");
    Serial.println("║                                         ║");
    Serial.println("║ Characteristic UUID:                    ║");
    Serial.println("║ " BLE_CHARACTERISTIC_UUID "  ║");
    Serial.println("╚═════════════════════════════════════════╝\n");
}

// --- CONNECT WIFI ---

bool connectWiFi() {
    if (wifiSSID == "") return false;

    Serial.println("\n╔════════════════════════╗");
    Serial.println("║  CONNECTING TO WIFI... ║");
    Serial.println("╚════════════════════════╝");
    Serial.printf("SSID: %s\n", wifiSSID.c_str());
    
    WiFi.begin(wifiSSID.c_str(), wifiPassword.c_str());

    unsigned long start = millis();
    while (WiFi.status() != WL_CONNECTED && millis() - start < 15000) {
        delay(500);
        Serial.print(".");
    }

    if (WiFi.status() == WL_CONNECTED) {
        Serial.println("\n✓ WiFi Connected!");
        Serial.print("IP Address: ");
        Serial.println(WiFi.localIP());
        return true;
    } else {
        Serial.println("\n✗ WiFi Connection Failed!");
        return false;
    }
}

// --- PROCESS CONFIG DATA FROM BLE ---

void processConfigData() {
    if (!shouldSaveConfig) return;
    
    Serial.println("\n╔═══════════════════════════╗");
    Serial.println("║  PROCESSING CONFIG DATA   ║");
    Serial.println("╚═══════════════════════════╝\n");
    
    Serial.printf("Buffer length: %d bytes\n", configJsonBuffer.length());
    Serial.println("Parsing JSON...");
    
    DynamicJsonDocument doc(2048); // Tăng size lên 2048 cho config lớn
    DeserializationError error = deserializeJson(doc, configJsonBuffer);

    if (!error) {
        Serial.println("✓ JSON parsed successfully!\n");
        
        wifiSSID = doc["ssid"].as<String>();
        wifiPassword = doc["pass"].as<String>();
        mqttHost = doc["mqtt"]["host"].as<String>();
        mqttPort = doc["mqtt"]["port"];
        mqttUsername = doc["mqtt"]["username"].as<String>();
        baseTopic = doc["mqtt"]["baseTopic"].as<String>();
        espDeviceId = doc["espDeviceId"].as<String>();
        claimToken = doc["claimToken"].as<String>();

        Serial.println("--- CONFIG DETAILS ---");
        Serial.printf("WiFi SSID: %s\n", wifiSSID.c_str());
        Serial.printf("MQTT Host: %s:%d\n", mqttHost.c_str(), mqttPort);
        Serial.printf("Base Topic: %s\n", baseTopic.c_str());
        Serial.printf("Device ID: %s\n", espDeviceId.c_str());
        Serial.println("----------------------\n");

        // Parse IDs từ baseTopic
        int homeStart = baseTopic.indexOf("/home/") + 6;
        int homeEnd = baseTopic.indexOf("/esp32");
        homeId = baseTopic.substring(homeStart, homeEnd);
        int espStart = baseTopic.indexOf("/esp32/") + 7;
        espId = baseTopic.substring(espStart);

        Serial.println("Saving to NVS...");
        saveConfigToNVS();
        
        Serial.println("\n✓ Config Saved Successfully!");
        Serial.println("✓ Restarting in 3 seconds...\n");
        
        delay(3000);
        ESP.restart();
    } else {
        Serial.println("✗ JSON PARSE ERROR!");
        Serial.print("Error: ");
        Serial.println(error.c_str());
        Serial.println("\n--- RAW BUFFER CONTENT ---");
        Serial.println(configJsonBuffer);
        Serial.println("--- END BUFFER ---\n");
    }
    
    shouldSaveConfig = false;
    configJsonBuffer = "";
}

// --- CHECK BUTTON RESET ---
// Hàm kiểm tra nút BOOT (GPIO 0)
void checkButtonReset() {
    // Nếu nút được nhấn (LOW)
    if (digitalRead(0) == LOW) {
        Serial.println("\n>> Dang giu nut BOOT...");
        unsigned long pressTime = millis();
        
        // Vòng lặp chờ: Vừa giữ nút vừa đếm thời gian
        while (digitalRead(0) == LOW) {
            delay(100); // Chờ 100ms mỗi lần check
            // Nếu giữ quá 2 giây (2000ms)
            if (millis() - pressTime > 2000) {
                Serial.println("\n!!! FACTORY RESET TRIGGERED !!!");
                
                // Gọi hàm xóa NVS từ storage.h
                clearNVS(); 
                
                // Chớp LED hoặc báo hiệu (nếu có)
                delay(500);
                
                Serial.println(">> Restarting ESP32...");
                ESP.restart();
            }
        }
        Serial.println(">> Da tha nut (Huy Reset)");
    }
}

// ================================================================
// 9. ARDUINO STANDARD FUNCTIONS (SETUP & LOOP)
// ================================================================

void setup() {
    Serial.begin(115200);
    
    // --- QUAN TRỌNG: Cấu hình nút BOOT ---
    pinMode(0, INPUT_PULLUP); 
    // -------------------------------------

    delay(1000);
    
    Serial.println("\n\n\n");
    Serial.println("╔═══════════════════════════╗");
    Serial.println("║   ESP32 BOOTING UP...     ║");
    Serial.println("╚═══════════════════════════╝\n");
    

    mqttClient.setBufferSize(1024);

    // Load config từ NVS
    bool hasConfig = loadConfigFromNVS();
    loadDevicesFromNVS();

    if (!hasConfig) {
        Serial.println("⚠ No configuration found in NVS");
        Serial.println("➜ Starting BLE configuration mode...\n");
        setupBLE();
    } else {
        Serial.println("✓ Configuration found in NVS");
        Serial.println("➜ Attempting WiFi connection...\n");
        
        // --- ĐOẠN ĐÃ SỬA LỖI ---
        if (!connectWiFi()) {
            Serial.println("\n✗ WiFi connection failed!");
            Serial.println("ℹ Hold BOOT button for 2 seconds to factory reset");
            
            // Thay vì delay(5000), ta dùng vòng lặp chờ 5s
            unsigned long waitStart = millis();
            while (millis() - waitStart < 5000) {
                checkButtonReset(); // Liên tục kiểm tra nút bấm trong 5s này
                delay(50);          // Nghỉ nhẹ để không chiếm CPU
            }
            
            Serial.println("Restarting...");
            ESP.restart();
        } 
        // -----------------------
        else {
            connectMQTT();
        }
    }
}

void loop() {
    // 1. Luôn kiểm tra nút Reset ở đầu vòng lặp
    checkButtonReset(); 

    // 2. Status report mỗi 10 giây
    static unsigned long lastStatus = 0;
    if (millis() - lastStatus > 10000) {
        lastStatus = millis();
        if (wifiSSID == "") {
            Serial.println("[Status] Mode: BLE Config | Waiting for data...");
        } else {
            Serial.printf("[Status] Mode: WiFi | MQTT: %s | Uptime: %lu s\n", 
                          mqttClient.connected() ? "Connected" : "Disconnected",
                          millis() / 1000);
        }
    }

    // 3. Main logic
    if (wifiSSID == "") {
        // BLE Config Mode
        processConfigData();
    } else {
        // Normal Operation Mode
        if (!mqttClient.connected()) {
            static unsigned long lastReconnect = 0;
            if (millis() - lastReconnect > 5000) {
                lastReconnect = millis();
                Serial.println("Attempting MQTT reconnection...");
                
                // Thêm kiểm tra nút bấm ở đây phòng khi kẹt kết nối MQTT
                checkButtonReset(); 
                
                connectMQTT();
            }
        } else {
            mqttClient.loop();
            loopReadSensors();
        }
    }
}
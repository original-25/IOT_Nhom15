# IoT Nhom15 - Hệ thống nhà thông minh

**Mô tả ngắn gọn**

Dự án này là hệ thống IoT cho quản lý nhà thông minh: backend Node.js (Express, MongoDB, MQTT), frontend Android (native), và firmware cho ESP32. Hỗ trợ quản lý nhà, thiết bị, ESP32 provisioning, gửi/nhận dữ liệu qua MQTT, lịch (scheduler), xác thực người dùng và email.

---

## 🎯 Tính năng chính

- Quản lý Homes, mời thành viên, phân quyền Owner/Member
- Quản lý Devices (tạo, sửa, xóa, điều khiển)
- Quản lý ESP32 (provision, claim, status)
- Thu thập logs/telemetry từ thiết bị và lưu DeviceLog
- Gửi/nhận lệnh qua MQTT
- Lập lịch điều khiển (scheduler)
- Xác thực bằng OTP email, refresh/access token
- API docs bằng Swagger (/api-docs)

---

## 🧭 Kiến trúc & thư mục chính

- `backend/` — Node.js + Express API, kết nối MongoDB, MQTT listener
  - `config/` — cấu hình (env, jwt, swagger, mail)
  - `routers/`, `controllers/`, `models/`, `middlewares/`
  - `mqtt/` — listener & publisher
- `frontend/` — Android app (Java/Kotlin; dùng Retrofit để gọi API)
- `esp32/` — code mẫu cho ESP32

---

## ⚙️ Yêu cầu trước

- Node.js (>=16) & npm
- MongoDB instance (local or cloud)
- Android Studio (để chạy frontend) hoặc Gradle
- MQTT broker (mặc định dự án kết nối `mqtt://mqtt.flespi.io`, có thể dùng broker khác)

---

## 🛠️ Cài đặt & chạy (Backend)

1. Sao chép repository và vào thư mục backend:

```bash
cd backend
npm install
```

2. Tạo file `.env` (ví dụ):

```
PORT=4000
MONGO_URI=mongodb://localhost:27017/iot_nhom15
JWT_SECRET=your_jwt_secret
REFRESH_SECRET=your_refresh_secret
MAIL_USER=your_mail_user
MAIL_PASS=your_mail_pass
MAIL_FROM=noreply@example.com
PUB_SUB_TOKEN=your_flespi_or_mqtt_token
MQTT_URL=mqtt://mqtt.flespi.io
MQTT_PORT=1883
MQTT_PASSWORD=
```

3. Chạy server (development):

```bash
npm run dev   # dùng nodemon
# hoặc
npm start
```

4. Mở Swagger UI: `http://localhost:4000/api-docs`

---

## 📱 Chạy Frontend (Android)

- Mở `frontend/app` bằng Android Studio.
- Nếu cần thử với backend local, chỉnh `BASE_URL` trong `app/src/main/java/com/example/smarthome/network/RetrofitClient.java` (mặc định đang trỏ tới `https://iot-nhom15.onrender.com/`).
- Chạy trên emulator hoặc thiết bị thật.
- Hoặc build bằng Gradle:

```bash
cd frontend
./gradlew assembleDebug
```

---

## 🔌 MQTT

- Mặc định broker: `mqtt://mqtt.flespi.io` (configable qua `MQTT_URL`, `MQTT_PORT`).
- Token dùng để connect đặt trong `PUB_SUB_TOKEN` hoặc `MQTT_USERNAME` / `MQTT_PASSWORD`.
- Topic chính: `iot_nhom15/home/{homeId}/esp32/{espId}/...` (ví dụ: `cmd`, `data`, `status`, `ack`)

---

## 📚 API chính (tóm tắt)

- `POST /api/auth/*` — đăng ký, đăng nhập, quên mật khẩu, refresh token
- `GET/POST /api/home/*` — tạo nhà, mời người dùng, thành viên
- `POST /api/home/:homeId/esp32/provision` — provision ESP32 (owner)
- `GET/POST /api/homes/:homeId/devices*` — quản lý devices
- `GET /api/...` — xem router files (`backend/routers`) để biết chi tiết
- Swagger: `/api-docs`

---

## 🧪 Kiểm thử

- Hiện chưa có bộ test tự động trong repo; bạn có thể thêm unit/integration tests.

---

## 🤝 Đóng góp

- Fork repo, tạo feature branch, tạo pull request mô tả thay đổi.
- Vui lòng giữ coding style & thêm tests nếu có chức năng mới.

---

## 📌 License

- Hiện `package.json` để `license: ISC`. Thêm `LICENSE` nếu muốn chỉ định rõ.

---

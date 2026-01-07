// src/services/scheduler.service.js
const cron = require("node-cron");
const Schedule = require("../models/Schedule");
const EspDevice = require("../models/EspDevice");
const Device = require("../models/Device");
const { publishCommand } = require("../mqtt/mqtt.publisher");

let activeJobs = [];

exports.startScheduler = async () => {
  console.log(">> [Scheduler] (Re)Starting...");

  // 1. Dọn dẹp job cũ
  activeJobs.forEach(job => job.stop());
  activeJobs = [];

  // 2. Lấy lịch active
  const schedules = await Schedule.find({ isActive: true }).populate("device");

  for (const item of schedules) {
    if (!item.device) continue;

    const [hour, minute] = item.time.split(":");
    
    // Validate format giờ
    if(!hour || !minute) {
        console.warn(`[Scheduler] Invalid time format for schedule: ${item.name}`);
        continue;
    }

    const cronTime = `${minute} ${hour} * * *`;

    console.log(`>> Lên lịch: "${item.name}" lúc ${item.time} (VN Time) -> ${item.action}`);

    // --- [FIX QUAN TRỌNG] --- 
    // Thêm tham số timezone vào option thứ 2 của cron.schedule
    const job = cron.schedule(cronTime, async () => {
      console.log(`\n⏰ [Scheduler Triggered] ${new Date().toLocaleString()}`);
      console.log(`   Command: ${item.name} -> Device: ${item.device.name} -> Action: ${item.action}`);
      
      try {
        const esp = await EspDevice.findById(item.device.esp);
        if (esp) {
          // Gửi lệnh MQTT
          await publishCommand({
            homeId: item.home.toString(),
            espId: esp._id.toString(),
            deviceId: item.device._id.toString(),
            payload: {
              action: "state",
              value: item.action, // "on" hoặc "off"
              // Đảm bảo payload có deviceId để ESP32 nhận diện
              deviceId: item.device._id.toString() 
            }
          });
          await Device.updateOne({ _id: item.device._id }, { lastState: item.action })
          console.log("   -> Command sent to MQTT successfully.");
        } else {
            console.error("   -> ESP Device not found in DB!");
        }
      } catch (err) {
        console.error(">> [Scheduler] Error sending command:", err);
      }
    }, {
        scheduled: true,
        timezone: "Asia/Ho_Chi_Minh" // <--- BẮT BUỘC PHẢI CÓ DÒNG NÀY
    });

    activeJobs.push(job);
  }
  
  console.log(`>> [Scheduler] Total jobs running: ${activeJobs.length}`);
};
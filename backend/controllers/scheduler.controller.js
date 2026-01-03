const Schedule = require("../models/Schedule");
const { startScheduler } = require("../services/scheduler.service");

// Tạo lịch mới
exports.createSchedule = async (req, res) => {
  try {
    const { name, deviceId, time, action } = req.body;
    
    // Lưu vào DB
    const schedule = await Schedule.create({
      name,
      home: req.home._id, // Lấy từ middleware auth
      device: deviceId,
      time,   // VD: "06:30"
      action  // VD: "on"
    });

    // QUAN TRỌNG: Khởi động lại Scheduler để nạp lịch mới ngay lập tức
    await startScheduler(); 

    res.json({ success: true, data: schedule });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
};

// Xóa lịch
exports.deleteSchedule = async (req, res) => {
  try {
    await Schedule.findByIdAndDelete(req.params.id);
    
    // QUAN TRỌNG: Nạp lại để hủy cái cron job vừa xóa
    await startScheduler();

    res.json({ success: true, message: "Deleted" });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
};

// Lấy danh sách lịch
exports.getSchedules = async (req, res) => {
  try {
    const schedules = await Schedule.find({ home: req.home._id }).populate("device", "name");
    res.json({ success: true, data: schedules });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
};
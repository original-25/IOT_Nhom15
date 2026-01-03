const mongoose = require("mongoose");

const ScheduleSchema = new mongoose.Schema({
  name: { type: String, required: true }, // VD: "Bật đèn tối"
  home: { type: mongoose.Schema.Types.ObjectId, ref: "Home", required: true },
  
  // Thiết bị cần điều khiển
  device: { type: mongoose.Schema.Types.ObjectId, ref: "Device", required: true },

  // Thời gian: Lưu dạng chuỗi "HH:MM" cho dễ (VD: "18:30")
  time: { type: String, required: true }, 

  // Hành động: "on" hoặc "off"
  action: { type: String, enum: ["on", "off"], required: true },

  isActive: { type: Boolean, default: true }
}, { timestamps: true });

module.exports = mongoose.model("Schedule", ScheduleSchema);
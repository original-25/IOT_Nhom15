// src/models/DeviceLog.js
const mongoose = require("mongoose");

const DeviceLogSchema = new mongoose.Schema({
  device: {
    type: mongoose.Schema.Types.ObjectId,
    ref: "Device",
    required: true
  },

  type: {
    type: String,
    enum: ["sensor", "state", "event"],
    required: true
  },

  data: mongoose.Schema.Types.Mixed,  // ví dụ: { temp: 30.2, hum: 65 }

  createdAt: {
    type: Date,
    default: Date.now
  }

});

// Index để query lịch sử nhanh
DeviceLogSchema.index({ device: 1, createdAt: -1 });

module.exports = mongoose.model("DeviceLog", DeviceLogSchema);

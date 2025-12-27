// src/models/Device.js
const mongoose = require("mongoose");

const DeviceSchema = new mongoose.Schema({

  // Thừa ->> bỏ qua
  // deviceId: {
  //   type: String,
  //   required: true,
  //   unique: true
  // },

  name: String,

  type: {
    type: String,
    enum: ["relay", "sensor", "light", "fan", "switch", "custom"],
    required: true
  },

  home: {
    type: mongoose.Schema.Types.ObjectId,
    ref: "Home",
    required: true
  },

  esp: {
    type: mongoose.Schema.Types.ObjectId,
    ref: "EspDevice",
    required: true
  },

  lastState: mongoose.Schema.Types.Mixed,

  config: mongoose.Schema.Types.Mixed,

  settings: mongoose.Schema.Types.Mixed,

  status: {
    type: String,
    enum: ["online", "offline", "error"],
    default: "online"
  }
}, {
  timestamps: true
});


module.exports = mongoose.model("Device", DeviceSchema);

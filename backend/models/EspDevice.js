// src/models/EspDevice.js
const mongoose = require("mongoose");

const EspDeviceSchema = new mongoose.Schema({
  espId: {
    type: String,
    required: true,
    unique: true
  },

  name: {
    type: String,
    default: "ESP32"
  },

  home: {
    type: mongoose.Schema.Types.ObjectId,
    ref: "Home",
    required: true
  },

  espToken: {
    type: String,
    required: true
  },

  mqttTopics: {
    base: String,
    control: String,
    sensor: String,
  },

  devices: [
    {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Device"
    }
  ],

  wifiSSID: String,
  wifiPassword: String
}, {
  timestamps: true
});

module.exports = mongoose.model("EspDevice", EspDeviceSchema);

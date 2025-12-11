// src/models/Device.js
const mongoose = require("mongoose");

const DeviceSchema = new mongoose.Schema({
  deviceId: {
    type: String,
    required: true,
    unique: true
  },

  name: {
    type: String,
    required: true
  },

  type: {
    type: String,
    enum: ["relay", "sensor", "light", "fan", "switch", "custom"],
    required: true
  },

  esp: {
    type: mongoose.Schema.Types.ObjectId,
    ref: "EspDevice",
    required: true
  },

  mqttPaths: {
    controlTopic: String,
    sensorTopic: String
  },

  lastValue: mongoose.Schema.Types.Mixed,
  settings: mongoose.Schema.Types.Mixed,
  config: mongoose.Schema.Types.Mixed
}, {
  timestamps: true
});

module.exports = mongoose.model("Device", DeviceSchema);

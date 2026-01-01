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
    // "sensor", "light", "fan"
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
    enum: ["provisioning", "online", "offline", "error"],
    default: "online"
  }
  ,
  // provisioning helpers
  provisioningCid: { type: String, default: null },
  provisioningExpiresAt: { type: Date, default: null },
  provisionAttempts: { type: Number, default: 0 },
  provisionedAt: { type: Date, default: null },
  provisionFailedAt: { type: Date, default: null }
}, {
  timestamps: true
});


module.exports = mongoose.model("Device", DeviceSchema);

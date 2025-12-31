// src/models/EspDevice.js
const mongoose = require("mongoose");

const EspDeviceSchema = new mongoose.Schema({

  name: String,

  home: { type: mongoose.Schema.Types.ObjectId, ref: "Home" },

  // ===== Provisioning =====
  claimTokenHash: String,
  claimExpiresAt: Date,
  claimedAt: Date,

  // ===== MQTT =====
  //
  mqttUsername: String,
  flespiTokenId: String,

  mqttBaseTopic: String, // iot/home/{homeId}/esp/{espDeviceId}
  
  status: {
    type: String,
    enum: ["unclaimed", "provisioned", "online", "offline"],
    default: "unclaimed"
  }

}, { timestamps: true });


module.exports = mongoose.model("EspDevice", EspDeviceSchema);

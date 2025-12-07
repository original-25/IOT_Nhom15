// src/models/Invitation.js
const mongoose = require("mongoose");

const InvitationSchema = new mongoose.Schema({
  home: {
    type: mongoose.Schema.Types.ObjectId,
    ref: "Home",
    required: true
  },

  //người mời
  inviter: {
    type: mongoose.Schema.Types.ObjectId,
    ref: "User",
    required: true
  },

  //email người được mời
  inviteeEmail: {
    type: String,
    required: true
  },

  token: {
    type: String,
    required: true,
    unique: true
  },

  status: {
    type: String,
    enum: ["pending", "accepted", "declined", "expired"],
    default: "pending"
  },

  expiresAt: {
    type: Date,
    required: true
  }

}, { timestamps: true });

InvitationSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });

module.exports = mongoose.model("Invitation", InvitationSchema);

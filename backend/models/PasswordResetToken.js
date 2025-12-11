// src/models/PasswordResetToken.js
const mongoose = require("mongoose");

const PasswordResetTokenSchema = new mongoose.Schema({
  email: {
    type: String,
    required: true,
    lowercase: true
  },

  token: {
    type: String,
    required: true,
    index: true
  },

  expiresAt: {
    type: Date,
    required: true
  }

}, {
  timestamps: true
});

PasswordResetTokenSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });

module.exports = mongoose.model("PasswordResetToken", PasswordResetTokenSchema);

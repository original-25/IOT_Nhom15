const mongoose = require("mongoose");
const bcrypt = require("bcryptjs");

const PasswordResetOtpSchema = new mongoose.Schema({
  email: {
    type: String,
    required: true,
    lowercase: true
  },

  token: {     // OTP đã hash
    type: String,
    required: true,
    index: true
  },

  used: {
    type: Boolean,
    default: false
  },

  resetToken: {
    type: String,
    default: null // tạo khi OTP verify
  },

  expiresAt: {
    type: Date,
    required: true
  }

}, {
  timestamps: true
});

// TTL auto delete
PasswordResetOtpSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });

// Hash OTP before save
PasswordResetOtpSchema.pre("save", async function(next) {
  if (!this.isModified("token")) return next();
  this.token = await bcrypt.hash(this.token, 10);
  next();
});

// Compare OTP
PasswordResetOtpSchema.methods.compareToken = async function(otp) {
  return bcrypt.compare(otp, this.token);
};

module.exports = mongoose.model("PasswordResetOtp", PasswordResetOtpSchema);

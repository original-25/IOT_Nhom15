const mongoose = require("mongoose");
const bcrypt = require("bcryptjs");

const EmailVerificationSchema = new mongoose.Schema({
  email: {
    type: String,
    required: true,
    lowercase: true,
  },

  otp: {
    type: String, // hashed OTP
    required: true,
  },

  expiresAt: {
    type: Date,
    required: true,
  },

  used: {
    type: Boolean,
    default: false,
  },

  verifyToken: {
    type: String, // token tạm sau khi user verify OTP
    default: null
  }
}, {
  timestamps: true
});

/**
 * TTL index
 * Auto delete OTP sau khi expiresAt
 */
EmailVerificationSchema.index(
  { expiresAt: 1 },
  { expireAfterSeconds: 0 }
);

/**
 * Hash OTP trước khi save
 */
EmailVerificationSchema.pre("save", async function (next) {
  if (!this.isModified("otp")) return next();
  this.otp = await bcrypt.hash(this.otp, 10);
  next();
});

/**
 * So sánh OTP người dùng nhập với hash
 */
EmailVerificationSchema.methods.compareOTP = function (otp) {
  return bcrypt.compare(otp, this.otp);
};

module.exports = mongoose.model("EmailVerification", EmailVerificationSchema);

// src/models/User.js
const mongoose = require("mongoose");
const bcrypt = require("bcryptjs");


const UserSchema = new mongoose.Schema({
  email: {
    type: String,
    required: true,
    unique: true,
    lowercase: true,
    index: true
  },
  password: {
    type: String,
    required: true
  },
  username: {
    type: String,
    default: "User"
  },

  role: {
    type: String,
    enum: ["user", "admin"],
    default: "user"
  }
}, {
  timestamps: true
});

UserSchema.pre('save', async function (next) {
  if (!this.isModified('password')) return next(); // Nếu mật khẩu không thay đổi, bỏ qua

  try {
    const salt = await bcrypt.genSalt(10);  // Tạo salt với độ dài 10
    this.password = await bcrypt.hash(this.password, salt); // Hash mật khẩu
    next(); // Tiến hành lưu vào cơ sở dữ liệu
  } catch (error) {
    next(error); // Nếu có lỗi trong quá trình hash, chuyền lỗi ra ngoài
  }
});


UserSchema.methods.comparePassword = function (password) {
  return bcrypt.compare(password, this.password);
};

module.exports = mongoose.model("User", UserSchema);

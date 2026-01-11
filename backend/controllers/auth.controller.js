const User = require("../models/User");
const PasswordResetOtp = require("../models/PasswordResetOTP");
// const sendEmail = require("../utils/sendEmail");
const { generateAccessToken, generateRefreshToken } = require("../utils/token");
const EmailVerification = require("../models/EmaiVerification");
const crypto = require("crypto");
const jwt = require("jsonwebtoken");

const sendEmail = require("../config/mailer");


// REGISTER
// ########################################################
module.exports.sendRegisterReq = async (req, res) => {
  try {
    console.log('Request Method:', req.method);
    console.log('Request Headers:', req.headers);
    console.log('Request Body:', req.body);
    
    const { email } = req.body;
    console.log("📧 [REGISTER] Step 1: Checking if email exists:", email);

    const exists = await User.findOne({ email });
    if (exists) {
      console.log("❌ [REGISTER] Email already exists");
      return res.status(409).json({ 
        success: false,
        message: "Email already registered",
        errorCode: "EMAIL_EXISTS"
      });
    }
    console.log("✅ [REGISTER] Step 2: Email available, proceeding...");

    await EmailVerification.deleteMany({ email });
    console.log("🗑️ [REGISTER] Step 3: Cleared old OTP records");

    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    console.log("🔢 [REGISTER] Step 4: Generated OTP:", otp);

    await EmailVerification.create({
      email,
      otp,
      expiresAt: new Date(Date.now() + 10 * 60 * 1000)
    });
    console.log("💾 [REGISTER] Step 5: OTP saved to database");

    console.log("📨 [REGISTER] Step 6: Starting to send email...");
    console.log("   → To:", email);
    console.log("   → Subject: OTP Verification");
    
    try {
      // --- sendRegisterReq ---
      await sendEmail(
        email,
        "OTP Verification",
        `<h2>${otp}</h2><p>OTP expires in 10 minutes.</p>`
      );

      console.log("✅ [REGISTER] Step 7: Email sent successfully!");
    } catch (emailError) {
      console.error("❌ [REGISTER] Failed to send email:");
      console.error("   → Error name:", emailError.name);
      console.error("   → Error message:", emailError.message);
      console.error("   → Full error:", emailError);
      
      // Xóa OTP vừa tạo nếu gửi email fail
      await EmailVerification.deleteMany({ email });
      console.log("🗑️ [REGISTER] Cleaned up OTP after email failure");
      
      return res.status(500).json({
        success: false,
        message: "Failed to send verification email. Please check your email configuration.",
        errorCode: "EMAIL_SEND_FAILED",
        details: emailError.message
      });
    }

    console.log("🎉 [REGISTER] Registration request completed successfully");
    return res.status(200).json({ 
      success: true,
      message: "OTP sent" 
    });
  } catch (error) {
    console.error("Send register OTP error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error",
      errorCode: "SERVER_ERROR"
    });
  }
};


module.exports.verifyRegisterOtp = async (req, res) => {
  try {
    const { email, otp } = req.body;

    const record = await EmailVerification.findOne({ email });
    if (!record) {
      return res.status(404).json({ 
        success: false,
        message: "OTP invalid",
        errorCode: "OTP_NOT_FOUND"
      });
    }

    if (record.used) {
      return res.status(400).json({ 
        success: false,
        message: "OTP already used",
        errorCode: "OTP_ALREADY_USED"
      });
    }

    if (record.expiresAt < Date.now()) {
      return res.status(400).json({ 
        success: false,
        message: "OTP expired",
        errorCode: "OTP_EXPIRED"
      });
    }

    const match = await record.compareOTP(otp);
    if (!match) {
      return res.status(400).json({ 
        success: false,
        message: "Incorrect OTP",
        errorCode: "INCORRECT_OTP"
      });
    }

    // OTP OK → generate token tạm
    const verifyToken = crypto.randomBytes(32).toString("hex");

    record.used = true;
    record.verifyToken = verifyToken;
    await record.save();

    return res.status(200).json({
      success: true,
      message: "OTP verified",
      data: {
        verifyToken
      }
    });
  } catch (error) {
    console.error("Verify register OTP error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error",
      errorCode: "SERVER_ERROR"
    });
  }
};


module.exports.createAccount = async (req, res) => {
  try {
    const { verifyToken, email, username, password } = req.body;

    const record = await EmailVerification.findOne({ email, verifyToken });
    if (!record) {
      return res.status(400).json({ 
        success: false,
        message: "Invalid verification step",
        errorCode: "INVALID_VERIFY_TOKEN"
      });
    }

    // Xóa token để không dùng lại
    await EmailVerification.deleteMany({ email });

    const user = await User.create({
      email,
      username,
      password  // pre('save') hashing
    });

    return res.status(201).json({
      success: true,
      message: "Account created successfully",
      data: {
        user: {
          id: user._id,
          email: user.email,
          username: user.username
        }
      }
    });
  } catch (error) {
    console.error("Create account error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error",
      errorCode: "SERVER_ERROR"
    });
  }
};

// ########################################################

module.exports.login = async (req, res) => {
  try {
    const { email, password } = req.body;

    const user = await User.findOne({ email });
    if (!user) {
      return res.status(401).json({ 
        success: false,
        message: "Invalid credentials",
        errorCode: "INVALID_CREDENTIALS"
      });
    }

    const isMatch = await user.comparePassword(password);
    if (!isMatch) {
      return res.status(401).json({ 
        success: false,
        message: "Invalid credentials",
        errorCode: "INVALID_CREDENTIALS"
      });
    }

    const accessToken = generateAccessToken({
      id: user._id,
      role: user.role
    });

    const refreshToken = generateRefreshToken({
      id: user._id,
      role: user.role
    });

    return res.status(200).json({
      success: true,
      message: "Login successful",
      data: {
        accessToken,
        refreshToken,
        user: {
          id: user._id,
          email: user.email,
          role: user.role,
          username: user.username
        }
      }
    });
  } catch (error) {
    console.error("Login error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error",
      errorCode: "SERVER_ERROR"
    });
  }
};

// ########################################################

module.exports.forgotPassword = async (req, res) => {
  try {
    const { email } = req.body;

    const user = await User.findOne({ email });
    if (!user) {
      // tránh lộ email tồn tại hay không
      return res.status(200).json({ 
        success: true,
        message: "If email exists, OTP has been sent"
      });
    }

    // OTP 6 số
    const otp = Math.floor(100000 + Math.random() * 900000).toString();

    // Xóa OTP cũ
    await PasswordResetOtp.deleteMany({ email });

    await PasswordResetOtp.create({
      email,
      token: otp, // hashed in pre-save
      expiresAt: new Date(Date.now() + 10 * 60 * 1000),
    });


    // --- forgotPassword ---
    await sendEmail(email, "Password reset code", `
      <h2>${otp}</h2>
      <p>This code expires in 10 minutes.</p>
    `);


    return res.status(200).json({ 
      success: true,
      message: "OTP sent to email" 
    });
  } catch (error) {
    console.error("Forgot password error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error",
      errorCode: "SERVER_ERROR"
    });
  }
};


module.exports.verifyOtp = async (req, res) => {
  try {
    const { email, otp } = req.body;

    const record = await PasswordResetOtp.findOne({ email });

    if (!record) {
      return res.status(404).json({ 
        success: false,
        message: "OTP invalid",
        errorCode: "OTP_NOT_FOUND"
      });
    }

    if (record.used) {
      return res.status(400).json({ 
        success: false,
        message: "OTP already used",
        errorCode: "OTP_ALREADY_USED"
      });
    }

    if (record.expiresAt < Date.now()) {
      return res.status(400).json({ 
        success: false,
        message: "OTP expired",
        errorCode: "OTP_EXPIRED"
      });
    }

    const match = await record.compareToken(otp);
    if (!match) {
      return res.status(400).json({ 
        success: false,
        message: "Incorrect OTP",
        errorCode: "INCORRECT_OTP"
      });
    }

    // Tạo reset token bảo mật
    const resetToken = crypto.randomBytes(32).toString("hex");

    record.used = true;
    record.resetToken = resetToken;
    await record.save();

    // 👇 Trả resetToken cho app (không gửi email)
    return res.status(200).json({
      success: true,
      message: "OTP verified",
      data: {
        resetToken
      }
    });
  } catch (error) {
    console.error("Verify OTP error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error",
      errorCode: "SERVER_ERROR"
    });
  }
};


module.exports.resetPassword = async (req, res) => {
  try {
    const { email, resetToken, newPassword } = req.body;

    const record = await PasswordResetOtp.findOne({ email });

    if (!record) {
      return res.status(404).json({ 
        success: false,
        message: "Invalid request",
        errorCode: "RESET_TOKEN_NOT_FOUND"
      });
    }

    if (!record.used) {
      return res.status(400).json({ 
        success: false,
        message: "OTP not verified",
        errorCode: "OTP_NOT_VERIFIED"
      });
    }

    if (record.resetToken !== resetToken) {
      return res.status(400).json({ 
        success: false,
        message: "Invalid reset token",
        errorCode: "INVALID_RESET_TOKEN"
      });
    }

    const user = await User.findOne({ email });
    if (!user) {
      return res.status(404).json({ 
        success: false,
        message: "Email not found",
        errorCode: "USER_NOT_FOUND"
      });
    }

    // cập nhật password (pre-save sẽ hash)
    user.password = newPassword;
    await user.save();

    // Xóa OTP
    await record.deleteOne();

    return res.status(200).json({ 
      success: true,
      message: "Password reset successfully" 
    });
  } catch (error) {
    console.error("Reset password error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error",
      errorCode: "SERVER_ERROR"
    });
  }
};


// ########################################################

module.exports.refreshToken = async (req, res) => {
  try {
    const { refreshToken } = req.body;
    
    if (!refreshToken) {
      return res.status(400).json({ 
        success: false,
        message: "Refresh token is required",
        errorCode: "MISSING_REFRESH_TOKEN"
      });
    }

    const decoded = jwt.verify(refreshToken, process.env.REFRESH_SECRET);

    const accessToken = jwt.sign(
      { id: decoded.id, role: decoded.role },
      process.env.ACCESS_SECRET,
      { expiresIn: "15m" }
    );

    return res.status(200).json({ 
      success: true,
      message: "Token refreshed successfully",
      data: {
        accessToken
      }
    });
  } catch (err) {
    console.error("Refresh token error:", err);
    return res.status(401).json({ 
      success: false,
      message: "Refresh token expired or invalid",
      errorCode: "INVALID_REFRESH_TOKEN"
    });
  }
};

// ########################################################

module.exports.getUserById = async (req, res) => {
  try {
    const { userId } = req.params;

    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({
        success: false,
        message: "User not found",
        errorCode: "USER_NOT_FOUND"
      });
    }

    return res.status(200).json({
      success: true,
      message: "User retrieved successfully",
      data: {
        user: {
          id: user._id,
          email: user.email,
          username: user.username,
          role: user.role
        }
      }
    });
  } catch (error) {
    console.error("Get user by ID error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error",
      errorCode: "SERVER_ERROR"
    });
  }
};








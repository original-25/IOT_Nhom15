const nodemailer = require("nodemailer");
const { mailUser, mailPass } = require("./env");

console.log("📧 [MAILER CONFIG]");
console.log("   → Mail User:", mailUser ? `${mailUser.substring(0, 3)}***` : "NOT SET");
console.log("   → Mail Pass:", mailPass ? "***SET***" : "NOT SET");

const transporter = nodemailer.createTransport({
  host: "smtp.gmail.com",
  port: 465,
  secure: true, // SSL
  auth: {
    user: mailUser,
    pass: mailPass
  }
});

// Test connection
transporter.verify(function (error, success) {
  if (error) {
    console.error("❌ [MAILER] Connection failed:", error.message);
  } else {
    console.log("✅ [MAILER] Server is ready to send emails");
  }
});

module.exports = transporter;

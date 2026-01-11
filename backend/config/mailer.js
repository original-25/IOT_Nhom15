// config/mailer.js
const sgMail = require("@sendgrid/mail");
require("dotenv").config();

// Lấy trực tiếp từ process.env
const SENDGRID_API_KEY = process.env.SENDGRID_API_KEY;
const MAIL_FROM = process.env.MAIL_FROM;

const MAIL_USER = process.env.MAIL_USER

if (!SENDGRID_API_KEY) {
  throw new Error("❌ [MAILER CONFIG] SENDGRID_API_KEY is not set in .env");
}
if (!MAIL_FROM) {
  throw new Error("❌ [MAILER CONFIG] MAIL_FROM is not set in .env");
}

sgMail.setApiKey(SENDGRID_API_KEY);

console.log("📧 [MAILER CONFIG]");
console.log("   → From:", MAIL_USER);

async function sendEmail(to, subject, html) {
  console.log("📮 [SEND EMAIL] Attempting to send email...");
  console.log("   → From:", MAIL_FROM);
  console.log("   → To:", to);
  console.log("   → Subject:", subject);

  const msg = {
    to,
    from: MAIL_USER,
    subject,
    html
  };

  try {
    const info = await sgMail.send(msg); // trả về array nếu gửi nhiều email
    console.log("✅ [SEND EMAIL] Email sent successfully!");
    return info;
  } catch (error) {
    console.error("❌ [SEND EMAIL] Failed to send email:");
    if (error.response) {
      console.error("   → Response body:", error.response.body);
    } else {
      console.error("   → Error message:", error.message);
    }
    throw error;
  }
}

module.exports = sendEmail;

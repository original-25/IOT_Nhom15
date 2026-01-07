const transporter = require("../config/mailer");
const { mailFrom } = require("../config/env");

module.exports = async (to, subject, html) => {
  console.log("📮 [SEND EMAIL] Attempting to send email...");
  console.log("   → From:", mailFrom);
  console.log("   → To:", to);
  console.log("   → Subject:", subject);
  
  try {
    const info = await transporter.sendMail({
      from: mailFrom,
      to,
      subject,
      html
    });
    
    console.log("✅ [SEND EMAIL] Email sent successfully!");
    console.log("   → Message ID:", info.messageId);
    console.log("   → Response:", info.response);
    
    return info;
  } catch (error) {
    console.error("❌ [SEND EMAIL] Failed to send email:");
    console.error("   → Error code:", error.code);
    console.error("   → Error message:", error.message);
    throw error;
  }
};

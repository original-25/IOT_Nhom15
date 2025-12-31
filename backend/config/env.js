require("dotenv").config();

module.exports = {
  port: process.env.PORT || 4000,

  mongoUri: process.env.MONGO_URI,

  mailUser: process.env.MAIL_USER,
  mailPass: process.env.MAIL_PASS,
  mailFrom: process.env.MAIL_FROM || "noreply@example.com",

  // clientUrl: process.env.CLIENT_URL || "http://localhost:5173"
};

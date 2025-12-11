const nodemailer = require("nodemailer");
const { mailUser, mailPass } = require("./env");

module.exports = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: mailUser,
    pass: mailPass
  }
});

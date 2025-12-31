const transporter = require("../config/mailer");
const { mailFrom } = require("../config/env");

module.exports = async (to, subject, html) => {
  await transporter.sendMail({
    from: mailFrom,
    to,
    subject,
    html
  });
};

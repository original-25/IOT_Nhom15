module.exports = {
  accessSecret: process.env.JWT_SECRET,
  accessExpiresIn: process.env.JWT_EXPIRES || "7d",

  refreshSecret: process.env.REFRESH_SECRET,
  refreshExpiresIn: process.env.REFRESH_EXPIRES || "30d",
};

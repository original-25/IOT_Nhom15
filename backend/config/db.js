const mongoose = require("mongoose");
const { mongoUri } = require("./env");

module.exports = async () => {
  try {
    await mongoose.connect(mongoUri);
    console.log("MongoDB connected");
  } catch (err) {
    console.error("MongoDB connect error:", err);
    process.exit(1);
  }
};

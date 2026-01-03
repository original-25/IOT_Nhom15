const mongoose = require("mongoose");
const { mongoUri } = require("./env");
const { startScheduler } = require("../services/scheduler.service");

module.exports = async () => {
  try {
    await mongoose.connect(mongoUri)
    .then(() => {
     console.log("DB Connected");
     
     // Kích hoạt Scheduler
     startScheduler(); 
  });
    
  } catch (err) {
    console.error("MongoDB connect error:", err);
    process.exit(1);
  }
};

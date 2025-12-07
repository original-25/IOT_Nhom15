// src/models/Home.js
const mongoose = require("mongoose");

const HomeSchema = new mongoose.Schema({
  name: {
    type: String,
    required: true
  },

  members: [
    {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      role: ["owner", "viewer", "editor"]
    }
  ]
}, {
  timestamps: true
});

module.exports = mongoose.model("Home", HomeSchema);

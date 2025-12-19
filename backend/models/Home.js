// src/models/Home.js
const mongoose = require("mongoose");

const HomeSchema = new mongoose.Schema({
  name: {
    type: String,
    required: true
  },

  members: [
    {
      user: {
        type: mongoose.Schema.Types.ObjectId,
        ref: "User",
        required: true
      },
      role: {
        type: String,
        enum: ["owner", "member"],
        default: "member"
      }
    }
  ]
}, {
  timestamps: true
});

module.exports = mongoose.model("Home", HomeSchema);
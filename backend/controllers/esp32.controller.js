const crypto = require("crypto");
const bcrypt = require("bcryptjs");
const EspDevice = require("../models/EspDevice");
const { createEsp32MqttToken, deleteAclToken } = require("../services/flespi.service");

/* =====================================================
   HELPERS
===================================================== */

function hashToken(token) {
  return crypto.createHash("sha256").update(token).digest("hex");
}

function generateClaimToken() {
  // Token ngắn, dễ nhập khi provision
  return crypto.randomBytes(4).toString("hex").toUpperCase();
}

/* =====================================================
   APP APIs (AUTH REQUIRED)
===================================================== */

/**
 * POST /homes/:homeId/esp32/provision
 * Chỉ HOME OWNER mới được gọi
 */
exports.provisionEsp32 = async (req, res) => {
  try {
    const { name } = req.body;
    const home = req.home; // từ requireHomeOwner

    const claimToken = generateClaimToken();

    // Create ESP32 device first
    const device = await EspDevice.create({
      name: name || "ESP32",
      home: home._id,
      claimTokenHash: hashToken(claimToken),
      claimExpiresAt: new Date(Date.now() + 10 * 60 * 1000), // 10 phút
      status: "unclaimed"
    });

    const espDeviceId = device._id.toString();

    // Create Flespi MQTT token immediately
    const {
      flespiTokenId,
      mqttUsername,
      ttl
    } = await createEsp32MqttToken({
      homeId: home._id.toString(),
      espDeviceId
    });

    // Update device with MQTT credentials
    device.mqttUsername = mqttUsername;
    device.flespiTokenId = flespiTokenId;
    device.mqttBaseTopic = `iot_nhom15/home/${home._id}/esp32/${espDeviceId}`;
    await device.save();

    // Set timeout to delete device if not claimed within 3 minutes
    setTimeout(async () => {
      try {
        const unclaimed = await EspDevice.findOne({
          _id: espDeviceId,
          status: "unclaimed"
        });
        
        if (unclaimed) {
          console.log(`ESP32 ${espDeviceId} not claimed within 3 minutes, deleting...`);
          // Revoke Flespi token
          if (unclaimed.flespiTokenId) {
            try {
              const accessToken = process.env.MASTER_FLESPI_TOKEN;
              await deleteAclToken(accessToken, unclaimed.flespiTokenId);
            } catch (err) {
              console.error("Error revoking Flespi token:", err);
            }
          }
          await unclaimed.deleteOne();
        }
      } catch (err) {
        console.error("Error in provision timeout:", err);
      }
    }, 10 * 60 * 1000); // 3 minutes

    return res.json({
      success: true,
      espDeviceId: device._id,
      claimToken,
      expiresIn: 180,
      mqtt: {
        host: "mqtt.flespi.io",
        port: 1883,
        username: device.mqttUsername,
        baseTopic: device.mqttBaseTopic
      }
    });
  } catch (error) {
    console.error("provisionEsp32 error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error"
    });
  }
};

exports.getEsp32List = async (req, res) => {
  try {
    const home = req.home;

    const devices = await EspDevice.find({ home: home._id })
      .select("-mqttUsername -claimTokenHash");

    return res.json({
      success: true,
      data: devices
    });
  } catch (error) {
    console.error("getEsp32List error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error"
    });
  }
};

/**
 * GET /homes/:homeId/esp32/:id
 * HOME MEMBER
 */
exports.getEsp32Detail = async (req, res) => {
  try {
    const home = req.home;
    const { id } = req.params;

    const device = await EspDevice.findOne({
      _id: id,
      home: home._id
    }).select("-mqttUsername -claimTokenHash");

    if (!device) {
      return res.status(404).json({
        success: false,
        message: "ESP32 not found"
      });
    }

    return res.json({
      success: true,
      data: device
    });
  } catch (error) {
    console.error("getEsp32Detail error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error"
    });
  }
};

/**
 * GET /homes/:homeId/esp32/:id/status
 * HOME MEMBER
 */
exports.getEsp32Status = async (req, res) => {
  try {
    const home = req.home;
    const { id } = req.params;

    const device = await EspDevice.findOne({
      _id: id,
      home: home._id
    }).select("status name");

    if (!device) {
      return res.status(404).json({
        success: false,
        message: "ESP32 not found"
      });
    }

    return res.json({
      success: true,
      data: {
        id: device._id,
        name: device.name,
        status: device.status
      }
    });
  } catch (error) {
    console.error("getEsp32Status error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error"
    });
  }
};

/**
 * PATCH /homes/:homeId/esp32/:id
 * HOME OWNER
 */
exports.updateEsp32 = async (req, res) => {
  try {
    const home = req.home;
    const { id } = req.params;
    const { name } = req.body;

    const device = await EspDevice.findOneAndUpdate(
      { _id: id, home: home._id },
      { name },
      { new: true }
    );

    if (!device) {
      return res.status(404).json({
        success: false,
        message: "ESP32 not found"
      });
    }

    return res.json({
      success: true,
      data: device
    });
  } catch (error) {
    console.error("updateEsp32 error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error"
    });
  }
};

/**
 * DELETE /homes/:homeId/esp32/:id
 * HOME OWNER
 */
exports.deleteEsp32 = async (req, res) => {
  try {
    const home = req.home;
    const { id } = req.params;

    const device = await EspDevice.findOne({
      _id: id,
      home: home._id
    });

    if (!device) {
      return res.status(404).json({
        success: false,
        message: "ESP32 not found"
      });
    }
    

    if (device.mqttUsername) {
      const accessToken = process.env.MASTER_FLESPI_TOKEN; // Lấy Flespi access token từ biến môi trường
      try {
        await deleteAclToken(accessToken, device.mqttUsername); // Gọi hàm revoke token
        
      } catch (error) {
        console.error('Error revoking Flespi token:', error);
        return res.status(500).json({
          success: false,
          message: 'Failed to revoke Flespi token'
        });
      }
    }

    // TODO: revoke flespi token bằng device.flespiTokenId
    await device.deleteOne();

    return res.json({
      success: true,
      message: "ESP32 deleted"
    });
  } catch (error) {
    console.error("deleteEsp32 error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error"
    });
  }
};

/* =====================================================
   ESP32 API (NO AUTH)
===================================================== */



const express = require("express");
const router = express.Router();
const deviceController = require("../controllers/device.controller");
const { authenticate } = require("../middlewares/auth.middleware");
const { requireHomeMember, requireHomeOwner } = require("../middlewares/home.middleware");

// 1 POST /homes/:homeId/devices - create (owner only)
router.post("/homes/:homeId/devices", authenticate, requireHomeOwner, deviceController.createDevice);

// 2 GET /homes/:homeId/devices - list by home (member)
router.get("/homes/:homeId/devices", authenticate, requireHomeMember, deviceController.listDevicesByHome);

// GET /homes/:homeId/esp32/:espId/devices - list devices for esp in home (member)
router.get("/homes/:homeId/esp32/:espId/devices", authenticate, requireHomeMember, deviceController.listDevicesByEspInHome);

// 3 GET /homes/:homeId/devices/:id - detail (member)
router.get("/homes/:homeId/devices/:id", authenticate, requireHomeMember, deviceController.getDevice);

// 4 PATCH /homes/:homeId/devices/:id - update (member)
router.patch("/homes/:homeId/devices/:id", authenticate, requireHomeMember, deviceController.updateDevice);

// 5 DELETE /homes/:homeId/devices/:id - remove (owner)
router.delete("/homes/:homeId/devices/:id", authenticate, requireHomeOwner, deviceController.deleteDevice);

// 6 POST /homes/:homeId/devices/:id/command - send command (member)
router.post("/homes/:homeId/devices/:id/command", authenticate, requireHomeMember, deviceController.sendCommand);

// 7 GET /homes/:homeId/devices/:id/state - get state (member)
router.get("/homes/:homeId/devices/:id/state", authenticate, requireHomeMember, deviceController.getDeviceState);

// 8 GET /homes/:homeId/devices/:id/logs - history (member)
router.get("/homes/:homeId/devices/:id/logs", authenticate, requireHomeMember, deviceController.getDeviceLogs);

// 9 GET /homes/:homeId/devices/:id/logs/latest - latest logs (member)
router.get("/homes/:homeId/devices/:id/logs/latest", authenticate, requireHomeMember, deviceController.getDeviceLogsLatest);

// 10 GET /esp/:espId/devices - list devices by esp (authenticated)
router.get("/esp/:espId/devices", authenticate, deviceController.listDevicesByEsp);

module.exports = router;

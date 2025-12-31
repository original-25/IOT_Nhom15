const router = require("express").Router();
const ctrl = require("../controllers/esp32.controller");
const { authenticate } = require("../middlewares/auth.middleware");
const {
  requireHomeMember,
  requireHomeOwner
} = require("../middlewares/home.middleware");

/* ================= APP APIs ================= */

// OWNER mới được add ESP32
router.post(
  "/home/:homeId/esp32/provision",
  authenticate,
  requireHomeOwner,
  ctrl.provisionEsp32
);

// MEMBER được xem list
router.get(
  "/home/:homeId/esp32",
  authenticate,
  requireHomeMember,
  ctrl.getEsp32List
);

// MEMBER được xem chi tiết
router.get(
  "/home/:homeId/esp32/:id",
  authenticate,
  requireHomeMember,
  ctrl.getEsp32Detail
);

// MEMBER được xem status
router.get(
  "/home/:homeId/esp32/:id/status",
  authenticate,
  requireHomeOwner,
  ctrl.getEsp32Status
);

// OWNER được sửa
router.patch(
  "/home/:homeId/esp32/:id",
  authenticate,
  requireHomeOwner,
  ctrl.updateEsp32
);

// OWNER được xóa
router.delete(
  "/home/:homeId/esp32/:id",
  authenticate,
  requireHomeOwner,
  ctrl.deleteEsp32
);

/* ================= ESP32 API ================= */

// ESP32 claim (KHÔNG auth)
// router.post("/esp32/claim", ctrl.claimEsp32);

module.exports = router;

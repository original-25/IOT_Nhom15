const router = require("express").Router();
const scheduler = require("../controllers/scheduler.controller"); // Đảm bảo đúng đường dẫn file controller
const { authenticate } = require("../middlewares/auth.middleware"); // Import từ file auth.middleware.js
const { requireHomeMember } = require("../middlewares/home.middleware"); // Import từ file home.middleware.js

// 1. Tạo lịch mới
// POST /homes/:homeId/schedules
router.post(
  "/homes/:homeId/schedules", 
  authenticate, 
  requireHomeMember, 
  scheduler.createSchedule
);

// 2. Lấy danh sách lịch
// GET /homes/:homeId/schedules
router.get(
  "/homes/:homeId/schedules", 
  authenticate, 
  requireHomeMember, 
  scheduler.getSchedules
);

// 3. Xóa lịch
// DELETE /homes/:homeId/schedules/:id
// - Cần :homeId để middleware check quyền thành viên
// - Cần :id để controller biết xóa lịch nào
router.delete(
  "/homes/:homeId/schedules/:id", 
  authenticate, 
  requireHomeMember, 
  scheduler.deleteSchedule
);

module.exports = router;
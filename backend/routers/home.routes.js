const router = require("express").Router();
const home = require("../controllers/home.controller");
const { authenticate } = require("../middlewares/auth.middleware");
const {
  requireHomeMember,
  requireHomeOwner
} = require("../middlewares/home.middleware");

// INVITATION ROUTES (MUST BE FIRST)
router.post("/invitation/accept", authenticate, home.acceptInvitation);
router.post("/invitation/decline", authenticate, home.declineInvitation);

// HOME ROUTES
router.post("/", authenticate, home.createHome);
router.get("/", authenticate, home.getMyHomes);

// OWNER ONLY
router.patch("/:homeId", authenticate, requireHomeOwner, home.updateHomeName);
router.post("/:homeId/invite", authenticate, requireHomeOwner, home.inviteMember);
router.delete("/:homeId/members/:userId", authenticate, requireHomeOwner, home.removeMember);

// MEMBER ROUTES
router.get("/:homeId/members", authenticate, requireHomeMember, home.getHomeMembers);
router.get("/:homeId", authenticate, requireHomeMember, home.getHomeDetail);

module.exports = router;
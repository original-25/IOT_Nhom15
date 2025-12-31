const Home = require("../models/Home");

/**
 * Check user là member của home
 */
async function requireHomeMember(req, res, next) {
  try {
    const { homeId } = req.params;
    const userId = req.userId;

    const home = await Home.findOne({
      _id: homeId,
      "members.user": userId
    });

    if (!home) {
      return res.status(403).json({
        success: false,
        message: "You are not a member of this home",
        errorCode: "NOT_HOME_MEMBER"
      });
    }

    req.home = home;
    next();
  } catch (error) {
    console.error("Home member check error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error",
      errorCode: "SERVER_ERROR"
    });
  }
}

/**
 * Check user là OWNER của home
 */
async function requireHomeOwner(req, res, next) {
  try {
    const { homeId } = req.params;
    const userId = req.userId;

    const home = await Home.findOne({
      _id: homeId,
      members: {
        $elemMatch: {
          user: userId,
          role: "owner"
        }
      }
    });

    if (!home) {
      return res.status(403).json({
        success: false,
        message: "Only home owner can perform this action",
        errorCode: "NOT_HOME_OWNER"
      });
    }

    req.home = home;
    next();
  } catch (error) {
    console.error("Home owner check error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error",
      errorCode: "SERVER_ERROR"
    });
  }
}

module.exports = {
  requireHomeMember,
  requireHomeOwner
};
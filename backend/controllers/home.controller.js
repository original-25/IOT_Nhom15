const Home = require("../models/Home");
const User = require("../models/User");
const Invitation = require("../models/Invitation");
const crypto = require("crypto");

module.exports.createHome = async (req, res) => {
  try {
    const { name } = req.body;
    const userId = req.userId;
    
    if (!name) {
      return res.status(400).json({
        success: false,
        message: "Home name is required",
        errorCode: "HOME_NAME_REQUIRED"
      });
    }

    const home = await Home.create({
      name,
      members: [
        { 
          user: userId, 
          role: "owner" 
        }
      ]
    });

    return res.status(201).json({
      success: true,
      message: "Home created successfully",
      data: {
        id: home._id,
        name: home.name,
        members: home.members
      }
    });
  } catch (error) {
    console.error("Create home error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error",
      errorCode: "SERVER_ERROR"
    });
  }
};


// Get my homes
module.exports.getMyHomes = async (req, res) => {
  try {
    const userId = req.userId;
    
    const homes = await Home.find({
      "members.user": userId
    });
    
    const data = homes.map(home => {
      const member = home.members.find(
        m => m.user.toString() === userId
      );

      return {
        id: home._id,
        name: home.name,
        role: member?.role || "member"
      };
    });

    return res.json({
      success: true,
      message: "Get homes successfully",
      data
    });
  } catch (error) {
    console.error("Get homes error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error",
      errorCode: "SERVER_ERROR"
    });
  }
};


// Get home detail
module.exports.getHomeDetail = async (req, res) => {
  try {
    const home = req.home;

    return res.json({
      success: true,
      message: "Get home detail successfully",
      data: {
        id: home._id,
        name: home.name,
        members: home.members.map(m => ({
          userId: m.user,
          role: m.role
        }))
      }
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Internal server error",
      errorCode: "SERVER_ERROR"
    });
  }
};


// Update home name (không đổi)
module.exports.updateHomeName = async (req, res) => {
  try {
    const { name } = req.body;
    const home = req.home;

    if (!name) {
      return res.status(400).json({
        success: false,
        message: "Home name is required",
        errorCode: "HOME_NAME_REQUIRED"
      });
    }

    home.name = name;
    await home.save();

    return res.json({
      success: true,
      message: "Home updated successfully",
      data: {
        id: home._id,
        name: home.name
      }
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Internal server error",
      errorCode: "SERVER_ERROR"
    });
  }
};


// Get home members
module.exports.getHomeMembers = async (req, res) => {
  try {
    const home = await Home.findById(req.params.homeId)
      .populate("members.user", "email username");

    const members = home.members.map(m => ({
      userId: m.user._id,
      email: m.user.email,
      username: m.user.username,
      role: m.role
    }));

    return res.json({
      success: true,
      message: "Get members successfully",
      data: members
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Internal server error",
      errorCode: "SERVER_ERROR"
    });
  }
};


// Invite member
module.exports.inviteMember = async (req, res) => {
  try {
    const { email } = req.body;
    const homeId = req.params.homeId;
    const inviterId = req.userId;

    if (!email) {
      return res.status(400).json({
        success: false,
        message: "Email is required",
        errorCode: "EMAIL_REQUIRED"
      });
    }

    const existedUser = await User.findOne({ email });

    if (existedUser) {
      const alreadyInHome = await Home.findOne({
        _id: homeId,
        "members.user": existedUser._id
      });

      if (alreadyInHome) {
        return res.status(409).json({
          success: false,
          message: "User already in home",
          errorCode: "USER_ALREADY_IN_HOME"
        });
      }
    }

    const token = crypto.randomBytes(32).toString("hex");

    await Invitation.create({
      home: homeId,
      inviter: inviterId,
      inviteeEmail: email,
      token,
      expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000)
    });

    return res.json({
      success: true,
      message: "Invitation sent"
    });
  } catch (error) {
    console.error("Invite member error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error",
      errorCode: "SERVER_ERROR"
    });
  }
};


// Accept invitation
module.exports.acceptInvitation = async (req, res) => {
  try {
    const { token } = req.body;
    const userId = req.userId;

    const invitation = await Invitation.findOne({
      token,
      status: "pending",
      expiresAt: { $gt: new Date() }
    });

    if (!invitation) {
      return res.status(400).json({
        success: false,
        message: "Invalid or expired invitation",
        errorCode: "INVITATION_INVALID"
      });
    }

    const home = await Home.findById(invitation.home);

    const alreadyMember = home.members.some(
      m => m.user.toString() === userId
    );

    if (alreadyMember) {
      return res.status(409).json({
        success: false,
        message: "Already a member",
        errorCode: "ALREADY_MEMBER"
      });
    }

    home.members.push({ user: userId, role: "member" });
    invitation.status = "accepted";

    await Promise.all([home.save(), invitation.save()]);

    return res.json({
      success: true,
      message: "Joined home successfully"
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Internal server error",
      errorCode: "SERVER_ERROR"
    });
  }
};


// Remove member
module.exports.removeMember = async (req, res) => {
  try {
    const { homeId, userId } = req.params;
    const ownerId = req.userId;

    if (userId === ownerId) {
      return res.status(400).json({
        success: false,
        message: "Owner cannot remove himself",
        errorCode: "CANNOT_REMOVE_OWNER"
      });
    }

    const home = await Home.findById(homeId);

    home.members = home.members.filter(
      m => m.user.toString() !== userId
    );

    await home.save();

    return res.json({
      success: true,
      message: "Member removed successfully"
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Internal server error",
      errorCode: "SERVER_ERROR"
    });
  }
};


// Decline invitation
module.exports.declineInvitation = async (req, res) => {
  try {
    const { token } = req.body;

    if (!token) {
      return res.status(400).json({
        success: false,
        message: "Invitation token is required",
        errorCode: "TOKEN_REQUIRED"
      });
    }

    const invitation = await Invitation.findOne({
      token,
      status: "pending"
    });

    if (!invitation) {
      return res.status(400).json({
        success: false,
        message: "Invalid or expired invitation",
        errorCode: "INVITATION_INVALID"
      });
    }

    invitation.status = "declined";
    await invitation.save();

    return res.json({
      success: true,
      message: "Invitation declined successfully"
    });
  } catch (error) {
    console.error("Decline invitation error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error",
      errorCode: "SERVER_ERROR"
    });
  }
};

/**
 * Lấy tất cả lời mời được gửi đến user hiện tại
 */
module.exports.getMyInvitations = async (req, res) => {
  try {
    const userId = req.userId;

    // Lấy email của user hiện tại
    const user = await User.findById(userId);
    
    if (!user) {
      return res.status(404).json({
        success: false,
        message: "User not found",
        errorCode: "USER_NOT_FOUND"
      });
    }

    // Tìm các lời mời pending gửi đến email của user
    const invitations = await Invitation.find({
      inviteeEmail: user.email,
      status: "pending",
      expiresAt: { $gt: new Date() }
    })
      .populate("home", "name")
      .populate("inviter", "username email")
      .sort({ createdAt: -1 });

    const data = invitations.map(inv => ({
      id: inv._id,
      token: inv.token,
      home: {
        id: inv.home._id,
        name: inv.home.name
      },
      inviter: {
        id: inv.inviter._id,
        username: inv.inviter.username,
        email: inv.inviter.email
      },
      inviteeEmail: inv.inviteeEmail,
      createdAt: inv.createdAt,
      expiresAt: inv.expiresAt
    }));

    return res.json({
      success: true,
      message: "Get invitations successfully",
      data
    });
  } catch (error) {
    console.error("Get invitations error:", error);
    return res.status(500).json({
      success: false,
      message: "Internal server error",
      errorCode: "SERVER_ERROR"
    });
  }
};
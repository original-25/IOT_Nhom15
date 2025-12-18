const { verifyAccessToken } = require("../utils/token");

// Kiểm tra user đăng nhập hay chưa
function authenticate(req, res, next) {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return res.status(401).json({ code: "NO_TOKEN", message: "No token provided" });
  }

  const token = authHeader.split(" ")[1];

  const decoded = verifyAccessToken(token);
  
  if (!decoded) {
    return res.status(401).json({ code: "TOKEN_INVALID", message: "Invalid token" });
  }

  // OK
  req.userId = decoded.id;
  next();
}

// Phân quyền theo role : Hiện tại chưa dùng
function authorize(roles = []) {
  return (req, res, next) => {
    if (!roles.includes(req.user.role))
      return res.status(403).json({ message: "Permission denied" });

    next();
  };
}

module.exports = {
  authenticate,
  authorize
};
const { verifyAccessToken } = require("../utils/token");

// Kiểm tra user đăng nhập hay chưa
function authenticate(req, res, next) {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return res.status(401).json({ code: "NO_TOKEN", message: "No token provided" });
  }

  const token = authHeader.split(" ")[1];

  jwt.verify(token, process.env.ACCESS_SECRET, (err, decoded) => {
    if (err) {
      // 🔥 Quan trọng: phân biệt lỗi để UI phản ứng đúng
      if (err.name === "TokenExpiredError") {
        return res.status(401).json({ code: "TOKEN_EXPIRED", message: "Access token expired" });
      }

      return res.status(401).json({ code: "TOKEN_INVALID", message: "Invalid token" });
    }

    // OK
    req.user = decoded;
    next();
  });
}

// Phân quyền theo role
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
const router = require("express").Router();
const auth = require("../controllers/auth.controller");

router.post("/register/sendRegisterReq", auth.sendRegisterReq);
router.post("/register/verifyRegisterOtp", auth.verifyRegisterOtp);
router.post("/register/createAccount", auth.createAccount);

router.post("/login", auth.login);

router.post("/forgot-password", auth.forgotPassword);
router.post("/verify-otp", auth.verifyOtp);
router.post("/reset-password", auth.resetPassword);


//acc và ref token
router.post("/refresh-token", auth.refreshToken);


module.exports = router;
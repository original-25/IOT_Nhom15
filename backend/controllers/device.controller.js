const Device = require("../models/Device");
const DeviceLog = require("../models/DeviceLog");
const EspDevice = require("../models/EspDevice");
const Home = require("../models/Home");
const { publishCommand } = require("../mqtt/mqtt.publisher");
const crypto = require("crypto");

exports.createDevice = async (req, res) => {
	try {
		// home is provided by requireHomeOwner middleware as req.home
		const home = req.home || null;
		const { name, type, espId, config, settings } = req.body;
		const homeId = req.params.homeId || (home && home._id.toString());

		if (!homeId || !espId || !type) return res.status(400).json({ success: false, message: "homeId (param), espId and type are required" });

		const homeDoc = home || await Home.findById(homeId);
		console.log("createDevice: homeId=", homeId, "espId=", espId, "type=", type, "name=", name);
		if (!homeDoc) return res.status(404).json({ success: false, message: "Home not found" });

		const esp = await EspDevice.findOne({ _id: espId, home: homeId });
		if (!esp) return res.status(404).json({ success: false, message: "ESP not found for this home" });

		let device;
		try {
			device = await Device.create({
				name: name || "New Device",
				type,
				home: homeDoc._id,
				esp: esp._id,
				config: config || {},
				settings: settings || {},
			});
			console.log("createDevice: created device", device._id.toString());
		} catch (e) {
			console.error("createDevice: failed to create Device record", e);
			return res.status(500).json({ success: false, message: "Failed to create device" });
		}

		// mark device as provisioning and save before notifying ESP
		const cid = crypto.randomUUID ? crypto.randomUUID() : require('crypto').randomBytes(16).toString('hex');
		device.status = "provisioning";
		device.provisioningCid = cid;
		const timeoutMs = parseInt(process.env.DEVICE_PROVISION_TIMEOUT_MS || "60000", 10);
		device.provisioningExpiresAt = new Date(Date.now() + timeoutMs);
		device.provisionAttempts = (device.provisionAttempts || 0) + 1;
		try {
			await device.save();
		} catch (e) {
			console.error("createDevice: failed to save provisioning metadata", { deviceId: device && device._id ? device._id.toString() : null, err: e });
			return res.status(500).json({ success: false, message: "Failed to persist device provisioning state" });
		}

		// publish create command to ESP so it can provision the device (include cid)
		let published = true;
		try {
			const payload = {
				action: "create",
				cid,
				device: {
					id: device._id.toString(),
					name: device.name,
					type: device.type,
					config: device.config || {},
					settings: device.settings || {}
				}
			};

			console.log("createDevice: publishing create cmd", { homeId: device.home.toString(), espId: esp._id.toString(), deviceId: device._id.toString(), cid });
			await publishCommand({
				homeId: device.home.toString(),
				espId: esp._id.toString(),
				deviceId: device._id.toString(),
				payload,
				opts: { attempts: 3 }
			});
			console.log("createDevice: publishCommand resolved for device", device._id.toString());
		} catch (err) {
			console.warn("publish create command failed:", err);
			published = false;
			// record publish failure to DeviceLog for troubleshooting
			try {
				await DeviceLog.create({ device: device._id, type: "event", data: { action: "publish_create_failed", error: err && err.message, payload: { action: "create", cid } }, createdAt: new Date() });
			} catch (logErr) {
				console.error("createDevice: failed to write DeviceLog for publish error", logErr);
			}
		}

		// schedule a best-effort timeout to mark provisioning failed if no ack arrives
		try {
			console.log("createDevice: scheduling provision timeout", { deviceId: device._id.toString(), cid, timeoutMs });
			setTimeout(async () => {
				try {
					const d = await Device.findById(device._id);
					if (!d) return;
					if (d.status === "provisioning" && d.provisioningCid === cid) {
						console.log("provision timeout triggered for device", device._id.toString(), { cid });
						d.status = "error";
						d.provisionFailedAt = new Date();
						d.provisioningCid = null;
						d.provisioningExpiresAt = null;
						await d.save();
						await DeviceLog.create({ device: d._id, type: "event", data: { reason: "provision_timeout", cid }, createdAt: new Date() });
					}
				} catch (e) {
					console.warn("provision timeout handler error:", e);
				}
			}, timeoutMs + 1000 * 30);
		} catch (e) {
			console.warn("createDevice: failed to schedule provision timeout", e);
		}

		return res.status(201).json({ success: true, data: device, published });
	} catch (err) {
		console.error("createDevice error:", err);
		return res.status(500).json({ success: false, message: "Internal server error" });
	}
};

exports.listDevicesByHome = async (req, res) => {
	try {
		const { homeId } = req.params;
		const devices = await Device.find({ home: homeId }).lean();
		return res.json({ success: true, data: devices });
	} catch (err) {
		console.error("listDevicesByHome error:", err);
		return res.status(500).json({ success: false, message: "Internal server error" });
	}
};

exports.getDevice = async (req, res) => {
	try {
		const { id, homeId } = req.params;
		const device = await Device.findById(id).lean();
		if (!device) return res.status(404).json({ success: false, message: "Device not found" });
		// ensure device belongs to requested home
		if (homeId && device.home.toString() !== homeId) return res.status(403).json({ success: false, message: "Device does not belong to this home" });
		return res.json({ success: true, data: device });
	} catch (err) {
		console.error("getDevice error:", err);
		return res.status(500).json({ success: false, message: "Internal server error" });
	}
};

exports.updateDevice = async (req, res) => {
	try {
		const { id, homeId } = req.params;
		const updates = req.body;
		const device = await Device.findById(id);
		if (!device) return res.status(404).json({ success: false, message: "Device not found" });
		if (homeId && device.home.toString() !== homeId) return res.status(403).json({ success: false, message: "Device does not belong to this home" });

		Object.assign(device, updates);
		await device.save();
		return res.json({ success: true, data: device });
	} catch (err) {
		console.error("updateDevice error:", err);
		return res.status(500).json({ success: false, message: "Internal server error" });
	}
};

exports.deleteDevice = async (req, res) => {
	try {
		const { id, homeId } = req.params;
		const device = await Device.findById(id);
		if (!device) return res.status(404).json({ success: false, message: "Device not found" });
		if (homeId && device.home.toString() !== homeId) return res.status(403).json({ success: false, message: "Device does not belong to this home" });

		// Tìm ESP trước khi xóa device để còn gửi lệnh
		const esp = await EspDevice.findById(device.esp);

		// Xóa device khỏi DB
		// Lưu ý: Mongoose mới khuyến nghị dùng deleteOne() thay vì remove()
		await Device.deleteOne({ _id: id }); 

		let published = true;
		try {
			if (!esp) throw new Error("ESP not found");

			// --- [FIX BEGIN] ---
			// 1. Tạo CID để ESP32 gửi ACK về log
			const cid = crypto.randomUUID ? crypto.randomUUID() : require('crypto').randomBytes(16).toString('hex');
			
			// 2. Gửi payload đúng chuẩn mà ESP32 mong đợi
			await publishCommand({
				homeId: device.home.toString(),
				espId: esp._id.toString(),
				deviceId: device._id.toString(),
				payload: {
					action: "delete",
					cid: cid,                       // Thêm CID
					deviceId: device._id.toString() // Gửi phẳng deviceId ra root
				}
			});
			// --- [FIX END] ---

		} catch (err) {
			console.warn("publish delete command failed:", err);
			published = false;
		}

		return res.json({ success: true, message: "Device removed", published });
	} catch (err) {
		console.error("deleteDevice error:", err);
		return res.status(500).json({ success: false, message: "Internal server error" });
	}
};

// send command to device via MQTT
exports.sendCommand = async (req, res) => {
	try {
		const { id, homeId } = req.params; // device id
		const payload = req.body || {};

		const device = await Device.findById(id);
		if (!device) return res.status(404).json({ success: false, message: "Device not found" });
		if (homeId && device.home.toString() !== homeId) return res.status(403).json({ success: false, message: "Device does not belong to this home" });

		const esp = await EspDevice.findById(device.esp);
		if (!esp) return res.status(404).json({ success: false, message: "ESP not found" });

		// ensure deviceId included in payload for esp-level topic
		if (!payload.deviceId) payload.deviceId = device._id.toString();

		await publishCommand({ homeId: device.home.toString(), espId: esp._id.toString(), deviceId: device._id.toString(), payload });

		return res.json({ success: true, message: "Command published" });
	} catch (err) {
		console.error("sendCommand error:", err);
		return res.status(500).json({ success: false, message: "Internal server error" });
	}
};

exports.getDeviceState = async (req, res) => {
	try {
		const { id, homeId } = req.params;
		const device = await Device.findById(id).lean();
		if (!device) return res.status(404).json({ success: false, message: "Device not found" });
		if (homeId && device.home.toString() !== homeId) return res.status(403).json({ success: false, message: "Device does not belong to this home" });
		console.log("đì vai: ", device);
		return res.json({ success: true, data: { lastState: device.lastState } });
	} catch (err) {
		console.error("getDeviceState error:", err);
		return res.status(500).json({ success: false, message: "Internal server error" });
	}
};

exports.getDeviceLogs = async (req, res) => {
	try {
		const { id, homeId } = req.params;
		const page = parseInt(req.query.page || "1", 10);
		const limit = parseInt(req.query.limit || "100", 10);
		const skip = (page - 1) * limit;

		const device = await Device.findById(id).lean();
		if (!device) return res.status(404).json({ success: false, message: "Device not found" });
		if (homeId && device.home.toString() !== homeId) return res.status(403).json({ success: false, message: "Device does not belong to this home" });

		const logs = await DeviceLog.find({ device: id }).sort({ createdAt: -1 }).skip(skip).limit(limit).lean();
		return res.json({ success: true, data: logs });
	} catch (err) {
		console.error("getDeviceLogs error:", err);
		return res.status(500).json({ success: false, message: "Internal server error" });
	}
};
exports.getDeviceLogsLatest = async (req, res) => {
	try {
		const { id, homeId } = req.params;
		const limit = parseInt(req.query.limit || "10", 10);

		const device = await Device.findById(id).lean();
		if (!device) return res.status(404).json({ success: false, message: "Device not found" });
		if (homeId && device.home.toString() !== homeId) return res.status(403).json({ success: false, message: "Device does not belong to this home" });

		console.log("đì vai: ", device);
		

		const logs = await DeviceLog.find({ device: id }).sort({ createdAt: -1 }).limit(limit).lean();
		return res.json({ success: true, data: logs });
	} catch (err) {
		console.error("getDeviceLogsLatest error:", err);
		return res.status(500).json({ success: false, message: "Internal server error" });
	}
};

// list devices belonging to an ESP
exports.listDevicesByEsp = async (req, res) => {
	try {
		const { espId } = req.params;
		const devices = await Device.find({ esp: espId }).lean();
		return res.json({ success: true, data: devices });
	} catch (err) {
		console.error("listDevicesByEsp error:", err);
		return res.status(500).json({ success: false, message: "Internal server error" });
	}
};

// list devices for an ESP within a specific home
exports.listDevicesByEspInHome = async (req, res) => {
	try {
		const { homeId, espId } = req.params;

		// verify esp belongs to home
		const esp = await EspDevice.findOne({ _id: espId, home: homeId });
		if (!esp) return res.status(404).json({ success: false, message: "ESP not found for this home" });

		const devices = await Device.find({ esp: espId, home: homeId }).lean();
		return res.json({ success: true, data: devices });
	} catch (err) {
		console.error("listDevicesByEspInHome error:", err);
		return res.status(500).json({ success: false, message: "Internal server error" });
	}
}


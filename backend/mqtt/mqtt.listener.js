const mqtt = require("mqtt");
const crypto = require("crypto");
const Device = require("../models/Device");
const DeviceLog = require("../models/DeviceLog");
const EspDevice = require("../models/EspDevice");
const { createEsp32MqttToken } = require("../services/flespi.service");

function hashToken(token) {
  return crypto.createHash("sha256").update(token).digest("hex");
}

const MQTT_URL = process.env.MQTT_URL || "mqtt://mqtt.flespi.io";
const MQTT_PORT = process.env.MQTT_PORT ? parseInt(process.env.MQTT_PORT, 10) : 1883;
// Flespi token stored in env
const FLESPI_TOKEN = process.env.PUB_SUB_TOKEN || process.env.MQTT_USERNAME || null;

const client = mqtt.connect(MQTT_URL, {
	username: FLESPI_TOKEN,
	password: process.env.MQTT_PASSWORD || "",
	port: MQTT_PORT,
	reconnectPeriod: 5000,
});

client.on("connect", () => {
	console.log("MQTT connected");
	client.subscribe("iot_nhom15/#", (err) => {
		if (err) console.error("MQTT subscribe error:", err);
		else {
			console.log("Subscribed to iot_nhom15/home/+/esp32/+/(ack, status, state, data, event)");
			console.log("Publish to iot_nhom15/home/+/esp32/+/cmd");
		}
	});
});

client.on("message", async (topic, message) => {
	try {
		let payload;
		try { payload = JSON.parse(message.toString()); } catch { payload = { raw: message.toString() }; }
		console.log("mqtt.listener: message received", { topic, payload });

		const parts = topic.split("/").filter(Boolean); // remove empty segments
		if (!parts.length || parts[0] !== "iot_nhom15") return;

		// expected structure: iot_nhom15 / home / <homeId> / esp32 / <espId> [ / device / <deviceId> / <action> ... ]
		if (parts[1] !== "home" || !parts[2] || !parts[3]) return;

		const homeId = parts[2];
		const nodeType = parts[3]; // typically 'esp32'
		const espId = parts[4];

		// ESP-level messages: iot_nhom15/home/{homeId}/esp32/{espId}/{action}
		if (nodeType === "esp32" && espId && parts[5]) {
			const action = parts[5]; // cmd/data/state/ack/status

			// handle ESP32 claim via ack topic
			if (action === "ack" && payload.action === "create esp32") {
				const { claimToken } = payload;
				console.log("check var: ", claimToken)
				if (!claimToken) {
					console.warn("mqtt.listener: Missing claimToken in create esp32 ack");
					return;
				}

				try {
					const tokenHash = hashToken(claimToken);
					const device = await EspDevice.findOne({
						_id: espId,
						home: homeId,
						claimTokenHash: tokenHash,
						status: "unclaimed"
					});

					console.log("vcl 2: ", device)

					if (!device) {
						console.warn("mqtt.listener: Invalid or expired claim token for espDeviceId:", espId);
						return;
					}

					else {
						console.log("vê cê lờ")
					}

					// Update device status
					device.status = "provisioned";
					device.claimedAt = new Date();
					device.claimTokenHash = null;
					device.claimExpiresAt = null;
					await device.save();

					console.log("mqtt.listener: ESP32 claimed successfully:", espId);

				} catch (error) {
					console.error("mqtt.listener: Error claiming ESP32:", error);
				}
				return;
			}

			// handle ESP status/heartbeat
			if (action === "status") {
				const esp = await EspDevice.findOne({ _id: espId, home: homeId });
				if (!esp) return;
				if (payload.status) {
					esp.status = payload.status;
					console.log("mqtt.listener: esp status update", { espId, status: payload.status });
					await esp.save();
				}
				return;
			}

			// all other actions should include deviceId in payload
			const deviceId = payload && (payload.deviceId || payload.device && (payload.device.id || payload.device._id));
			if (!deviceId) {
				console.warn("mqtt.listener: Message missing deviceId in payload for action:", action, "topic:", topic);
				return;
			}

			const device = await Device.findOne({ _id: deviceId, home: homeId });
			if (!device) {
				console.warn("Device not found for deviceId (esp-level):", deviceId);
				return;
			}

			// data/logs: accept batch or single sensor data
			if (action === "data" || action === "logs") {
				if (Array.isArray(payload.logs)) {
					
					const docs = payload.logs.map(l => ({
						device: device._id,
						// type: (l.type === "state" || l.type === "event") ? l.type : (l.type || "sensor"),
						type: l.type?l.type:"sensor",
						data: l.data ?? l,
						createdAt: l.timestamp ? new Date(l.timestamp) : new Date()
					}));
					await DeviceLog.insertMany(docs);
				} else {
					
					// const t = (payload.type === "state" || payload.type === "event") ? payload.type : (payload.type || "sensor");
					const t = payload.type
					await DeviceLog.create({
						device: device._id,
						type: t,
						data: payload.data ?? payload,
						createdAt: payload.timestamp ? new Date(payload.timestamp) : new Date()
					});
				}
				console.log("mqtt.listener: stored logs for device", deviceId);
				return;
			}

			// state: update lastState and save log
			if (action === "state") {
				const stateData = payload.state !== undefined ? payload.state : (payload.data ?? payload);
				device.lastState = stateData;
				await device.save();

				await DeviceLog.create({
					device: device._id,
					type: "state",
					data: stateData,
					createdAt: payload.ts ? new Date(payload.ts) : new Date()
				});
				console.log("mqtt.listener: updated state for device", deviceId, stateData);
				return;
			}

			// ack: handle provision/create/delete acknowledgements specially
			if (action === "ack") {
				const cid = payload && payload.cid;
				if (payload && payload.action === "create") {
					try {
						// validate cid matches device record
						if (!cid || device.provisioningCid !== cid) {
							console.warn("mqtt.listener: Ack cid mismatch or missing for device:", device._id.toString(), { expected: device.provisioningCid, got: cid });
							await DeviceLog.create({ device: device._id, type: "event", data: { action: "create_ack_invalid", payload }, createdAt: new Date() });
							return;
						}

						if (payload.success) {
							device.status = "online";
							device.provisionedAt = new Date();
						} else {
							device.status = "error";
							device.provisionFailedAt = new Date();
						}
						// clear provisioning meta
						device.provisioningCid = null;
						device.provisioningExpiresAt = null;
						await device.save();
					} catch (e) {
						console.warn("Failed to update device status on ack:", e);
					}
					await DeviceLog.create({ device: device._id, type: "event", data: payload, createdAt: payload.ts ? new Date(payload.ts) : new Date() });
					console.log("mqtt.listener: provisioning ack processed for device", deviceId, "cid", cid, "success", payload.success);
					return;
				} else if (payload && payload.action === "delete") {
					await DeviceLog.create({ device: device._id, type: "event", data: { action: "delete_ack", payload }, createdAt: payload.ts ? new Date(payload.ts) : new Date() });
					console.log("mqtt.listener: delete ack for device", deviceId);
					return;
				} else {
					await DeviceLog.create({ device: device._id, type: "event", data: payload, createdAt: payload.ts ? new Date(payload.ts) : new Date() });
					console.log("mqtt.listener: generic ack logged for device", deviceId);
					return;
				}
			}

			// event: generic event log
			if (action === "event") {
				await DeviceLog.create({
					device: device._id,
					type: "event",
					data: payload,
					createdAt: payload.ts ? new Date(payload.ts) : new Date()
				});
				return;
			}

			// fallback - store as sensor log
			// await DeviceLog.create({
			// 	device: device._id,
			// 	type: payload.type === "state" || payload.type === "event" ? payload.type : (payload.type || "sensor"),
			// 	data: payload.data ?? payload,
			// 	createdAt: payload.timestamp ? new Date(payload.timestamp) : new Date()
			// });
			
			return;
		}

		// esp-level messages (logs or status)
		// if (nodeType === "esp32" && espId && (parts[5] === "logs" || parts[5] === "status")) {
		// 	const esp = await EspDevice.findOne({ _id: espId, home: homeId });
		// 	if (!esp) return;
		// 	if (parts[5] === "status" && payload.status) {
		// 		esp.status = payload.status;
		// 		await esp.save();
		// 	}
		// 	// optionally persist esp logs or ignore
		// 	return;
		// }
	} catch (err) {
		console.error("Error processing MQTT message:", err);
	}
});

client.on("error", (err) => {
	console.error("MQTT error:", err);
});

module.exports = client;


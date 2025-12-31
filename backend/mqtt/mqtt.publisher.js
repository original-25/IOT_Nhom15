const client = require("./mqtt.listener"); // mqtt client export (listener file exports client)
const EspDevice = require("../models/EspDevice");
const Device = require("../models/Device");
const DeviceLog = require("../models/DeviceLog");

/**
 * publishCommand
 * opts: { qos, retain, attempts }
 */
async function publishCommand({ homeId, espId, deviceId, payload, opts = {} }) {
  const qos = opts.qos ?? 1;
  const retain = opts.retain ?? false;
  const attempts = opts.attempts ?? 3;
  // publish to esp32-level command topic; payload must include deviceId
  const topic = `iot_nhom15/home/${homeId}/esp32/${espId}/cmd`;
  const message = typeof payload === "string" ? payload : JSON.stringify(payload);

  // verify esp and device exist
  const esp = await EspDevice.findById(espId).lean();
  if (!esp) throw new Error("ESP not found");
  const device = await Device.findById(deviceId).lean();
  if (!device) throw new Error("Device not found");

  // increment provisionAttempts for create action (best-effort)
  if (payload && payload.action === "create") {
    try {
      await Device.updateOne({ _id: deviceId }, { $inc: { provisionAttempts: 1 } });
    } catch (e) { /* ignore */ }
  }

  // create outgoing command log (type: event)
  try {
    await DeviceLog.create({
      device: device._id,
      type: "event",
      data: { direction: "out", payload },
      createdAt: new Date()
    });
  } catch (err) {
    // non-fatal: log and continue
    console.warn("Failed to create outgoing DeviceLog:", err);
  }

  console.log("publishCommand: topic=", topic, "qos=", qos, "retain=", retain, "attempts=", attempts, "payload=", payload);

  return new Promise((resolve, reject) => {
    let tries = 0;
    const tryPublish = () => {
      tries += 1;
      client.publish(topic, message, { qos, retain }, (err) => {
        if (!err) {
          console.log("publishCommand: published", { topic, tries });
          return resolve();
        }
        console.warn("publishCommand: publish error", { topic, tries, err: err && err.message });
        if (tries < attempts) {
          setTimeout(tryPublish, 1000 * tries); // backoff
        } else {
          console.error("publishCommand: failed after attempts", { topic, attempts, err: err && err.message });
          reject(err);
        }
      });
    };
    tryPublish();
  });
}

function publishRaw(topic, payload, opts = {}) {
  const message = typeof payload === "string" ? payload : JSON.stringify(payload);
  return new Promise((resolve, reject) => {
    client.publish(topic, message, opts, (err) => err ? reject(err) : resolve());
  });
}

module.exports = { publishCommand, publishRaw };
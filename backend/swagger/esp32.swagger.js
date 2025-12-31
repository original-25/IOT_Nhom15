/**
 * @swagger
 * tags:
 *   name: Home
 *   description: APIs for managing homes and ESP32 devices
 */

/**
 * @swagger
 * /home/{homeId}/esp32/provision:
 *   post:
 *     tags: [ESP32]
 *     summary: Provision a new ESP32 device (HOME OWNER only)
 *     description: |
 *       Creates a new ESP32 device with MQTT credentials. This endpoint:
 *       - Generates a claim token for verification (valid for 3 minutes)
 *       - Creates Flespi MQTT credentials immediately
 *       - Returns full MQTT connection details
 *       - Auto-deletes the device if ESP32 doesn't claim within 3 minutes
 *       
 *       **Flow:**
 *       1. App calls this endpoint → receives credentials
 *       2. App sends credentials to ESP32 via POST /config endpoint
 *       3. ESP32 connects to MQTT and sends claim request to topic: `iot_nhom15/home/{homeId}/esp32/{espId}/ack`
 *       4. Server validates claim token and updates device status
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *           example: "507f1f77bcf86cd799439011"
 *         description: The ID of the home
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               name:
 *                 type: string
 *                 description: Friendly name for the ESP32 device
 *                 example: "Living Room ESP32"
 *     responses:
 *       200:
 *         description: ESP32 device provisioned successfully with MQTT credentials
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 espDeviceId:
 *                   type: string
 *                   description: The unique ID of the ESP32 device
 *                   example: "507f1f77bcf86cd799439011"
 *                 claimToken:
 *                   type: string
 *                   description: Token for ESP32 to claim/verify itself (8 characters, uppercase hex)
 *                   example: "A1B2C3D4"
 *                 expiresIn:
 *                   type: integer
 *                   description: Time in seconds until the claim token expires
 *                   example: 180
 *                 mqtt:
 *                   type: object
 *                   description: MQTT connection credentials (send these to ESP32)
 *                   properties:
 *                     host:
 *                       type: string
 *                       description: MQTT broker hostname
 *                       example: "mqtt.flespi.io"
 *                     port:
 *                       type: integer
 *                       description: MQTT broker port
 *                       example: 1883
 *                     username:
 *                       type: string
 *                       description: MQTT username (same as espDeviceId)
 *                       example: "507f1f77bcf86cd799439011"
 *                     password:
 *                       type: string
 *                       description: MQTT password (Flespi token)
 *                       example: "FlespiToken1234567890abcdef"
 *                     baseTopic:
 *                       type: string
 *                       description: Base MQTT topic for this ESP32 device
 *                       example: "iot_nhom15/home/507f1f77bcf86cd799439011/esp32/507f1f77bcf86cd799439011"
 *       401:
 *         description: Unauthorized - Authentication required
 *       403:
 *         description: Forbidden - Only HOME OWNER can provision devices
 *       500:
 *         description: Internal server error
 */


/**
 * @swagger
 * /esp32/claim:
 *   post:
 *     tags: [ESP32]
 *     summary: "[DEPRECATED] Claim an ESP32 device"
 *     description: |
 *       ⚠️ **This endpoint is now deprecated and disabled.**
 *       
 *       The claiming process has been moved to MQTT for better real-time communication.
 *       
 *       **New Flow:**
 *       1. App calls `/home/{homeId}/esp32/provision` to get MQTT credentials
 *       2. App sends credentials to ESP32 via its `/config` endpoint
 *       3. ESP32 connects to MQTT and publishes claim request to: `iot_nhom15/home/{homeId}/esp32/{espId}/ack`
 *       4. Backend MQTT listener validates the claim token and updates device status
 *       
 *       **MQTT Claim Message Format:**
 *       ```json
 *       {
 *         "action": "create esp32",
 *         "claimToken": "A1B2C3D4",
 *         "ts": "2025-12-30T10:30:00Z"
 *       }
 *       ```
 *     deprecated: true
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               espDeviceId:
 *                 type: string
 *                 example: "507f1f77bcf86cd799439011"
 *               claimToken:
 *                 type: string
 *                 example: "A1B2C3D4"
 *     responses:
 *       404:
 *         description: Endpoint disabled - Use MQTT claiming instead
 */


/**
 * @swagger
 * /home/{homeId}/esp32:
 *   get:
 *     tags: [ESP32]
 *     summary: Get all ESP32 devices of a home (HOME MEMBER)
 *     description: |
 *       Retrieves a list of all ESP32 devices associated with the specified home.
 *       All home members (including owner) can view this list.
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *           example: "507f1f77bcf86cd799439011"
 *         description: The ID of the home
 *     responses:
 *       200:
 *         description: ESP32 devices retrieved successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 data:
 *                   type: array
 *                   items:
 *                     type: object
 *                     properties:
 *                       _id:
 *                         type: string
 *                         example: "507f1f77bcf86cd799439011"
 *                       name:
 *                         type: string
 *                         example: "Living Room ESP32"
 *                       home:
 *                         type: string
 *                         example: "507f1f77bcf86cd799439011"
 *                       status:
 *                         type: string
 *                         enum: [unclaimed, provisioned, online, offline]
 *                         example: "provisioned"
 *                       mqttBaseTopic:
 *                         type: string
 *                         example: "iot_nhom15/home/507f1f77bcf86cd799439011/esp32/507f1f77bcf86cd799439011"
 *                       claimedAt:
 *                         type: string
 *                         format: date-time
 *                         example: "2025-12-30T10:30:00.000Z"
 *                       createdAt:
 *                         type: string
 *                         format: date-time
 *                         example: "2025-12-30T10:25:00.000Z"
 *                       updatedAt:
 *                         type: string
 *                         format: date-time
 *                         example: "2025-12-30T10:30:00.000Z"
 *       401:
 *         description: Unauthorized - Authentication required
 *       403:
 *         description: Forbidden - User is not a member of this home
 *       500:
 *         description: Internal server error
 */

/**
 * @swagger
 * /home/{homeId}/esp32/{id}:
 *   get:
 *     tags: [ESP32]
 *     summary: Get details of a specific ESP32 device (HOME MEMBER)
 *     description: |
 *       Retrieves detailed information about a specific ESP32 device.
 *       All home members can view device details.
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *           example: "507f1f77bcf86cd799439011"
 *         description: The ID of the home
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *           example: "507f1f77bcf86cd799439012"
 *         description: The ID of the ESP32 device
 *     responses:
 *       200:
 *         description: ESP32 device details retrieved successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 data:
 *                   type: object
 *                   properties:
 *                     _id:
 *                       type: string
 *                       example: "507f1f77bcf86cd799439012"
 *                     name:
 *                       type: string
 *                       example: "Living Room ESP32"
 *                     home:
 *                       type: string
 *                       example: "507f1f77bcf86cd799439011"
 *                     status:
 *                       type: string
 *                       enum: [unclaimed, provisioned, online, offline]
 *                       example: "online"
 *                     mqttBaseTopic:
 *                       type: string
 *                       example: "iot_nhom15/home/507f1f77bcf86cd799439011/esp32/507f1f77bcf86cd799439012"
 *                     claimedAt:
 *                       type: string
 *                       format: date-time
 *                       example: "2025-12-30T10:30:00.000Z"
 *                     createdAt:
 *                       type: string
 *                       format: date-time
 *                       example: "2025-12-30T10:25:00.000Z"
 *                     updatedAt:
 *                       type: string
 *                       format: date-time
 *                       example: "2025-12-30T10:35:00.000Z"
 *       401:
 *         description: Unauthorized - Authentication required
 *       403:
 *         description: Forbidden - User is not a member of this home
 *       404:
 *         description: ESP32 device not found
 *       500:
 *         description: Internal server error
 */

/**
 * @swagger
 * /home/{homeId}/esp32/{id}/status:
 *   get:
 *     tags: [ESP32]
 *     summary: Get ESP32 device status (HOME OWNER)
 *     description: |
 *       Retrieves the current operational status of a specific ESP32 device.
 *       Only home owner can check device status.
 *       
 *       **Status Values:**
 *       - `unclaimed`: Device created but ESP32 hasn't claimed yet (will auto-delete after 3 minutes)
 *       - `provisioned`: ESP32 successfully claimed and authenticated
 *       - `online`: ESP32 is connected and sending heartbeats
 *       - `offline`: ESP32 is not responding
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *           example: "507f1f77bcf86cd799439011"
 *         description: The ID of the home
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *           example: "507f1f77bcf86cd799439012"
 *         description: The ID of the ESP32 device
 *     responses:
 *       200:
 *         description: ESP32 device status retrieved successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 data:
 *                   type: object
 *                   properties:
 *                     id:
 *                       type: string
 *                       example: "507f1f77bcf86cd799439012"
 *                     name:
 *                       type: string
 *                       example: "Living Room ESP32"
 *                     status:
 *                       type: string
 *                       enum: [unclaimed, provisioned, online, offline]
 *                       example: "online"
 *       401:
 *         description: Unauthorized - Authentication required
 *       403:
 *         description: Forbidden - Only HOME OWNER can check status
 *       404:
 *         description: ESP32 device not found
 *       500:
 *         description: Internal server error
 */

/**
 * @swagger
 * /home/{homeId}/esp32/{id}:
 *   patch:
 *     tags: [ESP32]
 *     summary: Update a specific ESP32 device (HOME OWNER)
 *     description: |
 *       Updates the name or other details of a specific ESP32 device.
 *       Only home owner can update device information.
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *           example: "507f1f77bcf86cd799439011"
 *         description: The ID of the home
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *           example: "507f1f77bcf86cd799439012"
 *         description: The ID of the ESP32 device
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               name:
 *                 type: string
 *                 description: New friendly name for the ESP32 device
 *                 example: "Bedroom ESP32"
 *     responses:
 *       200:
 *         description: ESP32 device updated successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 data:
 *                   type: object
 *                   properties:
 *                     _id:
 *                       type: string
 *                       example: "507f1f77bcf86cd799439012"
 *                     name:
 *                       type: string
 *                       example: "Bedroom ESP32"
 *                     home:
 *                       type: string
 *                       example: "507f1f77bcf86cd799439011"
 *                     status:
 *                       type: string
 *                       example: "online"
 *                     updatedAt:
 *                       type: string
 *                       format: date-time
 *                       example: "2025-12-30T10:40:00.000Z"
 *       401:
 *         description: Unauthorized - Authentication required
 *       403:
 *         description: Forbidden - Only HOME OWNER can update devices
 *       404:
 *         description: ESP32 device not found
 *       500:
 *         description: Internal server error
 */

/**
 * @swagger
 * /home/{homeId}/esp32/{id}:
 *   delete:
 *     tags: [ESP32]
 *     summary: Delete a specific ESP32 device (HOME OWNER)
 *     description: |
 *       Permanently deletes an ESP32 device from the home.
 *       This action will:
 *       - Remove the device record from database
 *       - Revoke the Flespi MQTT token
 *       - Disconnect the ESP32 from MQTT broker
 *       
 *       ⚠️ **Warning:** This action cannot be undone. All device data and logs will be preserved.
 *       
 *       Only home owner can delete devices.
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *           example: "507f1f77bcf86cd799439011"
 *         description: The ID of the home
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *           example: "507f1f77bcf86cd799439012"
 *         description: The ID of the ESP32 device to delete
 *     responses:
 *       200:
 *         description: ESP32 device deleted successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 message:
 *                   type: string
 *                   example: "ESP32 deleted"
 *       401:
 *         description: Unauthorized - Authentication required
 *       403:
 *         description: Forbidden - Only HOME OWNER can delete devices
 *       404:
 *         description: ESP32 device not found
 *       500:
 *         description: Internal server error (e.g., failed to revoke Flespi token)
 */



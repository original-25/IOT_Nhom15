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
 *     summary: Provision a new ESP32 device
 *     description: This endpoint allows a HOME OWNER to provision a new ESP32 device.
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
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
 *                 example: "ESP32-Device"
 *     responses:
 *       200:
 *         description: ESP32 device provisioned successfully
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
 *                   example: "507f1f77bcf86cd799439011"
 *                 claimToken:
 *                   type: string
 *                   example: "ABCDEF12"
 *                 expiresIn:
 *                   type: integer
 *                   example: 300
 *       500:
 *         description: Internal server error
 */


/**
 * @swagger
 * /esp32/claim:
 *   post:
 *     tags: [ESP32]
 *     summary: Claim an ESP32 device
 *     description: This endpoint allows an ESP32 device to claim a token and receive MQTT credentials.
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               espDeviceId:
 *                 type: string
 *                 example: "ESP32-1234"
 *               claimToken:
 *                 type: string
 *                 example: "ABCDEF12"
 *     responses:
 *       200:
 *         description: Claim successful, MQTT credentials returned
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 mqtt:
 *                   type: object
 *                   properties:
 *                     host:
 *                       type: string
 *                       example: "mqtt.flespi.io"
 *                     port:
 *                       type: integer
 *                       example: 1883
 *                     username:
 *                       type: string
 *                       example: "ESP32-1234"
 *                     password:
 *                       type: string
 *                       example: "mqttpassword"
 *                     baseTopic:
 *                       type: string
 *                       example: "/iot_nhom15/home/507f1f77bcf86cd799439011/esp32/ESP32-1234"
 *       400:
 *         description: Invalid or expired claim token
 *       500:
 *         description: Internal server error
 */


/**
 * @swagger
 * /home/{homeId}/esp32:
 *   get:
 *     tags: [ESP32]
 *     summary: Get all ESP32 devices of a home
 *     description: This endpoint allows HOME MEMBERS to view all ESP32 devices associated with the home.
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
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
 *                         example: "ESP32-Device"
 *       500:
 *         description: Internal server error
 */

/**
 * @swagger
 * /home/{homeId}/esp32/{id}:
 *   get:
 *     tags: [ESP32]
 *     summary: Get details of a specific ESP32 device
 *     description: This endpoint allows HOME MEMBERS to view details of a specific ESP32 device.
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *         description: The ID of the home
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
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
 *                       example: "507f1f77bcf86cd799439011"
 *                     name:
 *                       type: string
 *                       example: "ESP32-Device"
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
 *     summary: Get ESP32 device status
 *     description: This endpoint allows HOME MEMBER to get the status of a specific ESP32 device.
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *         description: The ID of the home
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
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
 *                       example: "507f1f77bcf86cd799439011"
 *                     name:
 *                       type: string
 *                       example: "ESP32-Device"
 *                     status:
 *                       type: string
 *                       example: "provisioned"
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
 *     summary: Update a specific ESP32 device
 *     description: This endpoint allows HOME OWNER to update details of a specific ESP32 device.
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *         description: The ID of the home
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
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
 *                 example: "Updated ESP32-Device"
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
 *                       example: "507f1f77bcf86cd799439011"
 *                     name:
 *                       type: string
 *                       example: "Updated ESP32-Device"
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
 *     summary: Delete a specific ESP32 device
 *     description: This endpoint allows HOME OWNER to delete a specific ESP32 device.
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *         description: The ID of the home
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *         description: The ID of the ESP32 device
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
 *       404:
 *         description: ESP32 device not found
 *       500:
 *         description: Internal server error
 */



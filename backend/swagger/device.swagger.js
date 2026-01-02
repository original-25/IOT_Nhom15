/**
 * @swagger
 * tags:
 *   name: Device
 *   description: APIs for managing devices and device logs
 */

/**
 * @swagger
 * /homes/{homeId}/devices:
 *   post:
 *     tags: [Device]
 *     summary: Create a new device in a home
 *     description: Home owner can create a device for the specified home.
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
 *                 example: "Living room light"
 *               type:
 *                 type: string
 *                 example: "light"
 *               espId:
 *                 type: string
 *                 example: "607f1f77bcf86cd799439011"
 *               config:
 *                 type: object
 *                 description: Device-specific configuration object
 *                 example:
 *                   pin: 12
 *                   pin2: 5
 *               
 *     responses:
 *       201:
 *         description: Device created
 *       400:
 *         description: Bad request
 *       403:
 *         description: Not home owner
 *       500:
 *         description: Internal server error
 */

/**
 * @swagger
 * /homes/{homeId}/devices:
 *   get:
 *     tags: [Device]
 *     summary: List devices for a home
 *     description: Home members can list devices belonging to a home.
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *     responses:
 *       200:
 *         description: List of devices
 *       403:
 *         description: Not a member
 *       500:
 *         description: Internal server error
 */

/**
 * @swagger
 * /homes/{homeId}/devices/{id}:
 *   get:
 *     tags: [Device]
 *     summary: Get device details
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *     responses:
 *       200:
 *         description: Device detail
 *       403:
 *         description: Device does not belong to this home
 *       404:
 *         description: Device not found
 */

/**
 * @swagger
 * /homes/{homeId}/devices/{id}:
 *   patch:
 *     tags: [Device]
 *     summary: Update device
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               name:
 *                 type: string
 *               config:
 *                 type: object
 *     responses:
 *       200:
 *         description: Device updated
 *       403:
 *         description: Forbidden
 *       404:
 *         description: Device not found
 */

/**
 * @swagger
 * /homes/{homeId}/devices/{id}:
 *   delete:
 *     tags: [Device]
 *     summary: Delete device
 *     description: Home owner can delete a device.
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *     responses:
 *       200:
 *         description: Device removed
 *       403:
 *         description: Forbidden
 *       404:
 *         description: Device not found
 */

/**
 * @swagger
 * /homes/{homeId}/devices/{id}/command:
 *   post:
 *     tags: [Device]
 *     summary: Send command to device
 *     description: Home members can send commands to a device; command will be published to MQTT.
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             example: { "action": "set", "value": 1 }
 *     responses:
 *       200:
 *         description: Command published
 *       403:
 *         description: Forbidden
 *       404:
 *         description: Device or ESP not found
 */

/**
 * @swagger
 * /homes/{homeId}/devices/{id}/state:
 *   get:
 *     tags: [Device]
 *     summary: Get device last state
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *     responses:
 *       200:
 *         description: Device state
 *       403:
 *         description: Forbidden
 *       404:
 *         description: Device not found
 */

/**
 * @swagger
 * /homes/{homeId}/devices/{id}/logs:
 *   get:
 *     tags: [Device]
 *     summary: Get device logs (history)
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *       - in: query
 *         name: page
 *         schema:
 *           type: integer
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *     responses:
 *       200:
 *         description: List of logs
 */

/**
 * @swagger
 * /homes/{homeId}/devices/{id}/logs/latest:
 *   get:
 *     tags: [Device]
 *     summary: Get latest device logs (realtime)
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *     responses:
 *       200:
 *         description: Latest logs
 */

/**
 * @swagger
 * /esp/{espId}/devices:
 *   get:
 *     tags: [Device]
 *     summary: List devices attached to an ESP
 *     parameters:
 *       - in: path
 *         name: espId
 *         required: true
 *         schema:
 *           type: string
 *     responses:
 *       200:
 *         description: Devices for esp
 */

/**
 * @swagger
 * /homes/{homeId}/esp32/{espId}/devices:
 *   get:
 *     tags: [Device]
 *     summary: List devices attached to an ESP within a home
 *     description: Home members can list devices attached to a specific ESP in the home.
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *       - in: path
 *         name: espId
 *         required: true
 *         schema:
 *           type: string
 *     responses:
 *       200:
 *         description: Devices for esp within the home
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
 *                       name:
 *                         type: string
 *                       type:
 *                         type: string
 *       403:
 *         description: Not a member of the home
 *       404:
 *         description: ESP not found for this home
 */


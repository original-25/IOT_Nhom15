/**
 * @swagger
 * components:
 *   schemas:
 *     Schedule:
 *       type: object
 *       required:
 *         - name
 *         - device
 *         - time
 *         - action
 *       properties:
 *         name:
 *           type: string
 *           example: "Turn off lights at bedtime"
 *         device:
 *           type: string
 *           example: "device123"
 *         time:
 *           type: string
 *           example: "06:30"
 *         action:
 *           type: string
 *           enum: [on, off]
 *           example: "off"
 *         home:
 *           type: string
 *           example: "home123"
 *         createdAt:
 *           type: string
 *           format: date-time
 *           example: "2023-10-01T10:00:00Z"
 *         updatedAt:
 *           type: string
 *           format: date-time
 *           example: "2023-10-01T10:30:00Z"
 */

/**
 * @swagger
 * /homes/{homeId}/schedules:
 *   post:
 *     tags: [Schedule]
 *     summary: Create a new schedule
 *     description: This endpoint creates a new schedule to control a device at a specified time.
 *     parameters:
 *       - name: homeId
 *         in: path
 *         required: true
 *         description: The ID of the home where the schedule will be created
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
 *                 example: "Turn off lights at bedtime"
 *               deviceId:
 *                 type: string
 *                 example: "device123"
 *               time:
 *                 type: string
 *                 example: "06:30"
 *               action:
 *                 type: string
 *                 enum: [on, off]
 *                 example: "off"
 *     responses:
 *       200:
 *         description: Schedule created successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 data:
 *                   $ref: '#/components/schemas/Schedule'  # Tham chiếu đúng tới schema Schedule
 *       400:
 *         description: Invalid input or missing required fields
 *       500:
 *         description: Internal server error
 */

/**
 * @swagger
 * /homes/{homeId}/schedules:
 *   get:
 *     tags: [Schedule]
 *     summary: Get all schedules for a home
 *     description: This endpoint retrieves all schedules associated with a specific home.
 *     parameters:
 *       - name: homeId
 *         in: path
 *         required: true
 *         description: The ID of the home for which to fetch schedules
 *         schema:
 *           type: string
 *     responses:
 *       200:
 *         description: List of schedules for the home
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
 *                     $ref: '#/components/schemas/Schedule'
 *       500:
 *         description: Internal server error
 */

/**
 * @swagger
 * /homes/{homeId}/schedules/{id}:
 *   delete:
 *     tags: [Schedule]
 *     summary: Delete a schedule
 *     description: This endpoint deletes a schedule based on the provided ID.
 *     parameters:
 *       - name: homeId
 *         in: path
 *         required: true
 *         description: The ID of the home where the schedule exists
 *         schema:
 *           type: string
 *       - name: id
 *         in: path
 *         required: true
 *         description: The ID of the schedule to be deleted
 *         schema:
 *           type: string
 *     responses:
 *       200:
 *         description: Schedule deleted successfully
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
 *                   example: "Deleted"
 *       500:
 *         description: Internal server error
 */

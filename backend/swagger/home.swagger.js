/**
 * @swagger
 * tags:
 *   name: Home
 *   description: APIs for managing homes and invitations
 */

/**
 * @swagger
 * /home/invitation/accept:
 *   post:
 *     tags: [Home]
 *     summary: Accept invitation to join a home
 *     description: This endpoint allows a user to accept an invitation to join a home.
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               token:
 *                 type: string
 *                 example: "abc123def456"
 *     responses:
 *       200:
 *         description: Invitation accepted successfully
 *       400:
 *         description: Invalid or expired invitation
 *       409:
 *         description: User is already a member
 */

/**
 * @swagger
 * /home/invitation/decline:
 *   post:
 *     tags: [Home]
 *     summary: Decline invitation to join a home
 *     description: This endpoint allows a user to decline an invitation to join a home.
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               token:
 *                 type: string
 *                 example: "abc123def456"
 *     responses:
 *       200:
 *         description: Invitation declined successfully
 *       400:
 *         description: Invalid or expired invitation
 */

/**
 * @swagger
 * /home:
 *   post:
 *     tags: [Home]
 *     summary: Create a new home
 *     description: This endpoint allows an authenticated user to create a new home.
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               name:
 *                 type: string
 *                 example: "My New Home"
 *     responses:
 *       201:
 *         description: Home created successfully
 *       400:
 *         description: Home name is required
 *       500:
 *         description: Internal server error
 */

/**
 * @swagger
 * /home:
 *   get:
 *     tags: [Home]
 *     summary: Get all homes of the authenticated user
 *     description: This endpoint allows an authenticated user to get a list of homes they belong to.
 *     responses:
 *       200:
 *         description: Homes retrieved successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 type: object
 *                 properties:
 *                   id:
 *                     type: string
 *                   name:
 *                     type: string
 *                   role:
 *                     type: string
 *       500:
 *         description: Internal server error
 */

/**
 * @swagger
 * /home/{homeId}:
 *   get:
 *     tags: [Home]
 *     summary: Get details of a specific home
 *     description: This endpoint allows an authenticated user to get the details of a specific home they belong to.
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *         description: The ID of the home
 *     responses:
 *       200:
 *         description: Home details retrieved successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 id:
 *                   type: string
 *                 name:
 *                   type: string
 *                 members:
 *                   type: array
 *                   items:
 *                     type: object
 *                     properties:
 *                       userId:
 *                         type: string
 *                       role:
 *                         type: string
 *       400:
 *         description: Home not found
 *       500:
 *         description: Internal server error
 */

/**
 * @swagger
 * /home/{homeId}:
 *   patch:
 *     tags: [Home]
 *     summary: Update the name of a specific home
 *     description: This endpoint allows the home owner to update the name of a specific home.
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
 *                 example: "Updated Home Name"
 *     responses:
 *       200:
 *         description: Home updated successfully
 *       400:
 *         description: Home name is required
 *       500:
 *         description: Internal server error
 */

/**
 * @swagger
 * /home/{homeId}/invite:
 *   post:
 *     tags: [Home]
 *     summary: Invite a member to the home
 *     description: This endpoint allows the home owner to invite a member to the home.
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
 *               email:
 *                 type: string
 *                 example: "invitee@example.com"
 *     responses:
 *       200:
 *         description: Invitation sent successfully
 *       400:
 *         description: Email is required
 *       409:
 *         description: User is already a member of the home
 *       500:
 *         description: Internal server error
 */

/**
 * @swagger
 * /home/{homeId}/members/{userId}:
 *   delete:
 *     tags: [Home]
 *     summary: Remove a member from the home
 *     description: This endpoint allows the home owner to remove a member from the home.
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *         description: The ID of the home
 *       - in: path
 *         name: userId
 *         required: true
 *         schema:
 *           type: string
 *         description: The ID of the user to remove
 *     responses:
 *       200:
 *         description: Member removed successfully
 *       400:
 *         description: Cannot remove the owner
 *       500:
 *         description: Internal server error
 */

/**
 * @swagger
 * /home/{homeId}/members:
 *   get:
 *     tags: [Home]
 *     summary: Get members of a specific home
 *     description: This endpoint allows an authenticated user to get the list of members of a specific home.
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: string
 *         description: The ID of the home
 *     responses:
 *       200:
 *         description: Members retrieved successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 type: object
 *                 properties:
 *                   userId:
 *                     type: string
 *                   email:
 *                     type: string
 *                   username:
 *                     type: string
 *                   role:
 *                     type: string
 *       500:
 *         description: Internal server error
 */



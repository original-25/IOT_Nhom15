const swaggerJSDoc = require("swagger-jsdoc");
const swaggerUi = require("swagger-ui-express");

// Cấu hình Swagger
const swaggerOptions = {
  definition: {
    openapi: "3.0.0",

    "components": {
      "securitySchemes": {
        "bearerAuth": {
          "type": "http",
          "scheme": "bearer",
          "bearerFormat": "JWT"
        }
      }
    },
    "security": [{
      "bearerAuth": []
    }],

    info: {
      title: "IoT User Management API", // Tiêu đề API
      version: "1.0.0",
      description: "API documentation for user authentication and management.",
    },
    servers: [{
      url: "http://localhost:4000/api", // Đảm bảo URL đúng
    }, ],
  },
  apis: [
    "./routers/auth.routes.js", // Đúng với đường dẫn của bạn
    "./swagger/*.js", // Nếu bạn có nhiều file swagger mô tả
  ],
};

const swaggerDocs = swaggerJSDoc(swaggerOptions);

module.exports = {
  swaggerDocs,
  swaggerUi
};
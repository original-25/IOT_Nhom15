const express = require("express");
const cors = require("cors");
const authRoutes = require("./routers/auth.routes");
const homeRoutes = require("./routers/home.routes");

const { swaggerDocs, swaggerUi } = require("./config/swagger");


const app = express();

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));


app.use("/api/auth", authRoutes);
app.use("/api/home", homeRoutes);

app.use("/api-docs", swaggerUi.serve, swaggerUi.setup(swaggerDocs));


app.get("/", (req, res) => res.send("IoT API running"));

module.exports = app;

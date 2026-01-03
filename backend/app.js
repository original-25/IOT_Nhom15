const express = require("express");
const cors = require("cors");
const authRoutes = require("./routers/auth.routes");
const homeRoutes = require("./routers/home.routes");
const esp32Routes = require("./routers/home.esp32.routes");
const deviceRoutes = require("./routers/devices.routes");
const schedulerRoutes = require("./routers/scheduler.routes")
require('./mqtt/mqtt.listener');

const { swaggerDocs, swaggerUi } = require("./config/swagger");


const app = express();

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));


app.use("/api/auth", authRoutes);
app.use("/api/home", homeRoutes);

//router cho esp32 dùng root là /home
app.use("/api", esp32Routes)

app.use("/api", deviceRoutes)

app.use("/api", schedulerRoutes)



app.use("/api-docs", swaggerUi.serve, swaggerUi.setup(swaggerDocs));


app.get("/", (req, res) => res.send("IoT API running"));

module.exports = app;

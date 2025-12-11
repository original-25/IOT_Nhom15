// app.js

const express = require('express');
const app = express();
const port = 3000;

// Middleware (optional)
app.use(express.json());  // Để Express có thể hiểu dữ liệu JSON

// Route gốc
app.get('/', (req, res) => {
  res.send('Hello, World!');
});

// Một route khác
app.get('/api', (req, res) => {
  res.json({ message: 'API is working!' });
});

// Start server
app.listen(port, () => {
  console.log(`Server is running at http://localhost:${port}`);
});

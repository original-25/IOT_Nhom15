const axios = require("axios");

const FLESPI_API = "https://flespi.io/platform/tokens";
const MASTER_TOKEN = process.env.MASTER_FLESPI_TOKEN;

async function createEsp32MqttToken({ homeId, espDeviceId }) {
  const body = [
    {
      access: {
        type: 2,
        acl: [
          {
            uri: "mqtt",
            topic: `/iot_nhom15/home/${homeId}/esp32/${espDeviceId}/#`,
            actions: ["publish", "subscribe"]
          }
        ]
      },
      ttl: 60 * 60 * 24 * 365 // 1 year
    }
  ];

  const res = await axios.post(FLESPI_API, body, {
    headers: {
      Authorization: `FlespiToken ${MASTER_TOKEN}`,
      "Content-Type": "application/json"
    }
  });

  const tokenObj = res.data?.result?.[0];
  if (!tokenObj?.key) {
    throw new Error("Failed to create flespi token");
  }
  

  return {
    flespiTokenId: tokenObj.id,
    mqttPassword: tokenObj.key, // ESP32 dùng cái này
    ttl: tokenObj.ttl
  };
}


function deleteAclToken(accessToken, tokenId) {
  return axios.delete(`https://flespi.io/platform/tokens/${tokenId}`, {
    headers: {
      'Authorization': `FlespiToken ${accessToken}`
    }
  })
  .then(response => {
    console.log('Token deleted successfully:', response.data);
    return response.data; // Trả về kết quả thành công
  })
  .catch(error => {
    console.error('Error deleting token:', error.response ? error.response.data : error.message);
    throw error; // Ném lỗi để xử lý sau
  });
}



module.exports = {
  createEsp32MqttToken, deleteAclToken
};

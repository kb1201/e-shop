import axios from 'axios';

// Single entry point — all traffic goes through the API gateway.
// The gateway owns CORS, path-based routing, and (in future) rate limiting.
const GATEWAY_URL = 'http://localhost:8090';

const userApi = axios.create({
    baseURL: GATEWAY_URL,
    withCredentials: true,
});

const catalogApi = axios.create({
    baseURL: GATEWAY_URL,
    withCredentials: true,
});

const orderApi = axios.create({
    baseURL: GATEWAY_URL,
    withCredentials: true,
});

const shipmentApi = axios.create({
    baseURL: GATEWAY_URL,
    withCredentials: true,
});

const inventoryApi = axios.create({
    baseURL: GATEWAY_URL,
    withCredentials: true,
});

const analyticsApi = axios.create({
    baseURL: GATEWAY_URL,
    withCredentials: true,
});

export { userApi, catalogApi, shipmentApi, orderApi, inventoryApi, analyticsApi };

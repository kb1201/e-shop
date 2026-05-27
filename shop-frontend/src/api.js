import axios from 'axios';

const userApi = axios.create({
    baseURL: 'http://localhost:8080',
    withCredentials: true,
});

const catalogApi = axios.create({
    baseURL: 'http://localhost:8081',
    withCredentials: true,
});

const orderApi = axios.create({
    baseURL: 'http://localhost:8084',
    withCredentials: true,
});

const shipmentApi = axios.create({
    baseURL: 'http://localhost:8082',
    withCredentials: true,
});

const inventoryApi = axios.create({
    baseURL: 'http://localhost:8083',
    withCredentials: true,
});

const analyticsApi = axios.create({
    baseURL: 'http://localhost:8086',
    withCredentials: true,
});

export {userApi, catalogApi, shipmentApi, orderApi, inventoryApi, analyticsApi};

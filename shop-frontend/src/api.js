import axios from 'axios';

const userApi = axios.create({
    baseURL: 'http://localhost:8080',
});

const catalogApi = axios.create({
    baseURL: 'http://localhost:8081',
});

const orderApi = axios.create({
    baseURL: 'http://localhost:8084',
});

const shipmentApi = axios.create({
    baseURL: 'http://localhost:8082',
});

const inventoryApi = axios.create({
    baseURL: 'http://localhost:8083',
});

const addAuthToken = (config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
};

userApi.interceptors.request.use(addAuthToken, (error) => Promise.reject(error));
catalogApi.interceptors.request.use(addAuthToken, (error) => Promise.reject(error));
shipmentApi.interceptors.request.use(addAuthToken, (error) => Promise.reject(error));
orderApi.interceptors.request.use(addAuthToken, (error) => Promise.reject(error));
inventoryApi.interceptors.request.use(addAuthToken, (error) => Promise.reject(error));

export {userApi, catalogApi, shipmentApi, orderApi, inventoryApi};

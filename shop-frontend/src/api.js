import axios from 'axios';

const userApi = axios.create({
    baseURL: 'http://localhost:8080',
});

const catalogApi = axios.create({
    baseURL: 'http://localhost:8081',
});

const addAuthToken = (config) => {
    console.log("I am here")
    const token = localStorage.getItem('token');
    console.log(token)
    if (token) {
        console.log("I am here 2")
        config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
};

userApi.interceptors.request.use(addAuthToken, (error) => Promise.reject(error));
catalogApi.interceptors.request.use(addAuthToken, (error) => Promise.reject(error));

export {userApi, catalogApi};

import Axios from "axios";

const axios = Axios.create({
  baseURL: "http://127.0.0.1:5000",
  timeout: 10000,
});

axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

export default axios;
import Axios from "axios";

const axios = Axios.create({
  baseURL: "http://127.0.0.1:5000/api",
  timeout: 10000,
});

export default axios;
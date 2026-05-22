import axios from 'axios';
import { parseApiError } from '../utils/error';

const http = axios.create({
  baseURL: '/api',
  timeout: 20000,
  withCredentials: true
});

http.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(new Error(parseApiError(error)))
);

export default http;

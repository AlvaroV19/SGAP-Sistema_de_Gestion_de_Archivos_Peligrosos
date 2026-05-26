import axios from "axios";

const API = "http://localhost:8080/api/archivos";

export const subirArchivo = (formData) =>
  axios.post(API, formData, {
    headers: { "Content-Type": "multipart/form-data" }
  });

export const obtenerArchivos = () => axios.get(API);
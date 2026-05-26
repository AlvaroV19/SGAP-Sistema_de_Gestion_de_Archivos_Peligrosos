import axios from 'axios'
import { token } from './auth.js'

const BASE_URL = 'http://localhost:8080'
const API      = `${BASE_URL}/api/archivos`

// ── Interceptor: adjunta el JWT a cada petición ───────────────────────────────
// (También está en auth.js; este bloque es una salvaguarda por si api.js
//  se importa antes de que el interceptor de auth.js esté activo.)
axios.interceptors.request.use((config) => {
  const jwt = token?.value
  if (jwt && !config.headers['Authorization']) {
    config.headers['Authorization'] = `Bearer ${jwt}`
  }
  return config
})

// ── Archivos ──────────────────────────────────────────────────────────────────

/** Sube un archivo al pipeline de análisis (POST multipart/form-data). */
export const subirArchivo = (formData) =>
  axios.post(API, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

/** Lista todos los archivos visibles para el rol del usuario autenticado. */
export const obtenerArchivos = () => axios.get(API)

/** Obtiene un único archivo por id. */
export const obtenerArchivo = (id) => axios.get(`${API}/${id}`)

/** Elimina un archivo (solo ROLE_ADMIN). */
export const eliminarArchivo = (id) => axios.delete(`${API}/${id}`)

// ── Reportes PDF ──────────────────────────────────────────────────────────────

/**
 * Descarga el reporte PDF de análisis de un archivo específico.
 * El backend devuelve un stream application/pdf.
 *
 * @param {number} id - ID del archivo
 * @returns {Promise<Blob>} blob del PDF
 */
export const descargarReporteArchivo = async (id) => {
  const res = await axios.get(`${API}/${id}/reporte`, {
    responseType: 'blob'
  })
  return res.data
}

/**
 * Descarga el reporte PDF consolidado (todos los archivos).
 * Solo disponible para ROLE_ANALYST y ROLE_ADMIN.
 *
 * @returns {Promise<Blob>} blob del PDF
 */
export const descargarReporteConsolidado = async () => {
  const res = await axios.get(`${API}/reporte`, {
    responseType: 'blob'
  })
  return res.data
}

// ── Utilidad: forzar descarga desde un Blob ───────────────────────────────────

/**
 * Crea un enlace temporal y lo "clica" para descargar un Blob.
 *
 * @param {Blob}   blob     - contenido del PDF
 * @param {string} filename - nombre del archivo a guardar
 */
export function descargarBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a   = document.createElement('a')
  a.href     = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

import { ref } from 'vue'
import axios from 'axios'

const BASE_URL = 'http://localhost:8080'

// ── Estado reactivo ──────────────────────────────────────────────────────────
const stored = typeof localStorage !== 'undefined' ? localStorage.getItem('sgap_session') : null
const session = stored ? JSON.parse(stored) : null

const user  = ref(session?.user  || null)
const token = ref(session?.token || null)

// ── Axios: interceptor de JWT ─────────────────────────────────────────────────
axios.interceptors.request.use((config) => {
  if (token.value) {
    config.headers['Authorization'] = `Bearer ${token.value}`
  }
  return config
})

// ── Función de login real contra el backend ──────────────────────────────────
/**
 * Autentica al usuario con username y contraseña.
 * El backend (LoginResponse) devuelve:
 *   { token, tokenType, username, fullName, role, authorities, expiresIn }
 * Internamente se mapea a { nombre, rol } para el frontend.
 *
 * @param {string} username  - nombre de usuario (no email)
 * @param {string} password
 * @returns {{ nombre: string, rol: string }} datos del usuario autenticado
 * @throws Error si las credenciales son inválidas o el servidor falla
 */
async function login(username, password) {
  const res = await axios.post(`${BASE_URL}/api/auth/login`, { username, password })

  // Mapeo de campos del backend al modelo del frontend
  const { token: jwt, fullName, role } = res.data
  const nombre = fullName
  const rol    = role   // ej: "ROLE_ADMIN", "ROLE_ANALYST", "ROLE_AUDITOR"

  // Guardar token y datos en memoria
  token.value = jwt
  user.value  = { nombre, rol }

  // Persistir en localStorage
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('sgap_session', JSON.stringify({ token: jwt, user: { nombre, rol } }))
  }

  return user.value
}

function logout() {
  token.value = null
  user.value  = null
  if (typeof localStorage !== 'undefined') {
    localStorage.removeItem('sgap_session')
  }
}

function isAuthenticated() {
  return !!token.value && !!user.value
}

/**
 * Determina la ruta del dashboard según el rol devuelto por el backend.
 * El backend usa 'ROLE_ADMIN', 'ROLE_ANALYST', 'ROLE_AUDITOR'.
 */
function getDashboardRoute(u = user.value) {
  if (!u) return '/login'
  const rol = String(u.rol || '').toUpperCase()
  if (rol.includes('ADMIN'))   return '/admin'
  if (rol.includes('AUDIT'))   return '/auditor'
  return '/security'
}

export { user, token, login, logout, isAuthenticated, getDashboardRoute }

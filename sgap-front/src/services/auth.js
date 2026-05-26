import { ref } from 'vue'

const roles = {
  analista:  { dashboard: 'security', nombre: 'Sarah Jenkins', rol: 'Analista de Seguridad' },
  admin:     { dashboard: 'admin',    nombre: 'Marcus Hale',    rol: 'Administrador' },
  auditor:   { dashboard: 'auditor',  nombre: 'Elena Mora',     rol: 'Auditora' },
}

const stored = typeof localStorage !== 'undefined' ? localStorage.getItem('sgap_user') : null
const user = ref(stored ? JSON.parse(stored) : null)

function login(role) {
  const r = roles[role] || roles.analista
  const u = { nombre: r.nombre, rol: r.rol, tipo: role }
  user.value = u
  if (typeof localStorage !== 'undefined') localStorage.setItem('sgap_user', JSON.stringify(u))
  return u
}

function logout() {
  user.value = null
  if (typeof localStorage !== 'undefined') localStorage.removeItem('sgap_user')
}

function isAuthenticated() {
  return !!user.value
}

export { user, login, logout, isAuthenticated }

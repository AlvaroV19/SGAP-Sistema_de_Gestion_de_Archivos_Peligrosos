import { createRouter, createWebHistory } from 'vue-router'
import { user, getDashboardRoute } from '../src/services/auth.js'

const routes = [
  // Raíz: si hay sesión va al dashboard, si no al login
  {
    path: '/',
    redirect: () => user.value ? getDashboardRoute() : '/login'
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../src/views/LoginView.vue')
  },
  {
    path: '/welcome',
    name: 'welcome',
    component: () => import('../src/views/WelcomeView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/security',
    name: 'security',
    component: () => import('../src/views/SecAnalyst/SecurityDashboardView.vue'),
    meta: { requiresAuth: true, allowedRoles: ['ROLE_ANALYST'] }
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('../src/views/Admin/AdminDashboardView.vue'),
    meta: { requiresAuth: true, allowedRoles: ['ROLE_ADMIN'] }
  },
  {
    path: '/auditor',
    name: 'auditor',
    component: () => import('../src/views/AuditorDashboardView.vue'),
    meta: { requiresAuth: true, allowedRoles: ['ROLE_AUDITOR'] }
  },
  {
    path: '/repositorio',
    name: 'repositorio',
    component: () => import('../src/views/FileRepositoryView.vue'),
    meta: { requiresAuth: true, allowedRoles: ['ROLE_ANALYST', 'ROLE_ADMIN', 'ROLE_AUDITOR'] }
  },
  // Cualquier ruta desconocida: al login si no hay sesión, al dashboard si la hay
  {
    path: '/:pathMatch(.*)*',
    redirect: () => user.value ? getDashboardRoute() : '/login'
  }
]

const router = createRouter({ history: createWebHistory(), routes })

/**
 * Verifica si el usuario tiene permiso para acceder a la ruta.
 * Compara user.rol (ej: "ROLE_ADMIN") contra meta.allowedRoles (ej: ["ROLE_ADMIN"]).
 */
function canAccessRoute(to, currentUser) {
  const allowedRoles = to.meta?.allowedRoles
  if (!allowedRoles || allowedRoles.length === 0) return true
  const rol = String(currentUser?.rol || '').toUpperCase()
  return allowedRoles.some(r => rol.includes(r.replace('ROLE_', '')))
}

router.beforeEach((to, from, next) => {
  const requiresAuth = to.meta?.requiresAuth
  const loggedIn     = !!user.value

  // 1. Ruta protegida sin sesión → al login
  if (requiresAuth && !loggedIn) {
    return next({ name: 'login', query: { redirect: to.fullPath } })
  }

  // 2. Ya autenticado intentando ir al login → al dashboard
  if (to.name === 'login' && loggedIn) {
    return next(getDashboardRoute(user.value))
  }

  // 3. Ruta protegida pero el rol no tiene acceso → al dashboard propio
  if (requiresAuth && loggedIn && !canAccessRoute(to, user.value)) {
    return next(getDashboardRoute(user.value))
  }

  // 4. Todo bien → continuar
  next()
})

export default router

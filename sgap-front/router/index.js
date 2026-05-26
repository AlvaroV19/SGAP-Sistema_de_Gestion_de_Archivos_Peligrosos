import { createRouter, createWebHistory } from 'vue-router'
import { user } from '../src/services/auth.js'

const routes = [
	{ path: '/', redirect: '/welcome' },
	{ path: '/login', name: 'login', component: () => import('../src/views/LoginView.vue') },
	{ path: '/welcome', name: 'welcome', component: () => import('../src/views/WelcomeView.vue'), meta: { requiresAuth: true } },
	{ path: '/security', name: 'security', component: () => import('../src/views/SecurityDashboardView.vue'), meta: { requiresAuth: true } },
	{ path: '/admin', name: 'admin', component: () => import('../src/views/AdminDashboardView.vue'), meta: { requiresAuth: true } },
	{ path: '/auditor', name: 'auditor', component: () => import('../src/views/AuditorDashboardView.vue'), meta: { requiresAuth: true } },
	{ path: '/repositorio', name: 'repositorio', component: () => import('../src/views/FileRepositoryView.vue'), meta: { requiresAuth: true } },
	{ path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
	const requires = to.meta && to.meta.requiresAuth
	if (requires && !user.value) {
		next({ name: 'login', query: { redirect: to.fullPath } })
	} else if (to.name === 'login' && user.value) {
		const redirect = to.query.redirect || '/welcome'
		next(redirect)
	} else {
		next()
	}
})

export default router


<script setup>
import { provide, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { user, logout } from './services/auth.js'

const router = useRouter()
const route = useRoute()

function navegar(vista) {
  const mapa = {
    security: 'security',
    admin: 'admin',
    auditor: 'auditor',
    repositorio: 'repositorio'
  }
  const name = mapa[vista] || vista
  router.push({ name }).catch(() => {})
}

function cerrarSesion() {
  logout()
  router.push({ name: 'login' }).catch(() => {})
}

provide('usuario', user)
provide('navegar', navegar)
provide('cerrarSesion', cerrarSesion)

const vistaActual = computed(() => route.name || '')
</script>

<template>
  <router-view v-slot="{ Component }">
    <component
      :is="Component"
      :usuario="user"
      :vista-actual="vistaActual"
      @navegar="navegar"
      @cerrar-sesion="cerrarSesion"
    />
  </router-view>
</template>

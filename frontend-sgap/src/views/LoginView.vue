<template>
  <LoginForm class="col-6 mx-auto" @login="onLogin" />
</template>

<script setup>
import LoginForm from '../components/auth/LoginForm.vue'
import { useRouter } from 'vue-router'
import { getDashboardRoute } from '../services/auth.js'

const router = useRouter()

function onLogin(usuario) {
  // LoginForm ya ejecutó login() y emite el usuario autenticado {nombre, rol}
  // Solo queda redirigir al dashboard correspondiente
  router.push(getDashboardRoute(usuario))
}
</script>

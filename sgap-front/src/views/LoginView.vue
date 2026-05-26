<template>
  <LoginForm class="col-6 mx-auto" @login="onLogin" />
</template>

<script setup>
import LoginForm from '../components/auth/LoginForm.vue'
import { useRouter, useRoute } from 'vue-router'
import { login } from '../services/auth.js'

const router = useRouter()
const route = useRoute()

function onLogin(rol) {
  login(rol)
  const redirect = route.query.redirect || '/'
  router.push(redirect)
}
</script>

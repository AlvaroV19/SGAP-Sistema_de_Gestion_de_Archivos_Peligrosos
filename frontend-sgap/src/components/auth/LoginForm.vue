<template>
  <div class="login-bg">
    <div class="login-dots"></div>

    <div class="login-card">
      <div class="login-logo">
        <i class="bi bi-shield-fill"></i>
      </div>

      <h1 class="login-title">Bienvenido a SGAP</h1>
      <p class="login-subtitle">Inicia sesión para acceder al repositorio seguro de archivos.</p>

      <div class="login-form">
        <div class="lf-group">
          <label class="lf-label">Usuario</label>
          <input
            v-model="username"
            type="text"
            class="lf-input"
            placeholder="nombre.usuario"
            @keyup.enter="iniciarSesion"
          />
        </div>

        <div class="lf-group">
          <div class="lf-label-row">
            <label class="lf-label">Contraseña</label>
            <a href="#" class="lf-forgot" @click.prevent>¿Olvidaste tu contraseña?</a>
          </div>
          <div class="lf-input-wrap">
            <input
              v-model="password"
              :type="mostrarPass ? 'text' : 'password'"
              class="lf-input"
              placeholder="••••••••••••"
              @keyup.enter="iniciarSesion"
            />
            <button class="lf-eye" @click="mostrarPass = !mostrarPass" type="button">
              <i :class="mostrarPass ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
            </button>
          </div>
        </div>

        <!-- Mensaje de error -->
        <div v-if="errorMsg" class="lf-error">
          <i class="bi bi-exclamation-circle-fill"></i>
          {{ errorMsg }}
        </div>

        <button class="lf-submit" @click="iniciarSesion" :disabled="iniciando || !username || !password">
          <span v-if="iniciando">
            <span class="sgap-spinner"></span> Verificando...
          </span>
          <span v-else>Iniciar Sesión</span>
        </button>

        <p class="lf-help">
          ¿No tienes acceso?
          <strong style="color:var(--text-primary)">Contacta al Administrador del Sistema</strong>
        </p>
      </div>
    </div>

    <div class="login-warning">
      <i class="bi bi-shield-exclamation"></i>
      SISTEMA PARA USO EXCLUSIVO DE PERSONAL AUTORIZADO
    </div>
  </div>
</template>

<script>
import { login } from '../../services/auth.js'

export default {
  emits: ['login'],
  data() {
    return {
      username: '',
      password: '',
      mostrarPass: false,
      iniciando: false,
      errorMsg: '',
    }
  },
  methods: {
    async iniciarSesion() {
      if (!this.username || !this.password) return
      this.iniciando = true
      this.errorMsg  = ''
      try {
        const usuario = await login(this.username, this.password)
        // Emitir al padre el objeto de usuario con nombre y rol reales del backend
        this.$emit('login', usuario)
      } catch (err) {
        const status = err?.response?.status
        if (status === 401 || status === 403) {
          this.errorMsg = 'Credenciales incorrectas. Verifica tu correo y contraseña.'
        } else {
          this.errorMsg = 'No se pudo conectar con el servidor. Intenta más tarde.'
        }
      } finally {
        this.iniciando = false
      }
    }
  }
}
</script>

<style scoped>
.lf-error {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #fee2e2;
  border: 1px solid #fca5a5;
  border-radius: 8px;
  color: #dc2626;
  font-size: 13px;
  margin-bottom: 4px;
}
</style>

<template>
  <div class="login-bg">
    <!-- Dotted texture overlay -->
    <div class="login-dots"></div>

    <div class="login-card">
      <!-- Logo -->
      <div class="login-logo">
        <i class="bi bi-shield-fill"></i>
      </div>

      <h1 class="login-title">Bienvenido a SGAP</h1>
      <p class="login-subtitle">Inicia sesión para acceder al repositorio seguro de archivos.</p>

      <!-- Demo role picker -->
      <div class="login-role-pick">
        <span class="role-pick-label">Demostración — selecciona un rol:</span>
        <div class="role-pick-btns">
          <button
            v-for="r in roles" :key="r.key"
            class="role-btn"
            :class="{ active: rolSeleccionado === r.key }"
            @click="rolSeleccionado = r.key"
          >
            <i :class="r.icon"></i>
            {{ r.label }}
          </button>
        </div>
      </div>

      <div class="login-form">
        <!-- Email -->
        <div class="lf-group">
          <label class="lf-label">Correo institucional</label>
          <input
            v-model="email"
            type="email"
            class="lf-input"
            :placeholder="rolActual.email"
          />
        </div>

        <!-- Password -->
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
            />
            <button class="lf-eye" @click="mostrarPass = !mostrarPass">
              <i :class="mostrarPass ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
            </button>
          </div>
        </div>

        <!-- Sign in -->
        <button class="lf-submit" @click="iniciarSesion" :disabled="iniciando">
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

    <!-- Warning bar -->
    <div class="login-warning">
      <i class="bi bi-shield-exclamation"></i>
      SISTEMA PARA USO EXCLUSIVO DE PERSONAL AUTORIZADO
    </div>
  </div>
</template>

<script>
export default {
  emits: ['login'],
  data() {
    return {
      email: '',
      password: '',
      mostrarPass: false,
      iniciando: false,
      rolSeleccionado: 'analista',
      roles: [
        { key: 'analista', label: 'Analista',       icon: 'bi bi-person-badge',      email: 'analista@sgap.gov.co'  },
        { key: 'admin',    label: 'Administrador',   icon: 'bi bi-person-gear',       email: 'admin@sgap.gov.co'     },
        { key: 'auditor',  label: 'Auditor',         icon: 'bi bi-person-check',      email: 'auditor@sgap.gov.co'   },
      ],
    }
  },
  computed: {
    rolActual() {
      return this.roles.find(r => r.key === this.rolSeleccionado) || this.roles[0]
    }
  },
  methods: {
    async iniciarSesion() {
      this.iniciando = true
      await new Promise(r => setTimeout(r, 900))
      this.iniciando = false
      this.$emit('login', this.rolSeleccionado)
    }
  }
}
</script>

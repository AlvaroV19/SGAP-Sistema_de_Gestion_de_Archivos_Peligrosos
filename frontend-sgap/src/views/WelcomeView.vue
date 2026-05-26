<template>
  <div id="app">
    <Sidebar :usuario="usuario" :vista-actual="vistaActual" @cerrar-sesion="$emit('cerrar-sesion')" @navegar="$emit('navegar',$event)"></Sidebar>

    <div class="sgap-main">
      <header class="sgap-topbar">
        <div class="topbar-page-title">Welcome portal</div>
        <div class="topbar-env-badge"><i class="bi bi-circle-fill" style="color:#22c55e;font-size:8px;"></i> Authenticated session</div>
        <div class="topbar-search">
          <i class="bi bi-search search-icon"></i>
          <input type="text" placeholder="Search platform" />
        </div>
        <div class="topbar-user">
          <div class="topbar-user-info">
            <div class="topbar-user-name">{{ usuario.nombre }}</div>
            <div class="topbar-user-role">{{ usuario.rol }}</div>
          </div>
          <div class="topbar-avatar">{{ iniciales }}</div>
        </div>
      </header>

      <main class="sgap-content">
        <div class="welcome-hero">
          <h1 class="page-title">Plataforma de Seguridad SGAP</h1>
          <p class="page-subtitle">Un espacio de trabajo centralizado para control de archivos peligrosos, registro de riesgos, trazabilidad, alertas, y la generación de informes de toda la organización.</p>
        </div>

        <div class="capabilities" style="display:grid;grid-template-columns:repeat(2,1fr);gap:12px;margin-top:18px;">
          <div v-for="cap in capabilities" :key="cap.title" class="cap-card" style="padding:16px;border-radius:8px;border:1px solid #eef2f7;background:#fff;">
            <div style="font-weight:700;margin-bottom:6px;">{{ cap.title }}</div>
            <div style="color:var(--text-secondary);font-size:13px;">{{ cap.desc }}</div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script>
import Sidebar from '../components/Sidebar.vue'

export default {
  components: { Sidebar },
  props: ['usuario', 'vistaActual'],
  emits: ['navegar', 'cerrar-sesion'],
  data() {
    return {
      capabilities: [
            {title: 'Gestión de archivos peligrosos', desc: 'Registrar, cargar, clasificar, consultar, actualizar y eliminar archivos peligrosos del repositorio.'},
            {title: 'Gestión de riesgos de seguridad', desc: 'Registrar escenarios de malware, phishing, ransomware y fuga de información.'},
            {title: 'Gestión de usuarios y roles', desc: 'Crear usuarios, asignar roles y aplicar control de acceso basado en roles.'},
            {title: 'Monitoreo y trazabilidad', desc: 'Registrar cargas, descargas y eliminaciones preservando la identificación del usuario.'},
            {title: 'Gestión de alertas', desc: 'Generar alertas para archivos maliciosos e intentos de acceso no autorizado.'},
            {title: 'Reportes', desc: 'Generar reportes de archivos peligrosos, resúmenes de incidentes y reportes de actividad de usuarios para revisión.'}
      ]
    }
  },
  computed: {
    iniciales() {
      const n = (this.usuario && this.usuario.nombre) || 'AS'
      return n.split(' ').map(x => x[0] || '').join('').substring(0, 2).toUpperCase()
    }
  }
}
</script>

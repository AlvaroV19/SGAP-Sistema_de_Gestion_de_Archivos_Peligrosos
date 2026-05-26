<template>
  <div id="app">
    <Sidebar :usuario="usuario" :vista-actual="vistaActual" @cerrar-sesion="$emit('cerrar-sesion')" @navegar="$emit('navegar',$event)"></Sidebar>

    <div class="sgap-main">
      <header class="sgap-topbar">
        <div class="topbar-page-title">Centro de Operaciones de Seguridad</div>
        <div class="topbar-env-badge"><i class="bi bi-circle-fill" style="color:#22c55e;font-size:8px;"></i> Ambiente: Producción</div>
        <div class="topbar-search">
          <i class="bi bi-search search-icon"></i>
          <input v-model="busqueda" type="text" placeholder="Buscar archivos, amenazas, hashes..." />
        </div>
        <button class="topbar-upload-btn" @click="$emit('navegar','repositorio')">
          <i class="bi bi-upload"></i>Subir Archivo
        </button>
        <div class="topbar-user">
          <div class="topbar-user-info">
            <div class="topbar-user-name">{{ usuario.nombre }}</div>
            <div class="topbar-user-role">{{ usuario.rol }}</div>
          </div>
          <div class="topbar-avatar">{{ iniciales }}</div>
        </div>
      </header>

      <main class="sgap-content">
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-label">Total de Archivos Analizados<i class="bi bi-file-earmark-text stat-icon"></i></div>
            <div class="stat-value">14,289</div>
            <div class="stat-sub positive"><i class="bi bi-arrow-up"></i> +2.4% respecto a la semana pasada</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">Amenazas de Alto Riesgo<i class="bi bi-fire stat-icon" style="color:#dc2626"></i></div>
            <div class="stat-value" style="color:#dc2626">42</div>
            <div class="stat-sub negative"><i class="bi bi-arrow-up"></i> +12 nuevas detecciones hoy</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">Alertas Activas<i class="bi bi-bell-fill stat-icon" style="color:#d97706"></i></div>
            <div class="stat-value" style="color:#d97706">18</div>
            <div class="stat-sub" style="color:#d97706">8 requieren acción inmediata</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">Estado de Cuarentena<i class="bi bi-shield-check stat-icon" style="color:#16a34a"></i></div>
            <div class="stat-value" style="color:#16a34a">98.5%</div>
            <div class="stat-sub positive">Tasa de éxito en contención</div>
          </div>
        </div>

        <div class="dash-two-col">
          <div class="dash-main-col">
            <div class="section-card" style="margin-bottom:20px;">
              <div class="section-card-header">
                <div>
                  <h2>Alertas Críticas de Seguridad</h2>
                  <p class="section-subtitle">Accesos no autorizados y actividades sospechosas recientes.</p>
                </div>
                <button class="btn-ghost">Ver todas</button>
              </div>
              <div style="overflow-x:auto;">
                <table class="sgap-table">
                  <thead>
                    <tr>
                      <th>Tipo de Alerta</th>
                      <th>Fuente / Usuario</th>
                      <th>Impacto</th>
                      <th>Tiempo</th>
                      <th>Acción</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="a in alertas" :key="a.id">
                      <td>
                        <div style="display:flex;align-items:center;gap:9px;">
                          <div class="alert-type-icon" :class="a.color">
                            <i :class="a.icon"></i>
                          </div>
                          <span style="font-weight:600;font-size:13.5px;">{{ a.tipo }}</span>
                        </div>
                      </td>
                      <td style="color:var(--text-secondary);font-size:13px;">{{ a.fuente }}</td>
                      <td><span class="risk-badge" :class="a.impacto === 'High' ? 'high' : 'medium'">{{ a.impacto === 'High' ? 'Alto' : 'Medio' }}</span></td>
                      <td style="color:var(--text-muted);font-size:12.5px;white-space:nowrap;">{{ a.tiempo }}</td>
                      <td><button class="btn-ghost" style="padding:4px 10px;"><i class="bi bi-chevron-right"></i></button></td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <div class="section-card">
              <div class="section-card-header">
                <div>
                  <h2>Auditoría de Actividad Reciente</h2>
                  <p class="section-subtitle">Trazabilidad de acciones sobre archivos gestionados.</p>
                </div>
              </div>
              <div class="activity-feed">
                <div v-for="a in actividades" :key="a.id" class="activity-item">
                  <div class="activity-avatar">{{ a.iniciales }}</div>
                  <div class="activity-body">
                    <div class="activity-text">{{ a.texto }}</div>
                    <div class="activity-meta">{{ a.tiempo }} • IP: {{ a.ip }}</div>
                  </div>
                  <span class="activity-badge" :class="a.tipo">{{ a.tipoLabel }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="dash-side-col">
            <div class="section-card">
              <div class="section-card-header">
                <div>
                  <h2>Archivos Peligrosos</h2>
                  <p class="section-subtitle">Elementos clasificados recientemente.</p>
                </div>
              </div>
              <div class="danger-files-list">
                <div v-for="f in archivosPeligrosos" :key="f.id" class="danger-file-card">
                  <div class="df-header">
                    <div class="df-icon" :class="f.iconClass">
                      <i :class="f.icon"></i>
                    </div>
                    <div class="df-info">
                      <div class="df-name">{{ f.nombre }}</div>
                      <div class="df-type">{{ f.tipo }}</div>
                    </div>
                    <span class="risk-badge" :class="f.riesgo === 'Alto' ? 'high' : 'medium'">
                      {{ f.riesgo === 'Alto' ? 'Alto Riesgo' : 'Riesgo Medio' }}
                    </span>
                  </div>
                  <div class="df-meta">
                    <span><i class="bi bi-hdd"></i> {{ f.size }}</span>
                    <span><i class="bi bi-hash"></i> SHA-256: {{ f.hash }}</span>
                  </div>
                  <div class="df-actions">
                    <button class="btn-ghost" style="flex:1;justify-content:center;">Analizar</button>
                    <button class="btn-primary-sgap" style="flex:1;justify-content:center;padding:7px 12px;font-size:12.5px;">Cuarentena</button>
                  </div>
                </div>
                <div style="text-align:center;padding:14px;">
                  <button class="btn-ghost" style="width:100%;justify-content:center;" @click="$emit('navegar','repositorio')">
                    Ver todos los archivos clasificados
                  </button>
                </div>
              </div>
            </div>
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
      busqueda: '',
      alertas: [
        { id:1, tipo:'Ejecución de Malware Bloqueada', fuente:'DESKTOP-US889', impacto:'High', tiempo:'Hace 10 min', icon:'bi bi-shield-x', color:'alert-red' },
        { id:2, tipo:'Intento de Acceso No Autorizado', fuente:'j.doe@empresa.com', impacto:'Medium', tiempo:'Hace 1 hora', icon:'bi bi-person-x', color:'alert-orange' },
        { id:3, tipo:'Eliminación Masiva de Archivos', fuente:'Service Account Backup', impacto:'Medium', tiempo:'Hace 2 horas', icon:'bi bi-trash3', color:'alert-orange' },
        { id:4, tipo:'Fuga de Datos Sensibles', fuente:'m.smith@empresa.com', impacto:'High', tiempo:'Hace 4 horas', icon:'bi bi-file-earmark-x', color:'alert-red' },
      ],
      actividades: [
        { id:1, iniciales:'MT', texto:'M. Torres (Admin) eliminó permanentemente invoice_fake.pdf.exe', tiempo:'Hoy, 09:42 AM', ip:'192.168.1.45', tipo:'delete', tipoLabel:'Eliminación' },
        { id:2, iniciales:'SJ', texto:'Sarah Jenkins (Analista) subió suspicious_macro.docm para análisis', tiempo:'Hoy, 08:15 AM', ip:'192.168.1.112', tipo:'upload', tipoLabel:'Subida' },
        { id:3, iniciales:'CW', texto:'Chen Wei (Auditor) descargó Q3_Incident_Report.pdf', tiempo:'Ayer, 16:30 PM', ip:'10.0.0.5', tipo:'download', tipoLabel:'Descarga' },
      ],
      archivosPeligrosos: [
        { id:1, nombre:'win32_trojan.exe', tipo:'Troyano / Malware', riesgo:'Alto', size:'2.4 MB', hash:'8f4e2...', icon:'bi bi-bug-fill', iconClass:'df-icon-red' },
        { id:2, nombre:'urgent_invoice_update.html', tipo:'Phishing / Sospechoso', riesgo:'Medio', size:'45 KB', hash:'a1b2c...', icon:'bi bi-file-earmark-code', iconClass:'df-icon-orange' },
        { id:3, nombre:'Q4_Payroll_Export.csv', tipo:'Fuga de Información / Sensible', riesgo:'Alto', size:'12.8 MB', hash:'d4e5f...', icon:'bi bi-file-earmark-spreadsheet', iconClass:'df-icon-blue' },
      ],
    }
  },
  computed: {
    iniciales() {
      return (this.usuario.nombre || 'SJ').split(' ').map(n => n[0]).join('').substring(0,2).toUpperCase()
    }
  }
}
</script>

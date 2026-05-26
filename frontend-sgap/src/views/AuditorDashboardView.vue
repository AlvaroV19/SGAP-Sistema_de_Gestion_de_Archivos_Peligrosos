<template>
  <div id="app">
    <!-- SIDEBAR -->
    <Sidebar :usuario="usuario" :vista-actual="vistaActual" avatarColor="#0f766e" @cerrar-sesion="$emit('cerrar-sesion')" @navegar="$emit('navegar',$event)"></Sidebar>

    <!-- MAIN -->
    <div class="sgap-main">
      <!-- TOPBAR -->
      <header class="sgap-topbar">
        <div class="topbar-search" style="margin-left:0;max-width:300px;">
          <i class="bi bi-search search-icon"></i>
          <input type="text" placeholder="Buscar registros de auditoría, reportes o usuarios..." />
        </div>
        <div style="margin-left:auto;display:flex;align-items:center;gap:10px;">
          <div class="status-dot"></div>
          <div class="topbar-user">
            <div class="topbar-user-info">
              <div class="topbar-user-name">{{ usuario.nombre }}</div>
              <div class="topbar-user-role">{{ usuario.rol }}</div>
            </div>
            <div class="topbar-avatar" style="background:#0f766e;">EM</div>
          </div>
        </div>
      </header>

      <!-- CONTENT -->
      <main class="sgap-content">
        <!-- Role banner -->
        <div class="role-banner">
          <div class="role-banner-left">
            <div class="role-banner-tag">Panel específico por rol: Vista de Auditor</div>
            <p class="role-banner-desc">Acceso de solo lectura para monitorear la actividad de archivos, revisar rastros de auditoría y garantizar el cumplimiento de las políticas de seguridad.</p>
          </div>
          <div class="role-switch-btns">
            <button class="role-switch" @click="$emit('cerrar-sesion')">Vista Admin</button>
            <button class="role-switch active">Auditor</button>
          </div>
        </div>

        <!-- Page header -->
        <div style="margin-bottom:20px;">
          <div class="role-dash-label">Panel por rol</div>
          <div class="role-dash-header-row">
            <div>
              <h1 class="page-title" style="font-size:28px;letter-spacing:-0.5px;">Centro de Cumplimiento y Auditoría</h1>
              <p style="color:var(--text-secondary);font-size:13.5px;margin:4px 0 0;">Monitorea la actividad del sistema, traza acciones sobre archivos, revisa incidentes de seguridad y mantiene supervisión completa sin alterar datos.</p>
            </div>
            <div style="display:flex;gap:10px;flex-shrink:0;">
              <button class="btn-ghost"><i class="bi bi-file-earmark-text"></i>Generar reporte de incidentes</button>
              <button class="btn-primary-sgap"><i class="bi bi-download"></i>Exportar rastro de auditoría</button>
            </div>
          </div>
        </div>

        <!-- STATS -->
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-label">Acciones de archivos rastreadas (30d)<i class="bi bi-activity stat-icon" style="color:var(--primary)"></i></div>
            <div class="stat-value">12,450</div>
            <div class="stat-sub" style="color:var(--text-muted)">142 eliminaciones registradas</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">Actividades sospechosas registradas<i class="bi bi-exclamation-octagon stat-icon" style="color:#dc2626"></i></div>
            <div class="stat-value" style="color:#dc2626">34</div>
            <div class="stat-sub negative">9 intentos de acceso no autorizado</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">Reportes de incidentes presentados<i class="bi bi-clipboard2-x stat-icon" style="color:#d97706"></i></div>
            <div class="stat-value" style="color:#d97706">12</div>
            <div class="stat-sub" style="color:#d97706">2 requieren firma del auditor</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">Usuarios activos del sistema<i class="bi bi-people stat-icon" style="color:#16a34a"></i></div>
            <div class="stat-value">27</div>
            <div class="stat-sub positive">Actividad monitoreada en todos los roles</div>
          </div>
        </div>

        <!-- TWO-COL -->
        <div class="dash-two-col">
          <!-- LEFT - Traceability table -->
          <div class="dash-main-col" style="flex:1.4;">
            <div class="section-card">
              <div class="section-card-header">
                <h2>Actividad del Sistema y Trazabilidad de Archivos</h2>
                <div class="header-actions">
                  <button class="role-filter active-filter">Todos los eventos</button>
                  <button class="role-filter">Acciones de alto riesgo</button>
                </div>
              </div>
              <div style="overflow-x:auto;">
                <table class="sgap-table">
                  <thead>
                    <tr>
                      <th>Marca de tiempo</th>
                      <th>Acción</th>
                      <th>Recurso</th>
                      <th>Usuario responsable</th>
                      <th>Riesgo</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="e in eventos" :key="e.id">
                      <td style="color:var(--text-muted);font-size:12.5px;white-space:nowrap;">{{ e.timestamp }}</td>
                      <td>
                        <span class="action-label" :class="e.accionClass">{{ e.accion }}</span>
                      </td>
                      <td>
                        <div class="file-cell">
                          <div class="file-icon default" style="width:28px;height:28px;font-size:13px;flex-shrink:0;"><i class="bi bi-file-earmark"></i></div>
                          <div>
                            <div class="file-name" style="font-size:13px;">{{ e.recurso }}</div>
                            <div class="file-hash">{{ e.descripcion }}</div>
                          </div>
                        </div>
                      </td>
                      <td style="color:var(--text-secondary);font-size:13px;">{{ e.usuario }}</td>
                      <td>
                        <span v-if="e.riesgo !== 'Info'" class="risk-badge" :class="e.riesgo === 'Alto' ? 'high' : 'medium'">{{ e.riesgo }}</span>
                        <span v-else style="color:var(--text-muted);font-size:12px;">Info</span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <!-- RIGHT - Compliance alerts -->
          <div class="dash-side-col" style="flex:0.9;">
            <div class="section-card" style="height:100%;">
              <div class="section-card-header">
                <h2>Alertas de Cumplimiento y Seguridad</h2>
              </div>
              <div class="oversight-feed">
                <div v-for="a in alertasCumplimiento" :key="a.id" class="feed-item">
                  <div class="feed-dot" :class="a.dotClass"></div>
                  <div class="feed-body">
                    <div class="feed-text">{{ a.texto }}</div>
                    <div class="feed-meta">
                      <span>{{ a.tiempo }}</span>
                      <span v-if="a.tag" class="feed-tag">{{ a.tag }}</span>
                    </div>
                  </div>
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
      eventos: [
        { id:1, timestamp:'Hoy, 10:42 AM', accion:'Eliminación Permanente', accionClass:'action-delete', recurso:'invoice_oct_2024.pdf.exe', descripcion:'SHA256 • purga manual', usuario:'Marcus Hale (Admin)', riesgo:'Alto' },
        { id:2, timestamp:'Hoy, 09:15 AM', accion:'Descarga Segura', accionClass:'action-download', recurso:'sys_update_v2.dll', descripcion:'Firma troyano • transferencia sandbox', usuario:'Sarah Chen (Analista)', riesgo:'Alto' },
        { id:3, timestamp:'Ayer, 16:30', accion:'Subida al Registro', accionClass:'action-upload', recurso:'payroll_q4_export.xlsx', descripcion:'Documento sensible • auto-captura', usuario:'Servicio del Sistema', riesgo:'Medio' },
        { id:4, timestamp:'Ayer, 14:10', accion:'Generación de Reporte', accionClass:'action-info', recurso:'user_activity_q3.pdf', descripcion:'Exportación de cumplimiento programada', usuario:'Elena Mora (Auditora)', riesgo:'Info' },
      ],
      alertasCumplimiento: [
        { id:1, dotClass:'dot-yellow', texto:'Acceso no autorizado: un usuario estándar intentó descargar un ejecutable en cuarentena. Acceso denegado y registrado.', tiempo:'Hace 12 min', tag:'Disparado por ID usuario 4092' },
        { id:2, dotClass:'dot-blue', texto:'Reporte generado: resumen mensual de incidentes de seguridad compilado exitosamente.', tiempo:'Hace 2 horas', tag:'Automatizado por el sistema' },
        { id:3, dotClass:'dot-red', texto:'Alerta de malware: 3 nuevas firmas de troyano detectadas en adjuntos de correo entrantes. Archivos aislados.', tiempo:'Hoy', tag:'3 archivos en cuarentena' },
        { id:4, dotClass:'dot-yellow', texto:'Actividad sospechosa: múltiples intentos fallidos de autenticación en el portal de administración.', tiempo:'Ayer', tag:'IP de origen marcada' },
      ]
    }
  }
}
</script>

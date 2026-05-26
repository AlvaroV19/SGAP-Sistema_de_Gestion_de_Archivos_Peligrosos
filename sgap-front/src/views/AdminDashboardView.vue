<template>
  <div id="app">
    <!-- SIDEBAR -->
    <Sidebar :usuario="usuario" :vista-actual="vistaActual" avatarColor="#7c3aed" @cerrar-sesion="$emit('cerrar-sesion')" @navegar="$emit('navegar',$event)"></Sidebar>

    <!-- MAIN -->
    <div class="sgap-main">
      <!-- TOPBAR -->
      <header class="sgap-topbar">
        <div class="topbar-search" style="margin-left:0;max-width:300px;">
          <i class="bi bi-search search-icon"></i>
          <input type="text" placeholder="Buscar archivos, usuarios, alertas, reportes..." />
        </div>
        <div style="margin-left:auto;display:flex;align-items:center;gap:10px;">
          <div class="status-dot"></div>
          <div class="topbar-user">
            <div class="topbar-user-info">
              <div class="topbar-user-name">{{ usuario.nombre }}</div>
              <div class="topbar-user-role">{{ usuario.rol }}</div>
            </div>
            <div class="topbar-avatar" style="background:#7c3aed;">MH</div>
          </div>
        </div>
      </header>

      <!-- CONTENT -->
      <main class="sgap-content">
        <!-- Role banner -->
        <div class="role-banner">
          <div class="role-banner-left">
            <div class="role-banner-tag">Panel específico por rol: Vista de Administrador</div>
            <p class="role-banner-desc">Control total sobre archivos peligrosos, acceso de usuarios, historial de auditoría y reportes de seguridad.</p>
          </div>
          <div class="role-switch-btns">
            <button class="role-switch active">Admin</button>
            <button class="role-switch" @click="$emit('cerrar-sesion')">Vista de Auditor en pantalla 4</button>
          </div>
        </div>

        <!-- Page header -->
        <div style="margin-bottom:20px;">
          <div class="role-dash-label">Panel por rol</div>
          <div class="role-dash-header-row">
            <div>
              <h1 class="page-title" style="font-size:28px;letter-spacing:-0.5px;">Centro de Control del Administrador</h1>
              <p style="color:var(--text-secondary);font-size:13.5px;margin:4px 0 0;">Supervisa archivos de alto riesgo, gestiona permisos, revisa actividad sospechosa y actúa sobre alertas críticas desde un solo espacio de trabajo.</p>
            </div>
            <div style="display:flex;gap:10px;flex-shrink:0;">
              <button class="btn-ghost"><i class="bi bi-download"></i>Exportar reporte</button>
              <button class="btn-primary-sgap" style="background:#fff;color:var(--text-primary);border:1px solid var(--border);">
                <i class="bi bi-person-plus"></i>Crear usuario
              </button>
              <button class="btn-primary-sgap"><i class="bi bi-file-earmark-plus"></i>Registrar archivo</button>
            </div>
          </div>
        </div>

        <!-- STATS -->
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-label">Archivos maliciosos críticos<i class="bi bi-shield-exclamation stat-icon" style="color:#dc2626"></i></div>
            <div class="stat-value">42</div>
            <div class="stat-sub negative">7 requieren aprobación de eliminación hoy</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">Alertas de acceso no autorizado<i class="bi bi-exclamation-triangle stat-icon" style="color:#d97706"></i></div>
            <div class="stat-value">9</div>
            <div class="stat-sub" style="color:var(--text-muted)">Mayoría desde descargas de documentos restringidos</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">Usuarios por rol<i class="bi bi-people stat-icon" style="color:var(--primary)"></i></div>
            <div class="stat-value">27</div>
            <div class="stat-sub" style="color:var(--text-muted)">3 admins • 16 analistas • 8 auditores</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">Hallazgos de auditoría abiertos<i class="bi bi-clipboard-check stat-icon" style="color:#16a34a"></i></div>
            <div class="stat-value">11</div>
            <div class="stat-sub negative">2 seguimientos vencidos sin asignar</div>
          </div>
        </div>

        <!-- TWO-COL -->
        <div class="dash-two-col">
          <!-- LEFT - Admin queue -->
          <div class="dash-main-col" style="flex:1.4;">
            <div class="section-card">
              <div class="section-card-header">
                <div>
                  <h2>Cola admin: acciones de alta prioridad</h2>
                </div>
                <div class="header-actions">
                  <button class="role-filter active-filter">Todas las amenazas</button>
                  <button class="role-filter">Alto riesgo</button>
                </div>
              </div>
              <div style="overflow-x:auto;">
                <table class="sgap-table">
                  <thead>
                    <tr>
                      <th>Archivo</th>
                      <th>Amenaza</th>
                      <th>Riesgo</th>
                      <th>Responsable</th>
                      <th>Acción requerida</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="f in colaAdmin" :key="f.id">
                      <td>
                        <div class="file-cell">
                          <div class="file-icon default"><i class="bi bi-file-earmark"></i></div>
                          <div>
                            <div class="file-name">{{ f.nombre }}</div>
                            <div class="file-hash">{{ f.descripcion }}</div>
                          </div>
                        </div>
                      </td>
                      <td style="color:var(--text-secondary);font-size:13px;">{{ f.amenaza }}</td>
                      <td><span class="risk-badge" :class="f.riesgo === 'High' ? 'high' : 'medium'">{{ f.riesgo === 'High' ? 'Alto' : 'Medio' }}</span></td>
                      <td style="color:var(--text-secondary);font-size:13px;">{{ f.responsable }}</td>
                      <td style="color:var(--text-secondary);font-size:12.5px;">{{ f.accion }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <!-- RIGHT - Oversight feed -->
          <div class="dash-side-col" style="flex:0.9;">
            <div class="section-card" style="height:100%;">
              <div class="section-card-header">
                <h2>Feed de supervisión admin</h2>
                <button class="btn-ghost" style="padding:4px 8px;"><i class="bi bi-sliders"></i></button>
              </div>
              <div class="oversight-feed">
                <div v-for="e in feedItems" :key="e.id" class="feed-item">
                  <div class="feed-dot" :class="e.dotClass"></div>
                  <div class="feed-body">
                    <div class="feed-text">{{ e.texto }}</div>
                    <div class="feed-meta">
                      <span>{{ e.tiempo }}</span>
                      <span v-if="e.tag" class="feed-tag">{{ e.tag }}</span>
                      <span v-if="e.usuario" style="color:var(--text-muted)">Usuario: {{ e.usuario }}</span>
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
      colaAdmin: [
        { id:1, nombre:'invoice_oct_2024.pdf.exe', descripcion:'SHA256 • subido por auto-captura', amenaza:'Ransomware', riesgo:'High', responsable:'Sistema', accion:'Aprobar eliminación permanente' },
        { id:2, nombre:'payroll_q4_export.xlsx', descripcion:'Documento sensible • metadatos extraídos...', amenaza:'Fuga de información', riesgo:'Medium', responsable:'Dept. Finanzas', accion:'Restringir permisos de descarga' },
        { id:3, nombre:'urgent_reset_notice.eml', descripcion:'Muestra de phishing • denunciante verificado', amenaza:'Phishing', riesgo:'Medium', responsable:'Equipo Analistas', accion:'Escalar a reporte de incidentes' },
        { id:4, nombre:'sys_update_v2.dll', descripcion:'Familia malware • firma troyano...', amenaza:'Malware', riesgo:'High', responsable:'Endpoint 08', accion:'Asignar responsable de respuesta' },
      ],
      feedItems: [
        { id:1, dotClass:'dot-red', texto:'Acceso no autorizado: la cuenta de Auditor intentó descargar un ejecutable en cuarentena fuera del alcance aprobado.', tiempo:'Hace 12 min', tag:'Descarga bloqueada', usuario:'audit.mora' },
        { id:2, dotClass:'dot-yellow', texto:'Cambio de rol pendiente: 2 analistas solicitaron permisos elevados de eliminación para el flujo de limpieza de malware.', tiempo:'Hace 48 min', tag:'Requiere aprobación', usuario:null },
        { id:3, dotClass:'dot-blue', texto:'Reporte de auditoría generado: reporte semanal de archivos peligrosos exportado y compartido con el área de cumplimiento.', tiempo:'Hoy', tag:null, usuario:'Generado por Marcus Hale' },
        { id:4, dotClass:'dot-red', texto:'Solicitud de eliminación crítica: 7 archivos en cuarentena esperan confirmación del administrador antes de su eliminación definitiva.', tiempo:'Hoy', tag:'La ventana de retención termina en 4 horas', usuario:null },
      ]
    }
  }
}
</script>

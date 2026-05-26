<template>
  <div class="sgap-modal-overlay" v-if="show" @click.self="$emit('close')">
    <div class="sgap-modal">
      <div class="modal-header">
        <h3>
          <i class="bi bi-file-earmark-text" style="color:var(--primary);margin-right:8px;"></i>
          Detalle del Archivo
        </h3>
        <button class="modal-close" @click="$emit('close')"><i class="bi bi-x-lg"></i></button>
      </div>

      <div class="modal-body" v-if="archivo">
        <div class="d-flex align-items-start gap-3 mb-3">
          <div class="file-icon" :class="iconClase"><i :class="iconoArchivo"></i></div>
          <div class="flex-grow-1 w-100">
            <div class="fw-bold">{{ nombre }}</div>
            <div class="small text-secondary font-monospace text-break w-100 d-block">{{ hashLargo }}</div>
          </div>
        </div>

        <div style="display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px;">
          <div class="detail-item">
            <div class="detail-label">Nivel de Riesgo</div>
            <div><span class="risk-badge" :class="badgeClase">{{ riesgoLabel }}</span></div>
          </div>
          <div class="detail-item">
            <div class="detail-label">Estado</div>
            <div class="detail-value">{{ estadoLabel }}</div>
          </div>
          <div class="detail-item">
            <div class="detail-label">MIME Detectado (Tika)</div>
            <div class="detail-value font-monospace" style="font-size:12px;">{{ archivo.mimeDetectado || '—' }}</div>
          </div>
          <div class="detail-item">
            <div class="detail-label">Extensión Declarada</div>
            <div class="detail-value font-monospace">{{ archivo.extensionDeclarada || '—' }}</div>
          </div>
          <div class="detail-item">
            <div class="detail-label">Tamaño</div>
            <div class="detail-value">{{ formatSize(archivo.tamano) }}</div>
          </div>
          <div class="detail-item">
            <div class="detail-label">Fecha de Registro</div>
            <div class="detail-value">{{ fecha }}</div>
          </div>
          <div class="detail-item" style="grid-column:1 / -1;" v-if="archivo.flagsSeguridad">
            <div class="detail-label">Flags de Seguridad</div>
            <div style="display:flex;flex-wrap:wrap;gap:6px;margin-top:4px;">
              <span
                v-for="flag in flags"
                :key="flag"
                class="flag-tag"
              >{{ flag }}</span>
            </div>
          </div>
          <div class="detail-item" style="grid-column:1 / -1;" v-if="archivo.descripcionAnalisis">
            <div class="detail-label">Descripción del Análisis</div>
            <div class="detail-value" style="font-size:12.5px;line-height:1.5;">{{ archivo.descripcionAnalisis }}</div>
          </div>
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn-secondary-sgap" @click="$emit('close')">Cerrar</button>
        <!-- Botón de descarga de reporte PDF -->
        <button
          class="btn-primary-sgap"
          @click="descargarReporte"
          :disabled="descargando"
          style="display:flex;align-items:center;gap:6px;"
        >
          <span v-if="descargando"><span class="sgap-spinner"></span> Generando...</span>
          <span v-else><i class="bi bi-file-earmark-pdf"></i> Descargar Reporte PDF</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { descargarReporteArchivo, descargarBlob } from '../../services/api.js'

export default {
  name: 'FileDetailModal',
  emits: ['close'],
  props: {
    show:    { type: Boolean, default: false },
    archivo: { type: Object,  default: null  },
  },
  data() {
    return { descargando: false }
  },
  computed: {
    nombre()   { return this.archivo?.nombre || '—' },
    hashLargo(){ return this.archivo?.hash   || 'Sin hash' },
    riesgo()   { return (this.archivo?.nivelRiesgo || '').toUpperCase() },
    badgeClase() {
      if (this.riesgo === 'HIGH'   || this.riesgo === 'ALTO')  return 'high'
      if (this.riesgo === 'MEDIUM' || this.riesgo === 'MEDIO') return 'medium'
      return 'low'
    },
    riesgoLabel() {
      if (this.riesgo === 'HIGH'   || this.riesgo === 'ALTO')  return 'Alta Criticidad'
      if (this.riesgo === 'MEDIUM' || this.riesgo === 'MEDIO') return 'Riesgo Medio'
      return 'Riesgo Bajo'
    },
    iconClase() {
      if (this.riesgo === 'HIGH'   || this.riesgo === 'ALTO')  return 'malware'
      if (this.riesgo === 'MEDIUM' || this.riesgo === 'MEDIO') return 'sensitive'
      return 'default'
    },
    iconoArchivo() {
      const n = (this.archivo?.nombre || '').toLowerCase()
      if (n.endsWith('.exe') || n.endsWith('.dll')) return 'bi bi-file-earmark-binary'
      if (n.endsWith('.pdf'))                       return 'bi bi-file-earmark-pdf'
      if (n.endsWith('.csv') || n.endsWith('.xlsx'))return 'bi bi-file-earmark-spreadsheet'
      if (n.endsWith('.docx')|| n.endsWith('.doc')) return 'bi bi-file-earmark-word'
      if (n.endsWith('.html')|| n.endsWith('.htm')) return 'bi bi-file-earmark-code'
      return 'bi bi-file-earmark'
    },
    estadoLabel() {
      const map = { PENDING:'Pendiente', PROCESSING:'Procesando', SAFE:'Seguro', QUARANTINED:'En cuarentena', UNSAFE:'Inseguro', FAILED:'Fallido' }
      return map[this.archivo?.estado] || this.archivo?.estado || '—'
    },
    flags() {
      const f = this.archivo?.flagsSeguridad
      if (!f) return []
      return f.split(',').map(s => s.trim()).filter(Boolean)
    },
    fecha() {
      const f = this.archivo?.fechaSubida
      if (!f) return '—'
      const d = new Date(f)
      return d.toLocaleDateString('es-ES', { day:'2-digit', month:'short', year:'numeric' })
           + ', ' + d.toLocaleTimeString('es-ES', { hour:'2-digit', minute:'2-digit' })
    },
  },
  methods: {
    formatSize(b) {
      if (!b) return '—'
      if (b < 1024)    return `${b} B`
      if (b < 1048576) return `${(b / 1024).toFixed(1)} KB`
      return `${(b / 1048576).toFixed(1)} MB`
    },
    async descargarReporte() {
      if (!this.archivo?.id) return
      this.descargando = true
      try {
        const blob = await descargarReporteArchivo(this.archivo.id)
        const nombre = `reporte_sgap_${this.archivo.id}_${this.archivo.nombre || 'archivo'}.pdf`
        descargarBlob(blob, nombre)
      } catch (e) {
        alert('No se pudo generar el reporte PDF. Intenta nuevamente.')
      } finally {
        this.descargando = false
      }
    }
  }
}
</script>

<style scoped>
.detail-item {
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}
.detail-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 4px;
}
.detail-value {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 600;
}
.flag-tag {
  display: inline-block;
  padding: 2px 8px;
  background: #fee2e2;
  border: 1px solid #fca5a5;
  border-radius: 4px;
  color: #dc2626;
  font-size: 11px;
  font-weight: 600;
  font-family: monospace;
}
</style>

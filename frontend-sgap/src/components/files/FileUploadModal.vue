<template>
  <div class="sgap-modal-overlay" v-if="show" @click.self="$emit('close')">
    <div class="sgap-modal">
      <div class="modal-header">
        <h3><i class="bi bi-shield-exclamation" style="color:var(--primary);margin-right:8px;"></i>Registrar Archivo Sospechoso</h3>
        <button class="modal-close" @click="$emit('close')"><i class="bi bi-x-lg"></i></button>
      </div>

      <div class="modal-body">
        <label class="form-label-sgap">Archivo</label>
        <div class="drop-zone"
          :class="{ dragging: arrastrando }"
          @dragover.prevent="$emit('dragging-change', true)"
          @dragleave="$emit('dragging-change', false)"
          @drop.prevent="$emit('drop', $event)">
          <input type="file" @change="$emit('file-change', $event)" />
          <div v-if="!archivo">
            <div class="dz-icon"><i class="bi bi-cloud-upload"></i></div>
            <div class="dz-text"><strong>Haz clic para seleccionar</strong> o arrastra aquí<br><span style="color:var(--text-muted);font-size:12px;">Cualquier tipo de archivo</span></div>
          </div>
          <div v-else>
            <div class="dz-icon" style="color:var(--primary)"><i class="bi bi-file-earmark-check"></i></div>
            <div class="dz-selected">{{ archivo.name }}</div>
            <div class="dz-text" style="margin-top:4px;font-size:11.5px;">{{ formatSize(archivo.size) }}</div>
          </div>
        </div>

        <div style="margin-top:18px;">
          <label class="form-label-sgap">Nivel de Riesgo</label>
          <select class="sgap-select" :value="nivelRiesgo" @change="$emit('update:nivelRiesgo', $event.target.value)">
            <option value="ALTO">🔴 Alto — Riesgo Crítico</option>
            <option value="MEDIO">🟡 Medio — Revisión Requerida</option>
            <option value="BAJO">🟢 Bajo — Monitoreo Estándar</option>
          </select>
        </div>

        <div
          v-if="nivelRiesgo"
          style="margin-top:12px;padding:10px 14px;border-radius:8px;"
          :style="nivelRiesgo === 'ALTO' ? 'background:#fee2e2;border:1px solid #fca5a5;' :
                  nivelRiesgo === 'MEDIO' ? 'background:#fef9c3;border:1px solid #fde68a;' :
                  'background:#dcfce7;border:1px solid #86efac;'" >
          <div
            style="font-size:12.5px;font-weight:600;"
            :style="nivelRiesgo === 'ALTO' ? 'color:#dc2626;' : nivelRiesgo === 'MEDIO' ? 'color:#b45309;' : 'color:#16a34a;'" >
            <i class="bi" :class="nivelRiesgo === 'ALTO' ? 'bi-exclamation-octagon-fill' : nivelRiesgo === 'MEDIO' ? 'bi-exclamation-triangle-fill' : 'bi-check-circle-fill'"></i>
            {{ nivelRiesgo === 'ALTO' ? 'Este archivo será marcado para análisis inmediato' : nivelRiesgo === 'MEDIO' ? 'Este archivo entrará a la cola de revisión estándar' : 'Monitoreo de rutina aplicado' }}
          </div>
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn-secondary-sgap" @click="$emit('close')">Cancelar</button>
        <button class="btn-primary-sgap" @click="$emit('upload')" :disabled="!archivo || subiendo">
          <span v-if="subiendo"><span class="sgap-spinner"></span> Subiendo...</span>
          <span v-else><i class="bi bi-upload"></i> Registrar Archivo</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'FileUploadModal',
  emits: ['close', 'file-change', 'drop', 'dragging-change', 'upload', 'update:nivelRiesgo'],
  props: {
    show: { type: Boolean, default: false },
    archivo: { type: Object, default: null },
    arrastrando: { type: Boolean, default: false },
    nivelRiesgo: { type: String, default: 'MEDIO' },
    subiendo: { type: Boolean, default: false },
  },
  methods: {
    formatSize(b) {
      if (!b) return ''
      if (b < 1024) return `${b} B`
      if (b < 1048576) return `${(b / 1024).toFixed(1)} KB`
      return `${(b / 1048576).toFixed(1)} MB`
    },
  },
}
</script>

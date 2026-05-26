<template>
  <div id="app">
  <!-- ========== SIDEBAR ========== -->
  <Sidebar :usuario="usuario" :vista-actual="vistaActual" @cerrar-sesion="$emit('cerrar-sesion')" @navegar="$emit('navegar',$event)"></Sidebar>

  <!-- ========== MAIN ========== -->
  <div class="sgap-main">

    <!-- TOPBAR -->
    <header class="sgap-topbar">
      <div class="topbar-breadcrumb">
        <span class="crumb" @click="$emit('navegar','security')" style="cursor:pointer">Panel</span>
        <span class="sep"><i class="bi bi-chevron-right"></i></span>
        <span class="crumb active">Repositorio de Archivos Peligrosos</span>
      </div>

      <div class="topbar-search position-relative" ref="searchBox">
        <i class="bi bi-search search-icon"></i>
        <input v-model="busqueda" @input="onSearchInput" @focus="onSearchFocus" type="text" placeholder="Buscar hash, nombre de archivo o amenaza..." />

        <div class="dropdown-menu" :class="{show: showDropdown}" style="width:100%;max-height:300px;overflow:auto;z-index:2500;">
          <button v-for="r in dropdownResults" :key="r.id" class="dropdown-item" @click.prevent="selectDropdownItem(r)">
            <div style="display:flex;justify-content:space-between;align-items:center;gap:10px;">
              <div style="flex:1;text-align:left;">
                <strong>{{ r.nombre || r.name || 'archivo_desconocido' }}</strong>
                <div class="text-muted" style="font-size:12px;">{{ truncarHash(r.hash) }}</div>
              </div>
              <div style="font-size:12px;color:var(--text-secondary);white-space:nowrap;">{{ (r.nivelRiesgo||r.riskLevel||'').toUpperCase() }}</div>
            </div>
          </button>

          <div v-if="dropdownResults.length" class="dropdown-divider"></div>
          <button v-if="dropdownResults.length" class="dropdown-item" @click.prevent="applySearchAll">Mostrar todos los resultados ({{ dropdownResults.length }})</button>
          <div v-else class="dropdown-item text-muted">Sin resultados</div>
        </div>
      </div>

      
      <button class="topbar-upload-btn" @click="mostrarModal = true">
        <i class="bi bi-upload"></i>Registrar Archivo
      </button>
    </header>

    <!-- CONTENT -->
    <main class="sgap-content">

      <div class="page-header">
        <h1 class="page-title">Repositorio de Archivos</h1>
        <button class="topbar-upload-btn" @click="mostrarModal = true">
          <i class="bi bi-plus-lg"></i>Registrar Sospechoso
        </button>
      </div>

      <!-- STATS GRID -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-label">Total de Archivos Registrados<i class="bi bi-file-earmark-text stat-icon"></i></div>
          <div class="stat-value">{{ archivos.length }}</div>
          <div class="stat-sub positive"><i class="bi bi-arrow-up"></i> +{{ archivosHoy }} subidos hoy</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Riesgo Crítico (Malware)<i class="bi bi-fire stat-icon" style="color:#dc2626"></i></div>
          <div class="stat-value" style="color:#dc2626">{{ archivosAlto }}</div>
          <div class="stat-sub negative">Requiere acción inmediata</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Riesgo Medio<i class="bi bi-exclamation-triangle stat-icon" style="color:#b45309"></i></div>
          <div class="stat-value" style="color:#b45309">{{ archivosMedio }}</div>
          <div class="stat-sub">Pendientes de revisión</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Estado de Cuarentena<i class="bi bi-shield-check stat-icon" style="color:#16a34a"></i></div>
          <div class="stat-value" style="color:#16a34a">{{ archivosBajo }}</div>
          <div class="stat-sub positive">Riesgo bajo — bajo control</div>
        </div>
      </div>

      <!-- FILE TABLE -->
      <div class="section-card">
        <div class="section-card-header">
          <h2>Envíos Recientes de Archivos</h2>
          <div class="header-actions">
            <button class="btn-ghost"><i class="bi bi-funnel"></i>Filtrar</button>
            <button class="btn-ghost" @click="cargar"><i class="bi bi-arrow-clockwise"></i>Actualizar</button>
          </div>
        </div>

        <div v-if="cargando" class="empty-state">
          <i class="bi bi-hourglass-split"></i>
          <p>Cargando archivos...</p>
        </div>

        <div v-else-if="archivosFiltrados.length === 0" class="empty-state">
          <i class="bi bi-folder2-open"></i>
          <p v-if="busqueda">No se encontraron archivos para "{{ busqueda }}"</p>
          <p v-else>No hay archivos registrados. <a href="#" @click.prevent="mostrarModal = true" style="color:var(--primary)">Registra el primero.</a></p>
        </div>

        <div v-else style="overflow-x: auto;">
          <table class="sgap-table">
            <thead>
              <tr>
                <th>Archivo / Hash</th>
                <th>Clasificación</th>
                <th>Tipo de Amenaza</th>
                <th>Nivel de Riesgo</th>
                <th>Subido Por</th>
                <th>Fecha</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="a in archivosFiltrados" :key="a.id">
                <td>
                  <div class="file-cell">
                    <div class="file-icon" :class="iconClase(a)"><i :class="iconArchivo(a)"></i></div>
                    <div>
                      <div class="file-name">{{ a.nombre || a.name || 'archivo_desconocido' }}</div>
                      <div class="file-hash">{{ truncarHash(a.hash) }}</div>
                    </div>
                  </div>
                </td>
                <td style="color:var(--text-secondary);font-size:13px;">{{ clasificacion(a) }}</td>
                <td style="color:var(--text-secondary);font-size:13px;">{{ tipoAmenaza(a) }}</td>
                <td><span class="risk-badge" :class="badgeClase(a.nivelRiesgo || a.riskLevel)">{{ labelRiesgo(a.nivelRiesgo || a.riskLevel) }}</span></td>
                <td style="color:var(--text-secondary);font-size:13px;">{{ a.subidoPor || a.uploadedBy || 'Sistema' }}</td>
                <td style="color:var(--text-muted);font-size:12.5px;white-space:nowrap;">{{ formatFecha(a.fecha || a.createdAt) }}</td>
                <td>
                  <div style="display:flex;gap:6px;">
                    <button class="btn-ghost" style="padding:4px 10px;font-size:12px;"><i class="bi bi-eye"></i></button>
                    <button class="btn-ghost" style="padding:4px 10px;font-size:12px;color:#dc2626;border-color:#fca5a5;" @click="eliminar(a.id)"><i class="bi bi-trash3"></i></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </main>
  </div>

  <!-- ========== MODAL ========== -->
  <div class="sgap-modal-overlay" v-if="mostrarModal" @click.self="mostrarModal = false">
    <div class="sgap-modal">
      <div class="modal-header">
        <h3><i class="bi bi-shield-exclamation" style="color:var(--primary);margin-right:8px;"></i>Registrar Archivo Sospechoso</h3>
        <button class="modal-close" @click="mostrarModal = false"><i class="bi bi-x-lg"></i></button>
      </div>

      <div class="modal-body">
        <label class="form-label-sgap">Archivo</label>
        <div class="drop-zone" :class="{ dragging: arrastrando }"
          @dragover.prevent="arrastrando = true"
          @dragleave="arrastrando = false"
          @drop.prevent="onDrop">
          <input type="file" @change="handleFile" />
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
          <select class="sgap-select" v-model="nivelRiesgo">
            <option value="ALTO">🔴 Alto — Riesgo Crítico</option>
            <option value="MEDIO">🟡 Medio — Revisión Requerida</option>
            <option value="BAJO">🟢 Bajo — Monitoreo Estándar</option>
          </select>
        </div>

        <div v-if="nivelRiesgo" style="margin-top:12px;padding:10px 14px;border-radius:8px;"
          :style="nivelRiesgo === 'ALTO' ? 'background:#fee2e2;border:1px solid #fca5a5;' :
                  nivelRiesgo === 'MEDIO' ? 'background:#fef9c3;border:1px solid #fde68a;' :
                  'background:#dcfce7;border:1px solid #86efac;'">
          <div style="font-size:12.5px;font-weight:600;"
            :style="nivelRiesgo === 'ALTO' ? 'color:#dc2626;' : nivelRiesgo === 'MEDIO' ? 'color:#b45309;' : 'color:#16a34a;'">
            <i class="bi" :class="nivelRiesgo === 'ALTO' ? 'bi-exclamation-octagon-fill' : nivelRiesgo === 'MEDIO' ? 'bi-exclamation-triangle-fill' : 'bi-check-circle-fill'"></i>
            {{ nivelRiesgo === 'ALTO' ? 'Este archivo será marcado para análisis inmediato' : nivelRiesgo === 'MEDIO' ? 'Este archivo entrará a la cola de revisión estándar' : 'Monitoreo de rutina aplicado' }}
          </div>
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn-secondary-sgap" @click="mostrarModal = false">Cancelar</button>
        <button class="btn-primary-sgap" @click="subir" :disabled="!archivo || subiendo">
          <span v-if="subiendo"><span class="sgap-spinner"></span> Subiendo...</span>
          <span v-else><i class="bi bi-upload"></i> Registrar Archivo</span>
        </button>
      </div>
    </div>
  </div>

  <!-- TOASTS -->
  <div class="toast-container">
    <div v-for="t in toasts" :key="t.id" class="sgap-toast" :class="t.tipo">
      <i class="bi" :class="t.tipo === 'success' ? 'bi-check-circle-fill' : 'bi-x-circle-fill'"></i>
      {{ t.mensaje }}
    </div>
  </div>
  </div>
</template>

<script>
import { subirArchivo, obtenerArchivos } from "../services/api";
import Sidebar from './Sidebar.vue'

export default {
  components: { Sidebar },
  props: ['usuario', 'vistaActual'],
  emits: ['navegar', 'cerrar-sesion'],
  data() {
    return {
      archivo: null,
      nivelRiesgo: "MEDIO",
      archivos: [],
      mostrarModal: false,
      arrastrando: false,
      subiendo: false,
      cargando: false,
      busqueda: "",
      // búsqueda - dropdown
      showDropdown: false,
      dropdownResults: [],
      aplicarFiltro: false,
      toasts: [],
    };
  },
  computed: {
    iniciales() {
      const n = (this.usuario && this.usuario.nombre) || 'SJ'
      return n.split(' ').map(x => x[0]).join('').substring(0,2).toUpperCase()
    },
    archivosFiltrados() {
      // Only apply the filter to the main table when the user explicitly requests it
      if (!this.aplicarFiltro || !this.busqueda.trim()) return this.archivos;
      const q = this.busqueda.toLowerCase();
      return this.archivos.filter(a =>
        (a.nombre || a.name || "").toLowerCase().includes(q) ||
        (a.hash || "").toLowerCase().includes(q) ||
        (a.nivelRiesgo || a.riskLevel || "").toLowerCase().includes(q)
      );
    },
    archivosAlto() { return this.archivos.filter(a => (a.nivelRiesgo||a.riskLevel||"").toUpperCase() === "ALTO").length },
    archivosMedio() { return this.archivos.filter(a => (a.nivelRiesgo||a.riskLevel||"").toUpperCase() === "MEDIO").length },
    archivosBajo() { return this.archivos.filter(a => (a.nivelRiesgo||a.riskLevel||"").toUpperCase() === "BAJO").length },
    archivosHoy() {
      const hoy = new Date().toDateString();
      return this.archivos.filter(a => { const f = a.fecha||a.createdAt; return f && new Date(f).toDateString() === hoy }).length;
    }
  },
  methods: {
    handleFile(e) { this.archivo = e.target.files[0] || null },
    onDrop(e) { this.arrastrando = false; this.archivo = e.dataTransfer.files[0] || null },

    // --- Search dropdown handlers ---
    onSearchInput() {
      if (!this.busqueda.trim()) {
        this.showDropdown = false;
        this.dropdownResults = [];
        this.aplicarFiltro = false;
        return;
      }
      this.updateDropdownResults();
      this.showDropdown = true;
      this.aplicarFiltro = false;
    },
    onSearchFocus() {
      if (this.busqueda.trim()) {
        this.updateDropdownResults();
        this.showDropdown = true;
      }
    },
    updateDropdownResults() {
      const q = (this.busqueda || '').toLowerCase();
      this.dropdownResults = this.archivos.filter(a =>
        (a.nombre || a.name || '').toLowerCase().includes(q) ||
        (a.hash || '').toLowerCase().includes(q) ||
        (a.nivelRiesgo || a.riskLevel || '').toLowerCase().includes(q)
      ).slice(0,8);
    },
    selectDropdownItem(item) {
      this.busqueda = item.nombre || item.name || '';
      this.showDropdown = false;
      this.aplicarFiltro = true;
    },
    applySearchAll() {
      if (!this.busqueda.trim()) return;
      this.aplicarFiltro = true;
      this.showDropdown = false;
    },
    onDocumentClick(e) {
      const box = this.$refs.searchBox;
      if (!box) return;
      if (!box.contains(e.target)) this.showDropdown = false;
    },
    async subir() {
      if (!this.archivo) return;
      this.subiendo = true;
      try {
        const formData = new FormData();
        formData.append("file", this.archivo);
        formData.append("nivelRiesgo", this.nivelRiesgo);
        await subirArchivo(formData);
        this.mostrarModal = false;
        this.archivo = null;
        await this.cargar();
        this.toast("Archivo registrado exitosamente", "success");
      } catch (e) {
        this.toast("Error al registrar el archivo", "error");
      } finally {
        this.subiendo = false;
      }
    },
    async cargar() {
      this.cargando = true;
      try {
        const res = await obtenerArchivos();
        this.archivos = res.data || [];
      } catch (e) {
        this.archivos = this.demoData();
      } finally {
        this.cargando = false;
      }
    },
    async eliminar(id) {
      if (!confirm("¿Eliminar este archivo del registro?")) return;
      this.archivos = this.archivos.filter(a => a.id !== id);
      this.toast("Archivo eliminado del registro", "success");
    },
    demoData() {
      return [
        { id:1, nombre:"invoice_oct_2024.pdf.exe", hash:"e3b0c44298fc1c14...a8", nivelRiesgo:"ALTO", subidoPor:"Sistema Auto-Captura", fecha:new Date().toISOString() },
        { id:2, nombre:"customer_export_db.csv", hash:"8f4e3c2b1a9d8e7f...c3", nivelRiesgo:"MEDIO", subidoPor:"J. Doe (Auditor)", fecha:new Date(Date.now()-86400000).toISOString() },
        { id:3, nombre:"sys_update_v2.dll", hash:"5d41402abc4b2a76...d9", nivelRiesgo:"ALTO", subidoPor:"S. Jenkins (Analista)", fecha:new Date(Date.now()-172800000).toISOString() },
        { id:4, nombre:"quarterly_report_q3.docx", hash:"2cf24dba5fb0a30e...8f", nivelRiesgo:"BAJO", subidoPor:"M. Torres", fecha:new Date(Date.now()-259200000).toISOString() },
        { id:5, nombre:"urgent_invoice_update.html", hash:"a1b2c3d4e5f67890...1a", nivelRiesgo:"MEDIO", subidoPor:"Sistema Auto-Captura", fecha:new Date(Date.now()-345600000).toISOString() },
      ];
    },
    truncarHash(h) { if (!h) return "—"; return h.length > 20 ? h.substring(0,20)+"..." : h },
    formatFecha(f) {
      if (!f) return "—";
      const d = new Date(f);
      return d.toLocaleDateString("es-ES",{day:"2-digit",month:"short"}) + ", " + d.toLocaleTimeString("es-ES",{hour:"2-digit",minute:"2-digit"});
    },
    formatSize(b) {
      if (!b) return "";
      if (b < 1024) return b+" B";
      if (b < 1048576) return (b/1024).toFixed(1)+" KB";
      return (b/1048576).toFixed(1)+" MB";
    },
    badgeClase(n) { const v=(n||"").toUpperCase(); if(v==="ALTO")return"high"; if(v==="MEDIO")return"medium"; return"low" },
    labelRiesgo(n) { const v=(n||"").toUpperCase(); if(v==="ALTO")return"Alta Criticidad"; if(v==="MEDIO")return"Riesgo Medio"; return"Riesgo Bajo" },
    iconClase(a) { const n=(a.nivelRiesgo||a.riskLevel||"").toUpperCase(); if(n==="ALTO")return"malware"; if(n==="MEDIO")return"sensitive"; return"default" },
    iconArchivo(a) {
      const n=(a.nombre||a.name||"").toLowerCase();
      if(n.endsWith(".exe")||n.endsWith(".dll"))return"bi bi-file-earmark-binary";
      if(n.endsWith(".pdf"))return"bi bi-file-earmark-pdf";
      if(n.endsWith(".csv")||n.endsWith(".xlsx"))return"bi bi-file-earmark-spreadsheet";
      if(n.endsWith(".docx")||n.endsWith(".doc"))return"bi bi-file-earmark-word";
      if(n.endsWith(".html")||n.endsWith(".htm"))return"bi bi-file-earmark-code";
      return"bi bi-file-earmark";
    },
    clasificacion(a) { const n=(a.nivelRiesgo||a.riskLevel||"").toUpperCase(); if(n==="ALTO")return"Malware"; if(n==="MEDIO")return"Sensible"; return"Estándar" },
    tipoAmenaza(a) {
      const n=(a.nombre||a.name||"").toLowerCase();
      if(n.includes(".exe")||n.includes(".dll"))return"Ransomware / Troyano";
      if(n.includes(".html"))return"Phishing / Sospechoso";
      if(n.includes(".csv")||n.includes(".xlsx"))return"Filtración de Datos";
      return"Desconocido";
    },
    toast(m, tipo="success") {
      const id=Date.now(); this.toasts.push({id,mensaje:m,tipo});
      setTimeout(()=>{this.toasts=this.toasts.filter(t=>t.id!==id)},3500);
    }
  },
  mounted() { this.cargar(); document.addEventListener('click', this.onDocumentClick); },
  beforeUnmount() { document.removeEventListener('click', this.onDocumentClick); }
};
</script>

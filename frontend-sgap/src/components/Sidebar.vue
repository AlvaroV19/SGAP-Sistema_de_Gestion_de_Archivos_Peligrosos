<template>
    <aside class="sgap-sidebar d-flex flex-column vh-100 p-3 bg-dark text-white">
        <!-- BRAND -->
        <div class="d-flex align-items-center mb-2">
            <i class="bi bi-shield-fill fs-4 me-2"></i>
            <span class="fs-5 fw-semibold">SGAP</span>
        </div>

        <!-- NAV -->
        <nav class="nav flex-column">
            <div v-for="section in navSections" :key="section.label" class="mb-2">
                <div class="text-uppercase small mt-3 mb-1 text-ligth opacity-50">{{ section.label }}</div>

                <a
                    v-for="item in section.items"
                    :key="item.key || item.label"
                    href="#"
                    class="nav-link d-flex justify-content-between align-items-center"
                    :class="[ isActive(item) ? 'bg-light text-dark rounded' : 'text-white' ]"
                    @click.prevent="onNav(item)">
                    <div class="d-flex align-items-center">
                        <i :class="item.icon + ' me-2'"></i>
                        <span>{{ item.label }}</span>
                    </div>
                    <span v-if="item.badge" class="badge bg-danger ms-2">{{ item.badge }}</span>
                </a>
            </div>
        </nav>

        <!-- USER -->
        <div class="mt-auto d-flex align-items-center pt-3 border-top">
            <div class="me-2">
                <div class="rounded-circle d-flex align-items-center justify-content-center text-white"
                    :style="{ backgroundColor: avatarColor, width: '44px', height: '44px' }">{{ initials }}</div>
            </div>

            <div class="flex-grow-1">
                <div class="fw-semibold">{{ usuario?.nombre }}</div>
                <div class="small text-ligth opacity-75">{{ usuario?.rol }}</div>
            </div>

            <button class="btn btn-outline-light btn-sm ms-2" @click="$emit('cerrar-sesion')" title="Cerrar sesión">
                <i class="bi bi-box-arrow-right"></i>
            </button>
        </div>
    </aside>
</template>

<script>
export default {
    name: 'Sidebar',

    props: {
        usuario: {
            type: Object,
            default: () => ({ nombre: '', rol: '' })
        },
        avatarColor: {
            type: String,
            default: '#7c3aed'
        },
        vistaActual: {
            type: String,
            default: ''
        }
    },

    emits: ['navegar', 'cerrar-sesion'],

    computed: {
        initials() {
            const n = (this.usuario && this.usuario.nombre) || ''
            return n
                .split(' ')
                .map(x => x[0] || '')
                .join('')
                .substring(0, 2)
                .toUpperCase()
        },

        roleKey() {
            const r = (this.usuario && this.usuario.rol) || ''
            const s = String(r).toLowerCase()

            if (s.includes('admin')) return 'admin'
            if (s.includes('audit')) return 'auditor'
            if (s.includes('segur') || s.includes('security') || s.includes('analist')) return 'security'

            return 'default'
        },

        navSections() {
            const admin = [
                {
                    label: 'Resumen',
                    items: [{ label: 'Panel Administrador', icon: 'bi bi-speedometer2', key: 'admin' }]
                },
                {
                    label: 'Gestión de Archivos Peligrosos',
                    items: [
                        { label: 'Repositorio de Archivos', icon: 'bi bi-folder2-open', key: 'repositorio', emit: 'repositorio' },
                        { label: 'Análisis de Metadatos', icon: 'bi bi-cpu', key: 'metadatos', emit: 'metadatos' },
                        { label: 'Alertas', icon: 'bi bi-bell', key: 'alertas', emit: 'alertas', badge: 12 }
                    ]
                },
                {
                    label: 'Administración',
                    items: [
                        { label: 'Usuarios y Roles', icon: 'bi bi-people', key: 'usuarios', emit: 'usuarios' },
                        { label: 'Registros de Auditoría', icon: 'bi bi-journal-text', key: 'auditoria', emit: 'auditoria' },
                        { label: 'Reportes', icon: 'bi bi-bar-chart-line', key: 'reportes', emit: 'reportes' }
                    ]
                }
            ]

            const auditor = [
                {
                    label: 'Resumen',
                    items: [{ label: 'Panel Auditor', icon: 'bi bi-clipboard2-check', key: 'auditor' }]
                },
                {
                    label: 'Monitoreo y Trazabilidad',
                    items: [
                        { label: 'Registros de Actividad', icon: 'bi bi-activity', key: 'registros', emit: 'registros' },
                        { label: 'Historial de Auditoría', icon: 'bi bi-clock-history', key: 'historial', emit: 'historial' },
                        { label: 'Alertas', icon: 'bi bi-bell', key: 'alertas', emit: 'alertas', badge: 4 }
                    ]
                },
                {
                    label: 'Reportes',
                    items: [
                        { label: 'Incidentes de Seguridad', icon: 'bi bi-exclamation-circle', key: 'incidentes', emit: 'incidentes' },
                        { label: 'Archivos Peligrosos', icon: 'bi bi-folder2-open', key: 'repositorio', emit: 'repositorio' },
                        { label: 'Actividad de Usuarios', icon: 'bi bi-person-lines-fill', key: 'actividad', emit: 'actividad' }
                    ]
                }
            ]

            const security = [
                {
                    label: 'Resumen',
                    items: [{ label: 'Panel Principal', icon: 'bi bi-grid-1x2', key: 'security' }]
                },
                {
                    label: 'Gestión de Archivos',
                    items: [
                        { label: 'Repositorio de Archivos', icon: 'bi bi-folder2-open', key: 'repositorio', emit: 'repositorio' },
                        { label: 'Cuarentena', icon: 'bi bi-box-seam', key: 'cuarentena', emit: 'cuarentena' }
                    ]
                },
                {
                    label: 'Riesgo y Seguridad',
                    items: [
                        { label: 'Inteligencia de Amenazas', icon: 'bi bi-crosshair', key: 'inteligencia', emit: 'inteligencia' },
                        { label: 'Evaluación de Impacto', icon: 'bi bi-graph-up-arrow', key: 'evaluacion', emit: 'evaluacion' }
                    ]
                },
                {
                    label: 'Sistema',
                    items: [
                        { label: 'Usuarios y Roles', icon: 'bi bi-people', key: 'usuarios', emit: 'usuarios' },
                        { label: 'Registros de Auditoría', icon: 'bi bi-journal-text', key: 'auditoria', emit: 'auditoria' },
                        { label: 'Reportes', icon: 'bi bi-bar-chart-line', key: 'reportes', emit: 'reportes' }
                    ]
                }
            ]

            if (this.roleKey === 'admin') return admin
            if (this.roleKey === 'auditor') return auditor
            if (this.roleKey === 'security') return security

            return security
        }
    },

    methods: {
        isActive(item) {
            if (!item || !item.key) return false
            return String(this.vistaActual) === String(item.key)
        },

        onNav(item) {
            if (!item) return

            if (item.emit) return this.$emit('navegar', item.emit)
            if (item.key) return this.$emit('navegar', item.key)

            this.$emit('navegar', item.label.toLowerCase())
        }
    }
}
</script>
<template>
  <div class="toast-container" v-if="toasts.length">
    <div v-for="t in toasts" :key="t.id" class="sgap-toast" :class="t.tipo">
      <i class="bi" :class="t.tipo === 'success' ? 'bi-check-circle-fill' : 'bi-x-circle-fill'"></i>
      {{ t.mensaje }}
    </div>
  </div>
</template>

<script>
export default {
  name: 'ToastStack',
  props: {
    toasts: {
      type: Array,
      default: () => [],
    },
  },
}
</script>

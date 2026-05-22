<script setup lang="ts">
defineProps<{
  destinations: string[];
  interests: string[];
  selectedInterests: string[];
}>();

const emit = defineEmits<{
  (e: 'select-destination', value: string): void;
  (e: 'toggle-interest', value: string): void;
}>();
</script>

<template>
  <aside class="planner-panel planner-side-panel">
    <div class="planner-side-block">
      <span class="planner-side-eyebrow">推荐方向</span>
      <h3>本周热门目的地</h3>
      <div class="planner-chip-grid">
        <button
          v-for="item in destinations"
          :key="item"
          class="planner-chip"
          type="button"
          @click="emit('select-destination', item)"
        >
          {{ item }}
        </button>
      </div>
    </div>

    <div class="planner-side-block">
      <span class="planner-side-eyebrow">旅行偏好</span>
      <h3>先挑一个这次最重要的旅行心情</h3>
      <div class="planner-chip-grid">
        <button
          v-for="item in interests"
          :key="item"
          :class="['planner-chip', { 'planner-chip--active': selectedInterests.includes(item) }]"
          type="button"
          @click="emit('toggle-interest', item)"
        >
          {{ item }}
        </button>
      </div>
    </div>
  </aside>
</template>

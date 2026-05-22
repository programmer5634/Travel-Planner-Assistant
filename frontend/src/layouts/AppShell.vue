<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const displayName = computed(() => authStore.user?.nickname || authStore.user?.username || '旅行者');
const isDetailPage = computed(() => /^\/itineraries\/\d+$/.test(route.path));

async function handleLogout() {
  await authStore.logout();
  await router.push('/login');
}
</script>

<template>
  <div :class="['shell-layout', { 'shell-layout--detail': isDetailPage }]">
    <header v-if="!isDetailPage" class="shell-header">
      <div>
        <p class="shell-kicker">NIGHT FLIGHT ATLAS</p>
        <h1 class="shell-title">旅行智能助手</h1>
      </div>
      <nav class="shell-nav">
        <RouterLink to="/planner">规划录入</RouterLink>
        <RouterLink to="/history">历史行程</RouterLink>
      </nav>
      <div class="shell-userbox">
        <span class="shell-userlabel">{{ displayName }}</span>
        <button class="shell-ghost" type="button" @click="handleLogout">退出</button>
      </div>
    </header>
    <main class="shell-main">
      <RouterView />
    </main>
  </div>
</template>

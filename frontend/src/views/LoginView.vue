<script setup lang="ts">
import { reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const authStore = useAuthStore();
authStore.error = '';

const form = reactive({
  username: '',
  password: ''
});

async function submit() {
  try {
    await authStore.login({ ...form });
    await router.push('/');
  } catch {
  }
}
</script>

<template>
  <main class="auth-layout auth-layout--login">
    <section class="auth-story">
      <p class="auth-kicker">NIGHT FLIGHT ATLAS</p>
      <h1 class="auth-title">从一张登机牌，开始下一段旅程。</h1>
      <p class="auth-copy">
        把目的地、预算、节奏与兴趣交给系统，它会替你生成一份可以保存、继续修改、随时回看的中文行程方案。
      </p>

      <div class="auth-metrics">
        <article class="auth-metric">
          <span class="auth-metric-label">目的地偏好</span>
          <strong>城市 / 风格 / 节奏</strong>
          <p>把轻松度、酒店气质和兴趣标签合并成一条真正可执行的旅行路线。</p>
        </article>
        <article class="auth-metric">
          <span class="auth-metric-label">行程版本</span>
          <strong>保存并继续改稿</strong>
          <p>不是一次性输出，而是像旅行编辑室一样持续打磨你的计划。</p>
        </article>
        <article class="auth-metric">
          <span class="auth-metric-label">登录态恢复</span>
          <strong>刷新后继续工作</strong>
          <p>重新打开页面也能回到你的旅行账本，不会丢掉刚刚整理好的灵感。</p>
        </article>
      </div>

      <div class="auth-route-strip" aria-hidden="true">
        <span>上海 → 杭州</span>
        <span>成都 → 稻城</span>
        <span>西安 → 华清宫</span>
        <span>桂林 → 阳朔</span>
      </div>
    </section>

    <section class="auth-panel">
      <p class="auth-eyebrow">MEMBER ACCESS</p>
      <div class="auth-ticket-meta">
        <span>夜航会籍入口</span>
        <span>CN · 2026</span>
      </div>
      <h2 class="auth-heading">登录旅游助手</h2>
      <p class="auth-subtitle">登录后才能创建、保存并继续编辑你的专属行程。</p>

      <form class="auth-form" @submit.prevent="submit">
        <label class="auth-field">
          <span class="auth-label">用户名</span>
          <input v-model="form.username" class="auth-input" autocomplete="username" placeholder="输入你的用户名" />
        </label>
        <label class="auth-field">
          <span class="auth-label">密码</span>
          <input v-model="form.password" class="auth-input" type="password" autocomplete="current-password" placeholder="输入你的登录密码" />
        </label>
        <button :disabled="authStore.loading" class="auth-submit" type="submit">
          {{ authStore.loading ? '正在核验你的航线…' : '进入我的行程账本' }}
        </button>
      </form>

      <p v-if="authStore.error" class="auth-error">{{ authStore.error }}</p>
      <p class="auth-switch">还没有账号？<RouterLink to="/register">立即创建旅行身份</RouterLink></p>
    </section>
  </main>
</template>

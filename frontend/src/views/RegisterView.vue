<script setup lang="ts">
import { reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const authStore = useAuthStore();
authStore.error = '';

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
});

async function submit() {
  if (form.password !== form.confirmPassword) {
    authStore.error = '两次输入的密码不一致';
    return;
  }
  try {
    await authStore.register({ ...form });
    await router.push('/login');
  } catch {
  }
}
</script>

<template>
  <main class="auth-layout auth-layout--register">
    <section class="auth-story">
      <p class="auth-kicker">TRAVEL LEDGER</p>
      <h1 class="auth-title">先领取旅行身份，再开启自动规划。</h1>
      <p class="auth-copy">
        注册后，你的行程不再是一次性的临时草稿，而会变成一份可保存、可迭代、可继续打磨的私人旅行档案。
      </p>

      <div class="auth-metrics">
        <article class="auth-metric">
          <span class="auth-metric-label">专属昵称</span>
          <strong>欢迎语与你的风格同步</strong>
          <p>进入主页后会显示你的称呼，让这套工具真正属于你的旅行日常。</p>
        </article>
        <article class="auth-metric">
          <span class="auth-metric-label">行程留档</span>
          <strong>生成方案后继续改</strong>
          <p>从第一版灵感到最终出发版本，所有修改都能围绕同一份计划延展。</p>
        </article>
        <article class="auth-metric">
          <span class="auth-metric-label">受保护入口</span>
          <strong>先登录再进入主页</strong>
          <p>主页、接口与会话都已经接入鉴权，能更稳定地保存你的旅行上下文。</p>
        </article>
      </div>

      <div class="auth-route-strip" aria-hidden="true">
        <span>自然风光</span>
        <span>美食打卡</span>
        <span>市区便利</span>
        <span>经典平衡</span>
      </div>
    </section>

    <section class="auth-panel">
      <p class="auth-eyebrow">CREATE PROFILE</p>
      <div class="auth-ticket-meta">
        <span>建立你的旅行档案</span>
        <span>ID · NEW</span>
      </div>
      <h2 class="auth-heading">注册账号</h2>
      <p class="auth-subtitle">创建账号后即可保存与管理你的行程规划。</p>

      <ul class="auth-benefits">
        <li>保存行程版本并继续修改</li>
        <li>刷新页面后恢复登录状态</li>
        <li>统一管理你的规划入口</li>
      </ul>

      <form class="auth-form" @submit.prevent="submit">
        <label class="auth-field">
          <span class="auth-label">用户名</span>
          <input v-model="form.username" class="auth-input" autocomplete="username" placeholder="建议使用 3-50 位用户名" />
        </label>
        <label class="auth-field">
          <span class="auth-label">昵称</span>
          <input v-model="form.nickname" class="auth-input" autocomplete="nickname" placeholder="例如：阿青、露营局长" />
        </label>
        <label class="auth-field">
          <span class="auth-label">密码</span>
          <input v-model="form.password" class="auth-input" type="password" autocomplete="new-password" placeholder="至少 6 位" />
        </label>
        <label class="auth-field">
          <span class="auth-label">确认密码</span>
          <input v-model="form.confirmPassword" class="auth-input" type="password" autocomplete="new-password" placeholder="再次输入密码" />
        </label>
        <button :disabled="authStore.loading" class="auth-submit" type="submit">
          {{ authStore.loading ? '正在生成你的旅行身份…' : '创建并前往登录' }}
        </button>
      </form>

      <p v-if="authStore.error" class="auth-error">{{ authStore.error }}</p>
      <p class="auth-switch">已经有账号？<RouterLink to="/login">返回登录入口</RouterLink></p>
    </section>
  </main>
</template>

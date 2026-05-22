<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AmapRouteMap from '../components/map/AmapRouteMap.vue';
import TripSummaryStats from '../components/result/TripSummaryStats.vue';
import DayTimeline from '../components/result/DayTimeline.vue';
import BudgetSummaryCard from '../components/result/BudgetSummaryCard.vue';
import WeatherInfoCard from '../components/result/WeatherInfoCard.vue';
import { useItineraryStore } from '../stores/itinerary';

const route = useRoute();
const router = useRouter();
const itineraryStore = useItineraryStore();
const reviseForm = reactive({ message: '' });

const itineraryId = computed(() => Number(route.params.id));
const activeDay = ref<number | null>(null);
const activePointKey = ref('');
const activeSection = ref('overview');

const itinerary = computed(() => itineraryStore.itinerary);
const dayPlans = computed(() => itinerary.value?.plan.itinerary ?? []);
const summary = computed(() => itinerary.value?.plan.summary);
const weatherList = computed(() => itinerary.value?.plan.weather ?? []);
const hotelList = computed(() => itinerary.value?.plan.hotels ?? []);
const allMapPoints = computed(() => itinerary.value?.plan.mapPoints ?? []);
const activeDayPlan = computed(() => dayPlans.value.find((day) => day.day === activeDay.value) ?? null);

const navItems = [
  { key: 'overview', label: '行程概览', icon: '📋' },
  { key: 'budget', label: '预算明细', icon: '💰' },
  { key: 'map', label: '景点地图', icon: '🗺️' },
  { key: 'daily', label: '每日行程', icon: '📅' },
  { key: 'weather', label: '天气信息', icon: '🌡️' },
];

function pointKey(point: { day: number; sequence: number; name: string }) {
  return `${point.day}-${point.sequence}-${point.name}`;
}

function selectDay(day: number) {
  activeDay.value = day;
  activePointKey.value = '';
}

function selectPoint(day: number, key: string) {
  activeDay.value = day;
  activePointKey.value = key;
}

function scrollToSection(key: string) {
  activeSection.value = key;
  const el = document.getElementById(`section-${key}`);
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}

function startOver() {
  if (!itinerary.value) return;
  router.push({ path: '/planner', query: { from: String(itinerary.value.itineraryId) } });
}

watch(
  itinerary,
  (current) => {
    if (!current) {
      activeDay.value = null;
      activePointKey.value = '';
      return;
    }
    const firstDay = current.plan.itinerary[0]?.day ?? null;
    activeDay.value = firstDay;
    activePointKey.value = '';
  },
  { immediate: true }
);

onMounted(async () => {
  if (Number.isFinite(itineraryId.value)) {
    await itineraryStore.fetch(itineraryId.value);
  }
});

async function submitRevision() {
  if (!reviseForm.message.trim()) return;
  await itineraryStore.revise(itineraryId.value, { message: reviseForm.message.trim() });
  reviseForm.message = '';
}
</script>

<template>
  <div class="detail-layout">
    <!-- 左侧固定导航栏 -->
    <aside class="detail-sidebar">
      <nav class="sidebar-nav">
        <button
          v-for="item in navItems"
          :key="item.key"
          :class="['sidebar-nav__item', { 'sidebar-nav__item--active': activeSection === item.key }]"
          type="button"
          @click="scrollToSection(item.key)"
        >
          <span class="sidebar-nav__icon">{{ item.icon }}</span>
          <span class="sidebar-nav__label">{{ item.label }}</span>
          <span v-if="item.key === 'daily'" class="sidebar-nav__arrow">›</span>
        </button>
      </nav>
    </aside>

    <!-- 右侧主内容区 -->
    <div class="detail-main">
      <!-- 顶部导航栏 -->
      <header class="detail-topbar">
        <button class="topbar-back" type="button" @click="router.push('/')">
          ← 返回首页
        </button>
        <div class="topbar-actions">
          <button
            v-if="itineraryStore.itinerary"
            :class="['topbar-btn', { 'topbar-btn--active': itineraryStore.itinerary.favorite }]"
            :disabled="itineraryStore.togglingFavorite"
            type="button"
            @click="itineraryStore.toggleFavorite(itineraryStore.itinerary.itineraryId, !itineraryStore.itinerary.favorite)"
          >
            {{ itineraryStore.itinerary.favorite ? '★ 已收藏' : '☆ 收藏' }}
          </button>
          <button v-if="itineraryStore.itinerary" class="topbar-btn" type="button" @click="startOver">
            ✏️ 编辑行程
          </button>
          <RouterLink
            v-if="itineraryStore.itinerary"
            :to="`/itineraries/${itineraryStore.itinerary.itineraryId}/print`"
            class="topbar-btn topbar-btn--link"
          >
            📤 导出行程
          </RouterLink>
        </div>
      </header>

      <!-- 加载 / 错误状态 -->
      <div v-if="itineraryStore.error" class="page-error">{{ itineraryStore.error }}</div>
      <div v-if="itineraryStore.loading" class="page-loading">正在加载行程详情…</div>

      <!-- 内容区域 -->
      <div v-else-if="itineraryStore.itinerary" class="detail-content">
        <!-- 行程概览 -->
        <section id="section-overview" class="content-section">
          <div class="overview-grid">
            <!-- 左上：行程概览卡片 -->
            <article class="overview-card">
              <div class="overview-card__header">
                <p class="overview-card__kicker">ITINERARY DETAIL</p>
                <h2 class="overview-card__title">{{ itineraryStore.itinerary.plan.title || '行程详情' }}</h2>
              </div>
              <div class="overview-card__meta">
                <span>版本 {{ itineraryStore.itinerary.revisionNo }}</span>
                <span>{{ itineraryStore.itinerary.departureCity }} → {{ itineraryStore.itinerary.destination }}</span>
                <span>{{ itineraryStore.itinerary.startDate }} 至 {{ itineraryStore.itinerary.endDate }}</span>
              </div>
              <p class="overview-card__copy">{{ itineraryStore.itinerary.plan.overview }}</p>
              <TripSummaryStats v-if="summary" :summary="summary" />
            </article>

            <!-- 左下：预算明细 -->
            <div id="section-budget">
              <BudgetSummaryCard
                :budget="itineraryStore.itinerary.plan.budget"
              />
            </div>

            <!-- 右侧：景点地图 -->
            <div id="section-map" class="overview-map">
              <AmapRouteMap
                :points="allMapPoints"
                :route-days="itineraryStore.itinerary.plan.routeDays ?? []"
                :active-day="activeDay"
                :active-point-key="activePointKey"
                @select-point="selectPoint($event.day, $event.key)"
              />
            </div>
          </div>
        </section>

        <!-- 每日行程（通栏） -->
        <section id="section-daily" class="content-section">
          <div class="daily-card">
            <div class="daily-card__header">
              <span class="daily-card__icon">📅</span>
              <h3>每日行程</h3>
            </div>
            <DayTimeline
              :days="dayPlans"
              :active-day="activeDay"
              :hotel-list="hotelList"
              :map-points="allMapPoints"
              @select-day="selectDay"
            />
          </div>
        </section>

        <!-- 天气信息（通栏） -->
        <section id="section-weather" class="content-section">
          <WeatherInfoCard :weather-list="weatherList" />
        </section>

        <!-- 修改行程表单 -->
        <section v-if="itineraryStore.itinerary" class="content-section revise-section">
          <article class="revise-card">
            <div class="revise-card__header">
              <span class="revise-card__icon">💬</span>
              <h3>行程调整</h3>
            </div>
            <p class="revise-card__hint">对行程不满意？输入调整建议，AI 将重新规划。</p>
            <textarea
              v-model="reviseForm.message"
              rows="3"
              placeholder="例如：第二天下午想改得更轻松一些。"
            />
            <button
              class="revise-submit"
              :disabled="itineraryStore.revising || !reviseForm.message.trim()"
              @click="submitRevision"
            >
              {{ itineraryStore.revising ? '正在调整…' : '提交调整' }}
            </button>
          </article>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== 整体布局 ===== */
.detail-layout {
  display: flex;
  min-height: 100vh;
  gap: 0;
}

/* ===== 左侧导航栏 ===== */
.detail-sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 220px;
  padding: 24px 14px;
  background: #fff;
  border-right: 1px solid #e8ecf2;
  box-shadow: 2px 0 12px rgba(15, 23, 42, 0.04);
  z-index: 20;
  overflow-y: auto;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 24px;
}

.sidebar-nav__item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px 16px;
  border: none;
  border-radius: 14px;
  background: transparent;
  color: #64748b;
  font-size: 15px;
  cursor: pointer;
  text-align: left;
  transition: background 150ms, color 150ms;
}

.sidebar-nav__item:hover {
  background: #f1f5f9;
  color: #1e293b;
}

.sidebar-nav__item--active {
  background: #eff6ff;
  color: #2563eb;
  font-weight: 600;
}

.sidebar-nav__icon {
  font-size: 20px;
  flex: none;
  width: 28px;
  text-align: center;
}

.sidebar-nav__label {
  flex: 1;
}

.sidebar-nav__arrow {
  color: #94a3b8;
  font-size: 18px;
}

/* ===== 右侧主内容区 ===== */
.detail-main {
  flex: 1;
  margin-left: 220px;
  padding: 0 28px 40px;
  min-width: 0;
}

/* ===== 顶部导航栏 ===== */
.detail-topbar {
  position: sticky;
  top: 0;
  z-index: 15;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
  background: linear-gradient(180deg, #f5f7fb 0%, #f5f7fb 85%, transparent 100%);
  margin-bottom: 8px;
}

.topbar-back {
  border: 1px solid #e4ebf3;
  border-radius: 12px;
  background: #fff;
  padding: 10px 18px;
  color: #64748b;
  font-size: 14px;
  cursor: pointer;
  transition: border-color 150ms, color 150ms;
}

.topbar-back:hover {
  border-color: #2563eb;
  color: #2563eb;
}

.topbar-actions {
  display: flex;
  gap: 10px;
}

.topbar-btn {
  border: 1px solid #e4ebf3;
  border-radius: 12px;
  background: #fff;
  padding: 10px 18px;
  color: #1e293b;
  font-size: 14px;
  cursor: pointer;
  text-decoration: none;
  transition: border-color 150ms, background 150ms;
}

.topbar-btn:hover {
  border-color: #2563eb;
  background: #f8fbff;
}

.topbar-btn--active {
  border-color: #f5a623;
  background: #fef7e8;
  color: #b8860b;
}

.topbar-btn--link {
  display: inline-flex;
  align-items: center;
}

/* ===== 内容区域 ===== */
.detail-content {
  display: grid;
  gap: 24px;
}

.content-section {
  scroll-margin-top: 80px;
}

/* ===== 概览网格：左上+左下+右侧 ===== */
.overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 1.05fr);
  grid-template-rows: auto auto;
  gap: 20px;
}

.overview-card {
  padding: 28px;
  border-radius: 20px;
  border: 1px solid #e4ebf3;
  background: #fff;
  display: grid;
  gap: 16px;
}

.overview-card__kicker {
  margin: 0;
  color: #3b82f6;
  letter-spacing: 0.22em;
  font-size: 12px;
}

.overview-card__title {
  margin: 6px 0 0;
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
}

.overview-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.overview-card__meta span {
  padding: 6px 12px;
  border-radius: 999px;
  background: #f4f8fd;
  border: 1px solid #e4ebf3;
  color: #1e293b;
  font-size: 13px;
}

.overview-card__copy {
  margin: 0;
  color: #475569;
  line-height: 1.8;
  font-size: 15px;
}

.overview-map {
  grid-row: 1 / 3;
  grid-column: 2;
  min-height: 500px;
}

/* ===== 每日行程卡片 ===== */
.daily-card {
  padding: 28px;
  border-radius: 20px;
  border: 1px solid #e4ebf3;
  background: #fff;
}

.daily-card__header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.daily-card__header h3 {
  margin: 0;
  font-size: 18px;
  color: #1e293b;
}

.daily-card__icon {
  font-size: 22px;
}

/* ===== 修改行程 ===== */
.revise-section {
  margin-top: 8px;
}

.revise-card {
  padding: 24px 28px;
  border-radius: 20px;
  border: 1px solid #e4ebf3;
  background: #fff;
}

.revise-card__header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.revise-card__header h3 {
  margin: 0;
  font-size: 18px;
  color: #1e293b;
}

.revise-card__icon {
  font-size: 22px;
}

.revise-card__hint {
  margin: 8px 0 14px;
  color: #94a3b8;
  font-size: 13px;
}

.revise-card textarea {
  width: 100%;
  min-height: 80px;
  padding: 14px 16px;
  border: 1px solid #dfe5ee;
  border-radius: 14px;
  background: #fff;
  color: #1e293b;
  resize: vertical;
  font-size: 14px;
}

.revise-card textarea::placeholder {
  color: rgba(126, 147, 177, 0.72);
}

.revise-submit {
  margin-top: 12px;
  min-height: 46px;
  padding: 0 28px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  cursor: pointer;
  transition: opacity 150ms;
}

.revise-submit:disabled {
  opacity: 0.55;
  cursor: default;
}

.revise-submit:not(:disabled):hover {
  opacity: 0.9;
}

/* ===== 响应式 ===== */
@media (max-width: 1120px) {
  .detail-sidebar {
    display: none;
  }

  .detail-main {
    margin-left: 0;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }

  .overview-map {
    grid-row: auto;
    grid-column: auto;
    min-height: 400px;
  }
}

@media (max-width: 720px) {
  .detail-main {
    padding: 0 16px 32px;
  }

  .detail-topbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .topbar-actions {
    flex-wrap: wrap;
  }

  .overview-card {
    padding: 20px;
  }

  .overview-card__title {
    font-size: 22px;
  }

  .daily-card {
    padding: 20px;
  }
}
</style>

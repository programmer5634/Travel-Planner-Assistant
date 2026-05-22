<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useHistoryStore } from '../stores/history';

const router = useRouter();
const historyStore = useHistoryStore();
const filters = reactive({
  keyword: '',
  destination: '',
  favoriteOnly: false,
  startDate: '',
  endDate: ''
});

const hasRecords = computed(() => historyStore.records.length > 0);
const pageStart = computed(() => historyStore.total === 0 ? 0 : (historyStore.pageNo - 1) * historyStore.pageSize + 1);
const pageEnd = computed(() => Math.min(historyStore.pageNo * historyStore.pageSize, historyStore.total));

onMounted(async () => {
  await historyStore.fetch();
});

async function search() {
  await historyStore.fetch({ ...filters, pageNo: 1 });
}

async function remove(itineraryId: number) {
  await historyStore.remove(itineraryId);
}

function viewDetail(itineraryId: number) {
  router.push(`/itineraries/${itineraryId}`);
}

function editAgain(itineraryId: number) {
  router.push({ path: '/planner', query: { from: String(itineraryId) } });
}
</script>

<template>
  <section class="page-section history-page">
    <div class="page-hero">
      <div>
        <p class="page-kicker">TRAVEL ARCHIVE</p>
        <h2 class="page-title">历史行程</h2>
        <p class="page-copy">像翻阅旅行档案一样管理每一次方案，保留版本，也保留临出发前的每次改动。</p>
      </div>
    </div>

    <div class="history-filterbar">
      <input v-model="filters.keyword" placeholder="搜索标题或概览" />
      <input v-model="filters.destination" placeholder="目的地" />
      <input v-model="filters.startDate" type="date" />
      <input v-model="filters.endDate" type="date" />
      <label class="history-checkbox">
        <input v-model="filters.favoriteOnly" type="checkbox" /> 仅看收藏
      </label>
      <button class="page-primary-btn" @click="search">筛选</button>
    </div>

    <div v-if="historyStore.error" class="page-error">{{ historyStore.error }}</div>
    <div v-if="historyStore.loading" class="page-loading">正在加载历史行程…</div>

    <div v-else-if="hasRecords" class="history-list-wrap">
      <div class="history-list-meta">
        <span>共 {{ historyStore.total }} 条</span>
        <span>当前显示 {{ pageStart }} - {{ pageEnd }}</span>
      </div>

      <div class="history-list">
        <article v-for="item in historyStore.records" :key="item.itineraryId" class="history-card">
          <div>
            <p class="history-meta">{{ item.departureCity }} → {{ item.destination }}</p>
            <h3>{{ item.title }}</h3>
            <p>{{ item.overview }}</p>
            <div class="history-tags">
              <span>{{ item.startDate }} - {{ item.endDate }}</span>
              <span>版本 {{ item.revisionNo }}</span>
              <span v-if="item.favorite">已收藏</span>
            </div>
          </div>
          <div class="history-actions">
            <button
              :class="['page-link-btn', { 'page-link-btn--active': item.favorite }]"
              :disabled="historyStore.togglingFavoriteId === item.itineraryId"
              @click="historyStore.toggleFavorite(item.itineraryId, !item.favorite)"
            >
              {{ item.favorite ? '已收藏' : '收藏' }}
            </button>
            <button class="page-link-btn" @click="viewDetail(item.itineraryId)">查看</button>
            <button class="page-link-btn" @click="editAgain(item.itineraryId)">重新规划</button>
            <button class="page-danger-btn" :disabled="historyStore.deletingId === item.itineraryId" @click="remove(item.itineraryId)">
              {{ historyStore.deletingId === item.itineraryId ? '删除中…' : '删除' }}
            </button>
          </div>
        </article>
      </div>

      <div class="history-pagination">
        <button class="page-link-btn" :disabled="!historyStore.hasPrevPage" @click="historyStore.goToPage(historyStore.pageNo - 1)">上一页</button>
        <span>第 {{ historyStore.pageNo }} 页</span>
        <button class="page-link-btn" :disabled="!historyStore.hasNextPage" @click="historyStore.goToPage(historyStore.pageNo + 1)">下一页</button>
      </div>
    </div>

    <div v-else class="page-empty">还没有历史行程，先去创建第一份旅行方案吧。</div>
  </section>
</template>

<style scoped>
.history-list-wrap {
  display: grid;
  gap: 16px;
}

.history-list-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #64748b;
  font-size: 14px;
}

.history-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}
</style>

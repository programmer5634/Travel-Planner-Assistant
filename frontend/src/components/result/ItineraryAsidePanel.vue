<script setup lang="ts">
import type { WeatherSnapshot, HotelRecommendation, MapPoint } from '../../types/travel';

defineProps<{
  points: MapPoint[];
  activePointKey: string;
  weatherList: WeatherSnapshot[];
  hotelList: HotelRecommendation[];
  travelTips: string[];
  reviseMessage: string;
  revising: boolean;
}>();

const emit = defineEmits<{
  (e: 'select-point', day: number, key: string): void;
  (e: 'update:reviseMessage', value: string): void;
  (e: 'submit-revision'): void;
}>();

function pointKey(point: { day: number; sequence: number; name: string }) {
  return `${point.day}-${point.sequence}-${point.name}`;
}

function pointLabel(point: { day: number; sequence: number; type: string }) {
  if (point.day === 0 || point.type.includes('酒店')) {
    return '酒店落脚点';
  }
  return `第 ${point.day} 天 · 第 ${point.sequence} 站`;
}
</script>

<template>
  <div class="detail-aside-grid">
    <article class="mini-card mini-card--map-points">
      <h3>地图点位</h3>
      <p class="mini-card-copy">每个编号代表一站，酒店会以独立落脚点样式展示。</p>
      <ul class="point-list">
        <li v-for="point in points" :key="pointKey(point)">
          <button
            :class="['point-button', { 'point-button--active': activePointKey === pointKey(point) }]"
            type="button"
            @click="emit('select-point', point.day, pointKey(point))"
          >
            <strong>{{ point.name }}</strong>
            <span>{{ pointLabel(point) }} · {{ point.type }} · {{ point.district }}</span>
          </button>
        </li>
      </ul>
    </article>
    <article class="mini-card">
      <h3>天气与住宿补充</h3>
      <ul>
        <li v-for="weather in weatherList" :key="weather.date">
          {{ weather.date }} · {{ weather.condition }} · {{ weather.temperature }}
        </li>
      </ul>
      <ul class="hotel-list">
        <li v-for="hotel in hotelList" :key="hotel.name" class="hotel-item">
          <img v-if="hotel.imageUrl" :src="hotel.imageUrl" :alt="hotel.name" class="hotel-thumb" loading="lazy" />
          <div>
            <strong>{{ hotel.name }}</strong>
            <span>{{ hotel.district }} · {{ hotel.priceBand }}</span>
          </div>
        </li>
      </ul>
    </article>
    <article class="mini-card">
      <h3>游玩贴士与调整</h3>
      <ul>
        <li v-for="tip in travelTips" :key="tip">{{ tip }}</li>
      </ul>
      <textarea
        :value="reviseMessage"
        rows="4"
        placeholder="例如：第二天下午想改得更轻松一些。"
        @input="emit('update:reviseMessage', ($event.target as HTMLTextAreaElement).value)"
      />
      <button
        class="page-primary-btn"
        :disabled="revising || !reviseMessage.trim()"
        @click="emit('submit-revision')"
      >
        {{ revising ? '正在调整…' : '提交调整' }}
      </button>
    </article>
  </div>
</template>

<style scoped>
.hotel-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 10px;
}

.hotel-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hotel-thumb {
  width: 56px;
  height: 42px;
  border-radius: 8px;
  object-fit: cover;
  flex: none;
}

.hotel-item strong {
  display: block;
  font-size: 13px;
  color: #1e293b;
}

.hotel-item span {
  font-size: 12px;
  color: #64748b;
}
</style>

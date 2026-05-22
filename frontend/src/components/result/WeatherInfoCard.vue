<script setup lang="ts">
import type { WeatherSnapshot } from '../../types/travel';

defineProps<{
  weatherList: WeatherSnapshot[];
}>();

function conditionIcon(condition: string): string {
  const c = condition.toLowerCase();
  if (c.includes('晴') || c.includes('sunny') || c.includes('clear')) return '☀️';
  if (c.includes('多云') || c.includes('cloud')) return '⛅';
  if (c.includes('阴') || c.includes('overcast')) return '☁️';
  if (c.includes('雨') || c.includes('rain')) return '🌧️';
  if (c.includes('雪') || c.includes('snow')) return '❄️';
  if (c.includes('雾') || c.includes('fog') || c.includes('霾')) return '🌫️';
  if (c.includes('风') || c.includes('wind')) return '💨';
  return '🌤️';
}
</script>

<template>
  <article class="weather-card">
    <div class="weather-card__header">
      <span class="weather-card__icon">🌡️</span>
      <h3>天气信息</h3>
    </div>
    <div v-if="weatherList.length" class="weather-list">
      <div v-for="w in weatherList" :key="w.date" class="weather-row">
        <span class="weather-row__icon">{{ conditionIcon(w.condition) }}</span>
        <div class="weather-row__info">
          <div class="weather-row__top">
            <strong>{{ w.date }}</strong>
            <span class="weather-row__condition">{{ w.condition }}</span>
          </div>
          <div class="weather-row__bottom">
            <span class="weather-row__temp">{{ w.temperature }}</span>
            <span v-if="w.advice" class="weather-row__advice">{{ w.advice }}</span>
          </div>
        </div>
      </div>
    </div>
    <p v-else class="weather-empty">暂无天气数据</p>
  </article>
</template>

<style scoped>
.weather-card {
  padding: 22px 24px;
  border-radius: 20px;
  border: 1px solid #e4ebf3;
  background: #fff;
}

.weather-card__header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
}

.weather-card__header h3 {
  margin: 0;
  font-size: 18px;
  color: #1e293b;
}

.weather-card__icon {
  font-size: 22px;
}

.weather-list {
  display: grid;
  gap: 10px;
}

.weather-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid #eef2f7;
  background: #f8fbff;
}

.weather-row__icon {
  font-size: 28px;
  flex: none;
}

.weather-row__info {
  flex: 1;
  min-width: 0;
}

.weather-row__top {
  display: flex;
  align-items: center;
  gap: 10px;
}

.weather-row__top strong {
  color: #1e293b;
  font-size: 14px;
}

.weather-row__condition {
  color: #64748b;
  font-size: 13px;
}

.weather-row__bottom {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
}

.weather-row__temp {
  color: #2563eb;
  font-size: 14px;
  font-weight: 600;
}

.weather-row__advice {
  color: #94a3b8;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.weather-empty {
  margin: 0;
  color: #94a3b8;
  text-align: center;
  padding: 24px 0;
}
</style>

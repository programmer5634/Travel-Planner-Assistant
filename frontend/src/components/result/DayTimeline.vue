<script setup lang="ts">
import { computed } from 'vue';
import type { DailyPlan, HotelRecommendation, MapPoint } from '../../types/travel';

const props = defineProps<{
  days: DailyPlan[];
  activeDay: number | null;
  hotelList: HotelRecommendation[];
  mapPoints: MapPoint[];
}>();

const emit = defineEmits<{
  (e: 'select-day', day: number): void;
}>();

const hotelEntries = computed(() => props.hotelList.slice(0, 2));

function isExpanded(day: DailyPlan) {
  return props.activeDay === day.day;
}

function getDaySightEntries(day: DailyPlan) {
  const seen = new Set<string>();
  return props.mapPoints.filter((point) => {
    if (point.day !== day.day || point.type !== '景点' || seen.has(point.name)) {
      return false;
    }
    seen.add(point.name);
    return true;
  });
}
</script>

<template>
  <div>
    <div class="detail-day-tabs">
      <button
        v-for="day in days"
        :key="day.day"
        :class="['planner-chip', { 'planner-chip--active': activeDay === day.day }]"
        type="button"
        @click="emit('select-day', day.day)"
      >
        DAY {{ day.day }}
      </button>
    </div>

    <div class="timeline-list">
      <article
        v-for="day in days"
        :key="day.day"
        :class="['timeline-card', { 'timeline-card--active': activeDay === day.day }]"
      >
        <button class="timeline-card__header" type="button" @click="emit('select-day', day.day)">
          <div class="timeline-day">DAY {{ day.day }}</div>
          <div class="timeline-card__headline">
            <h3>{{ day.theme }}</h3>
            <p class="timeline-date">{{ day.date }}</p>
          </div>
          <span :class="['timeline-card__chevron', { 'timeline-card__chevron--open': isExpanded(day) }]">⌄</span>
        </button>

        <div v-if="isExpanded(day)" class="timeline-card__body">
          <section class="timeline-section">
            <div class="timeline-section__title">景点安排</div>
            <ul class="timeline-agenda-list">
              <li v-for="agenda in day.agenda" :key="agenda">{{ agenda }}</li>
            </ul>
            <div v-if="getDaySightEntries(day).length" class="timeline-sight-grid">
              <article v-for="spot in getDaySightEntries(day)" :key="spot.name" class="timeline-sight-card">
                <img v-if="spot.imageUrl" :src="spot.imageUrl" :alt="spot.name" class="timeline-sight-thumb" loading="lazy" />
                <div class="timeline-sight-copy">
                  <strong>{{ spot.name }}</strong>
                  <span>{{ spot.district }}<template v-if="spot.description"> · {{ spot.description }}</template></span>
                </div>
              </article>
            </div>
          </section>

          <section class="timeline-section">
            <div class="timeline-section__title">住宿安排</div>
            <ul v-if="hotelEntries.length" class="timeline-hotel-list">
              <li v-for="hotel in hotelEntries" :key="hotel.name" class="timeline-hotel-item">
                <img v-if="hotel.imageUrl" :src="hotel.imageUrl" :alt="hotel.name" class="timeline-hotel-thumb" loading="lazy" />
                <div>
                  <strong>{{ hotel.name }}</strong>
                  <span>{{ hotel.district }} · {{ hotel.priceBand }} · {{ hotel.highlight }}</span>
                </div>
              </li>
            </ul>
            <p v-else class="timeline-empty">当前没有可展示的住宿推荐。</p>
          </section>

          <section class="timeline-section timeline-section--inline">
            <div class="timeline-section__title">餐饮安排</div>
            <p>{{ day.foodSuggestion }}</p>
          </section>

          <section class="timeline-section timeline-section--inline">
            <div class="timeline-section__title">晚间安排</div>
            <p>{{ day.eveningSuggestion }}</p>
          </section>
        </div>
      </article>
    </div>
  </div>
</template>

<style scoped>
.timeline-list {
  display: grid;
  gap: 14px;
}

.timeline-card {
  border: 1px solid #dfe5ee;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
  overflow: hidden;
  transition: border-color 150ms ease, box-shadow 150ms ease;
}

.timeline-card--active {
  border-color: #8bbcff;
  box-shadow: 0 14px 30px rgba(59, 130, 246, 0.12);
}

.timeline-card__header {
  width: 100%;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.timeline-card__headline h3 {
  margin: 0;
  font-size: 18px;
  color: #1e293b;
}

.timeline-day {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 74px;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.timeline-date {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.timeline-card__chevron {
  color: #94a3b8;
  font-size: 22px;
  line-height: 1;
  transition: transform 150ms ease;
}

.timeline-card__chevron--open {
  transform: rotate(180deg);
}

.timeline-card__body {
  display: grid;
  gap: 14px;
  padding: 0 20px 20px;
}

.timeline-section {
  padding: 14px 16px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid #e5edf8;
}

.timeline-section--inline p,
.timeline-empty {
  margin: 0;
  color: #475569;
  line-height: 1.7;
}

.timeline-section__title {
  margin-bottom: 10px;
  color: #1e3a8a;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.timeline-agenda-list,
.timeline-hotel-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}

.timeline-agenda-list li {
  position: relative;
  padding-left: 18px;
  color: #334155;
  line-height: 1.7;
}

.timeline-agenda-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 11px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #60a5fa;
}

.timeline-sight-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.timeline-sight-card,
.timeline-hotel-item {
  display: flex;
  gap: 12px;
  align-items: center;
}

.timeline-sight-card {
  padding: 10px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #deebfb;
}

.timeline-sight-thumb,
.timeline-hotel-thumb {
  width: 64px;
  height: 48px;
  border-radius: 10px;
  object-fit: cover;
  flex: none;
}

.timeline-sight-copy strong,
.timeline-hotel-item strong {
  display: block;
  color: #1e293b;
  font-size: 14px;
}

.timeline-sight-copy span,
.timeline-hotel-item span {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 720px) {
  .timeline-card__header {
    grid-template-columns: 1fr auto;
    align-items: start;
  }

  .timeline-day {
    grid-column: 1 / -1;
    justify-self: start;
  }

  .timeline-card__body {
    padding: 0 16px 16px;
  }

  .timeline-section {
    padding: 12px 14px;
  }
}
</style>

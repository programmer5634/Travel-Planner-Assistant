<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useItineraryStore } from '../stores/itinerary';

const route = useRoute();
const itineraryStore = useItineraryStore();
const itineraryId = computed(() => Number(route.params.id));

onMounted(async () => {
  if (!itineraryStore.itinerary && Number.isFinite(itineraryId.value)) {
    await itineraryStore.fetch(itineraryId.value);
  }
});

function printPage() {
  window.print();
}
</script>

<template>
  <section class="print-page">
    <header class="print-toolbar no-print">
      <button class="page-primary-btn" type="button" @click="printPage">打印为 PDF</button>
    </header>

    <article v-if="itineraryStore.itinerary" class="print-sheet">
      <p class="print-kicker">NIGHT FLIGHT ATLAS</p>
      <h1>{{ itineraryStore.itinerary.plan.title }}</h1>
      <p class="print-range">{{ itineraryStore.itinerary.departureCity }} → {{ itineraryStore.itinerary.destination }} ｜ {{ itineraryStore.itinerary.startDate }} 至 {{ itineraryStore.itinerary.endDate }}</p>
      <p class="print-overview">{{ itineraryStore.itinerary.plan.overview }}</p>

      <section class="print-section">
        <h2>每日行程</h2>
        <article v-for="day in itineraryStore.itinerary.plan.itinerary" :key="day.day" class="print-day-card">
          <h3>DAY {{ day.day }} · {{ day.theme }}</h3>
          <p>{{ day.date }}</p>

          <div class="print-day-group">
            <h4>景点安排</h4>
            <ul>
              <li v-for="agenda in day.agenda" :key="agenda">{{ agenda }}</li>
            </ul>
          </div>

          <div class="print-day-group">
            <h4>住宿安排</h4>
            <ul>
              <li v-for="hotel in itineraryStore.itinerary.plan.hotels.slice(0, 2)" :key="hotel.name">
                {{ hotel.name }} · {{ hotel.district }} · {{ hotel.priceBand }} · {{ hotel.highlight }}
              </li>
            </ul>
          </div>

          <div class="print-day-group">
            <h4>餐饮安排</h4>
            <p>{{ day.foodSuggestion }}</p>
          </div>

          <div class="print-day-group">
            <h4>晚间安排</h4>
            <p>{{ day.eveningSuggestion }}</p>
          </div>
        </article>
      </section>

      <section class="print-section">
        <h2>天气与贴士</h2>
        <ul>
          <li v-for="weather in itineraryStore.itinerary.plan.weather" :key="weather.date">{{ weather.date }} · {{ weather.condition }} · {{ weather.temperature }} · {{ weather.advice }}</li>
        </ul>
        <ul>
          <li v-for="tip in itineraryStore.itinerary.plan.travelTips" :key="tip">{{ tip }}</li>
        </ul>
      </section>
    </article>
  </section>
</template>

<style scoped>
.print-day-group + .print-day-group {
  margin-top: 12px;
}

.print-day-group h4 {
  margin: 0 0 6px;
  font-size: 14px;
  color: #1e3a8a;
}

.print-day-group p,
.print-day-group ul {
  margin: 0;
}
</style>

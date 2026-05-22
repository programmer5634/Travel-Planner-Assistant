<script setup lang="ts">
import type { TripSummary } from '../../types/travel';

defineProps<{
  summary: TripSummary;
}>();
</script>

<template>
  <div class="summary-stats">
    <article class="summary-stat-card">
      <span>行程天数</span>
      <strong>{{ summary.days }} 天 / {{ summary.nights }} 晚</strong>
    </article>
    <article class="summary-stat-card">
      <span>预算与节奏</span>
      <strong>{{ summary.budgetLevel }} · {{ summary.pace }}</strong>
    </article>
    <article class="summary-stat-card">
      <span>住宿建议</span>
      <strong>{{ summary.stayArea }}</strong>
    </article>
    <article class="summary-stat-card">
      <span>交通建议</span>
      <strong>{{ summary.transportAdvice }}</strong>
    </article>
  </div>
</template>

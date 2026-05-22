<script setup lang="ts">
import { computed } from 'vue';
import type { BudgetSummary } from '../../types/travel';

const props = defineProps<{
  budget?: BudgetSummary;
}>();

const basisLabel: Record<string, string> = {
  MIXED: '基于高德路线估算',
  RULE_ESTIMATED: '规则估算',
  REAL_AMAP_INPUT: '高德真实输入'
};

const estimateLabel: Record<string, string> = {
  MEDIUM: '可信度中',
  LOW: '可信度低'
};

const items = computed(() => props.budget?.items ?? []);
const notes = computed(() => props.budget?.notes ?? []);
const travelerText = computed(() => {
  if (!props.budget) {
    return '';
  }
  return `${props.budget.travelerCount} 人 · ${props.budget.days} 天 ${props.budget.nights} 晚`;
});
const perPersonAmount = computed(() => {
  if (!props.budget || props.budget.travelerCount <= 0) {
    return 0;
  }
  return Math.round(props.budget.totalAmount / props.budget.travelerCount);
});

function formatCurrency(value: number): string {
  return `¥${value.toLocaleString()}`;
}

function formatBasis(value: string): string {
  return basisLabel[value] ?? value;
}

function formatEstimate(value: string): string {
  return estimateLabel[value] ?? value;
}
</script>

<template>
  <article class="budget-card">
    <div class="budget-card__header">
      <span class="budget-card__icon">💰</span>
      <h3>预算明细</h3>
    </div>

    <template v-if="budget">
      <p class="budget-card__hint">{{ travelerText }} · 总预算与人均预算均由后端统一返回</p>

      <div class="budget-total">
        <div>
          <span>预估总费用</span>
          <small>人均约 {{ formatCurrency(perPersonAmount) }}</small>
        </div>
        <strong>{{ formatCurrency(budget.totalAmount) }}</strong>
      </div>

      <div class="budget-grid">
        <article v-for="item in items" :key="item.code" class="budget-cell">
          <div class="budget-cell__topline">
            <span class="budget-cell__label">{{ item.label }}</span>
            <span class="budget-cell__tag">{{ formatBasis(item.basis) }}</span>
          </div>
          <strong class="budget-cell__value">{{ formatCurrency(item.amount) }}</strong>
          <p class="budget-cell__desc">{{ item.formulaDescription }}</p>
          <div class="budget-cell__meta">
            <span>{{ formatEstimate(item.estimateLevel) }}</span>
          </div>
          <ul class="budget-cell__details">
            <li v-for="detail in item.details" :key="`${item.code}-${detail.label}`">
              <span>{{ detail.label }}</span>
              <strong>{{ detail.value }}</strong>
            </li>
          </ul>
        </article>
      </div>

      <ul class="budget-notes">
        <li v-for="note in notes" :key="note">{{ note }}</li>
      </ul>
    </template>

    <div v-else class="budget-empty">
      该历史行程尚未生成预算明细，请重新规划后查看最新预算。
    </div>
  </article>
</template>

<style scoped>
.budget-card {
  padding: 22px 24px;
  border-radius: 20px;
  border: 1px solid #e4ebf3;
  background: #fff;
}

.budget-card__header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.budget-card__header h3 {
  margin: 0;
  font-size: 18px;
  color: #1e293b;
}

.budget-card__icon {
  font-size: 22px;
}

.budget-card__hint {
  margin: 8px 0 0;
  color: #94a3b8;
  font-size: 13px;
}

.budget-total {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: 14px;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  color: #fff;
}

.budget-total span,
.budget-total small {
  display: block;
}

.budget-total small {
  margin-top: 6px;
  opacity: 0.9;
}

.budget-total strong {
  font-size: 24px;
  font-weight: 800;
}

.budget-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-top: 18px;
}

.budget-cell {
  display: grid;
  gap: 8px;
  padding: 16px;
  border-radius: 14px;
  border: 1px solid #eef2f7;
  background: #f8fbff;
}

.budget-cell__topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.budget-cell__label {
  color: #1e293b;
  font-size: 15px;
  font-weight: 700;
}

.budget-cell__tag {
  padding: 4px 8px;
  border-radius: 999px;
  background: #e0ecff;
  color: #2563eb;
  font-size: 12px;
}

.budget-cell__value {
  color: #1e293b;
  font-size: 24px;
  font-weight: 800;
}

.budget-cell__desc {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.budget-cell__meta {
  color: #94a3b8;
  font-size: 12px;
}

.budget-cell__details {
  display: grid;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.budget-cell__details li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #475569;
  font-size: 13px;
}

.budget-cell__details strong {
  color: #1e293b;
  font-weight: 600;
}

.budget-notes {
  display: grid;
  gap: 8px;
  margin: 16px 0 0;
  padding-left: 18px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.budget-empty {
  margin-top: 16px;
  padding: 16px 18px;
  border-radius: 14px;
  background: #f8fafc;
  color: #64748b;
  font-size: 14px;
}

@media (max-width: 720px) {
  .budget-grid {
    grid-template-columns: 1fr;
  }
}
</style>

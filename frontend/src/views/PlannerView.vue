<script setup lang="ts">
import { computed, onMounted, reactive, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getItinerary } from '../api/travel';
import { usePlannerStore } from '../stores/planner';
import type { PlannerFormState, TravelPlanRequestPayload } from '../types/travel';
import { parseApiError } from '../utils/error';
import PlannerSidePanel from '../components/planner/PlannerSidePanel.vue';

const route = useRoute();
const router = useRouter();
const plannerStore = usePlannerStore();

const form = reactive<PlannerFormState>({
  departureCity: '',
  destination: '',
  startDate: '2026-06-01',
  endDate: '2026-06-03',
  adults: 2,
  children: 0,
  budgetLevel: '均衡舒适',
  pace: '经典平衡',
  hotelStyle: '市区便利',
  interests: ['自然风光', '美食打卡'],
  notes: ''
});

const draftState = reactive({
  restoring: false,
  restoredFrom: null as number | null
});

const fieldErrors = reactive({
  departureCity: '',
  destination: '',
  startDate: '',
  endDate: '',
  adults: '',
  children: '',
  budgetLevel: '',
  pace: '',
  hotelStyle: '',
  interests: ''
});

const budgetOptions = computed(() => plannerStore.options.budgetLevels.length ? plannerStore.options.budgetLevels : [form.budgetLevel]);
const paceOptions = computed(() => plannerStore.options.paceOptions.length ? plannerStore.options.paceOptions : [form.pace]);
const hotelStyleOptions = computed(() => plannerStore.options.hotelStyles.length ? plannerStore.options.hotelStyles : [form.hotelStyle]);
const restoreMessage = computed(() => {
  if (draftState.restoring) {
    return '正在读取历史行程并回填表单…';
  }
  if (draftState.restoredFrom !== null) {
    return `已根据历史行程 #${draftState.restoredFrom} 回填参数，可直接调整后重新生成。`;
  }
  return '';
});

function applyForm(payload: TravelPlanRequestPayload) {
  form.departureCity = payload.departureCity;
  form.destination = payload.destination;
  form.startDate = payload.startDate;
  form.endDate = payload.endDate;
  form.adults = payload.adults;
  form.children = payload.children;
  form.budgetLevel = payload.budgetLevel;
  form.pace = payload.pace;
  form.hotelStyle = payload.hotelStyle;
  form.interests = [...payload.interests];
  form.notes = payload.notes;
}

function resetFieldErrors() {
  fieldErrors.departureCity = '';
  fieldErrors.destination = '';
  fieldErrors.startDate = '';
  fieldErrors.endDate = '';
  fieldErrors.adults = '';
  fieldErrors.children = '';
  fieldErrors.budgetLevel = '';
  fieldErrors.pace = '';
  fieldErrors.hotelStyle = '';
  fieldErrors.interests = '';
}

function normalizeInterestInput(value: string) {
  return value
    .split(/[、,，]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function updateInterestText(value: string) {
  form.interests = normalizeInterestInput(value);
  if (form.interests.length > 0) {
    fieldErrors.interests = '';
  }
}

function toggleInterest(item: string) {
  if (form.interests.includes(item)) {
    form.interests = form.interests.filter((interest) => interest !== item);
  } else {
    form.interests = [...form.interests, item];
  }

  if (form.interests.length > 0) {
    fieldErrors.interests = '';
  }
}

function validateForm() {
  resetFieldErrors();
  let valid = true;

  if (!form.departureCity.trim()) {
    fieldErrors.departureCity = '请填写出发地';
    valid = false;
  }

  if (!form.destination.trim()) {
    fieldErrors.destination = '请填写目的地';
    valid = false;
  }

  if (!form.startDate) {
    fieldErrors.startDate = '请选择开始日期';
    valid = false;
  }

  if (!form.endDate) {
    fieldErrors.endDate = '请选择结束日期';
    valid = false;
  } else if (form.startDate && form.endDate < form.startDate) {
    fieldErrors.endDate = '结束日期不能早于开始日期';
    valid = false;
  }

  if (!Number.isInteger(form.adults) || form.adults < 1) {
    fieldErrors.adults = '至少需要 1 位成人';
    valid = false;
  }

  if (!Number.isInteger(form.children) || form.children < 0) {
    fieldErrors.children = '儿童人数不能小于 0';
    valid = false;
  }

  if (!form.budgetLevel) {
    fieldErrors.budgetLevel = '请选择预算档位';
    valid = false;
  }

  if (!form.pace) {
    fieldErrors.pace = '请选择出行节奏';
    valid = false;
  }

  if (!form.hotelStyle) {
    fieldErrors.hotelStyle = '请选择酒店风格';
    valid = false;
  }

  if (form.interests.length === 0) {
    fieldErrors.interests = '至少保留一个出行偏好';
    valid = false;
  }

  return valid;
}

async function restoreFromHistory(fromQuery: unknown) {
  const itineraryId = Number(fromQuery);
  if (!Number.isFinite(itineraryId) || itineraryId <= 0 || draftState.restoredFrom === itineraryId) {
    return;
  }

  draftState.restoring = true;
  plannerStore.error = '';

  try {
    const record = await getItinerary(itineraryId);
    applyForm(record.request);
    draftState.restoredFrom = itineraryId;
  } catch (error) {
    plannerStore.error = parseApiError(error);
  } finally {
    draftState.restoring = false;
  }
}

watch(
  () => route.query.from,
  (fromQuery) => {
    void restoreFromHistory(fromQuery);
  },
  { immediate: true }
);

onMounted(async () => {
  await plannerStore.loadOptions();
});

async function submit() {
  if (!validateForm()) {
    return;
  }

  const created = await plannerStore.create({
    departureCity: form.departureCity.trim(),
    destination: form.destination.trim(),
    startDate: form.startDate,
    endDate: form.endDate,
    adults: form.adults,
    children: form.children,
    budgetLevel: form.budgetLevel,
    pace: form.pace,
    hotelStyle: form.hotelStyle,
    interests: [...form.interests],
    notes: form.notes.trim()
  });

  if (created?.itineraryId) {
    await router.push(`/itineraries/${created.itineraryId}`);
  }
}
</script>

<template>
  <section class="page-section planner-page">
    <div class="page-hero">
      <div>
        <p class="page-kicker">TRIP COMPOSER</p>
        <h2 class="page-title">规划录入页</h2>
        <p class="page-copy">
          用一张像登机牌一样清晰的表单，把目的地、时间、预算和旅行偏好压缩成一份可保存、可修改、可导出的智能行程。
        </p>
      </div>
      <div class="planner-status-card">
        <span class="planner-status-label">AI PROGRESS</span>
        <strong>{{ plannerStore.loading ? plannerStore.loadingStageText : '等待输入旅行条件' }}</strong>
        <p>这一版先使用阶段式反馈，后续可以无缝替换成真实规划进度。</p>
      </div>
    </div>

    <div v-if="restoreMessage" class="page-note">{{ restoreMessage }}</div>

    <div class="planner-grid">
      <form class="planner-panel planner-form-grid" @submit.prevent="submit">
        <label class="planner-field" :class="{ 'planner-field--error': fieldErrors.departureCity }">
          出发地
          <input v-model="form.departureCity" placeholder="例如：上海" />
          <span v-if="fieldErrors.departureCity" class="planner-field-hint planner-field-hint--error">{{ fieldErrors.departureCity }}</span>
        </label>
        <label class="planner-field" :class="{ 'planner-field--error': fieldErrors.destination }">
          目的地
          <input v-model="form.destination" list="destinations" placeholder="例如：杭州" />
          <datalist id="destinations">
            <option v-for="item in plannerStore.options.featuredDestinations" :key="item" :value="item" />
          </datalist>
          <span v-if="fieldErrors.destination" class="planner-field-hint planner-field-hint--error">{{ fieldErrors.destination }}</span>
        </label>
        <label class="planner-field" :class="{ 'planner-field--error': fieldErrors.startDate }">
          开始日期
          <input v-model="form.startDate" type="date" />
          <span v-if="fieldErrors.startDate" class="planner-field-hint planner-field-hint--error">{{ fieldErrors.startDate }}</span>
        </label>
        <label class="planner-field" :class="{ 'planner-field--error': fieldErrors.endDate }">
          结束日期
          <input v-model="form.endDate" type="date" />
          <span v-if="fieldErrors.endDate" class="planner-field-hint planner-field-hint--error">{{ fieldErrors.endDate }}</span>
        </label>
        <label class="planner-field" :class="{ 'planner-field--error': fieldErrors.adults }">
          成人
          <input v-model.number="form.adults" type="number" min="1" />
          <span v-if="fieldErrors.adults" class="planner-field-hint planner-field-hint--error">{{ fieldErrors.adults }}</span>
        </label>
        <label class="planner-field" :class="{ 'planner-field--error': fieldErrors.children }">
          儿童
          <input v-model.number="form.children" type="number" min="0" />
          <span v-if="fieldErrors.children" class="planner-field-hint planner-field-hint--error">{{ fieldErrors.children }}</span>
        </label>
        <label class="planner-field" :class="{ 'planner-field--error': fieldErrors.budgetLevel }">
          预算
          <select v-model="form.budgetLevel">
            <option v-for="item in budgetOptions" :key="item" :value="item">{{ item }}</option>
          </select>
          <span v-if="fieldErrors.budgetLevel" class="planner-field-hint planner-field-hint--error">{{ fieldErrors.budgetLevel }}</span>
        </label>
        <label class="planner-field" :class="{ 'planner-field--error': fieldErrors.pace }">
          节奏
          <select v-model="form.pace">
            <option v-for="item in paceOptions" :key="item" :value="item">{{ item }}</option>
          </select>
          <span v-if="fieldErrors.pace" class="planner-field-hint planner-field-hint--error">{{ fieldErrors.pace }}</span>
        </label>
        <label class="planner-field" :class="{ 'planner-field--error': fieldErrors.hotelStyle }">
          酒店风格
          <select v-model="form.hotelStyle">
            <option v-for="item in hotelStyleOptions" :key="item" :value="item">{{ item }}</option>
          </select>
          <span v-if="fieldErrors.hotelStyle" class="planner-field-hint planner-field-hint--error">{{ fieldErrors.hotelStyle }}</span>
        </label>
        <label class="planner-field planner-form-full" :class="{ 'planner-field--error': fieldErrors.interests }">
          出行偏好
          <input
            :value="form.interests.join('、')"
            placeholder="用顿号分隔，例如：自然风光、摄影出片、美食打卡"
            @input="updateInterestText(String(($event.target as HTMLInputElement).value))"
          />
          <span v-if="fieldErrors.interests" class="planner-field-hint planner-field-hint--error">{{ fieldErrors.interests }}</span>
        </label>
        <label class="planner-field planner-form-full">
          备注
          <textarea v-model="form.notes" rows="4" placeholder="例如：希望住在景区附近；第二天想看日落；带孩子希望节奏轻松一些。" />
        </label>
        <div class="planner-form-full planner-submit-row">
          <button class="page-primary-btn" :disabled="plannerStore.loading || draftState.restoring" type="submit">
            {{ draftState.restoring ? '正在回填历史行程…' : plannerStore.loading ? '正在生成行程…' : '生成并进入结果页' }}
          </button>
          <p v-if="plannerStore.error" class="page-error-inline">{{ plannerStore.error }}</p>
        </div>
      </form>

      <PlannerSidePanel
        :destinations="plannerStore.options.featuredDestinations"
        :interests="plannerStore.options.interests"
        :selected-interests="form.interests"
        @select-destination="(v) => form.destination = v"
        @toggle-interest="toggleInterest"
      />
    </div>
  </section>
</template>

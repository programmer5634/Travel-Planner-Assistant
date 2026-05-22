import { defineStore } from 'pinia';
import { createItinerary, getOptions } from '../api/travel';
import type {
  StoredTravelPlanResponse,
  TravelPlanRequestPayload,
  TravelPlanningOptions
} from '../types/travel';
import { parseApiError } from '../utils/error';

const loadingStages = ['正在检索景点', '查询天气', '匹配酒店', 'AI 生成行程'];

export const usePlannerStore = defineStore('planner', {
  state: () => ({
    options: {
      featuredDestinations: [] as string[],
      interests: [] as string[],
      budgetLevels: [] as string[],
      paceOptions: [] as string[],
      hotelStyles: [] as string[]
    } as TravelPlanningOptions,
    latestCreated: null as StoredTravelPlanResponse | null,
    loading: false,
    error: '',
    loadingStageIndex: -1,
    loadingStages
  }),
  getters: {
    loadingStageText(state) {
      return state.loadingStageIndex >= 0
        ? state.loadingStages[Math.min(state.loadingStageIndex, state.loadingStages.length - 1)]
        : '';
    }
  },
  actions: {
    async loadOptions() {
      try {
        this.options = await getOptions();
      } catch (error) {
        this.error = parseApiError(error);
      }
    },
    async create(payload: TravelPlanRequestPayload) {
      if (this.loading) {
        return null;
      }

      this.loading = true;
      this.error = '';
      this.loadingStageIndex = 0;

      let timer: ReturnType<typeof window.setInterval> | null = window.setInterval(() => {
        if (this.loadingStageIndex < this.loadingStages.length - 1) {
          this.loadingStageIndex += 1;
        }
      }, 900);

      try {
        this.latestCreated = await createItinerary(payload);
        return this.latestCreated;
      } catch (error) {
        this.error = parseApiError(error);
        return null;
      } finally {
        if (timer !== null) {
          window.clearInterval(timer);
          timer = null;
        }
        this.loading = false;
        this.loadingStageIndex = -1;
      }
    }
  }
});


import { defineStore } from 'pinia';
import { deleteItinerary, listItineraries, updateFavorite, type ListItinerariesParams } from '../api/travel';
import type { ItinerarySummary } from '../types/travel';
import { parseApiError } from '../utils/error';

export const useHistoryStore = defineStore('history', {
  state: () => ({
    records: [] as ItinerarySummary[],
    loading: false,
    deletingId: null as number | null,
    togglingFavoriteId: null as number | null,
    error: '',
    pageNo: 1,
    pageSize: 10,
    total: 0,
    filters: {} as ListItinerariesParams
  }),
  getters: {
    hasPrevPage(state) {
      return state.pageNo > 1;
    },
    hasNextPage(state) {
      return state.pageNo * state.pageSize < state.total;
    }
  },
  actions: {
    async fetch(params: ListItinerariesParams = {}) {
      this.loading = true;
      this.error = '';
      const merged = {
        ...this.filters,
        ...params,
        pageNo: params.pageNo ?? this.pageNo,
        pageSize: params.pageSize ?? this.pageSize
      };
      this.filters = merged;
      try {
        const page = await listItineraries(merged);
        this.records = page.records;
        this.total = page.total;
        this.pageNo = page.pageNo;
        this.pageSize = page.pageSize;
      } catch (error) {
        this.error = parseApiError(error);
      } finally {
        this.loading = false;
      }
    },
    async goToPage(pageNo: number) {
      await this.fetch({ pageNo });
    },
    async toggleFavorite(itineraryId: number, favorite: boolean) {
      this.togglingFavoriteId = itineraryId;
      this.error = '';
      try {
        await updateFavorite(itineraryId, favorite);
        const index = this.records.findIndex((item) => item.itineraryId === itineraryId);
        if (index !== -1) {
          this.records[index] = {
            ...this.records[index],
            favorite
          };
        }
      } catch (error) {
        this.error = parseApiError(error);
      } finally {
        this.togglingFavoriteId = null;
      }
    },
    async remove(itineraryId: number) {
      this.deletingId = itineraryId;
      this.error = '';
      try {
        await deleteItinerary(itineraryId);
        this.records = this.records.filter((item) => item.itineraryId !== itineraryId);
        this.total = Math.max(this.total - 1, 0);
        if (this.records.length === 0 && this.pageNo > 1) {
          await this.fetch({ pageNo: this.pageNo - 1 });
        }
      } catch (error) {
        this.error = parseApiError(error);
        throw error;
      } finally {
        this.deletingId = null;
      }
    }
  }
});

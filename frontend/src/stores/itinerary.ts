import { defineStore } from 'pinia';
import { getItinerary, reviseItinerary, updateFavorite, type ReviseItineraryPayload } from '../api/travel';
import type { StoredTravelPlanResponse } from '../types/travel';
import { parseApiError } from '../utils/error';

export const useItineraryStore = defineStore('itinerary', {
  state: () => ({
    itinerary: null as StoredTravelPlanResponse | null,
    loading: false,
    revising: false,
    togglingFavorite: false,
    error: ''
  }),
  actions: {
    async fetch(itineraryId: number) {
      this.loading = true;
      this.error = '';
      try {
        this.itinerary = await getItinerary(itineraryId);
      } catch (error) {
        this.error = parseApiError(error);
      } finally {
        this.loading = false;
      }
    },
    async toggleFavorite(itineraryId: number, favorite: boolean) {
      this.togglingFavorite = true;
      this.error = '';
      try {
        this.itinerary = await updateFavorite(itineraryId, favorite);
      } catch (error) {
        this.error = parseApiError(error);
      } finally {
        this.togglingFavorite = false;
      }
    },
    async revise(itineraryId: number, payload: ReviseItineraryPayload) {
      this.revising = true;
      this.error = '';
      try {
        this.itinerary = await reviseItinerary(itineraryId, payload);
        return this.itinerary;
      } catch (error) {
        this.error = parseApiError(error);
        throw error;
      } finally {
        this.revising = false;
      }
    }
  }
});

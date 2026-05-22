import http from './http';
import type {
  ItinerarySummary,
  PageResult,
  StoredTravelPlanResponse,
  TravelPlanRequestPayload,
  TravelPlanningOptions
} from '../types/travel';

export interface ListItinerariesParams {
  keyword?: string;
  destination?: string;
  favoriteOnly?: boolean;
  startDate?: string;
  endDate?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface ReviseItineraryPayload {
  message: string;
}

export async function getOptions() {
  const { data } = await http.get<TravelPlanningOptions>('/travel/options');
  return data;
}

export async function createItinerary(payload: TravelPlanRequestPayload) {
  const { data } = await http.post<StoredTravelPlanResponse>('/travel/itineraries', payload, {
    timeout: 180_000
  });
  return data;
}

export async function getItinerary(itineraryId: number) {
  const { data } = await http.get<StoredTravelPlanResponse>(`/travel/itineraries/${itineraryId}`);
  return data;
}

export async function reviseItinerary(itineraryId: number, payload: ReviseItineraryPayload) {
  const { data } = await http.post<StoredTravelPlanResponse>(`/travel/itineraries/${itineraryId}/revise`, payload, {
    timeout: 180_000
  });
  return data;
}

export async function listItineraries(params: ListItinerariesParams = {}) {
  const { data } = await http.get<PageResult<ItinerarySummary>>('/travel/itineraries', { params });
  return data;
}

export async function deleteItinerary(itineraryId: number) {
  await http.delete(`/travel/itineraries/${itineraryId}`);
}

export async function updateFavorite(itineraryId: number, favorite: boolean) {
  const { data } = await http.put<StoredTravelPlanResponse>(
    `/travel/itineraries/${itineraryId}/favorite`,
    null,
    { params: { favorite } }
  );
  return data;
}

import type { PlannerFormState, StoredTravelPlanResponse, TravelPlan } from './travel';

export interface HistoryRecord {
  itineraryId: number;
  title: string;
  destination: string;
  departureCity: string;
  startDate: string;
  endDate: string;
  createdAt: string;
  updatedAt: string;
  revisionNo: number;
  favorite: boolean;
  saved: boolean;
  formSnapshot: PlannerFormState;
  planSnapshot: TravelPlan;
}

export interface HistoryFilters {
  keyword: string;
  dateRange: [string, string] | [];
  favoritesOnly: boolean;
}

export interface PersistableItinerary {
  response: StoredTravelPlanResponse;
  form: PlannerFormState;
}

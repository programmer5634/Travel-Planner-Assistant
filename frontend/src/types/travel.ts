export interface TravelPlanningOptions {
  featuredDestinations: string[];
  interests: string[];
  budgetLevels: string[];
  paceOptions: string[];
  hotelStyles: string[];
}

export interface PlannerFormState {
  departureCity: string;
  destination: string;
  startDate: string;
  endDate: string;
  adults: number;
  children: number;
  budgetLevel: string;
  pace: string;
  hotelStyle: string;
  interests: string[];
  notes: string;
}

export interface TravelPlanRequestPayload {
  departureCity: string;
  destination: string;
  startDate: string;
  endDate: string;
  adults: number;
  children: number;
  budgetLevel: string;
  pace: string;
  hotelStyle: string;
  interests: string[];
  notes: string;
}

export interface TripSummary {
  days: number;
  nights: number;
  budgetLevel: string;
  pace: string;
  stayArea: string;
  transportAdvice: string;
}

export interface DailyPlan {
  day: number;
  date: string;
  theme: string;
  agenda: string[];
  foodSuggestion: string;
  eveningSuggestion: string;
}

export interface SightRecommendation {
  name: string;
  category: string;
  district: string;
  address?: string;
  highlight: string;
  recommendedDuration: string;
  latitude: number;
  longitude: number;
  imageUrl?: string;
}

export interface HotelRecommendation {
  name: string;
  district: string;
  address?: string;
  style: string;
  priceBand: string;
  highlight: string;
  imageUrl: string;
  latitude: number;
  longitude: number;
}

export interface WeatherSnapshot {
  date: string;
  condition: string;
  temperature: string;
  advice: string;
}

export interface MapPoint {
  day: number;
  sequence: number;
  name: string;
  type: string;
  district: string;
  address?: string;
  description?: string;
  latitude: number;
  longitude: number;
  imageUrl?: string;
}

export interface RouteCoordinate {
  latitude: number;
  longitude: number;
}

export interface RouteSegment {
  sequence: number;
  fromName: string;
  toName: string;
  distanceMeters: number;
  durationSeconds: number;
  polyline: RouteCoordinate[];
}

export interface RouteDay {
  day: number;
  mode: string;
  segments: RouteSegment[];
}

export interface BudgetDetail {
  label: string;
  value: string;
  sourceType: string;
}

export interface BudgetItem {
  code: string;
  label: string;
  amount: number;
  basis: string;
  estimateLevel: string;
  formulaDescription: string;
  details: BudgetDetail[];
}

export interface BudgetSummary {
  currency: string;
  pricingMode: string;
  travelerCount: number;
  days: number;
  nights: number;
  items: BudgetItem[];
  totalAmount: number;
  notes: string[];
}

export interface TravelPlan {
  title: string;
  overview: string;
  summary: TripSummary;
  itinerary: DailyPlan[];
  attractions: SightRecommendation[];
  hotels: HotelRecommendation[];
  weather: WeatherSnapshot[];
  mapPoints: MapPoint[];
  routeDays: RouteDay[];
  budget?: BudgetSummary;
  travelTips: string[];
}

export interface ItinerarySummary {
  itineraryId: number;
  revisionNo: number;
  favorite: boolean;
  departureCity: string;
  destination: string;
  startDate: string;
  endDate: string;
  createdAt: string;
  updatedAt: string;
  title: string;
  overview: string;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  pageNo: number;
  pageSize: number;
}

export interface StoredTravelPlanResponse {
  itineraryId: number;
  sessionCode: string;
  revisionNo: number;
  favorite: boolean;
  departureCity: string;
  destination: string;
  startDate: string;
  endDate: string;
  createdAt: string;
  updatedAt: string;
  request: TravelPlanRequestPayload;
  plan: TravelPlan;
}

export interface TravelPlanRevisionPayload {
  message: string;
}

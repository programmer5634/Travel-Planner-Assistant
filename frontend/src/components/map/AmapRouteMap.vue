<script setup lang="ts">
import AMapLoader from '@amap/amap-jsapi-loader';
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import type { MapPoint, RouteDay, RouteSegment } from '../../types/travel';

interface NearbyPoi {
  id: string;
  name: string;
  type: string;
  address: string;
  latitude: number;
  longitude: number;
  sourcePointKey: string;
}

const DEFAULT_NEARBY_TYPES = ['餐饮', '酒店', '交通', '景点'];

const props = withDefaults(defineProps<{
  points: MapPoint[];
  routeDays?: RouteDay[] | null;
  activeDay: number | null;
  activePointKey: string;
  nearbyEnabled?: boolean;
  nearbyRadius?: number;
  nearbyTypes?: string | string[];
  nearbyLimitPerPoint?: number;
}>(), {
  routeDays: () => [],
  nearbyEnabled: true,
  nearbyRadius: 500,
  nearbyTypes: () => ['餐饮服务', '住宿服务', '交通设施服务', '风景名胜'],
  nearbyLimitPerPoint: 8
});

const emit = defineEmits<{
  selectPoint: [payload: { day: number; key: string }];
}>();

const containerRef = ref<HTMLElement | null>(null);
const loading = ref(false);
const error = ref('');

let AMapRef: any = null;
let map: any = null;
let routeOverlays: Array<{ day: number; base: any; dash: any }> = [];
let infoWindow: any = null;
let hoveredMarkerKey = '';
let overlays: Array<{ key: string; point: MapPoint; marker: any; element: HTMLButtonElement }> = [];
let nearbyMarkers: any[] = [];
let nearbySearchToken = 0;

const amapKey = import.meta.env.VITE_AMAP_KEY;
const amapSecurityCode = import.meta.env.VITE_AMAP_SECURITY_CODE;

const usablePoints = computed(() => props.points.filter((point) => Number.isFinite(point.latitude) && Number.isFinite(point.longitude)));
const sortedPoints = computed(() => usablePoints.value.slice().sort((left, right) => left.day - right.day || left.sequence - right.sequence));
const safeRouteDays = computed(() => Array.isArray(props.routeDays) ? props.routeDays : []);
const hasMapKey = computed(() => Boolean(amapKey));
const normalizedNearbyTypes = computed(() => {
  const values = Array.isArray(props.nearbyTypes) ? props.nearbyTypes : (props.nearbyTypes ? props.nearbyTypes.split(',') : DEFAULT_NEARBY_TYPES);
  return values.map((value) => value.trim()).filter(Boolean);
});
const nearbyTypeQuery = computed(() => normalizedNearbyTypes.value.join('|'));

function pointKey(point: MapPoint) {
  return `${point.day}-${point.sequence}-${point.name}`;
}

function isHotel(point: MapPoint) {
  return point.type.includes('酒店') || point.day === 0;
}

function markerBadge(point: MapPoint) {
  return isHotel(point) ? '住' : String(Math.max(point.sequence, 1));
}

function markerClass(point: MapPoint) {
  return isHotel(point) ? 'route-map-marker route-map-marker--hotel' : 'route-map-marker route-map-marker--spot';
}

function updateFitView() {
  if (!map) {
    return;
  }
  const routeLayers = routeOverlays.flatMap((item) => [item.base, item.dash]).filter(Boolean);
  const boundsOverlays = [
    ...overlays.map((item) => item.marker),
    ...nearbyMarkers,
    ...routeLayers
  ];
  if (boundsOverlays.length > 0) {
    map.setFitView(boundsOverlays, false, [56, 56, 56, 56]);
  }
}

function createMarkerContent(point: MapPoint) {
  const key = pointKey(point);
  const button = document.createElement('button');
  button.type = 'button';
  button.className = markerClass(point);
  button.innerHTML = `
    <span class="route-map-marker__balloon">
      <span class="route-map-marker__shine"></span>
    </span>
    <span class="route-map-marker__stem"></span>
    <span class="route-map-marker__badge">${markerBadge(point)}</span>
  `;
  button.setAttribute('aria-label', point.name);
  button.title = point.name;
  button.addEventListener('click', (event) => {
    event.preventDefault();
    emit('selectPoint', { day: point.day, key });
    showItineraryInfoWindow(point);
  });
  button.addEventListener('mouseenter', () => {
    hoveredMarkerKey = key;
    updateMarkerStyles();
    updateRouteStyles();
  });
  button.addEventListener('mouseleave', () => {
    hoveredMarkerKey = '';
    updateMarkerStyles();
    updateRouteStyles();
  });
  return button;
}

function updateMarkerStyles() {
  overlays.forEach(({ key, point, marker, element }) => {
    const active = key === props.activePointKey;
    const hovered = key === hoveredMarkerKey;
    element.classList.toggle('is-active', active);
    element.classList.toggle('is-hover', hovered);
    marker.setzIndex(active ? 240 : hovered ? 220 : 180);
    marker.setAngle(active && !isHotel(point) ? -8 : 0);
  });

  const current = overlays.find((item) => item.key === props.activePointKey);
  if (map && current) {
    map.panTo([current.point.longitude, current.point.latitude]);
  }
}

function routeStyle(day: number, hovered: boolean) {
  const active = props.activeDay === null ? true : props.activeDay === day;
  return {
    baseOpacity: hovered ? (active ? 0.34 : 0.18) : (active ? 0.26 : 0.12),
    baseWeight: hovered ? (active ? 12 : 10) : (active ? 10 : 8),
    dashColor: hovered ? '#1677ff' : active ? '#2f7bff' : '#60a5fa',
    dashOpacity: hovered ? 1 : active ? 0.94 : 0.42,
    dashWeight: hovered ? (active ? 6 : 5) : (active ? 5 : 4)
  };
}

function applyRouteStyle(day: number, base: any, dash: any, hovered: boolean) {
  const style = routeStyle(day, hovered);
  base.setOptions({
    strokeOpacity: style.baseOpacity,
    strokeWeight: style.baseWeight
  });
  dash.setOptions({
    strokeColor: style.dashColor,
    strokeOpacity: style.dashOpacity,
    strokeWeight: style.dashWeight
  });
}

function setRouteHover(day: number, hovered: boolean) {
  const route = routeOverlays.find((item) => item.day === day);
  if (!route) {
    return;
  }
  applyRouteStyle(day, route.base, route.dash, hovered);
}

function bindRouteHover(day: number, base: any, dash: any) {
  const onEnter = () => setRouteHover(day, true);
  const onLeave = () => setRouteHover(day, false);
  base.on('mouseover', onEnter);
  base.on('mouseout', onLeave);
  dash.on('mouseover', onEnter);
  dash.on('mouseout', onLeave);
}

function updateRouteStyles() {
  routeOverlays.forEach((route) => applyRouteStyle(route.day, route.base, route.dash, false));
}

function clearRoute() {
  if (!map) {
    return;
  }
  routeOverlays.forEach(({ base, dash }) => map.remove([base, dash]));
  routeOverlays = [];
}

function clearNearbyMarkers() {
  if (!map) {
    return;
  }
  nearbyMarkers.forEach((marker) => map.remove(marker));
  nearbyMarkers = [];
}

function clearMarkers() {
  if (!map) {
    return;
  }
  overlays.forEach(({ marker }) => map.remove(marker));
  overlays = [];
  clearNearbyMarkers();
  if (infoWindow) {
    infoWindow.close();
  }
  hoveredMarkerKey = '';
}

function escapeHtml(value?: string) {
  return (value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function showItineraryInfoWindow(point: MapPoint) {
  if (!map || !infoWindow) {
    return;
  }

  const address = point.address || point.district;
  const description = point.description || `${point.type} · ${point.district}`;
  const extraInfo = isHotel(point)
    ? '<span class="info-window__tag info-window__tag--hotel">住宿点</span>'
    : `<span class="info-window__tag info-window__tag--spot">DAY ${point.day} · 第${point.sequence}站</span>`;

  const content = `
    <div class="info-window">
      <div class="info-window__body">
        <h4 class="info-window__title">${escapeHtml(point.name)}</h4>
        ${address ? `<p class="info-window__address">${escapeHtml(address)}</p>` : ''}
        <p class="info-window__meta">${escapeHtml(description)}</p>
        <div class="info-window__tags">${extraInfo}</div>
      </div>
    </div>
  `;

  infoWindow.setContent(content);
  infoWindow.open(map, [point.longitude, point.latitude]);
}

function showNearbyInfoWindow(poi: NearbyPoi) {
  if (!map || !infoWindow) {
    return;
  }

  const content = `
    <div class="info-window">
      <div class="info-window__body">
        <h4 class="info-window__title">${escapeHtml(poi.name)}</h4>
        ${poi.address ? `<p class="info-window__address">${escapeHtml(poi.address)}</p>` : ''}
        <p class="info-window__meta">${escapeHtml(poi.type || '周边点位')}</p>
        <div class="info-window__tags"><span class="info-window__tag info-window__tag--nearby">周边 POI</span></div>
      </div>
    </div>
  `;

  infoWindow.setContent(content);
  infoWindow.open(map, [poi.longitude, poi.latitude]);
}

function buildNearbyPoi(rawPoi: any, sourcePointKey: string) {
  if (!rawPoi?.location) {
    return null;
  }

  const lng = typeof rawPoi.location.lng === 'number' ? rawPoi.location.lng : rawPoi.location.getLng?.();
  const lat = typeof rawPoi.location.lat === 'number' ? rawPoi.location.lat : rawPoi.location.getLat?.();
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) {
    return null;
  }

  const name = rawPoi.name?.trim();
  if (!name) {
    return null;
  }

  const address = rawPoi.address?.trim() || rawPoi.pname?.trim() || rawPoi.cityname?.trim() || '';
  const id = rawPoi.id || `${name}-${address}-${lng}-${lat}`;
  return {
    id,
    name,
    type: rawPoi.type || '周边点位',
    address,
    latitude: lat,
    longitude: lng,
    sourcePointKey
  } satisfies NearbyPoi;
}

async function searchNearbyPois() {
  if (!map || !AMapRef || !props.nearbyEnabled || sortedPoints.value.length === 0 || normalizedNearbyTypes.value.length === 0) {
    clearNearbyMarkers();
    updateFitView();
    return;
  }

  const currentToken = ++nearbySearchToken;
  clearNearbyMarkers();

  const poiMap = new Map<string, NearbyPoi>();

  for (const point of sortedPoints.value) {
    for (const keyword of normalizedNearbyTypes.value) {
      await new Promise<void>((resolve) => {
        const searchInstance = new AMapRef.PlaceSearch({
          pageSize: props.nearbyLimitPerPoint,
          pageIndex: 1,
          extensions: 'base'
        });
        searchInstance.searchNearBy(
          keyword,
          [point.longitude, point.latitude],
          props.nearbyRadius,
          (status: string, result: any) => {
            if (currentToken !== nearbySearchToken) {
              resolve();
              return;
            }

            const pois = status === 'complete' ? result?.poiList?.pois ?? [] : [];
            for (const rawPoi of pois) {
              const nearbyPoi = buildNearbyPoi(rawPoi, pointKey(point));
              if (!nearbyPoi) {
                continue;
              }
              if (nearbyPoi.name === point.name && Math.abs(nearbyPoi.latitude - point.latitude) < 0.00001 && Math.abs(nearbyPoi.longitude - point.longitude) < 0.00001) {
                continue;
              }
              if (!poiMap.has(nearbyPoi.id)) {
                poiMap.set(nearbyPoi.id, nearbyPoi);
              }
            }
            resolve();
          }
        );
      });
    }
  }

  if (currentToken !== nearbySearchToken || !map) {
    return;
  }

  nearbyMarkers = Array.from(poiMap.values()).map((poi) => {
    const marker = new AMapRef.Marker({
      position: [poi.longitude, poi.latitude],
      zIndex: 90,
      extData: poi
    });
    marker.on('click', () => {
      showNearbyInfoWindow(poi);
    });
    map.add(marker);
    return marker;
  });

  updateFitView();
}

function segmentPath(segment: RouteSegment) {
  return segment.polyline
    .filter((point) => Number.isFinite(point.latitude) && Number.isFinite(point.longitude))
    .map((point) => [point.longitude, point.latitude]);
}

function renderRoute() {
  if (!map) {
    return;
  }

  clearMarkers();
  clearRoute();

  if (sortedPoints.value.length === 0) {
    return;
  }

  overlays = sortedPoints.value.map((point) => {
    const element = createMarkerContent(point);
    const marker = new AMapRef.Marker({
      position: [point.longitude, point.latitude],
      content: element,
      anchor: 'bottom-center',
      offset: new AMapRef.Pixel(0, -6),
      zIndex: 180,
      extData: point
    });
    map.add(marker);
    return { key: pointKey(point), point, marker, element };
  });

  routeOverlays = safeRouteDays.value.flatMap((routeDay) => {
    return routeDay.segments
      .map((segment) => {
        const path = segmentPath(segment);
        if (path.length < 2) {
          return null;
        }
        const base = new AMapRef.Polyline({
          path,
          strokeColor: '#81b9ff',
          strokeWeight: 10,
          strokeOpacity: 0.22,
          lineJoin: 'round',
          lineCap: 'round',
          zIndex: 120,
          showDir: false
        });
        const dash = new AMapRef.Polyline({
          path,
          strokeColor: '#3794ff',
          strokeWeight: 4,
          strokeOpacity: 0.88,
          strokeStyle: 'dashed',
          strokeDasharray: [18, 10],
          lineJoin: 'round',
          lineCap: 'round',
          zIndex: 140,
          showDir: false
        });
        map.add([base, dash]);
        bindRouteHover(routeDay.day, base, dash);
        applyRouteStyle(routeDay.day, base, dash, false);
        return { day: routeDay.day, base, dash };
      })
      .filter((route): route is { day: number; base: any; dash: any } => route !== null);
  });

  updateMarkerStyles();
  updateRouteStyles();
  updateFitView();
  void searchNearbyPois();
}

let amapLoaderPromise: Promise<any> | null = null;


function loadAmap() {
  if (AMapRef) {
    return Promise.resolve(AMapRef);
  }

  if (amapLoaderPromise) {
    return amapLoaderPromise;
  }

  if (!amapKey) {
    return Promise.reject(new Error('未配置 VITE_AMAP_KEY'));
  }

  if (amapSecurityCode) {
    window._AMapSecurityConfig = {
      securityJsCode: amapSecurityCode
    };
  }

  amapLoaderPromise = AMapLoader.load({
    key: amapKey,
    version: '2.0',
    plugins: ['AMap.PlaceSearch']
  }).then((AMap) => {
    AMapRef = AMap;
    return AMap;
  });

  return amapLoaderPromise;
}

let infoWindowStyleElement: HTMLStyleElement | null = null;

function injectInfoWindowStyles() {
  if (infoWindowStyleElement) {
    return;
  }
  const style = document.createElement('style');
  style.dataset.infoWindowStyles = 'true';
  style.textContent = `
    .amap-info-window .info-window {
      background: #fff;
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
      max-width: 280px;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Microsoft YaHei', sans-serif;
    }
    .amap-info-window .info-window__body {
      padding: 12px 14px;
    }
    .amap-info-window .info-window__title {
      margin: 0 0 8px;
      font-size: 15px;
      font-weight: 700;
      color: #1e293b;
    }
    .amap-info-window .info-window__address {
      margin: 0 0 6px;
      font-size: 12px;
      color: #475569;
      line-height: 1.5;
    }
    .amap-info-window .info-window__meta {
      margin: 0 0 10px;
      font-size: 12px;
      color: #64748b;
      line-height: 1.6;
    }
    .amap-info-window .info-window__tag {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 600;
    }
    .amap-info-window .info-window__tag--spot {
      background: #eff6ff;
      color: #2563eb;
    }
    .amap-info-window .info-window__tag--hotel {
      background: #ecfeff;
      color: #0f766e;
    }
    .amap-info-window .info-window__tag--nearby {
      background: #f8fafc;
      color: #475569;
    }
  `;
  document.head.appendChild(style);
  infoWindowStyleElement = style;
}

function removeInfoWindowStyles() {
  if (infoWindowStyleElement) {
    document.head.removeChild(infoWindowStyleElement);
    infoWindowStyleElement = null;
  }
}

async function ensureMap() {
  if (!containerRef.value || !hasMapKey.value) {
    return;
  }

  loading.value = true;
  error.value = '';

  try {
    const AMap = await loadAmap();

    if (!containerRef.value) {
      return;
    }

    if (!map) {
      map = new AMap.Map(containerRef.value, {
        viewMode: '2D',
        zoom: 11,
        center: sortedPoints.value[0] ? [sortedPoints.value[0].longitude, sortedPoints.value[0].latitude] : [120.1551, 30.2741],
        resizeEnable: true,
        mapStyle: 'amap://styles/normal'
      });
      map.setFeatures(['bg', 'road', 'building', 'point']);

      injectInfoWindowStyles();

      infoWindow = new AMap.InfoWindow({
        isCustom: true,
        offset: new AMap.Pixel(0, -40),
        content: ''
      });
    }

    renderRoute();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '高德地图初始化失败';
  } finally {
    loading.value = false;
  }
}

watch(() => props.activePointKey, updateMarkerStyles);
watch(() => props.activeDay, updateRouteStyles);
watch(sortedPoints, () => {
  void ensureMap();
});
watch(safeRouteDays, () => {
  void ensureMap();
}, { deep: true });
watch(() => [props.nearbyEnabled, props.nearbyRadius, nearbyTypeQuery.value, props.nearbyLimitPerPoint], () => {
  if (!map || !AMapRef) {
    return;
  }
  void searchNearbyPois();
});

onMounted(() => {
  void ensureMap();
});

onBeforeUnmount(() => {
  nearbySearchToken += 1;
  if (map) {
    map.destroy();
    map = null;
  }
  overlays = [];
  nearbyMarkers = [];
  routeOverlays = [];
  infoWindow = null;
  AMapRef = null;
  removeInfoWindowStyles();
});
</script>

<template>
  <div class="route-map-card">
    <div v-if="!hasMapKey" class="map-placeholder map-placeholder--config">
      <p>尚未配置高德地图 Key</p>
      <span>请在前端环境变量中补充 VITE_AMAP_KEY，若启用了安全校验再补 VITE_AMAP_SECURITY_CODE。</span>
    </div>
    <div v-else-if="error" class="map-placeholder map-placeholder--config">
      <p>地图暂时不可用</p>
      <span>{{ error }}</span>
    </div>
    <div v-else class="route-map-stage">
      <div ref="containerRef" class="route-map-canvas"></div>
      <div v-if="loading" class="route-map-mask">地图加载中…</div>
      <div v-else-if="usablePoints.length === 0" class="route-map-mask">当前行程还没有可展示的结构化点位。</div>
      <div v-else class="route-map-controls">
        <div class="route-map-legend">
          <span><i class="route-map-legend__dot route-map-legend__dot--spot"></i>景点</span>
          <span><i class="route-map-legend__dot route-map-legend__dot--hotel"></i>住宿</span>
          <span><i class="route-map-legend__dot route-map-legend__dot--nearby"></i>周边</span>
          <span><i class="route-map-legend__line"></i>路线</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.route-map-card {
  display: grid;
}

.route-map-stage {
  position: relative;
  min-height: 500px;
  overflow: hidden;
  border: 1px solid #dfe5ee;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 10px 26px rgba(15, 53, 120, 0.08);
}

.route-map-canvas {
  width: 100%;
  min-height: 500px;
}

.route-map-mask {
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 18px;
  z-index: 9;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(31, 45, 61, 0.82);
  border: 1px solid rgba(223, 229, 238, 0.4);
  color: #fff;
  backdrop-filter: blur(10px);
}

.route-map-controls {
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 18px;
  z-index: 7;
  display: flex;
  justify-content: flex-start;
}

.route-map-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.route-map-legend span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid #dfe5ee;
  color: #33405d;
  font-size: 13px;
  box-shadow: 0 6px 14px rgba(49, 72, 135, 0.08);
}

.route-map-legend__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.route-map-legend__dot--spot {
  background: #2f7bff;
}

.route-map-legend__dot--hotel {
  background: #0ea5a4;
}

.route-map-legend__dot--nearby {
  background: #94a3b8;
}

.route-map-legend__line {
  width: 20px;
  height: 0;
  border-top: 3px dashed #2f7bff;
}

:deep(.route-map-marker) {
  position: relative;
  display: inline-flex;
  align-items: flex-start;
  justify-content: center;
  width: 60px;
  height: 74px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
  transform-origin: 50% 100%;
  transition: transform 180ms ease, filter 180ms ease;
}

:deep(.route-map-marker:hover),
:deep(.route-map-marker.is-hover) {
  transform: translateY(-3px) scale(1.04);
}

:deep(.route-map-marker.is-active) {
  transform: translateY(-5px) scale(1.08);
  filter: drop-shadow(0 12px 18px rgba(35, 94, 206, 0.22));
}

:deep(.route-map-marker__balloon) {
  position: relative;
  display: block;
  width: 22px;
  height: 32px;
  margin-top: 16px;
  border-radius: 14px 14px 14px 2px;
  background: linear-gradient(180deg, #74b8ff 0%, #4b95f2 56%, #3076d6 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.45),
    0 6px 12px rgba(27, 86, 170, 0.28);
  transform: rotate(-45deg);
}

:deep(.route-map-marker__shine) {
  position: absolute;
  left: 3px;
  top: 4px;
  width: 8px;
  height: 14px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0));
  transform: rotate(18deg);
}

:deep(.route-map-marker__stem) {
  position: absolute;
  left: 30px;
  top: 44px;
  width: 2px;
  height: 16px;
  border-radius: 999px;
  background: rgba(38, 106, 214, 0.9);
}

:deep(.route-map-marker__badge) {
  position: absolute;
  left: 34px;
  top: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  padding: 0 7px;
  border-radius: 3px;
  border: 2px solid #3f86df;
  background: linear-gradient(180deg, #65db59 0%, #36bc43 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 800;
  line-height: 1;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.38),
    0 4px 10px rgba(48, 107, 57, 0.18);
}

:deep(.route-map-marker__badge::before) {
  content: '';
  position: absolute;
  inset: -4px;
  border: 1px solid rgba(255, 255, 255, 0.92);
  border-radius: 4px;
}

:deep(.route-map-marker--hotel .route-map-marker__balloon) {
  background: linear-gradient(180deg, #45c7c4 0%, #1fb7b3 56%, #129894 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.42),
    0 6px 12px rgba(11, 112, 108, 0.24);
}

:deep(.route-map-marker--hotel .route-map-marker__stem) {
  background: rgba(15, 118, 110, 0.9);
}

:deep(.route-map-marker--hotel .route-map-marker__badge) {
  border-color: #0f9c96;
  background: linear-gradient(180deg, #34d399 0%, #10b981 100%);
}

@media (max-width: 720px) {
  .route-map-stage,
  .route-map-canvas {
    min-height: 420px;
  }

  :deep(.route-map-marker) {
    transform: scale(0.92);
    transform-origin: 50% 100%;
  }

  .route-map-controls {
    left: 12px;
    right: 12px;
    bottom: 12px;
  }

  .route-map-legend {
    max-width: 100%;
  }
}
</style>

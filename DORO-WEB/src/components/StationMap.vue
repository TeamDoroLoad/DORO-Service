<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import type { GeoPoint, NearbyStation } from '../types'

const props = defineProps<{
  center: GeoPoint
  confirmedPin: GeoPoint | null
  radiusMeters: number
  stations: NearbyStation[]
}>()

const emit = defineEmits<{
  (e: 'pick', point: GeoPoint): void
  (e: 'select', stationId: number): void
}>()

const mapEl = ref<HTMLDivElement | null>(null)
let map: Tmapv2.Map | null = null
let confirmedMarker: Tmapv2.Marker | null = null
let radiusCircle: Tmapv2.Circle | null = null
let stationMarkers: Tmapv2.Marker[] = []

// index.html의 TMAP SDK <script>가 이 모듈보다 먼저(동기적으로) 로드되므로 이 시점엔 항상 존재한다고 가정한다.
// 다만 appKey·도메인 등록 문제로 로드에 실패하는 경우를 대비해 onMounted에서 별도로 존재 여부를 확인한다.
const Tmap = window.Tmapv2

function toLatLng(point: GeoPoint) {
  return new Tmap!.LatLng(point.latitude, point.longitude)
}

// 별도 이미지 자산 없이, 기존 placeholder와 같은 색상 팔레트로 원형 핀 아이콘을 SVG data URI로 생성한다.
function pinIcon(color: string, size: number): string {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" fill="${color}" stroke="white" stroke-width="2"/></svg>`
  return `data:image/svg+xml,${encodeURIComponent(svg)}`
}

function clearStationMarkers() {
  stationMarkers.forEach((marker) => marker.setMap(null))
  stationMarkers = []
}

function renderStationMarkers() {
  if (!map) return
  clearStationMarkers()
  stationMarkers = props.stations.map((s) => {
    const color = !s.compatible ? '#86888C' : s.availableCompatibleChargerCount > 0 ? '#006FCF' : '#00175A'
    const marker = new Tmap!.Marker({
      position: toLatLng(s.location),
      map: map!,
      icon: pinIcon(color, 22),
      iconSize: new Tmap!.Size(22, 22),
      title: s.stationName,
    })
    // 지도 검색을 새로 실행하지 않고, 목록에서 이 충전소를 최상단으로 올려 펼친다.
    marker.addListener('click', () => emit('select', s.stationId))
    return marker
  })
}

function renderConfirmedMarker() {
  if (!map) return
  confirmedMarker?.setMap(null)
  confirmedMarker = props.confirmedPin
    ? new Tmap!.Marker({
        position: toLatLng(props.confirmedPin),
        map,
        icon: pinIcon('#00175A', 28),
        iconSize: new Tmap!.Size(28, 28),
        title: '검색 기준 위치',
      })
    : null
}

function renderRadiusCircle() {
  if (!map) return
  radiusCircle?.setMap(null)
  radiusCircle = new Tmap!.Circle({
    center: toLatLng(props.center),
    radius: props.radiusMeters,
    map,
    strokeColor: '#006FCF',
    strokeWeight: 2,
    strokeOpacity: 0.6,
    fillColor: '#006FCF',
    fillOpacity: 0.06,
  })
}

onMounted(() => {
  if (!mapEl.value || !Tmap) {
    console.error('TMAP SDK를 불러오지 못했습니다. VITE_TMAP_APP_KEY와 TMAP 콘솔의 서비스 도메인 등록 상태를 확인하세요.')
    return
  }
  map = new Tmap.Map(mapEl.value, { center: toLatLng(props.center), zoom: 15 })
  // 이벤트 이름 대소문자는 실제 SDK 응답으로 검증되지 않았다. 클릭이 안 잡히면 'Click'으로 바꿔볼 것.
  map.addListener('click', (evt) => {
    emit('pick', { latitude: evt.latLng.lat(), longitude: evt.latLng.lng() })
  })
  renderRadiusCircle()
  renderConfirmedMarker()
  renderStationMarkers()
})

onUnmounted(() => {
  clearStationMarkers()
  confirmedMarker?.setMap(null)
  radiusCircle?.setMap(null)
})

watch(
  () => props.center,
  (point) => {
    map?.setCenter(toLatLng(point))
    renderRadiusCircle()
  },
)
watch(() => props.confirmedPin, renderConfirmedMarker)
watch(() => props.stations, renderStationMarkers)
watch(() => props.radiusMeters, renderRadiusCircle)
</script>

<template>
  <div class="map-wrap">
    <div ref="mapEl" class="map"></div>
    <div class="badge map-badge radius-badge">반경 {{ Math.round(radiusMeters / 1000) }}km</div>
    <div v-if="!confirmedPin" class="badge map-badge hint-badge">지도를 클릭해 검색 위치를 확정하세요</div>
  </div>
</template>

<style scoped>
.map-wrap {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  max-height: 480px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--doro-border);
  box-shadow: 0 1px 4px rgba(0, 23, 90, 0.1);
}
.map {
  width: 100%;
  height: 100%;
}
.map-badge {
  position: absolute;
  background: rgba(255, 255, 255, 0.92);
  color: var(--doro-text);
  z-index: 10;
  pointer-events: none;
}
.radius-badge {
  top: 12px;
  right: 12px;
}
.hint-badge {
  top: 12px;
  left: 12px;
  color: var(--doro-muted-2);
  font-weight: 500;
}
</style>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { GeoPoint, NearbyStation } from '../types'
import { offsetMeters, pointFromOffset } from '../utils/geo'

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

const containerRef = ref<HTMLDivElement | null>(null)
// 3km 반경 원이 카드 안에 여유있게 들어오도록 실제 표시 반경은 검색 반경보다 25% 넓게 잡는다.
const viewRadiusMeters = computed(() => props.radiusMeters * 1.25)

function toPercent(point: GeoPoint) {
  const { dNorth, dEast } = offsetMeters(props.center, point)
  const x = 50 + (dEast / viewRadiusMeters.value) * 50
  const y = 50 - (dNorth / viewRadiusMeters.value) * 50
  return { x: Math.min(97, Math.max(3, x)), y: Math.min(97, Math.max(3, y)) }
}

const ringDiameterPercent = computed(() => (props.radiusMeters / viewRadiusMeters.value) * 100)

const markers = computed(() =>
  props.stations.map((s) => ({
    id: s.stationId,
    ...toPercent(s.location),
    color: !s.compatible ? '#86888C' : s.availableCompatibleChargerCount > 0 ? '#006FCF' : '#00175A',
  })),
)

const confirmedPercent = computed(() => (props.confirmedPin ? toPercent(props.confirmedPin) : null))

function onMapClick(evt: MouseEvent) {
  const el = containerRef.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const xPercent = ((evt.clientX - rect.left) / rect.width) * 100
  const yPercent = ((evt.clientY - rect.top) / rect.height) * 100
  const dEast = ((xPercent - 50) / 50) * viewRadiusMeters.value
  const dNorth = ((50 - yPercent) / 50) * viewRadiusMeters.value
  emit('pick', pointFromOffset(props.center, dNorth, dEast))
}

function onStationClick(stationId: number) {
  emit('select', stationId)
}
</script>

<template>
  <div ref="containerRef" class="map" @click="onMapClick">
    <div class="map-bg"></div>
    <div
      class="ring"
      :style="{ width: ringDiameterPercent + '%', height: ringDiameterPercent + '%' }"
    ></div>

    <div class="badge map-badge radius-badge">반경 {{ Math.round(radiusMeters / 1000) }}km</div>
    <div v-if="!confirmedPin" class="badge map-badge hint-badge">지도를 클릭해 검색 위치를 확정하세요</div>
    <div class="badge map-badge sdk-badge">지도 영역 자리표시 · TMAP SDK 연동 예정</div>

    <div
      v-for="m in markers"
      :key="m.id"
      class="pin station-pin"
      :style="{ left: m.x + '%', top: m.y + '%', background: m.color }"
      :title="'이 충전소를 목록에서 보기'"
      @click.stop="onStationClick(m.id)"
    ></div>

    <div
      v-if="confirmedPercent"
      class="pin confirmed-pin"
      :style="{ left: confirmedPercent.x + '%', top: confirmedPercent.y + '%' }"
    ></div>
  </div>
</template>

<style scoped>
.map {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  max-height: 480px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--doro-border);
  background: #eaf1fb;
  cursor: crosshair;
  box-shadow: 0 1px 4px rgba(0, 23, 90, 0.1);
}
.map-bg {
  position: absolute;
  inset: 0;
  background-image: repeating-linear-gradient(45deg, #eaf1fb, #eaf1fb 8px, #dce7f5 8px, #dce7f5 10px);
}
.ring {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  border: 2px dashed var(--doro-blue);
  border-radius: 50%;
  opacity: 0.55;
  pointer-events: none;
}
.map-badge {
  position: absolute;
  background: rgba(255, 255, 255, 0.92);
  color: var(--doro-text);
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
.sdk-badge {
  bottom: 12px;
  left: 12px;
  color: var(--doro-muted-2);
  font-weight: 500;
}
.pin {
  position: absolute;
  width: 18px;
  height: 18px;
  border-radius: 50% 50% 50% 0;
  transform: translate(-50%, -100%) rotate(-45deg);
  border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(0, 23, 90, 0.3);
  pointer-events: none;
}
.station-pin {
  width: 20px;
  height: 20px;
  cursor: pointer;
  pointer-events: auto;
}
.confirmed-pin {
  background: var(--doro-navy);
  width: 26px;
  height: 26px;
  z-index: 5;
}
</style>

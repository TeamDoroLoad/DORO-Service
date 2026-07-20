<script setup lang="ts">
import { ref, watch } from 'vue'
import type { NearbyStation, StationDetail } from '../types'
import { fetchStationDetail } from '../api/client'
import { formatDistance, formatRelativeTime } from '../utils/geo'
import { STATUS_META } from '../utils/statusMeta'

const props = defineProps<{
  stations: NearbyStation[]
  loading: boolean
  error: string | null
  selectedStationId: number | null
}>()
const sortBy = defineModel<'distance' | 'recommend'>('sortBy', { required: true })

const expandedId = ref<number | null>(null)
const detailCache = ref<Map<number, StationDetail>>(new Map())
const detailLoading = ref<number | null>(null)

function availabilityMeta(station: NearbyStation) {
  if (!station.compatible) return STATUS_META.UNKNOWN
  if (station.availableCompatibleChargerCount > 0) return STATUS_META.AVAILABLE
  if (station.totalCompatibleChargerCount > 0) return STATUS_META.CHARGING
  return STATUS_META.OUT_OF_SERVICE
}

async function expand(stationId: number) {
  expandedId.value = stationId
  if (detailCache.value.has(stationId)) return
  detailLoading.value = stationId
  try {
    const detail = await fetchStationDetail(stationId)
    detailCache.value.set(stationId, detail)
  } finally {
    detailLoading.value = null
  }
}

function toggle(stationId: number) {
  if (expandedId.value === stationId) {
    expandedId.value = null
    return
  }
  expand(stationId)
}

// 지도에서 충전소 핀을 클릭하면 그 카드를 목록에서 펼쳐 보여준다(목록 자체를 최상단으로 올리는 것은 정렬 로직에서 처리).
watch(
  () => props.selectedStationId,
  (stationId) => {
    if (stationId != null) expand(stationId)
  },
)
</script>

<template>
  <div class="toolbar">
    <div class="count">
      <span>{{ loading ? '검색 중...' : `${stations.length}개 충전소` }}</span>
      <span class="sep">·</span>
      <span>반경 3km 이내</span>
    </div>
    <div class="sort-buttons">
      <button :class="{ active: sortBy === 'distance' }" @click="sortBy = 'distance'">거리순</button>
      <button :class="{ active: sortBy === 'recommend' }" @click="sortBy = 'recommend'">추천순</button>
    </div>
  </div>

  <p v-if="error" class="state-message error">{{ error }}</p>
  <p v-else-if="!loading && stations.length === 0" class="state-message">
    반경 3km 이내에 충전소가 없습니다. 위치를 변경해보세요.
  </p>

  <div class="list">
    <div v-for="s in stations" :key="s.stationId" class="card" :class="{ expanded: expandedId === s.stationId }">
      <div class="card-head" @click="toggle(s.stationId)">
        <div class="info">
          <div class="title-row">
            <span class="name">{{ s.stationName }}</span>
            <span class="badge" :class="s.compatible ? 'badge-blue' : 'badge-red'">
              {{ s.compatible ? '커넥터 호환' : '호환 안됨' }}
            </span>
            <span class="badge" :style="{ background: availabilityMeta(s).bg, color: availabilityMeta(s).color }">
              {{ availabilityMeta(s).label }}
            </span>
          </div>
          <div class="address">{{ s.address }}</div>
          <div class="meta-row">
            <span>거리 <b>{{ formatDistance(s.straightDistanceMeters) }}</b></span>
            <span>호환 이용가능 <b>{{ s.availableCompatibleChargerCount }}</b>/{{ s.totalCompatibleChargerCount }}기</span>
            <span>전체 <b>{{ s.totalChargerCount }}</b>기</span>
            <span>수집 <b>{{ formatRelativeTime(s.source.collectedAt) }}</b></span>
          </div>
        </div>
        <div class="chevron">{{ expandedId === s.stationId ? '⌃' : '⌄' }}</div>
      </div>

      <div v-if="expandedId === s.stationId" class="detail">
        <p v-if="detailLoading === s.stationId" class="state-message">상세 정보를 불러오는 중...</p>
        <template v-else-if="detailCache.get(s.stationId)">
          <div class="detail-grid">
            <div><span class="label">운영 시간</span><span>{{ s.operatingHours ?? '정보 없음' }}</span></div>
            <div><span class="label">사업자</span><span>{{ s.network.operatorName }} · {{ s.network.networkName }}</span></div>
            <div><span class="label">호환 커넥터</span><span>{{ s.compatibleConnectorCodes.join(', ') || '없음' }}</span></div>
            <div><span class="label">데이터 출처</span><span>{{ s.source.sourceName }}</span></div>
          </div>
          <p v-if="!s.compatible" class="warning">차량에 등록된 커넥터와 호환되는 충전기가 없어 이용이 어렵습니다.</p>
          <ul class="charger-list">
            <li v-for="c in detailCache.get(s.stationId)!.chargers" :key="c.chargerId">
              <span
                class="badge"
                :style="{ background: STATUS_META[c.currentStatus].bg, color: STATUS_META[c.currentStatus].color }"
              >
                {{ STATUS_META[c.currentStatus].label }}
              </span>
              <span class="charger-spec">{{ c.chargerType }} · {{ c.connectorCodes.join('/') }} · {{ c.maxPowerKw ?? '-' }}kW</span>
              <span class="muted">{{ formatRelativeTime(c.sourceUpdatedAt) }} 갱신</span>
            </li>
          </ul>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  margin: 14px 0;
}
.count {
  font-size: 14px;
  color: var(--doro-muted-2);
}
.count b {
  color: var(--doro-text);
}
.sep {
  margin: 0 6px;
}
.sort-buttons {
  display: flex;
  gap: 8px;
}
.sort-buttons button {
  background: #fff;
  color: var(--doro-muted-2);
  border: 1px solid var(--doro-border);
  border-radius: 6px;
  padding: 7px 14px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}
.sort-buttons button.active {
  background: var(--doro-blue);
  color: #fff;
  border-color: var(--doro-blue);
}
.state-message {
  font-size: 13.5px;
  color: var(--doro-muted-2);
  background: #fff;
  border: 1px dashed var(--doro-border);
  border-radius: 10px;
  padding: 18px;
  text-align: center;
}
.state-message.error {
  color: var(--doro-red);
  border-color: var(--doro-red-bg);
  background: var(--doro-red-bg);
}
.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.card {
  background: #fff;
  border: 1px solid var(--doro-border);
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 23, 90, 0.08);
  overflow: hidden;
}
.card.expanded {
  border-color: var(--doro-blue);
}
.card-head {
  padding: 16px 18px;
  cursor: pointer;
  display: flex;
  gap: 14px;
  align-items: flex-start;
}
.info {
  flex: 1;
  min-width: 0;
}
.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.name {
  font-size: 15.5px;
  font-weight: 600;
}
.address {
  font-size: 12.5px;
  color: var(--doro-muted);
  margin-top: 3px;
}
.meta-row {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--doro-muted-2);
}
.meta-row b {
  color: var(--doro-text);
}
.chevron {
  font-size: 20px;
  color: var(--doro-muted);
  flex: none;
}
.detail {
  border-top: 1px solid #ecedee;
  background: var(--doro-bg);
  padding: 16px 18px 18px;
}
.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 10px 20px;
  margin-bottom: 12px;
}
.detail-grid .label {
  display: block;
  font-size: 11px;
  color: var(--doro-muted);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}
.warning {
  font-size: 12.5px;
  color: var(--doro-red);
  background: var(--doro-red-bg);
  border-radius: 6px;
  padding: 8px 10px;
  margin-bottom: 12px;
}
.charger-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.charger-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  background: #fff;
  border: 1px solid var(--doro-border);
  border-radius: 8px;
  padding: 8px 12px;
}
.charger-spec {
  flex: 1;
}
.muted {
  color: var(--doro-muted);
  font-size: 11.5px;
}
</style>

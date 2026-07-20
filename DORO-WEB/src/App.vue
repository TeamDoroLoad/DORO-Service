<script setup lang="ts">
import { computed } from 'vue'
import AddressSearchBar from './components/AddressSearchBar.vue'
import StationMap from './components/StationMap.vue'
import StationList from './components/StationList.vue'
import VehicleSettingsPanel from './components/VehicleSettingsPanel.vue'
import { useVehicleSettings } from './composables/useVehicleSettings'
import { useStationSearch } from './composables/useStationSearch'
import type { GeocodeCandidate, GeoPoint } from './types'

const { settings } = useVehicleSettings()
const isConfigured = computed(() => settings.value !== null)
const vehicleTrimId = computed(() => settings.value?.vehicleTrimId ?? null)
const connectorCodes = computed(() => settings.value?.connectorCodes ?? [])

const search = useStationSearch(
  () => vehicleTrimId.value,
  () => connectorCodes.value,
)

// 상단 주소 검색: 후보를 고르면 그 좌표로 바로 검색을 확정한다(추가 지도 클릭 불필요).
function onAddressSelect(candidate: GeocodeCandidate) {
  search.confirmAt({ latitude: candidate.latitude, longitude: candidate.longitude })
}

// 지도 클릭: 빈 영역을 클릭해 내 위치를 직접 지정하고 그 자리에서 검색한다.
function onMapPick(point: GeoPoint) {
  search.confirmAt(point)
}

// 지도의 충전소 핀 클릭: 새 검색을 실행하지 않고, 목록에서 해당 충전소를 최상단으로 올려 펼친다.
function onStationSelect(stationId: number) {
  search.selectStation(stationId)
}

// 차량정보 저장 버튼: 아직 한 번도 위치를 확정한 적 없으면 기본 좌표(내 위치) 기준으로 첫 검색을 실행한다.
// 이미 위치가 확정된 상태라면 vehicleTrimId/connectorCodes watch가 자동으로 재조회하므로 여기서는 아무 것도 하지 않는다.
function onVehicleSaved() {
  if (!search.confirmedPin.value) {
    search.confirmAt(search.mapCenter.value)
  }
}
</script>

<template>
  <div class="layout">
    <header class="header">
      <div class="brand">
        <div class="logo">D</div>
        <span class="brand-name">DORO Load</span>
        <span class="brand-sub">전기차 충전소 통합 검색</span>
      </div>
      <AddressSearchBar v-if="isConfigured" @select="onAddressSelect" />
    </header>

    <main class="main">
      <section class="left">
        <div v-if="!isConfigured" class="placeholder">
          <p class="placeholder-title">차량 정보를 먼저 저장해주세요</p>
          <p class="placeholder-desc">
            오른쪽에서 차량 모델·커넥터를 선택하고 저장하면 도로명 주소 검색과 반경 3km 이내 충전소 조회를
            사용할 수 있습니다.
          </p>
        </div>
        <template v-else>
          <StationMap
            :center="search.mapCenter.value"
            :confirmed-pin="search.confirmedPin.value"
            :radius-meters="search.radiusMeters"
            :stations="search.stations.value"
            @pick="onMapPick"
            @select="onStationSelect"
          />
          <StationList
            v-model:sort-by="search.sortBy.value"
            :stations="search.sortedStations.value"
            :loading="search.loading.value"
            :error="search.error.value"
            :selected-station-id="search.selectedStationId.value"
          />
        </template>
      </section>

      <VehicleSettingsPanel @saved="onVehicleSaved" />
    </main>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.header {
  height: 64px;
  flex: none;
  background: var(--doro-navy);
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 28px;
  position: sticky;
  top: 0;
  z-index: 20;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: none;
}
.logo {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: var(--doro-blue);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 15px;
}
.brand-name {
  color: #fff;
  font-weight: 600;
  font-size: 17px;
}
.brand-sub {
  color: #b7c3d9;
  font-size: 12px;
}
.main {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 20px;
  padding: 20px 28px 40px;
  max-width: 1560px;
  width: 100%;
  margin: 0 auto;
}
.left {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}
.placeholder {
  flex: 1;
  min-height: 420px;
  border-radius: 12px;
  border: 1px dashed var(--doro-border);
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px;
  text-align: center;
}
.placeholder-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}
.placeholder-desc {
  font-size: 13.5px;
  color: var(--doro-muted-2);
  line-height: 1.6;
  max-width: 360px;
  margin: 0;
}
@media (max-width: 900px) {
  .main {
    grid-template-columns: 1fr;
  }
}
</style>

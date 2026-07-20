import { computed, ref, watch } from 'vue'
import type { GeoPoint, NearbyStation } from '../types'
import { fetchNearbyStations } from '../api/client'

const DEFAULT_CENTER: GeoPoint = { latitude: 37.5665, longitude: 126.978 } // 서울시청, 실제 위치 미확정 시 기본 중심
const RADIUS_METERS = 3000

function recommendScore(station: NearbyStation): number {
  let score = station.compatible ? 40 : 0
  score += Math.min(30, station.availableCompatibleChargerCount * 10)
  score += Math.max(0, 20 - station.straightDistanceMeters / 150)
  return score
}

/**
 * 지도 검색은 사용자의 명시적 동작(주소 검색, 지도 클릭, 차량정보 저장)에서만 실행되며,
 * 새로고침·브라우저 재시작 시점에는 confirmedPin이 항상 null로 시작해 자동으로 실행되지 않는다.
 */
export function useStationSearch(vehicleTrimId: () => number | null, connectorCodes: () => string[]) {
  const mapCenter = ref<GeoPoint>(DEFAULT_CENTER)
  const confirmedPin = ref<GeoPoint | null>(null)
  const stations = ref<NearbyStation[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const sortBy = ref<'distance' | 'recommend'>('recommend')
  // 지도의 충전소 핀을 클릭했을 때 목록 최상단으로 올리고 펼칠 대상
  const selectedStationId = ref<number | null>(null)

  async function search() {
    if (!confirmedPin.value) return
    loading.value = true
    error.value = null
    try {
      const result = await fetchNearbyStations({
        latitude: confirmedPin.value.latitude,
        longitude: confirmedPin.value.longitude,
        radiusMeters: RADIUS_METERS,
        vehicleTrimId: vehicleTrimId(),
        connectorCodes: connectorCodes(),
      })
      stations.value = result.stations
    } catch (e) {
      error.value = e instanceof Error ? e.message : '충전소 조회 중 오류가 발생했습니다.'
      stations.value = []
    } finally {
      loading.value = false
    }
  }

  async function confirmAt(point: GeoPoint) {
    confirmedPin.value = point
    mapCenter.value = point
    await search()
  }

  function selectStation(stationId: number) {
    selectedStationId.value = stationId
  }

  // 차량·커넥터 설정이 바뀌면 같은 위치를 기준으로 호환성을 다시 평가한다.
  watch(vehicleTrimId, () => {
    if (confirmedPin.value) search()
  })
  watch(connectorCodes, () => {
    if (confirmedPin.value) search()
  })

  const sortedStations = computed(() => {
    const list = [...stations.value]
    if (sortBy.value === 'distance') {
      list.sort((a, b) => a.straightDistanceMeters - b.straightDistanceMeters)
    } else {
      list.sort((a, b) => recommendScore(b) - recommendScore(a))
    }
    // 지도에서 선택한 충전소는 정렬 기준과 무관하게 항상 최상단에 노출한다.
    const selectedIndex = list.findIndex((s) => s.stationId === selectedStationId.value)
    if (selectedIndex > 0) {
      const [selected] = list.splice(selectedIndex, 1)
      list.unshift(selected)
    }
    return list
  })

  return {
    mapCenter,
    confirmedPin,
    stations,
    sortedStations,
    loading,
    error,
    sortBy,
    selectedStationId,
    radiusMeters: RADIUS_METERS,
    confirmAt,
    selectStation,
  }
}

import type {
  ChargerDetail,
  ChargingNetwork,
  ConnectorType,
  GeocodeCandidate,
  GeoPoint,
  NearbyStation,
  StationDetail,
  VehicleTrim,
} from '../types'
import { pointFromOffset } from '../utils/geo'
import { hashString, mulberry32 } from '../utils/seededRandom'

export const MOCK_CONNECTOR_TYPES: ConnectorType[] = [
  { connectorCode: 'DC_COMBO', connectorName: 'DC 콤보', chargeMode: 'DC' },
  { connectorCode: 'DC_CHADEMO', connectorName: 'DC 차데모', chargeMode: 'DC' },
  { connectorCode: 'AC_SLOW', connectorName: 'AC 완속(5핀)', chargeMode: 'AC' },
  { connectorCode: 'AC3', connectorName: 'AC 3상', chargeMode: 'AC' },
  { connectorCode: 'NACS', connectorName: 'NACS(테슬라)', chargeMode: 'DC' },
]

export const MOCK_VEHICLE_TRIMS: VehicleTrim[] = [
  { vehicleTrimId: 101, brand: '현대', modelName: '아이오닉5', trimName: '롱레인지 AWD', batteryKwh: 84, normalRangeKm: 458, coldRangeKm: 380, maxAcKw: 11, maxDcKw: 220, connectorCodes: ['DC_COMBO', 'AC_SLOW'] },
  { vehicleTrimId: 102, brand: '현대', modelName: '아이오닉6', trimName: '스탠다드 2WD', batteryKwh: 63, normalRangeKm: 401, coldRangeKm: 340, maxAcKw: 11, maxDcKw: 140, connectorCodes: ['DC_COMBO', 'AC_SLOW'] },
  { vehicleTrimId: 103, brand: '기아', modelName: 'EV6', trimName: 'GT-Line AWD', batteryKwh: 77.4, normalRangeKm: 475, coldRangeKm: 400, maxAcKw: 11, maxDcKw: 240, connectorCodes: ['DC_COMBO', 'AC_SLOW'] },
  { vehicleTrimId: 104, brand: '기아', modelName: 'EV9', trimName: '롱레인지 2WD', batteryKwh: 99.8, normalRangeKm: 501, coldRangeKm: 430, maxAcKw: 11, maxDcKw: 210, connectorCodes: ['DC_COMBO', 'AC_SLOW'] },
  { vehicleTrimId: 105, brand: '테슬라', modelName: '모델Y', trimName: 'RWD', batteryKwh: 60, normalRangeKm: 350, coldRangeKm: 290, maxAcKw: 11, maxDcKw: 170, connectorCodes: ['NACS'] },
  { vehicleTrimId: 106, brand: '제네시스', modelName: 'GV60', trimName: '스탠다드 AWD', batteryKwh: 77.4, normalRangeKm: 451, coldRangeKm: 380, maxAcKw: 11, maxDcKw: 235, connectorCodes: ['DC_COMBO', 'AC_SLOW'] },
]

export const MOCK_CHARGING_NETWORKS: ChargingNetwork[] = [
  { networkId: 21, networkName: '환경부 급속', operatorName: '한국환경공단' },
  { networkId: 22, networkName: '환경부 완속', operatorName: '한국환경공단' },
  { networkId: 31, networkName: '로드히어로', operatorName: '(주)로드히어로' },
  { networkId: 41, networkName: '기타 민간 사업자', operatorName: '기타' },
]

const SEOUL_CITY_HALL: GeoPoint = { latitude: 37.5665, longitude: 126.978 }

export function mockGeocode(query: string): GeocodeCandidate[] {
  const trimmed = query.trim()
  if (trimmed.length < 2) return []
  const seed = hashString(trimmed)
  const candidateCount = 1 + Math.floor(mulberry32(seed)() * 2)
  return Array.from({ length: candidateCount }, (_, i) => {
    const rand = mulberry32(seed + i * 97)
    const point = pointFromOffset(SEOUL_CITY_HALL, (rand() - 0.5) * 8000, (rand() - 0.5) * 8000)
    return {
      formattedAddress: i === 0 ? trimmed : `${trimmed} 인근`,
      roadAddress: trimmed,
      jibunAddress: null,
      latitude: point.latitude,
      longitude: point.longitude,
    }
  })
}

const STATION_NAME_POOL = [
  '공영주차장 충전소',
  '해피스테이션 충전소',
  '스마트파크 충전소',
  '그린모빌리티 충전소',
  '시티타워 지하주차장 충전소',
  '역전 환승주차장 충전소',
  '테크노밸리 충전소',
]

const CONNECTOR_POOL = ['DC_COMBO', 'AC_SLOW', 'DC_CHADEMO', 'NACS']

function buildStation(index: number, center: GeoPoint, radiusMeters: number, seed: number): NearbyStation {
  const rand = mulberry32(seed + index * 733)
  const distance = 150 + rand() * (radiusMeters - 150)
  const angle = rand() * Math.PI * 2
  const location = pointFromOffset(center, Math.cos(angle) * distance, Math.sin(angle) * distance)
  const network = MOCK_CHARGING_NETWORKS[index % MOCK_CHARGING_NETWORKS.length]

  const compatibleConnectorCodes = [CONNECTOR_POOL[index % CONNECTOR_POOL.length]]
  if (rand() > 0.5) compatibleConnectorCodes.push('AC_SLOW')

  const totalChargerCount = 2 + Math.floor(rand() * 4)
  const totalCompatibleChargerCount = Math.max(1, Math.round(totalChargerCount * (0.4 + rand() * 0.6)))
  const availableCompatibleChargerCount = Math.round(totalCompatibleChargerCount * Math.random())
  const latestStatusUpdatedAt = new Date(Date.now() - Math.round(Math.random() * 15 * 60000)).toISOString()

  return {
    stationId: 5000 + index,
    stationName: STATION_NAME_POOL[index % STATION_NAME_POOL.length],
    address: `서울특별시 중구 인근로 ${10 + index}`,
    location,
    straightDistanceMeters: Math.round(distance),
    operatingHours: rand() > 0.3 ? '24시간' : '06:00~24:00',
    network,
    compatible: true,
    compatibleConnectorCodes,
    availableCompatibleChargerCount,
    totalCompatibleChargerCount,
    totalChargerCount,
    latestStatusUpdatedAt,
    source: { sourceName: '한국환경공단_전기자동차 충전소 정보', collectedAt: latestStatusUpdatedAt },
  }
}

/** 기준좌표를 seed로 고정해 배치는 안정적으로, 상태(가용 대수)는 매 조회마다 실시간처럼 변하도록 구성 */
export function mockNearby(center: GeoPoint, radiusMeters: number, vehicleConnectorCodes: string[]): NearbyStation[] {
  const seed = Math.round(center.latitude * 1000) + Math.round(center.longitude * 1000)
  return Array.from({ length: 7 }, (_, i) => buildStation(i, center, radiusMeters, seed)).map((station) => ({
    ...station,
    compatible:
      vehicleConnectorCodes.length === 0 ||
      station.compatibleConnectorCodes.some((code) => vehicleConnectorCodes.includes(code)),
  }))
}

export function mockStationDetail(stationId: number, nearby: NearbyStation[]): StationDetail | null {
  const station = nearby.find((s) => s.stationId === stationId)
  if (!station) return null

  const chargers: ChargerDetail[] = Array.from({ length: station.totalChargerCount }, (_, i) => {
    const rand = mulberry32(stationId * 31 + i)
    const isCompatible = i < station.totalCompatibleChargerCount
    const connectorCodes = isCompatible ? station.compatibleConnectorCodes : ['AC3']
    const isAvailable = i < station.availableCompatibleChargerCount
    const isDcFast = connectorCodes.includes('DC_COMBO') || connectorCodes.includes('DC_CHADEMO')
    return {
      chargerId: stationId * 10 + i,
      externalChargerId: `KECO:${stationId}:${i + 1}`,
      chargerType: isDcFast ? 'DC_FAST' : 'AC_SLOW',
      maxPowerKw: connectorCodes.includes('DC_COMBO') ? 100 : connectorCodes.includes('DC_CHADEMO') ? 50 : 7,
      connectorCodes,
      currentStatus: isAvailable ? 'AVAILABLE' : rand() > 0.5 ? 'CHARGING' : 'UNDER_MAINTENANCE',
      sourceUpdatedAt: station.latestStatusUpdatedAt,
      collectedAt: station.source.collectedAt,
    }
  })

  return {
    stationId: station.stationId,
    stationName: station.stationName,
    address: station.address,
    location: station.location,
    operatingHours: station.operatingHours,
    network: station.network,
    chargers,
    updatedAt: station.source.collectedAt,
  }
}

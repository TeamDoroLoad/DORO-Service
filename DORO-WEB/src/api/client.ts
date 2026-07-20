// DORO_Load_AWS_Endpoint_API_명세서.md §4~7 Endpoint에 대응하는 얇은 fetch 클라이언트.
// VITE_USE_MOCK=true(기본값)이면 mockData.ts로 응답하고, false면 VITE_API_BASE_URL로 실제 요청한다.
import type {
  ChargingNetwork,
  ConnectorType,
  GeocodeCandidate,
  GeoPoint,
  NearbyStation,
  StationDetail,
  VehicleTrim,
} from '../types'
import {
  MOCK_CHARGING_NETWORKS,
  MOCK_CONNECTOR_TYPES,
  MOCK_VEHICLE_TRIMS,
  mockGeocode,
  mockNearby,
  mockStationDetail,
} from './mockData'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'
const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

function delay<T>(value: T, ms = 250): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), ms))
}

type QueryValue = string | number | boolean | undefined | null

async function getJson<T>(path: string, params?: Record<string, QueryValue>): Promise<T> {
  const url = new URL(BASE_URL + path)
  Object.entries(params ?? {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') url.searchParams.set(key, String(value))
  })
  const res = await fetch(url.toString(), { headers: { Accept: 'application/json' } })
  if (!res.ok) {
    const body = await res.json().catch(() => null)
    throw new Error(body?.message ?? `요청이 실패했습니다. (${res.status})`)
  }
  const body = await res.json()
  return body.data as T
}

export async function fetchVehicleTrims(): Promise<VehicleTrim[]> {
  if (USE_MOCK) return delay(MOCK_VEHICLE_TRIMS)
  return getJson<VehicleTrim[]>('/vehicle-trims', { size: 100 })
}

export async function fetchConnectorTypes(): Promise<ConnectorType[]> {
  if (USE_MOCK) return delay(MOCK_CONNECTOR_TYPES)
  return getJson<ConnectorType[]>('/connector-types')
}

export async function fetchChargingNetworks(): Promise<ChargingNetwork[]> {
  if (USE_MOCK) return delay(MOCK_CHARGING_NETWORKS)
  return getJson<ChargingNetwork[]>('/charging-networks')
}

export async function geocodeAddress(query: string): Promise<GeocodeCandidate[]> {
  if (USE_MOCK) return delay(mockGeocode(query), 300)
  return getJson<GeocodeCandidate[]>('/locations/geocode', { query })
}

export interface NearbySearchParams {
  latitude: number
  longitude: number
  radiusMeters: number
  vehicleTrimId?: number | null
  connectorCodes?: string[]
}

export interface NearbySearchResult {
  center: GeoPoint
  radiusMeters: number
  stations: NearbyStation[]
}

// mock 모드에서 상세 조회(fetchStationDetail)가 최근 검색 결과를 참조하기 위한 캐시.
let lastNearbyResult: NearbyStation[] = []

export async function fetchNearbyStations(params: NearbySearchParams): Promise<NearbySearchResult> {
  if (USE_MOCK) {
    const center = { latitude: params.latitude, longitude: params.longitude }
    const stations = mockNearby(center, params.radiusMeters, params.connectorCodes ?? [])
    lastNearbyResult = stations
    return delay({ center, radiusMeters: params.radiusMeters, stations }, 400)
  }
  const result = await getJson<NearbySearchResult>('/stations/nearby', {
    latitude: params.latitude,
    longitude: params.longitude,
    radiusMeters: params.radiusMeters,
    vehicleTrimId: params.vehicleTrimId ?? undefined,
    connectorCodes: params.connectorCodes?.join(','),
  })
  lastNearbyResult = result.stations
  return result
}

export async function fetchStationDetail(stationId: number): Promise<StationDetail> {
  if (USE_MOCK) {
    const detail = mockStationDetail(stationId, lastNearbyResult)
    if (!detail) throw new Error('충전소 정보를 찾을 수 없습니다.')
    return delay(detail, 200)
  }
  return getJson<StationDetail>(`/stations/${stationId}`)
}

// DORO_Load_AWS_Endpoint_API_명세서.md §5~7 기준 (camelCase, WGS84, Meter, kW, KRW)

export type ChargeMode = 'AC' | 'DC'

export interface ConnectorType {
  connectorCode: string
  connectorName: string
  chargeMode: ChargeMode
}

export interface VehicleTrim {
  vehicleTrimId: number
  brand: string
  modelName: string
  trimName: string
  batteryKwh: number
  normalRangeKm: number
  coldRangeKm: number
  maxAcKw: number
  maxDcKw: number
  connectorCodes: string[]
}

export interface ChargingNetwork {
  networkId: number
  networkName: string
  operatorName: string
}

export interface GeoPoint {
  latitude: number
  longitude: number
}

export interface GeocodeCandidate {
  formattedAddress: string
  roadAddress: string | null
  jibunAddress: string | null
  latitude: number
  longitude: number
}

export type ChargerStatus =
  | 'AVAILABLE'
  | 'CHARGING'
  | 'OUT_OF_SERVICE'
  | 'UNDER_MAINTENANCE'
  | 'COMMUNICATION_ERROR'
  | 'RESERVED'
  | 'UNKNOWN'

export interface NearbyStation {
  stationId: number
  stationName: string
  address: string
  location: GeoPoint
  straightDistanceMeters: number
  operatingHours: string | null
  network: ChargingNetwork
  compatible: boolean
  compatibleConnectorCodes: string[]
  availableCompatibleChargerCount: number
  totalCompatibleChargerCount: number
  totalChargerCount: number
  latestStatusUpdatedAt: string | null
  source: { sourceName: string; collectedAt: string }
}

export interface ChargerDetail {
  chargerId: number
  externalChargerId: string | null
  chargerType: string
  maxPowerKw: number | null
  connectorCodes: string[]
  currentStatus: ChargerStatus
  sourceUpdatedAt: string | null
  collectedAt: string
}

export interface StationDetail {
  stationId: number
  stationName: string
  address: string
  location: GeoPoint
  operatingHours: string | null
  network: ChargingNetwork
  chargers: ChargerDetail[]
  updatedAt: string
}

// Local Storage 저장 형태 (계정 ID·비밀번호·인증 Token은 저장하지 않음)
export interface VehicleSettings {
  vehicleTrimId: number
  brand: string
  modelName: string
  trimName: string
  connectorCodes: string[]
  memberNetworkId: number | null
  isMember: boolean
  currentSoc: number
  targetSoc: number
}

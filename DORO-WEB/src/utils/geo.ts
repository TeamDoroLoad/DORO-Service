import type { GeoPoint } from '../types'

const EARTH_RADIUS_M = 6371000
const METERS_PER_DEG_LAT = 111320

/** 기준점(center)으로부터 point까지의 북쪽/동쪽 방향 거리(m). 화면 좌표 변환에 사용하는 근사식(수 km 범위에서 충분히 정확). */
export function offsetMeters(center: GeoPoint, point: GeoPoint) {
  const dNorth = (point.latitude - center.latitude) * METERS_PER_DEG_LAT
  const dEast =
    (point.longitude - center.longitude) *
    METERS_PER_DEG_LAT *
    Math.cos((center.latitude * Math.PI) / 180)
  return { dNorth, dEast }
}

/** 기준점에서 북쪽/동쪽으로 dNorth/dEast(m) 떨어진 위경도 좌표를 계산 (offsetMeters의 역연산). */
export function pointFromOffset(center: GeoPoint, dNorth: number, dEast: number): GeoPoint {
  return {
    latitude: center.latitude + dNorth / METERS_PER_DEG_LAT,
    longitude:
      center.longitude + dEast / (METERS_PER_DEG_LAT * Math.cos((center.latitude * Math.PI) / 180)),
  }
}

export function haversineMeters(a: GeoPoint, b: GeoPoint): number {
  const toRad = (deg: number) => (deg * Math.PI) / 180
  const dLat = toRad(b.latitude - a.latitude)
  const dLng = toRad(b.longitude - a.longitude)
  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(a.latitude)) * Math.cos(toRad(b.latitude)) * Math.sin(dLng / 2) ** 2
  return 2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(h))
}

export function formatDistance(meters: number): string {
  return meters < 1000 ? `${Math.round(meters)}m` : `${(meters / 1000).toFixed(1)}km`
}

export function formatRelativeTime(iso: string | null): string {
  if (!iso) return '정보 없음'
  const diffMin = Math.round((Date.now() - new Date(iso).getTime()) / 60000)
  if (diffMin < 1) return '방금 전'
  if (diffMin < 60) return `${diffMin}분 전`
  return `${Math.round(diffMin / 60)}시간 전`
}

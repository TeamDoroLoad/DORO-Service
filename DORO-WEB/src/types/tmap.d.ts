// TMAP JS SDK v2(Tmapv2) 최소 타입 선언. 공식 TypeScript 타입 배포가 없어 실제 사용하는 API만 선언한다.
// index.html에서 <script src="https://apis.openapi.sk.com/tmap/jsv2?...">로 전역 window.Tmapv2를 주입한다.
declare namespace Tmapv2 {
  class LatLng {
    constructor(lat: number, lng: number)
    lat(): number
    lng(): number
  }

  class Size {
    constructor(width: number, height: number)
  }

  interface MapOptions {
    center?: LatLng
    width?: string
    height?: string
    zoom?: number
    zoomControl?: boolean
  }

  interface MapClickEvent {
    latLng: LatLng
  }

  class Map {
    constructor(container: HTMLElement, options?: MapOptions)
    setCenter(latLng: LatLng): void
    setZoom(zoom: number): void
    // 이벤트 이름 대소문자는 TMAP 공식 문서로 검증 전이라 string으로 느슨하게 둔다(실사용 시 'click'/'Click' 확인 필요).
    addListener(eventName: string, handler: (evt: MapClickEvent) => void): void
  }

  interface MarkerOptions {
    position: LatLng
    map?: Map
    icon?: string
    iconSize?: Size
    title?: string
  }

  class Marker {
    constructor(options: MarkerOptions)
    setMap(map: Map | null): void
    addListener(eventName: string, handler: () => void): void
  }

  interface CircleOptions {
    center: LatLng
    radius: number
    map?: Map
    strokeColor?: string
    strokeWeight?: number
    strokeOpacity?: number
    fillColor?: string
    fillOpacity?: number
  }

  class Circle {
    constructor(options: CircleOptions)
    setMap(map: Map | null): void
  }
}

interface Window {
  Tmapv2?: typeof Tmapv2
}

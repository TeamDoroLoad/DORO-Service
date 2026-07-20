import { ref } from 'vue'
import type { VehicleSettings } from '../types'

// 계정 ID·비밀번호·인증 Token은 저장하지 않는다 (도로교통공사_2차_프로젝트_설명 4-1-0).
const STORAGE_KEY = 'doro_vehicle_settings'

function load(): VehicleSettings | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as VehicleSettings) : null
  } catch {
    return null
  }
}

// 모듈 스코프의 단일 ref를 공유해 별도 store 라이브러리 없이 여러 컴포넌트가 같은 상태를 본다.
const settings = ref<VehicleSettings | null>(load())

export function useVehicleSettings() {
  function save(next: VehicleSettings) {
    settings.value = next
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
  }

  function clear() {
    settings.value = null
    localStorage.removeItem(STORAGE_KEY)
  }

  return { settings, save, clear }
}

import type { ChargerStatus } from '../types'

export const STATUS_META: Record<ChargerStatus, { label: string; bg: string; color: string }> = {
  AVAILABLE: { label: '이용 가능', bg: 'var(--doro-green-bg)', color: 'var(--doro-green)' },
  CHARGING: { label: '충전 중', bg: 'var(--doro-orange-bg)', color: 'var(--doro-orange)' },
  RESERVED: { label: '예약 중', bg: 'var(--doro-orange-bg)', color: 'var(--doro-orange)' },
  UNDER_MAINTENANCE: { label: '점검 중', bg: 'var(--doro-red-bg)', color: 'var(--doro-red)' },
  OUT_OF_SERVICE: { label: '운영 중지', bg: 'var(--doro-red-bg)', color: 'var(--doro-red)' },
  COMMUNICATION_ERROR: { label: '통신 이상', bg: 'var(--doro-red-bg)', color: 'var(--doro-red)' },
  UNKNOWN: { label: '상태 미확인', bg: '#ECEDEE', color: 'var(--doro-muted-2)' },
}

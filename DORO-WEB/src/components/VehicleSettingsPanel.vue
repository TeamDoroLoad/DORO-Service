<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { ChargingNetwork, VehicleSettings, VehicleTrim } from '../types'
import { fetchChargingNetworks, fetchVehicleTrims } from '../api/client'
import { useVehicleSettings } from '../composables/useVehicleSettings'

const { settings, save } = useVehicleSettings()
const emit = defineEmits<{ (e: 'saved'): void }>()

const trims = ref<VehicleTrim[]>([])
const networks = ref<ChargingNetwork[]>([])

const form = reactive({
  brand: '',
  modelName: '',
  vehicleTrimId: null as number | null,
  memberNetworkId: null as number | null,
  isMember: false,
  currentSoc: 50,
  targetSoc: 80,
})

const brands = computed(() => [...new Set(trims.value.map((t) => t.brand))])
const models = computed(() => [...new Set(trims.value.filter((t) => t.brand === form.brand).map((t) => t.modelName))])
const trimOptions = computed(() => trims.value.filter((t) => t.brand === form.brand && t.modelName === form.modelName))
const selectedTrim = computed(() => trims.value.find((t) => t.vehicleTrimId === form.vehicleTrimId) ?? null)

onMounted(async () => {
  const [trimList, networkList] = await Promise.all([fetchVehicleTrims(), fetchChargingNetworks()])
  trims.value = trimList
  networks.value = networkList

  const current = settings.value
  if (current) {
    form.brand = current.brand
    form.modelName = current.modelName
    form.vehicleTrimId = current.vehicleTrimId
    form.memberNetworkId = current.memberNetworkId
    form.isMember = current.isMember
    form.currentSoc = current.currentSoc
    form.targetSoc = current.targetSoc
  } else if (trimList.length > 0) {
    form.brand = trimList[0].brand
    form.modelName = trimList[0].modelName
    form.vehicleTrimId = trimList[0].vehicleTrimId
  }
})

function onBrandChange() {
  form.modelName = models.value[0] ?? ''
  form.vehicleTrimId = trimOptions.value[0]?.vehicleTrimId ?? null
}
function onModelChange() {
  form.vehicleTrimId = trimOptions.value[0]?.vehicleTrimId ?? null
}

const savedFlash = ref(false)
function onSave() {
  const trim = selectedTrim.value
  if (!trim) return
  const next: VehicleSettings = {
    vehicleTrimId: trim.vehicleTrimId,
    brand: trim.brand,
    modelName: trim.modelName,
    trimName: trim.trimName,
    connectorCodes: trim.connectorCodes,
    memberNetworkId: form.memberNetworkId,
    isMember: form.isMember,
    currentSoc: form.currentSoc,
    targetSoc: form.targetSoc,
  }
  save(next)
  emit('saved')
  savedFlash.value = true
  setTimeout(() => (savedFlash.value = false), 1500)
}
</script>

<template>
  <aside class="panel">
    <section class="section">
      <h2>차량 정보</h2>
      <label class="field">
        <span>브랜드 <em>*</em></span>
        <select v-model="form.brand" @change="onBrandChange">
          <option v-for="b in brands" :key="b" :value="b">{{ b }}</option>
        </select>
      </label>
      <label class="field">
        <span>모델 <em>*</em></span>
        <select v-model="form.modelName" @change="onModelChange">
          <option v-for="m in models" :key="m" :value="m">{{ m }}</option>
        </select>
      </label>
      <label class="field">
        <span>트림 <em>*</em></span>
        <select v-model.number="form.vehicleTrimId">
          <option v-for="t in trimOptions" :key="t.vehicleTrimId" :value="t.vehicleTrimId">{{ t.trimName }}</option>
        </select>
      </label>
      <div v-if="selectedTrim" class="connectors">
        <span class="label">지원 커넥터</span>
        <span v-for="c in selectedTrim.connectorCodes" :key="c" class="badge badge-blue">{{ c }}</span>
      </div>
    </section>

    <section class="section">
      <h2>충전 플랫폼 · 회원 정보</h2>
      <label class="field">
        <span>충전 플랫폼 · 사업자</span>
        <select v-model.number="form.memberNetworkId">
          <option :value="null">선택 안함</option>
          <option v-for="n in networks" :key="n.networkId" :value="n.networkId">{{ n.networkName }}</option>
        </select>
      </label>
      <label class="switch-row">
        <span>회원 가입 여부</span>
        <input v-model="form.isMember" type="checkbox" />
      </label>
      <p class="hint">계정 ID·비밀번호·인증 Token은 저장하지 않으며, 회원 여부만 이 브라우저에 보관합니다.</p>
    </section>

    <section class="section">
      <h2>배터리 정보 (선택)</h2>
      <label class="field">
        <span>현재 배터리 잔량 <b>{{ form.currentSoc }}%</b></span>
        <input v-model.number="form.currentSoc" type="range" min="0" max="100" />
      </label>
      <label class="field">
        <span>목표 충전량 <b>{{ form.targetSoc }}%</b></span>
        <input v-model.number="form.targetSoc" type="range" min="0" max="100" />
      </label>
    </section>

    <button class="save-btn" :disabled="!selectedTrim" @click="onSave">
      {{ savedFlash ? '저장 완료' : '차량정보 저장' }}
    </button>
    <p class="hint center">저장된 정보는 이 브라우저의 Local Storage에만 보관되며, 재접속 시 자동으로 불러옵니다.</p>
  </aside>
</template>

<style scoped>
.panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.section {
  background: #fff;
  border: 1px solid var(--doro-border);
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 23, 90, 0.08);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
h2 {
  font-size: 13.5px;
  font-weight: 700;
  margin: 0;
  color: var(--doro-muted-2);
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--doro-muted-2);
}
.field em {
  color: var(--doro-red);
  font-style: normal;
}
.field select,
.field input[type='range'] {
  width: 100%;
}
select {
  border: 1px solid var(--doro-border);
  border-radius: 6px;
  padding: 9px 10px;
  font-size: 13.5px;
  background: var(--doro-bg);
  color: var(--doro-text);
}
input[type='range'] {
  accent-color: var(--doro-blue);
}
.connectors {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  font-size: 12px;
}
.connectors .label {
  color: var(--doro-muted);
  font-weight: 600;
  margin-right: 4px;
}
.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  font-weight: 600;
  color: var(--doro-muted-2);
}
.hint {
  font-size: 11.5px;
  color: var(--doro-muted);
  line-height: 1.5;
  margin: 0;
}
.hint.center {
  text-align: center;
}
.save-btn {
  background: var(--doro-blue);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 13px 0;
  font-size: 14.5px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0, 23, 90, 0.1);
}
.save-btn:disabled {
  background: var(--doro-border);
  cursor: not-allowed;
}
</style>

<script setup lang="ts">
import { ref } from 'vue'
import type { GeocodeCandidate } from '../types'
import { geocodeAddress } from '../api/client'

const emit = defineEmits<{ (e: 'select', candidate: GeocodeCandidate): void }>()

const query = ref('')
const candidates = ref<GeocodeCandidate[]>([])
const searching = ref(false)
const searched = ref(false)

async function onSearch() {
  if (query.value.trim().length < 2) return
  searching.value = true
  searched.value = true
  try {
    candidates.value = await geocodeAddress(query.value)
  } finally {
    searching.value = false
  }
}

function onPick(candidate: GeocodeCandidate) {
  emit('select', candidate)
  candidates.value = []
  searched.value = false
}
</script>

<template>
  <div class="search-wrap">
    <div class="search-box">
      <input v-model="query" placeholder="도로명 주소 검색 (예: 서울 강남대로 396)" @keyup.enter="onSearch" />
      <button @click="onSearch">검색</button>
    </div>
    <ul v-if="candidates.length" class="candidates">
      <li v-for="c in candidates" :key="c.formattedAddress + c.latitude" @click="onPick(c)">
        {{ c.formattedAddress }}
      </li>
    </ul>
    <p v-else-if="searched && !searching" class="no-result">검색 결과가 없습니다.</p>
  </div>
</template>

<style scoped>
.search-wrap {
  position: relative;
  flex: 1;
  display: flex;
  justify-content: center;
  min-width: 0;
}
.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #000c3d;
  border-radius: 8px;
  padding: 6px 8px 6px 14px;
  width: min(560px, 100%);
}
.search-box input {
  flex: 1;
  background: transparent;
  border: none;
  color: #fff;
  font-size: 14px;
  padding: 6px 0;
  outline: none;
}
.search-box input::placeholder {
  color: #8f9bc2;
}
.search-box button {
  background: var(--doro-blue);
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}
.candidates {
  position: absolute;
  top: calc(100% + 6px);
  width: min(560px, 100%);
  background: #fff;
  border: 1px solid var(--doro-border);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 23, 90, 0.15);
  list-style: none;
  margin: 0;
  padding: 6px;
  z-index: 30;
}
.candidates li {
  padding: 10px 12px;
  font-size: 13.5px;
  border-radius: 6px;
  cursor: pointer;
}
.candidates li:hover {
  background: var(--doro-blue-bg);
}
.no-result {
  position: absolute;
  top: calc(100% + 6px);
  font-size: 12.5px;
  color: #d5d9dc;
  margin: 0;
}
</style>

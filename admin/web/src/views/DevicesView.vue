<template>
  <div class="page">
    <el-menu mode="horizontal" :default-active="'devices'" router>
      <el-menu-item index="dashboard" route="/">总览</el-menu-item>
      <el-menu-item index="map" route="/map">地图</el-menu-item>
      <el-menu-item index="devices" route="/devices">设备</el-menu-item>
    </el-menu>
    <div class="body">
      <el-card>
        <el-input v-model="keyword" placeholder="设备名/标识" style="width:220px" clearable @change="() => load(true)" />
        <el-select v-model="type" placeholder="类型" clearable style="width:140px;margin-left:8px" @change="() => load(true)">
          <el-option label="WiFi" value="wifi" />
          <el-option label="蓝牙" value="bluetooth" />
        </el-select>
        <el-switch v-model="keyOnly" active-text="仅重点" style="margin-left:12px" @change="() => load(true)" />
        <el-table :data="items" class="mt" @row-click="goDetail" v-loading="loading">
          <el-table-column prop="device_key" label="标识" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="device_type" label="类型" />
          <el-table-column prop="total_count" label="出现次数" />
          <el-table-column prop="cluster_count" label="地点数" sortable>
            <template #default="{ row }">
              <el-tag v-if="row.is_key" type="danger">{{ row.cluster_count }}</el-tag>
              <span v-else>{{ row.cluster_count }}</span>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination class="mt" :total="total" :page-size="pageSize" @current-change="onPage" />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api';

const router = useRouter();
const items = ref([]);
const total = ref(0);
const pageSize = 20;
const page = ref(1);
const keyword = ref('');
const type = ref('');
const keyOnly = ref(false);
const loading = ref(false);

async function load(resetPage = false) {
  if (resetPage) page.value = 1;
  loading.value = true;
  try {
    const r = await api.getDevices({ keyword: keyword.value || undefined, type: type.value || undefined, keyOnly: keyOnly.value ? '1' : undefined, page: page.value, pageSize });
    items.value = r.items;
    total.value = r.total;
  } finally { loading.value = false; }
}

function onPage(p) { page.value = p; load(); }
function goDetail(row) { router.push(`/devices/${row.id}`); }

onMounted(load);
</script>

<style scoped>
.page { height: 100vh; }
.body { padding: 16px; }
.mt { margin-top: 16px; }
</style>

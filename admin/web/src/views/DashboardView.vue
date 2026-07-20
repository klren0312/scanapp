<template>
  <div class="page">
    <el-menu mode="horizontal" :default-active="'dashboard'" router>
      <el-menu-item index="dashboard" route="/">总览</el-menu-item>
      <el-menu-item index="map" route="/map">地图</el-menu-item>
      <el-menu-item index="devices" route="/devices">设备</el-menu-item>
    </el-menu>
    <div class="body">
      <el-row :gutter="16">
        <el-col :span="6"><el-card><el-statistic title="设备总数" :value="stats.devices" /></el-card></el-col>
        <el-col :span="6"><el-card><el-statistic title="目击总数" :value="stats.sightings" /></el-card></el-col>
        <el-col :span="6"><el-card><el-statistic title="重点设备" :value="stats.keyDevices" :value-style="{ color: '#f56c6c' }" /></el-card></el-col>
        <el-col :span="6"><el-card><el-statistic title="今日新增" :value="stats.todaySightings" /></el-card></el-col>
      </el-row>
      <el-card class="mt">
        <template #header><span>重点设备提示</span></template>
        <el-table :data="keyList" v-loading="loading" @row-click="goDetail">
          <el-table-column prop="device_key" label="标识" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="cluster_count" label="地点数">
            <template #default="{ row }"><el-tag type="danger">{{ row.cluster_count }}</el-tag></template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api';

const router = useRouter();
const stats = ref({ devices: 0, sightings: 0, keyDevices: 0, todaySightings: 0 });
const keyList = ref([]);
const loading = ref(false);

async function load() {
  loading.value = true;
  try {
    const [s, r] = await Promise.all([
      api.getStats(),
      api.getDevices({ keyOnly: '1', pageSize: 100 }),
    ]);
    stats.value = s;
    keyList.value = r.items;
  } catch (e) {
    console.error('dashboard load failed', e);
  } finally { loading.value = false; }
}

function goDetail(row) { router.push(`/devices/${row.id}`); }

onMounted(load);
</script>

<style scoped>
.page { height: 100vh; }
.body { padding: 16px; }
.mt { margin-top: 16px; }
</style>

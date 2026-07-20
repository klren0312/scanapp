<template>
  <div class="page">
    <el-menu mode="horizontal" :default-active="'devices'" router>
      <el-menu-item index="dashboard" route="/">总览</el-menu-item>
      <el-menu-item index="map" route="/map">地图</el-menu-item>
      <el-menu-item index="devices" route="/devices">设备</el-menu-item>
    </el-menu>
    <div class="body" v-loading="loading">
      <el-alert v-if="device && device.is_key" title="重点设备：在多个区域被发现" type="error" show-icon class="mt" />
      <el-descriptions :column="2" border class="mt">
        <el-descriptions-item label="标识">{{ device?.device_key }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ device?.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ device?.device_type }}</el-descriptions-item>
        <el-descriptions-item label="出现次数">{{ device?.total_count }}</el-descriptions-item>
        <el-descriptions-item label="地点数">{{ device?.cluster_count }}</el-descriptions-item>
        <el-descriptions-item label="末次发现">{{ device?.last_seen }}</el-descriptions-item>
      </el-descriptions>
      <el-card class="mt">
        <template #header><span>轨迹</span></template>
        <div ref="mapEl" class="map"></div>
      </el-card>
      <el-card class="mt">
        <template #header><span>各地点遇见次数</span></template>
        <el-table :data="clusters">
          <el-table-column prop="id" label="地点" />
          <el-table-column prop="point_count" label="次数" />
          <el-table-column label="中心坐标">
            <template #default="{ row }">{{ row.center_lat.toFixed(5) }}, {{ row.center_lng.toFixed(5) }}</template>
          </el-table-column>
          <el-table-column prop="last_seen" label="末次" />
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRoute } from 'vue-router';
import { api } from '../api';
import { loadAMap } from '../utils/amap';

const route = useRoute();
const device = ref(null);
const clusters = ref([]);
const mapEl = ref(null);
const loading = ref(false);
let map = null;

onMounted(async () => {
  loading.value = true;
  try {
    const data = await api.getDevice(route.params.id);
    device.value = data.device;
    clusters.value = data.clusters;
    const traj = await api.getTrajectory(route.params.id);
    const AMap = await loadAMap();
    map = new AMap.Map(mapEl.value, { zoom: 12, center: traj.points.length ? [traj.points[0].lng, traj.points[0].lat] : [116.397, 39.908] });
    if (traj.points.length) {
      new AMap.Polyline({ path: traj.points.map((p) => [p.lng, p.lat]), strokeColor: '#f56c6c', strokeWeight: 4 }).setMap(map);
      traj.points.forEach((p) => new AMap.Marker({ position: [p.lng, p.lat] }).setMap(map));
    }
  } finally { loading.value = false; }
});

onBeforeUnmount(() => {
  if (map) { try { map.destroy(); } catch (e) {} map = null; }
});
</script>

<style scoped>
.page { height: 100vh; }
.body { padding: 16px; }
.mt { margin-top: 16px; }
.map { height: 360px; }
</style>

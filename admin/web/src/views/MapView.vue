<template>
  <div class="page">
    <el-card class="filter">
      <el-input v-model="keyword" placeholder="设备名/标识搜索" style="width:220px" clearable />
      <el-select v-model="type" placeholder="类型" clearable style="width:140px;margin-left:8px">
        <el-option label="WiFi" value="wifi" />
        <el-option label="蓝牙" value="bluetooth" />
      </el-select>
      <el-switch v-model="keyOnly" active-text="仅看重点" style="margin-left:12px" @change="refresh" />
      <el-button type="primary" style="margin-left:12px" @click="refresh">刷新</el-button>
    </el-card>
    <div ref="mapEl" class="map"></div>
    <el-drawer v-model="drawer" title="设备详情" size="40%">
      <template v-if="detail">
        <el-alert v-if="detail.device.is_key" title="重点设备：在多个区域被发现" type="error" show-icon />
        <el-descriptions :column="1" border>
          <el-descriptions-item label="标识">{{ detail.device.device_key }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ detail.device.name }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ detail.device.device_type }}</el-descriptions-item>
          <el-descriptions-item label="出现次数">{{ detail.device.total_count }}</el-descriptions-item>
          <el-descriptions-item label="地点数">{{ detail.device.cluster_count }}</el-descriptions-item>
        </el-descriptions>
        <el-divider>轨迹</el-divider>
        <div ref="trajEl" class="traj-map"></div>
        <el-divider>各地点遇见次数</el-divider>
        <el-table :data="detail.clusters">
          <el-table-column prop="id" label="地点" />
          <el-table-column prop="point_count" label="次数" />
          <el-table-column label="中心">
            <template #default="{ row }">{{ row.center_lat.toFixed(5) }}, {{ row.center_lng.toFixed(5) }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import { api } from '../api';
import { loadAMap } from '../utils/amap';

const mapEl = ref(null);
const trajEl = ref(null);
const keyword = ref('');
const type = ref('');
const keyOnly = ref(false);
const drawer = ref(false);
const detail = ref(null);
let map = null;
let markers = [];
let trajMap = null;

async function refresh() {
  const { points } = await api.getMapPoints({
    keyword: keyword.value || undefined,
    type: type.value || undefined,
    keyOnly: keyOnly.value ? '1' : undefined,
  });
  if (!map) return;
  markers.forEach((m) => map.remove(m));
  markers = [];
  points.forEach((p) => {
    const color = p.isKey ? '#f56c6c' : p.type === 'wifi' ? '#409eff' : '#67c23a';
    const m = new window.AMap.Marker({
      position: [p.lng, p.lat],
      title: p.key,
      zIndex: p.isKey ? 100 : 1,
      icon: new window.AMap.Icon({
        size: new window.AMap.Size(14, 14),
        image: colorDot(color),
        imageSize: new window.AMap.Size(14, 14),
      }),
    });
    m.setMap(map);
    m.on('click', () => openDetail(p.id));
    markers.push(m);
  });
}

// 14x14 solid-color PNG via data URL, used as a colored dot marker icon (AMap.Icon doesn't accept
// raw colors). Returns a red/blue/green dot for wifi/bluetooth/key-device highlighting.
function colorDot(color) {
  const c = color.replace('#', '');
  const r = parseInt(c.slice(0, 2), 16);
  const g = parseInt(c.slice(2, 4), 16);
  const b = parseInt(c.slice(4, 6), 16);
  const svg = `<svg xmlns='http://www.w3.org/2000/svg' width='14' height='14'><circle cx='7' cy='7' r='6' fill='rgb(${r},${g},${b})'/></svg>`;
  return 'data:image/svg+xml;base64,' + btoa(svg);
}

async function openDetail(id) {
  if (trajMap) { trajMap.destroy(); trajMap = null; }
  const data = await api.getDevice(id);
  const traj = await api.getTrajectory(id);
  detail.value = data;
  drawer.value = true;
  const AMap = window.AMap;
  setTimeout(() => {
    if (!trajEl.value) return;
    trajMap = new AMap.Map(trajEl.value, { zoom: 12, center: traj.points.length ? [traj.points[0].lng, traj.points[0].lat] : [116.397, 39.908] });
    if (traj.points.length) {
      new AMap.Polyline({ path: traj.points.map((pt) => [pt.lng, pt.lat]), strokeColor: '#f56c6c', strokeWeight: 4 }).setMap(trajMap);
      traj.points.forEach((pt) => new AMap.Marker({ position: [pt.lng, pt.lat] }).setMap(trajMap));
    }
  }, 50);
}

watch([keyword, type], () => refresh());

onMounted(async () => {
  const AMap = await loadAMap();
  map = new AMap.Map(mapEl.value, { zoom: 11, center: [116.397, 39.908] });
  refresh();
});

onBeforeUnmount(() => {
  markers.forEach((m) => { try { m.setMap(null); } catch (e) {} });
  markers = [];
  if (trajMap) { try { trajMap.destroy(); } catch (e) {} trajMap = null; }
  if (map) { try { map.destroy(); } catch (e) {} map = null; }
});
</script>

<style scoped>
.page { height: 100vh; display: flex; flex-direction: column; }
.filter { border-radius: 0; }
.map { flex: 1; }
.traj-map { height: 300px; }
</style>

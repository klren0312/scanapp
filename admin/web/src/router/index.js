import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../store/auth';
import LoginView from '../views/LoginView.vue';
import DashboardView from '../views/DashboardView.vue';
import MapView from '../views/MapView.vue';
import DevicesView from '../views/DevicesView.vue';
import DeviceDetailView from '../views/DeviceDetailView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView },
    { path: '/', name: 'dashboard', component: DashboardView, meta: { auth: true } },
    { path: '/map', name: 'map', component: MapView, meta: { auth: true } },
    { path: '/devices', name: 'devices', component: DevicesView, meta: { auth: true } },
    { path: '/devices/:id', name: 'device-detail', component: DeviceDetailView, meta: { auth: true } },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  if (to.meta.auth && !auth.isAuthenticated) return { name: 'login' };
  if (to.name === 'login' && auth.isAuthenticated) return { name: 'dashboard' };
  return true;
});

export default router;

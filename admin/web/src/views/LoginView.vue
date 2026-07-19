<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <h2>ScanApp 管理后台</h2>
      <el-form @submit.prevent="onSubmit">
        <el-form-item label="用户名">
          <el-input v-model="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="onSubmit">登录</el-button>
        <el-alert v-if="error" :title="error" type="error" show-icon class="mt" />
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api';
import { useAuthStore } from '../store/auth';

const router = useRouter();
const auth = useAuthStore();
const username = ref('admin');
const password = ref('');
const loading = ref(false);
const error = ref('');

async function onSubmit() {
  error.value = '';
  loading.value = true;
  try {
    const { token } = await api.login(username.value, password.value);
    auth.setToken(token);
    router.push({ name: 'dashboard' });
  } catch (e) {
    error.value = (e.response && e.response.data && e.response.data.error) || '登录失败';
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-wrap { display: flex; height: 100vh; align-items: center; justify-content: center; }
.login-card { width: 360px; }
.mt { margin-top: 12px; }
</style>

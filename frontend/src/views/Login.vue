<template>
  <div class="login-page">
    <div class="login-box">
      <div class="login-title">
        <h1>灵渠 · 数据接口平台</h1>
        <p>将 SQL 转化为 RESTful API 的低代码平台</p>
      </div>
      <el-form :model="form" @submit.prevent="doLogin" size="large">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password @keyup.enter="doLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="doLogin">登 录</el-button>
        </el-form-item>
      </el-form>
      <p class="login-tip">默认账号 admin / 123456（可在部署时通过环境变量修改）</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { authApi } from '../api/modules'

const router = useRouter()
const form = ref({ username: 'admin', password: '' })
const loading = ref(false)

async function doLogin() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const user = await authApi.login(form.value)
    sessionStorage.setItem('lingqu_user', JSON.stringify(user))
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f3b73 0%, #2d5aa8 50%, #3f7fd4 100%);
}
.login-box {
  width: 380px;
  padding: 40px 36px 24px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.25);
}
.login-title { text-align: center; margin-bottom: 28px; }
.login-title h1 { font-size: 22px; margin: 0 0 8px; color: #1f3b73; }
.login-title p { font-size: 13px; color: #909399; margin: 0; }
.login-tip { text-align: center; font-size: 12px; color: #c0c4cc; margin: 12px 0 0; }
</style>

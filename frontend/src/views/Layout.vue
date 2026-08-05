<template>
  <el-container style="height: 100%">
    <el-aside width="200px" class="aside">
      <div class="logo">灵渠 · API 平台</div>
      <el-menu :default-active="activeMenu" router background-color="#1f3b73" text-color="#c8d6ef" active-text-color="#ffffff">
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon><span>系统概览</span>
        </el-menu-item>
        <el-menu-item index="/projects">
          <el-icon><Folder /></el-icon><span>项目管理</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/datasources">
          <el-icon><Coin /></el-icon><span>数据源管理</span>
        </el-menu-item>
        <el-menu-item index="/apis">
          <el-icon><Connection /></el-icon><span>接口管理</span>
        </el-menu-item>
        <el-menu-item index="/docs">
          <el-icon><Document /></el-icon><span>接口文档</span>
        </el-menu-item>
        <el-menu-item index="/debug">
          <el-icon><Monitor /></el-icon><span>在线调试</span>
        </el-menu-item>
        <el-menu-item index="/logs">
          <el-icon><Tickets /></el-icon><span>调用日志</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/alerts">
          <el-icon><Bell /></el-icon><span>告警规则</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/users">
          <el-icon><User /></el-icon><span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="page-title">{{ pageTitle }}</span>
        <div class="user-box">
          <el-tag v-if="!isAdmin" size="small" type="info">普通用户</el-tag>
          <span class="username">{{ user?.username }}</span>
          <el-button link type="primary" @click="pwdVisible = true">修改密码</el-button>
          <el-button link type="primary" @click="doLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <!-- 修改密码 -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="420px" destroy-on-close>
      <el-form :model="pwdForm" label-width="90px">
        <el-form-item label="原密码" required>
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="确认新密码" required>
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSaving" @click="changePassword">确定</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Odometer, Folder, Coin, Connection, Document, Monitor, Tickets, Bell, User } from '@element-plus/icons-vue'
import { authApi, userApi } from '../api/modules'

const route = useRoute()
const router = useRouter()
const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta?.title || '')
const user = computed(() => {
  try {
    return JSON.parse(sessionStorage.getItem('lingqu_user') || 'null')
  } catch {
    return null
  }
})
const isAdmin = computed(() => user.value?.role === 'ADMIN')

const pwdVisible = ref(false)
const pwdSaving = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

async function changePassword() {
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  pwdSaving.value = true
  try {
    await userApi.changePassword({
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    pwdVisible.value = false
    sessionStorage.removeItem('lingqu_user')
    router.push('/login')
  } finally {
    pwdSaving.value = false
  }
}

async function doLogout() {
  await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
  try { await authApi.logout() } catch (e) { /* 忽略 */ }
  sessionStorage.removeItem('lingqu_user')
  router.push('/login')
}
</script>

<style scoped>
.aside { background: #1f3b73; }
.logo {
  height: 60px; line-height: 60px; text-align: center;
  color: #fff; font-size: 16px; font-weight: 600; letter-spacing: 1px;
}
.aside :deep(.el-menu) { border-right: none; }
.header {
  background: #fff; display: flex; align-items: center; justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08); height: 60px;
}
.page-title { font-size: 16px; font-weight: 600; color: #303133; }
.user-box { display: flex; align-items: center; gap: 10px; }
.username { color: #606266; font-size: 14px; }
.main { padding: 16px; overflow: auto; }
</style>

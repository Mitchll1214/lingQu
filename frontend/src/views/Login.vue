<template>
  <div class="login-page">
    <!-- 左侧品牌面板 -->
    <aside class="login-brand">
      <div class="brand-inner">
        <div class="brand-logo">
          <svg viewBox="0 0 64 64" aria-hidden="true">
            <rect width="64" height="64" rx="14" fill="#ffffff" opacity="0.96" />
            <path d="M10 44 L22 44 L32 18 L42 44 L54 44" fill="none" stroke="#1f3b73" stroke-width="5"
                  stroke-linecap="round" stroke-linejoin="round" />
            <circle cx="32" cy="52" r="2.6" fill="#3f7fd4" />
          </svg>
          <span class="brand-name">灵渠 · 数据接口平台</span>
        </div>
        <h1 class="brand-title">让数据库，开口说话</h1>
        <p class="brand-desc">
          将 SQL 转化为 RESTful API 的低代码平台 —— 无需编写一行后端代码，即可安全、高效地对外提供数据服务。
        </p>
        <ul class="brand-points">
          <li>SQL 一键发布为 RESTful 接口</li>
          <li>项目级鉴权与 Token 生命周期管理</li>
          <li>限流保护与全量调用日志</li>
        </ul>
      </div>
      <!-- 水纹装饰（灵渠意象） -->
      <svg class="brand-waves" viewBox="0 0 600 200" preserveAspectRatio="none" aria-hidden="true">
        <path d="M0 70 Q 75 40 150 70 T 300 70 T 450 70 T 600 70 V 200 H 0 Z" fill="#ffffff" opacity="0.05" />
        <path d="M0 100 Q 75 70 150 100 T 300 100 T 450 100 T 600 100 V 200 H 0 Z" fill="#ffffff" opacity="0.07" />
        <path d="M0 130 Q 75 100 150 130 T 300 130 T 450 130 T 600 130 V 200 H 0 Z" fill="#ffffff" opacity="0.04" />
      </svg>
    </aside>

    <!-- 右侧登录表单 -->
    <main class="login-main">
      <div class="login-box">
        <h2 class="login-title">欢迎登录</h2>
        <p class="login-sub">请使用管理员分配的账号</p>
        <el-form :model="form" @submit.prevent="doLogin" size="large">
          <el-form-item>
            <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock"
                      show-password @keyup.enter="doLogin" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="login-btn" :loading="loading" @click="doLogin">登 录</el-button>
          </el-form-item>
        </el-form>
        <p class="login-tip">默认账号 admin / 123456（可通过部署环境变量修改）</p>
      </div>
    </main>
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
}

/* ---- 左侧品牌面板 ---- */
.login-brand {
  position: relative;
  flex: 1 1 52%;
  display: flex;
  align-items: center;
  background: linear-gradient(150deg, #2d5aa8 0%, #1f3b73 62%, #16294f 100%);
  color: #fff;
  overflow: hidden;
}
.brand-inner {
  position: relative;
  z-index: 1;
  max-width: 460px;
  margin: 0 auto;
  padding: 48px 40px;
}
.brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 36px;
}
.brand-logo svg {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.25);
}
.brand-name {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.5px;
}
.brand-title {
  font-size: 30px;
  line-height: 1.3;
  font-weight: 700;
  margin: 0 0 14px;
  letter-spacing: 1px;
}
.brand-desc {
  font-size: 14px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.82);
  margin: 0 0 26px;
}
.brand-points {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.brand-points li {
  position: relative;
  padding-left: 22px;
  font-size: 13.5px;
  color: rgba(255, 255, 255, 0.9);
}
.brand-points li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  transform: translateY(-50%);
  background: #8fc0ff;
}
.brand-waves {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 200px;
  pointer-events: none;
}

/* ---- 右侧表单面板 ---- */
.login-main {
  flex: 1 1 48%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f7f9fc;
  padding: 32px;
}
.login-box {
  width: 380px;
}
.login-title {
  font-size: 24px;
  color: #1f3b73;
  margin: 0 0 6px;
  font-weight: 700;
}
.login-sub {
  font-size: 13px;
  color: #6b7a90;
  margin: 0 0 28px;
}
.login-btn {
  width: 100%;
  letter-spacing: 4px;
  height: 44px;
  font-size: 15px;
}
.login-tip {
  text-align: center;
  font-size: 12px;
  color: #a3aec0;
  margin: 16px 0 0;
}

/* ---- 响应式：窄屏隐藏品牌面板 ---- */
@media (max-width: 960px) {
  .login-brand {
    display: none;
  }
  .login-main {
    flex: 1 1 100%;
  }
}
</style>

<template>
  <div class="app-layout">
    <header class="top-nav">
      <div class="top-nav-inner">
        <div class="logo-area">
          <span class="logo-text">DataLink</span>
        </div>
        <nav class="phase-tabs">
          <div
            v-for="phase in phases"
            :key="phase.key"
            :class="['phase-tab', { active: activePhase === phase.key }]"
            @click="switchPhase(phase)"
          >
            <el-icon class="phase-icon"><component :is="phase.icon" /></el-icon>
            <span>{{ phase.label }}</span>
          </div>
        </nav>
        <div class="top-nav-right">
          <span class="env-badge">{{ currentEnv }}</span>
        </div>
      </div>
    </header>
    <div class="main-body">
      <aside class="side-nav">
        <div class="side-nav-header">
          <div class="side-nav-header-bar" :style="headerBarStyle"></div>
          <div class="side-nav-header-content">
            <el-icon class="side-nav-header-icon"><component :is="activePhaseIcon" /></el-icon>
            <span class="side-nav-header-title">{{ activePhaseLabel }}</span>
          </div>
        </div>
        <div class="side-nav-menu">
          <div
            v-for="item in currentSideMenu"
            :key="item.path"
            :class="['side-menu-item', { active: activeMenu === item.path }]"
            @click="navigateTo(item.path)"
          >
            <div class="side-menu-item-bar" :style="activeMenu === item.path ? headerBarStyle : {}"></div>
            <div class="side-menu-item-content">
              <el-icon class="side-menu-icon"><component :is="item.icon" /></el-icon>
              <div class="side-menu-text">
                <span class="side-menu-label">{{ item.label }}</span>
                <span class="side-menu-desc">{{ item.desc }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="side-nav-footer">
          <span class="side-nav-version">v1.0.0</span>
        </div>
      </aside>
      <main class="content-area">
        <div class="content-header">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ activePhaseLabel }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="content-body">
          <router-view />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Setting, Grid, EditPen, Monitor, Coin,
  Document, Upload, Connection, Files, FolderOpened,
  Share, OfficeBuilding, Clock, Tickets, TrendCharts
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const phases = [
  {
    key: 'modeling',
    label: '建模',
    icon: Grid,
    defaultPath: '/domains',
    children: [
      { path: '/domains', label: '数据域', icon: FolderOpened, desc: '定义业务数据域' },
      { path: '/processes', label: '业务过程', icon: Share, desc: '管理业务流程' },
      { path: '/wordroots', label: '词根', icon: Document, desc: '统一命名规范' },
      { path: '/modifiers', label: '修饰词', icon: Tickets, desc: '管理修饰词' },
      { path: '/timeperiods', label: '时间周期', icon: Clock, desc: '定义时间粒度' },
      { path: '/dimensions', label: '维度表', icon: Grid, desc: '维度建模设计' },
      { path: '/facttables', label: '事实表', icon: TrendCharts, desc: '事实表设计' },
      { path: '/summarytables', label: '汇总表', icon: OfficeBuilding, desc: '汇总表设计' },
      { path: '/publish-history', label: '模型发布', icon: Upload, desc: '发布历史' }
    ]
  },
  {
    key: 'development',
    label: '开发',
    icon: EditPen,
    defaultPath: '/data-development',
    children: [
      { path: '/data-development', label: '数据开发', icon: EditPen, desc: 'SQL脚本开发' },
      { path: '/script-publish', label: '脚本发布', icon: Upload, desc: '发布到生产' }
    ]
  },
  {
    key: 'assets',
    label: '资产',
    icon: Monitor,
    defaultPath: '/datasources',
    children: [
      { path: '/datasources', label: '数据源管理', icon: Connection, desc: '管理数据连接' },
      { path: '/sync-tasks', label: '同步任务', icon: Files, desc: '数据同步配置' }
    ]
  }
]

const activePhase = computed(() => {
  return route.meta.phase || 'modeling'
})

const activePhaseObj = computed(() => {
  return phases.find(p => p.key === activePhase.value) || phases[0]
})

const activePhaseLabel = computed(() => {
  return activePhaseObj.value.label
})

const activePhaseIcon = computed(() => {
  return activePhaseObj.value.icon
})

const phaseColors = {
  planning: '#1890ff',
  modeling: '#52c41a',
  development: '#faad14',
  assets: '#722ed1'
}

const headerBarStyle = computed(() => ({
  background: `linear-gradient(180deg, ${phaseColors[activePhase.value] || '#1890ff'}, ${(phaseColors[activePhase.value] || '#1890ff')}88)`
}))

const currentSideMenu = computed(() => {
  return activePhaseObj.value.children
})

const activeMenu = computed(() => route.path)

const currentTitle = computed(() => route.meta.title || 'DataLink')

const currentEnv = computed(() => {
  return 'DEV'
})

function navigateTo(path) {
  router.push(path)
}

function switchPhase(phase) {
  if (phase.key !== activePhase.value) {
    if (phase.key === 'development') {
      window.location.href = window.location.origin + '/workspace.html#/home'
    } else {
      router.push(phase.defaultPath)
    }
  }
}
</script>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.top-nav {
  height: 48px;
  background: #001529;
  flex-shrink: 0;
  z-index: 100;
}

.top-nav-inner {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 24px;
  max-width: 100%;
}

.logo-area {
  display: flex;
  align-items: center;
  margin-right: 40px;
  flex-shrink: 0;
}

.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
}

.phase-tabs {
  display: flex;
  align-items: center;
  height: 100%;
  gap: 4px;
}

.phase-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 100%;
  padding: 0 20px;
  color: #ffffffb3;
  font-size: 14px;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s ease;
  user-select: none;
}

.phase-tab:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.06);
}

.phase-tab.active {
  color: #fff;
  border-bottom-color: #1890ff;
  background: rgba(24, 144, 255, 0.1);
}

.phase-icon {
  font-size: 16px;
}

.top-nav-right {
  margin-left: auto;
  display: flex;
  align-items: center;
}

.env-badge {
  color: #52c41a;
  font-size: 12px;
  font-weight: 500;
  padding: 2px 10px;
  border: 1px solid #52c41a;
  border-radius: 2px;
}

.main-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.side-nav {
  width: 220px;
  background: #fff;
  border-right: 1px solid #e8ecf1;
  flex-shrink: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 8px rgba(0,0,0,.04);
}

.side-nav-header {
  position: relative;
  padding: 0;
  border-bottom: 1px solid #f0f0f0;
}

.side-nav-header-bar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  border-radius: 0 2px 2px 0;
}

.side-nav-header-content {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 20px;
}

.side-nav-header-icon {
  font-size: 18px;
  color: #1890ff;
}

.side-nav-header-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
}

.side-nav-menu {
  flex: 1;
  padding: 8px 0;
  overflow-y: auto;
}

.side-menu-item {
  position: relative;
  display: flex;
  align-items: stretch;
  margin: 2px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all .18s ease;
  overflow: hidden;
}

.side-menu-item:hover {
  background: #f5f7fa;
}

.side-menu-item.active {
  background: #f0f5ff;
}

.side-menu-item-bar {
  width: 0;
  flex-shrink: 0;
  transition: width .18s ease;
  border-radius: 0 2px 2px 0;
}

.side-menu-item.active .side-menu-item-bar {
  width: 3px;
}

.side-menu-item-content {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 14px;
  width: 100%;
}

.side-menu-icon {
  margin-top: 2px;
  font-size: 16px;
  flex-shrink: 0;
  color: #8c939d;
  transition: color .18s;
}

.side-menu-item.active .side-menu-icon {
  color: #1890ff;
}

.side-menu-item:hover .side-menu-icon {
  color: #555;
}

.side-menu-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.side-menu-label {
  font-size: 13px;
  color: #333;
  font-weight: 400;
  transition: color .18s;
}

.side-menu-item.active .side-menu-label {
  color: #1890ff;
  font-weight: 500;
}

.side-menu-desc {
  font-size: 11px;
  color: #b0b8c1;
  line-height: 1.3;
  white-space: normal;
}

.side-nav-footer {
  padding: 12px 20px;
  border-top: 1px solid #f0f0f0;
  text-align: center;
}

.side-nav-version {
  font-size: 11px;
  color: #c0c4cc;
}

.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f0f2f5;
}

.content-header {
  height: 40px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  padding: 0 24px;
  flex-shrink: 0;
}

.content-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}
</style>
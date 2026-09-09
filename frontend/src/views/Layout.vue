<template>
  <el-container style="height: 100vh;">
    <el-aside width="220px" style="background: #001529;">
      <div class="logo">
        <h2 style="color: #fff; text-align: center; padding: 20px 0; margin: 0;">DataNote</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        background-color="#001529"
        text-color="#ffffffb3"
        active-text-color="#1890ff"
        router
      >
        <el-sub-menu index="standard">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>规范定义</span>
          </template>
          <el-menu-item index="/domains">数据域</el-menu-item>
          <el-menu-item index="/processes">业务过程</el-menu-item>
          <el-menu-item index="/wordroots">词根</el-menu-item>
          <el-menu-item index="/modifiers">修饰词</el-menu-item>
          <el-menu-item index="/timeperiods">时间周期</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="modeling">
          <template #title>
            <el-icon><Grid /></el-icon>
            <span>数据建模</span>
          </template>
          <el-menu-item index="/dimensions">维度表</el-menu-item>
          <el-menu-item index="/facttables">事实表</el-menu-item>
          <el-menu-item index="/summarytables">汇总表</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="integration">
          <template #title>
            <el-icon><Connection /></el-icon>
            <span>数据集成</span>
          </template>
          <el-menu-item index="/datasources">数据源管理</el-menu-item>
          <el-menu-item index="/sync-tasks">同步任务</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/data-development">
          <el-icon><EditPen /></el-icon>
          <span>数据开发</span>
        </el-menu-item>
        <el-menu-item index="/publish-history">
          <el-icon><Document /></el-icon>
          <span>发布历史</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background: #fff; border-bottom: 1px solid #f0f0f0; display: flex; align-items: center;">
        <h3 style="margin: 0;">{{ currentTitle }}</h3>
      </el-header>
      <el-main style="background: #f0f2f5; padding: 20px;">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Setting, Grid, Document, Connection, EditPen } from '@element-plus/icons-vue'

const route = useRoute()
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || 'DataNote')
</script>

<style scoped>
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #ffffff1a;
}
</style>
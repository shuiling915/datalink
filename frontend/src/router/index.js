import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/domains',
    children: [
      { path: 'domains', name: 'Domains', component: () => import('@/views/DataDomain.vue'), meta: { title: '数据域' } },
      { path: 'processes', name: 'Processes', component: () => import('@/views/BusinessProcess.vue'), meta: { title: '业务过程' } },
      { path: 'wordroots', name: 'WordRoots', component: () => import('@/views/WordRoot.vue'), meta: { title: '词根' } },
      { path: 'modifiers', name: 'Modifiers', component: () => import('@/views/Modifier.vue'), meta: { title: '修饰词' } },
      { path: 'timeperiods', name: 'TimePeriods', component: () => import('@/views/TimePeriod.vue'), meta: { title: '时间周期' } },
      { path: 'dimensions', name: 'Dimensions', component: () => import('@/views/Dimension.vue'), meta: { title: '维度表' } },
      { path: 'dimensions/:id', name: 'DimensionDetail', component: () => import('@/views/DimensionDetail.vue'), meta: { title: '维度详情' } },
      { path: 'facttables', name: 'FactTables', component: () => import('@/views/FactTable.vue'), meta: { title: '事实表' } },
      { path: 'facttables/:id', name: 'FactTableDetail', component: () => import('@/views/FactTableDetail.vue'), meta: { title: '事实表详情' } },
      { path: 'summarytables', name: 'SummaryTables', component: () => import('@/views/SummaryTable.vue'), meta: { title: '汇总表' } },
      { path: 'summarytables/:id', name: 'SummaryTableDetail', component: () => import('@/views/SummaryTableDetail.vue'), meta: { title: '汇总表详情' } },
      { path: 'publish-history', name: 'PublishHistory', component: () => import('@/views/PublishHistory.vue'), meta: { title: '发布历史' } },
      { path: 'datasources', name: 'Datasources', component: () => import('@/views/Datasource.vue'), meta: { title: '数据源管理' } },
      { path: 'sync-tasks', name: 'SyncTasks', component: () => import('@/views/SyncTask.vue'), meta: { title: '同步任务' } },
      { path: 'data-development', name: 'DataDevelopment', component: () => import('@/views/DataDevelopment.vue'), meta: { title: '数据开发' } }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
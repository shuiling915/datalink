import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/domains',
    children: [
      {
        path: 'domains',
        name: 'Domains',
        component: () => import('@/views/DataDomain.vue'),
        meta: { title: '数据域', phase: 'planning' }
      },
      {
        path: 'processes',
        name: 'Processes',
        component: () => import('@/views/BusinessProcess.vue'),
        meta: { title: '业务过程', phase: 'planning' }
      },
      {
        path: 'wordroots',
        name: 'WordRoots',
        component: () => import('@/views/WordRoot.vue'),
        meta: { title: '词根', phase: 'planning' }
      },
      {
        path: 'modifiers',
        name: 'Modifiers',
        component: () => import('@/views/Modifier.vue'),
        meta: { title: '修饰词', phase: 'planning' }
      },
      {
        path: 'timeperiods',
        name: 'TimePeriods',
        component: () => import('@/views/TimePeriod.vue'),
        meta: { title: '时间周期', phase: 'planning' }
      },
      {
        path: 'dimensions',
        name: 'Dimensions',
        component: () => import('@/views/Dimension.vue'),
        meta: { title: '维度表', phase: 'modeling' }
      },
      {
        path: 'dimensions/:id',
        name: 'DimensionDetail',
        component: () => import('@/views/DimensionDetail.vue'),
        meta: { title: '维度详情', phase: 'modeling' }
      },
      {
        path: 'facttables',
        name: 'FactTables',
        component: () => import('@/views/FactTable.vue'),
        meta: { title: '事实表', phase: 'modeling' }
      },
      {
        path: 'facttables/:id',
        name: 'FactTableDetail',
        component: () => import('@/views/FactTableDetail.vue'),
        meta: { title: '事实表详情', phase: 'modeling' }
      },
      {
        path: 'summarytables',
        name: 'SummaryTables',
        component: () => import('@/views/SummaryTable.vue'),
        meta: { title: '汇总表', phase: 'modeling' }
      },
      {
        path: 'summarytables/:id',
        name: 'SummaryTableDetail',
        component: () => import('@/views/SummaryTableDetail.vue'),
        meta: { title: '汇总表详情', phase: 'modeling' }
      },
      {
        path: 'publish-history',
        name: 'PublishHistory',
        component: () => import('@/views/PublishHistory.vue'),
        meta: { title: '模型发布', phase: 'modeling' }
      },
      {
        path: 'script-publish',
        name: 'ScriptPublish',
        component: () => import('@/views/ScriptPublishHistory.vue'),
        meta: { title: '脚本发布', phase: 'development' }
      },
      {
        path: 'data-development',
        name: 'DataDevelopment',
        component: () => import('@/views/DataDevelopment.vue'),
        meta: { title: '数据开发', phase: 'development' }
      },
      {
        path: 'datasources',
        name: 'Datasources',
        component: () => import('@/views/Datasource.vue'),
        meta: { title: '数据源管理', phase: 'assets' }
      },
      {
        path: 'sync-tasks',
        name: 'SyncTasks',
        component: () => import('@/views/SyncTask.vue'),
        meta: { title: '同步任务', phase: 'assets' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
<template>
  <div class="data-dev-container">
    <!-- 左侧任务树 -->
    <div class="left-panel">
      <div class="panel-header">
        <h4>任务列表</h4>
        <el-button type="primary" size="small" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>新建
        </el-button>
      </div>
      <!-- 环境切换 -->
      <div class="env-switcher">
        <el-radio-group v-model="currentEnv" size="small" @change="switchEnv">
          <el-radio-button value="dev">
            <el-icon><EditPen /></el-icon> 开发
          </el-radio-button>
          <el-radio-button value="prod">
            <el-icon><Lock /></el-icon> 生产
          </el-radio-button>
        </el-radio-group>
      </div>
      <div class="panel-toolbar">
        <el-input v-model="searchKeyword" placeholder="搜索任务..." size="small" clearable @input="loadTasks" />
      </div>
      <div class="task-tree">
        <el-collapse v-model="activeLayers">
          <el-collapse-item v-for="layer in layers" :key="layer.key" :name="layer.key">
            <template #title>
              <div class="layer-title">
                <el-tag :type="layer.tagType" size="small">{{ layer.key }}</el-tag>
                <span class="layer-label">{{ layer.label }}</span>
                <span class="layer-count">({{ getLayerTaskCount(layer.key) }})</span>
              </div>
            </template>
            <div v-if="getLayerTasks(layer.key).length === 0" class="empty-tip">暂无任务</div>
            <div
              v-for="task in getLayerTasks(layer.key)"
              :key="task.type + '-' + task.id"
              class="task-item"
              :class="{ active: selectedTask?.id === task.id && selectedTask?.type === task.type }"
              @click="selectTask(task)"
            >
              <div class="task-info">
                <span class="task-name">{{ task.name }}</span>
                <span class="task-type-tag">{{ task.type === 'script' ? 'SQL' : '同步' }}</span>
              </div>
              <div class="task-meta">
                <el-tag v-if="task.scheduleStatus === 'online'" type="success" size="small" effect="plain">调度中</el-tag>
                <el-tag v-else type="info" size="small" effect="plain">未调度</el-tag>
                <el-tag v-if="task.environment === 'prod'" type="warning" size="small" effect="plain">生产</el-tag>
                <el-tag v-if="task.prodScriptId && task.environment === 'dev'" type="success" size="small" effect="plain">已发布</el-tag>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
    </div>

    <!-- 中间编辑区 -->
    <div class="center-panel">
      <!-- 工具栏 -->
      <div class="editor-toolbar" v-if="selectedTask">
        <div class="toolbar-left">
          <span class="current-task-name">{{ selectedTask.name }}</span>
          <el-tag size="small" :type="getLayerTagType(selectedTask.layer)">{{ selectedTask.layer }}</el-tag>
          <el-tag v-if="selectedTask.type === 'script'" size="small" type="warning">SQL</el-tag>
          <el-tag v-else size="small" type="success">同步任务</el-tag>
          <el-tag v-if="selectedTask.environment === 'prod'" size="small" type="danger">生产环境</el-tag>
          <el-tag v-else size="small" type="primary">开发环境</el-tag>
        </div>
        <div class="toolbar-right">
          <el-select v-model="selectedDatasourceId" placeholder="选择数据源" size="small" style="width: 180px;" clearable>
            <el-option v-for="ds in datasources" :key="ds.id" :label="ds.name" :value="ds.id" />
          </el-select>
          <el-button type="primary" size="small" @click="executeSql" :loading="executing" :disabled="currentEnv === 'prod'">
            <el-icon><VideoPlay /></el-icon>运行
          </el-button>
          <el-button size="small" @click="saveScript" :disabled="currentEnv === 'prod'">
            <el-icon><Check /></el-icon>保存
          </el-button>
          <el-button size="small" @click="formatSql">
            <el-icon><Operation /></el-icon>格式化
          </el-button>
          <el-button
            v-if="currentEnv === 'dev' && selectedTask?.type === 'script'"
            type="success"
            size="small"
            @click="openPublishDialog"
          >
            <el-icon><Upload /></el-icon>发布
          </el-button>
        </div>
      </div>
      <div v-else class="welcome-panel">
        <div class="welcome-content">
          <el-icon :size="64" color="#c0c4cc"><EditPen /></el-icon>
          <h2>数据开发工作台</h2>
          <p>从左侧选择一个任务开始编辑，或创建新任务</p>
          <el-tag style="margin-top: 12px;">当前环境: {{ currentEnv === 'dev' ? '开发环境' : '生产环境' }}</el-tag>
        </div>
      </div>

      <!-- SQL编辑器 -->
      <div v-if="selectedTask" class="editor-area">
        <textarea
          ref="editorRef"
          v-model="sqlContent"
          class="sql-editor"
          :readonly="currentEnv === 'prod'"
          :placeholder="currentEnv === 'prod' ? '生产环境只读...' : '输入SQL语句...'"
          @keydown="handleEditorKeydown"
        ></textarea>
      </div>

      <!-- 执行结果 -->
      <div v-if="execResults.length > 0" class="result-panel">
        <div class="result-header">
          <span>执行结果</span>
          <span class="result-meta">
            耗时: {{ execDuration }}ms | 
            总行数: {{ execTotalRows }} |
            数据源: {{ execDatasource }}
          </span>
          <el-button text size="small" @click="execResults = []">清空</el-button>
        </div>
        <div v-for="(res, idx) in execResults" :key="idx" class="result-item">
          <div v-if="res.affectedRows !== undefined" class="update-result">
            <el-tag type="success">执行成功</el-tag>
            <span>影响行数: {{ res.affectedRows }}</span>
            <div class="exec-sql">{{ res.sql }}</div>
          </div>
          <div v-else class="query-result">
            <el-tag type="primary">查询结果</el-tag>
            <span>返回 {{ res.rowCount }} 行</span>
            <div class="exec-sql">{{ res.sql }}</div>
            <el-table :data="res.rows" border size="small" max-height="300" style="margin-top: 8px;">
              <el-table-column
                v-for="(col, colIdx) in res.columns"
                :key="colIdx"
                :prop="String(colIdx)"
                :label="col"
                min-width="120"
                show-overflow-tooltip
              />
            </el-table>
          </div>
        </div>
      </div>
      <div v-if="execError" class="error-panel">
        <div class="error-header">
          <el-icon color="#f56c6c"><CircleCloseFilled /></el-icon>
          <span>执行失败</span>
        </div>
        <pre class="error-msg">{{ execError }}</pre>
      </div>
    </div>

    <!-- 右侧配置面板 -->
    <div v-if="selectedTask" class="right-panel">
      <el-tabs v-model="rightTab">
        <el-tab-pane label="基础信息" name="basic">
          <el-form :model="taskForm" label-width="80px" size="small">
            <el-form-item label="任务名称">
              <el-input v-model="taskForm.name" :disabled="currentEnv === 'prod'" />
            </el-form-item>
            <el-form-item label="数据分层">
              <el-select v-model="taskForm.layer" style="width: 100%;" :disabled="currentEnv === 'prod'">
                <el-option v-for="l in layers" :key="l.key" :label="l.label" :value="l.key" />
              </el-select>
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="taskForm.description" type="textarea" :rows="3" :disabled="currentEnv === 'prod'" />
            </el-form-item>
            <el-form-item label="负责人">
              <el-input v-model="taskForm.owner" :disabled="currentEnv === 'prod'" />
            </el-form-item>
            <el-form-item v-if="currentEnv === 'dev'">
              <el-button type="primary" size="small" @click="updateBasicInfo">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="发布管理" name="publish">
          <div class="publish-section">
            <div class="publish-status">
              <span>Dev ↔ Prod 状态</span>
              <el-tag v-if="diffResult.isSame" type="success" size="small">已同步</el-tag>
              <el-tag v-else-if="diffResult.prodStatus === 'not_published'" type="info" size="small">未发布</el-tag>
              <el-tag v-else type="warning" size="small">有差异</el-tag>
            </div>
            <div style="margin-top: 8px;">
              <el-button size="small" @click="loadDiff" :loading="diffLoading">
                <el-icon><Search /></el-icon>对比差异
              </el-button>
              <el-button v-if="currentEnv === 'dev'" size="small" type="success" @click="openPublishDialog">
                <el-icon><Upload /></el-icon>发布到生产
              </el-button>
            </div>
            <div v-if="diffResult.prodContent !== undefined" class="diff-preview" style="margin-top: 12px;">
              <el-alert
                :title="diffResult.isSame ? 'dev和prod内容一致' : (diffResult.prodStatus === 'not_published' ? '尚未发布到生产环境' : 'dev和prod内容有差异，请发布最新版本')"
                :type="diffResult.isSame ? 'success' : 'warning'"
                :closable="false"
                show-icon
              />
            </div>
          </div>
          <el-divider />
          <div class="publish-history-section">
            <div class="section-header">
              <span>发布历史</span>
              <el-button size="small" text type="primary" @click="loadPublishHistory">
                <el-icon><Refresh /></el-icon>刷新
              </el-button>
            </div>
            <div v-if="publishHistory.length === 0" class="empty-tip">暂无发布记录</div>
            <div v-for="pub in publishHistory" :key="pub.id" class="publish-item">
              <div class="publish-header">
                <el-tag size="small" :type="pub.publishType === 'rollback' ? 'danger' : 'success'">
                  {{ pub.publishType === 'rollback' ? '回滚' : '发布' }}
                </el-tag>
                <span class="publish-version">{{ pub.publishVersion }}</span>
              </div>
              <div class="publish-meta">
                <span v-if="pub.grayscaleMatch !== null">
                  灰度: <el-tag :type="pub.grayscaleMatch === 1 ? 'success' : 'danger'" size="small">
                    {{ pub.grayscaleMatch === 1 ? '通过' : '未通过' }}
                  </el-tag>
                </span>
                <span>{{ pub.publishedBy }}</span>
                <span>{{ formatTime(pub.publishedAt) }}</span>
              </div>
              <div v-if="pub.publishComment" class="publish-comment">{{ pub.publishComment }}</div>
              <div v-if="currentEnv === 'dev' && pub.publishType !== 'rollback'" style="margin-top: 4px;">
                <el-button size="small" text type="danger" @click="rollbackPublish(pub.id)">
                  回滚到此版本
                </el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="调度配置" name="schedule" v-if="currentEnv === 'dev'">
          <el-form :model="scheduleForm" label-width="80px" size="small">
            <el-form-item label="调度状态">
              <el-switch
                v-model="scheduleForm.scheduleStatus"
                active-value="online"
                inactive-value="offline"
                active-text="开启"
                inactive-text="关闭"
              />
            </el-form-item>
            <el-form-item label="Cron表达式">
              <el-input v-model="scheduleForm.scheduleCron" placeholder="0 0 2 * * ?">
                <template #append>
                  <el-tooltip content="每天2点: 0 0 2 * * ? | 每小时: 0 0 * * * ?">
                    <el-icon><QuestionFilled /></el-icon>
                  </el-tooltip>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="超时(秒)">
              <el-input-number v-model="scheduleForm.timeoutSeconds" :min="0" :max="86400" />
            </el-form-item>
            <el-form-item label="重试次数">
              <el-input-number v-model="scheduleForm.retryTimes" :min="0" :max="10" />
            </el-form-item>
            <el-form-item label="重试间隔(秒)">
              <el-input-number v-model="scheduleForm.retryInterval" :min="0" :max="3600" />
            </el-form-item>
            <el-form-item label="告警类型">
              <el-select v-model="scheduleForm.warningType" style="width: 100%;">
                <el-option label="不告警" value="NONE" />
                <el-option label="失败告警" value="FAILURE" />
                <el-option label="成功告警" value="SUCCESS" />
                <el-option label="全部告警" value="ALL" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="small" @click="updateSchedule">保存调度配置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="依赖关系" name="dependency">
          <div class="dep-section">
            <div class="dep-header">
              <span>上游依赖</span>
              <el-button size="small" text type="primary" @click="refreshDependencies">
                <el-icon><Refresh /></el-icon>刷新
              </el-button>
            </div>
            <div v-if="dependencies.upstream.length === 0" class="empty-tip">暂无上游依赖</div>
            <div v-for="dep in dependencies.upstream" :key="dep.id" class="dep-item upstream">
              <span class="dep-name">{{ dep.relatedTaskName }}</span>
              <el-tag size="small">{{ dep.relatedTaskLayer }}</el-tag>
              <el-tag v-if="dep.relatedTaskEnv" size="small" type="warning">{{ dep.relatedTaskEnv }}</el-tag>
              <span v-if="dep.depTable" class="dep-table">表: {{ dep.depTable }}</span>
            </div>
          </div>
          <el-divider />
          <div class="dep-section">
            <div class="dep-header">
              <span>下游依赖</span>
            </div>
            <div v-if="dependencies.downstream.length === 0" class="empty-tip">暂无下游依赖</div>
            <div v-for="dep in dependencies.downstream" :key="dep.id" class="dep-item downstream">
              <span class="dep-name">{{ dep.relatedTaskName }}</span>
              <el-tag size="small">{{ dep.relatedTaskLayer }}</el-tag>
              <el-tag v-if="dep.relatedTaskEnv" size="small" type="warning">{{ dep.relatedTaskEnv }}</el-tag>
              <span v-if="dep.depTable" class="dep-table">表: {{ dep.depTable }}</span>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="执行历史" name="history">
          <div v-if="execHistory.length === 0" class="empty-tip">暂无执行记录</div>
          <div v-for="exec in execHistory" :key="exec.id" class="history-item">
            <div class="history-header">
              <el-tag :type="exec.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
                {{ exec.status === 'SUCCESS' ? '成功' : '失败' }}
              </el-tag>
              <span class="history-time">{{ formatTime(exec.startTime) }}</span>
            </div>
            <div class="history-meta">
              耗时: {{ exec.duration }}s | 触发: {{ exec.triggerType || '手动' }}
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 发布对话框 -->
    <el-dialog v-model="showPublishDialog" title="发布到生产环境" width="650px" :close-on-click-modal="false">
      <div class="publish-flow">
        <!-- 步骤1: 灰度测试 -->
        <div class="publish-step">
          <div class="step-title">
            <el-icon color="#1890ff"><DataAnalysis /></el-icon>
            <span>步骤1: 灰度测试</span>
          </div>
          <el-form :model="publishForm" label-width="100px" size="small">
            <el-form-item label="启用灰度测试">
              <el-switch v-model="publishForm.grayscaleEnabled" />
            </el-form-item>
            <el-form-item label="采样行数" v-if="publishForm.grayscaleEnabled">
              <el-input-number v-model="publishForm.grayscaleLimit" :min="10" :max="10000" :step="100" />
              <span class="form-tip">用LIMIT限制采样，对比dev和prod执行结果</span>
            </el-form-item>
            <el-form-item v-if="!publishForm.grayscaleEnabled">
              <el-alert title="跳过灰度测试将直接发布，建议保留灰度测试以降低风险" type="warning" :closable="false" show-icon />
            </el-form-item>
          </el-form>
          <el-button v-if="publishForm.grayscaleEnabled" size="small" type="primary" @click="runGrayscaleTest" :loading="grayscaleTesting">
            <el-icon><VideoPlay /></el-icon>运行灰度测试
          </el-button>
        </div>

        <!-- 灰度测试结果 -->
        <div v-if="grayscaleResult" class="grayscale-result">
          <el-divider />
          <div class="step-title">
            <el-icon :color="grayscaleResult.match ? '#67c23a' : '#f56c6c'"><DataBoard /></el-icon>
            <span>灰度测试结果</span>
          </div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="Dev 返回行数">{{ grayscaleResult.devRows }}</el-descriptions-item>
            <el-descriptions-item label="Prod 返回行数">{{ grayscaleResult.prodRows }}</el-descriptions-item>
            <el-descriptions-item label="Dev 耗时">{{ grayscaleResult.devTime }}ms</el-descriptions-item>
            <el-descriptions-item label="Prod 耗时">{{ grayscaleResult.prodTime }}ms</el-descriptions-item>
            <el-descriptions-item label="Dev 执行状态">
              <el-tag :type="grayscaleResult.devSuccess ? 'success' : 'danger'" size="small">
                {{ grayscaleResult.devSuccess ? '成功' : '失败' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="Prod 执行状态">
              <el-tag :type="grayscaleResult.prodSuccess ? 'success' : 'danger'" size="small">
                {{ grayscaleResult.prodSuccess ? '成功' : '失败' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="结果一致性" :span="2">
              <el-tag v-if="grayscaleResult.skipped" type="info">跳过（非查询语句）</el-tag>
              <el-tag v-else-if="grayscaleResult.match" type="success">一致 ✓</el-tag>
              <el-tag v-else type="danger">不一致 ✗ — {{ grayscaleResult.mismatchReason }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 步骤2: 确认发布 -->
        <el-divider />
        <div class="publish-step">
          <div class="step-title">
            <el-icon color="#67c23a"><Upload /></el-icon>
            <span>步骤2: 确认发布</span>
          </div>
          <el-form :model="publishForm" label-width="100px" size="small">
            <el-form-item label="发布说明">
              <el-input v-model="publishForm.publishComment" type="textarea" :rows="2" placeholder="本次发布的内容说明" />
            </el-form-item>
            <el-form-item label="发布人">
              <el-input v-model="publishForm.publishedBy" placeholder="admin" />
            </el-form-item>
          </el-form>
          <el-button type="success" @click="doPublish" :loading="publishing" :disabled="publishForm.grayscaleEnabled && !grayscaleResult">
            <el-icon><Upload /></el-icon>确认发布到生产环境
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 新建任务弹窗 -->
    <el-dialog v-model="showCreateDialog" title="新建任务" width="500px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="任务名称">
          <el-input v-model="createForm.name" placeholder="如: dwd_order_detail" />
        </el-form-item>
        <el-form-item label="数据分层">
          <el-select v-model="createForm.layer" style="width: 100%;">
            <el-option v-for="l in layers" :key="l.key" :label="l.label" :value="l.key" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务类型">
          <el-radio-group v-model="createForm.type">
            <el-radio value="script">SQL脚本</el-radio>
            <el-radio value="syncTask">同步任务</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="所属文件夹" v-if="createForm.type === 'script'">
          <el-select v-model="createForm.folderId" style="width: 100%;" placeholder="选择文件夹">
            <el-option v-for="f in folders" :key="f.id" :label="f.folderName" :value="f.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createTask">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, VideoPlay, Check, Operation, EditPen, QuestionFilled, CircleCloseFilled, 
         Refresh, Upload, Lock, Search, DataAnalysis, DataBoard } from '@element-plus/icons-vue'
import api from '@/api/request'

const layers = [
  { key: 'ODS', label: '操作数据层', tagType: 'info' },
  { key: 'DWD', label: '明细数据层', tagType: 'warning' },
  { key: 'DWS', label: '汇总数据层', tagType: 'success' },
  { key: 'ADS', label: '应用数据层', tagType: 'danger' },
  { key: 'DIM', label: '维度层', tagType: '' }
]

const searchKeyword = ref('')
const activeLayers = ref(['ODS', 'DWD', 'DWS', 'ADS', 'DIM'])
const tasksByLayer = ref({})
const selectedTask = ref(null)
const sqlContent = ref('')
const executing = ref(false)
const execResults = ref([])
const execError = ref('')
const execDuration = ref(0)
const execTotalRows = ref(0)
const execDatasource = ref('')
const datasources = ref([])
const selectedDatasourceId = ref(null)
const rightTab = ref('basic')
const showCreateDialog = ref(false)
const folders = ref([])
const currentEnv = ref('dev')

const taskForm = reactive({
  name: '',
  layer: 'ODS',
  description: '',
  owner: ''
})

const scheduleForm = reactive({
  scheduleStatus: 'offline',
  scheduleCron: '0 0 2 * * ?',
  timeoutSeconds: 3600,
  retryTimes: 1,
  retryInterval: 60,
  warningType: 'FAILURE'
})

const dependencies = reactive({
  upstream: [],
  downstream: []
})

const execHistory = ref([])
const publishHistory = ref([])
const diffResult = ref({})
const diffLoading = ref(false)

const showPublishDialog = ref(false)
const publishing = ref(false)
const grayscaleTesting = ref(false)
const grayscaleResult = ref(null)
const publishForm = reactive({
  devScriptId: null,
  grayscaleEnabled: true,
  grayscaleLimit: 100,
  publishComment: '',
  publishedBy: 'admin'
})

const createForm = reactive({
  name: '',
  layer: 'ODS',
  type: 'script',
  folderId: null,
  description: ''
})

const switchEnv = (env) => {
  currentEnv.value = env
  selectedTask.value = null
  sqlContent.value = ''
  execResults.value = []
  execError.value = ''
  grayscaleResult.value = null
  loadTasks()
}

const loadTasks = async () => {
  try {
    const res = await api.get('/data-development/tasks', {
      params: { 
        keyword: searchKeyword.value || undefined,
        environment: currentEnv.value
      }
    })
    tasksByLayer.value = res.data.tasksByLayer || {}
  } catch (e) {
    console.error('加载任务列表失败', e)
  }
}

const loadDatasources = async () => {
  try {
    const res = await api.get('/datasource/list')
    datasources.value = res.data || []
  } catch (e) {
    console.error('加载数据源失败', e)
  }
}

const loadFolders = async () => {
  try {
    const res = await api.get('/data-development/folders')
    const extractFolders = (nodes) => {
      let result = []
      for (const node of nodes) {
        if (node.type === 'folder') {
          result.push(node)
          if (node.children) result = result.concat(extractFolders(node.children))
        }
      }
      return result
    }
    folders.value = extractFolders(res.data || [])
  } catch (e) {
    console.error('加载文件夹失败', e)
  }
}

const getLayerTasks = (layer) => {
  return (tasksByLayer.value[layer] || []).filter(t => {
    if (!searchKeyword.value) return true
    return t.name.toLowerCase().includes(searchKeyword.value.toLowerCase())
  })
}

const getLayerTaskCount = (layer) => (tasksByLayer.value[layer] || []).length
const getLayerTagType = (layer) => {
  const found = layers.find(l => l.key === layer)
  return found ? found.tagType : 'info'
}

const selectTask = async (task) => {
  selectedTask.value = task
  execResults.value = []
  execError.value = ''
  grayscaleResult.value = null
  publishHistory.value = []
  diffResult.value = {}
  
  taskForm.name = task.name
  taskForm.layer = task.layer
  taskForm.description = task.description || ''
  taskForm.owner = task.owner || ''
  
  scheduleForm.scheduleStatus = task.scheduleStatus || 'offline'
  scheduleForm.scheduleCron = task.scheduleCron || '0 0 2 * * ?'
  scheduleForm.timeoutSeconds = task.timeoutSeconds || 3600
  scheduleForm.retryTimes = task.retryTimes || 1
  scheduleForm.retryInterval = task.retryInterval || 60
  scheduleForm.warningType = task.warningType || 'FAILURE'
  
  if (task.type === 'script') {
    try {
      const res = await api.get(`/data-development/script/${task.id}`)
      sqlContent.value = res.data.content || ''
      if (res.data.databaseName && datasources.value.length > 0) {
        const ds = datasources.value.find(d => d.databaseName === res.data.databaseName)
        if (ds) selectedDatasourceId.value = ds.id
      }
    } catch (e) {
      sqlContent.value = ''
    }
    
    loadDependencies(task.id, 'script')
    loadExecHistory(task.id, 'script')
    
    if (currentEnv.value === 'dev') {
      loadDiff()
      loadPublishHistory()
    }
    
    publishForm.devScriptId = currentEnv.value === 'dev' ? task.id : (task.devScriptId || null)
  } else {
    sqlContent.value = `-- 同步任务: ${task.name}\n-- 源表: ${task.sourceDb}.${task.sourceTable}\n-- 目标表: ${task.targetTable}`
    loadDependencies(task.id, 'syncTask')
    loadExecHistory(task.id, 'syncTask')
  }
}

const loadDependencies = async (taskId, taskType) => {
  try {
    const res = await api.get(`/data-development/dependencies/${taskId}`, { params: { taskType } })
    dependencies.upstream = res.data.upstream || []
    dependencies.downstream = res.data.downstream || []
  } catch (e) {
    console.error('加载依赖失败', e)
  }
}

const loadExecHistory = async (taskId, taskType) => {
  try {
    const res = await api.get(`/data-development/executions/${taskId}`, { params: { taskType, limit: 20 } })
    execHistory.value = res.data || []
  } catch (e) {
    console.error('加载执行历史失败', e)
  }
}

const loadDiff = async () => {
  if (!selectedTask.value || selectedTask.value.type !== 'script') return
  const devId = currentEnv.value === 'dev' ? selectedTask.value.id : (selectedTask.value.devScriptId)
  if (!devId) return
  
  diffLoading.value = true
  try {
    const res = await api.get(`/data-development/diff/${devId}`)
    diffResult.value = res.data
  } catch (e) {
    console.error('加载差异失败', e)
  } finally {
    diffLoading.value = false
  }
}

const loadPublishHistory = async () => {
  if (!selectedTask.value || selectedTask.value.type !== 'script') return
  const devId = currentEnv.value === 'dev' ? selectedTask.value.id : (selectedTask.value.devScriptId)
  if (!devId) return
  
  try {
    const res = await api.get(`/data-development/publish-history/${devId}`)
    publishHistory.value = res.data || []
  } catch (e) {
    console.error('加载发布历史失败', e)
  }
}

const executeSql = async () => {
  if (!sqlContent.value.trim()) {
    ElMessage.warning('请输入SQL语句')
    return
  }
  executing.value = true
  execResults.value = []
  execError.value = ''
  
  try {
    const res = await api.post('/data-development/execute', {
      scriptId: selectedTask.value?.type === 'script' ? selectedTask.value.id : null,
      sql: sqlContent.value,
      datasourceId: selectedDatasourceId.value
    })
    const data = res.data
    if (data.success) {
      execResults.value = data.results || []
      execTotalRows.value = data.totalRows || 0
      execDatasource.value = data.datasource || ''
      ElMessage.success(`执行成功，耗时 ${data.durationMs}ms`)
    } else {
      execError.value = data.error || '执行失败'
      ElMessage.error('执行失败')
    }
    execDuration.value = data.durationMs || 0
    if (selectedTask.value) {
      loadExecHistory(selectedTask.value.id, selectedTask.value.type)
    }
  } catch (e) {
    execError.value = e.message || '执行异常'
    ElMessage.error('执行异常')
  } finally {
    executing.value = false
  }
}

const saveScript = async () => {
  if (!selectedTask.value || selectedTask.value.type !== 'script') {
    ElMessage.warning('只能保存SQL脚本类型任务')
    return
  }
  try {
    await api.post('/data-development/script/save', {
      id: selectedTask.value.id,
      scriptName: taskForm.name,
      content: sqlContent.value,
      taskLayer: taskForm.layer,
      description: taskForm.description,
      owner: taskForm.owner,
      environment: currentEnv.value
    })
    ElMessage.success('保存成功')
    loadTasks()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const formatSql = () => {
  const keywords = ['SELECT', 'FROM', 'WHERE', 'AND', 'OR', 'JOIN', 'LEFT JOIN', 'RIGHT JOIN',
    'INNER JOIN', 'ON', 'GROUP BY', 'ORDER BY', 'HAVING', 'LIMIT', 'INSERT INTO',
    'UPDATE', 'DELETE FROM', 'CREATE TABLE', 'ALTER TABLE', 'DROP TABLE',
    'UNION ALL', 'UNION', 'CASE', 'WHEN', 'THEN', 'ELSE', 'END', 'AS']
  let formatted = sqlContent.value
  keywords.forEach(kw => {
    const regex = new RegExp('\\b' + kw.replace(/ /g, '\\s+') + '\\b', 'gi')
    formatted = formatted.replace(regex, kw)
  })
  const breakKeywords = ['FROM', 'WHERE', 'AND', 'OR', 'JOIN', 'LEFT JOIN', 'RIGHT JOIN',
    'INNER JOIN', 'ON', 'GROUP BY', 'ORDER BY', 'HAVING', 'LIMIT', 'UNION ALL', 'UNION']
  breakKeywords.forEach(kw => {
    const regex = new RegExp('\\s+(' + kw.replace(/ /g, '\\s+') + ')\\b', 'gi')
    formatted = formatted.replace(regex, '\n$1')
  })
  sqlContent.value = formatted
}

const handleEditorKeydown = (e) => {
  if (currentEnv.value === 'prod') return
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    executeSql()
  }
  if (e.key === 'Tab') {
    e.preventDefault()
    const start = e.target.selectionStart
    const end = e.target.selectionEnd
    sqlContent.value = sqlContent.value.substring(0, start) + '  ' + sqlContent.value.substring(end)
    setTimeout(() => {
      e.target.selectionStart = e.target.selectionEnd = start + 2
    })
  }
}

const updateBasicInfo = async () => {
  if (!selectedTask.value || currentEnv.value === 'prod') return
  try {
    if (selectedTask.value.type === 'script') {
      await api.post('/data-development/script/save', {
        id: selectedTask.value.id,
        scriptName: taskForm.name,
        content: sqlContent.value,
        taskLayer: taskForm.layer,
        description: taskForm.description,
        owner: taskForm.owner,
        environment: currentEnv.value
      })
    }
    ElMessage.success('基本信息更新成功')
    loadTasks()
  } catch (e) {
    ElMessage.error('更新失败')
  }
}

const updateSchedule = async () => {
  if (!selectedTask.value) return
  try {
    await api.put(`/data-development/schedule/${selectedTask.value.id}`, scheduleForm, {
      params: { taskType: selectedTask.value.type }
    })
    ElMessage.success('调度配置更新成功')
    loadTasks()
  } catch (e) {
    ElMessage.error('调度配置更新失败')
  }
}

const refreshDependencies = async () => {
  try {
    const res = await api.post('/data-development/dependencies/refresh')
    ElMessage.success(`依赖刷新完成，更新 ${res.data.updatedCount} 条`)
    if (selectedTask.value) {
      loadDependencies(selectedTask.value.id, selectedTask.value.type)
    }
  } catch (e) {
    ElMessage.error('依赖刷新失败')
  }
}

// ======================== 发布管理 ========================

const openPublishDialog = () => {
  if (!selectedTask.value || selectedTask.value.type !== 'script') {
    ElMessage.warning('只能发布SQL脚本类型任务')
    return
  }
  publishForm.devScriptId = currentEnv.value === 'dev' ? selectedTask.value.id : (selectedTask.value.devScriptId)
  publishForm.grayscaleEnabled = true
  publishForm.grayscaleLimit = 100
  publishForm.publishComment = ''
  publishForm.publishedBy = 'admin'
  grayscaleResult.value = null
  showPublishDialog.value = true
}

const runGrayscaleTest = async () => {
  if (!publishForm.devScriptId) {
    ElMessage.warning('请先保存脚本')
    return
  }
  grayscaleTesting.value = true
  grayscaleResult.value = null
  try {
    const res = await api.post('/data-development/grayscale-test', {
      devScriptId: publishForm.devScriptId,
      grayscaleLimit: publishForm.grayscaleLimit,
      prodDatasourceId: selectedDatasourceId.value
    })
    grayscaleResult.value = res.data
    if (res.data.skipped) {
      ElMessage.info('跳过了灰度测试（非查询语句）')
    } else if (res.data.match) {
      ElMessage.success('灰度测试通过！dev和prod执行结果一致')
    } else {
      ElMessage.warning('灰度测试未通过: ' + (res.data.mismatchReason || '结果不一致'))
    }
  } catch (e) {
    ElMessage.error('灰度测试失败')
  } finally {
    grayscaleTesting.value = false
  }
}

const doPublish = async () => {
  if (!publishForm.devScriptId) {
    ElMessage.warning('请输入发布说明')
    return
  }
  
  if (publishForm.grayscaleEnabled && grayscaleResult.value && !grayscaleResult.value.match && !grayscaleResult.value.skipped) {
    try {
      await ElMessageBox.confirm('灰度测试未通过，确定要继续发布吗？', '风险提示', {
        type: 'warning',
        confirmButtonText: '继续发布',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
  }
  
  publishing.value = true
  try {
    const res = await api.post('/data-development/publish', {
      devScriptId: publishForm.devScriptId,
      grayscaleEnabled: publishForm.grayscaleEnabled,
      grayscaleLimit: publishForm.grayscaleLimit,
      publishComment: publishForm.publishComment,
      publishedBy: publishForm.publishedBy,
      prodDatasourceId: selectedDatasourceId.value
    })
    ElMessage.success(`发布成功！版本: ${res.data.version}`)
    showPublishDialog.value = false
    loadTasks()
    loadDiff()
    loadPublishHistory()
  } catch (e) {
    ElMessage.error('发布失败')
  } finally {
    publishing.value = false
  }
}

const rollbackPublish = async (publishId) => {
  try {
    await ElMessageBox.confirm('确定要回滚到此版本吗？这将覆盖当前生产环境的脚本内容。', '回滚确认', {
      type: 'warning',
      confirmButtonText: '确定回滚',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  
  try {
    await api.post(`/data-development/rollback/${publishId}`, {
      publishedBy: 'admin'
    })
    ElMessage.success('回滚成功')
    loadTasks()
    loadDiff()
    loadPublishHistory()
  } catch (e) {
    ElMessage.error('回滚失败')
  }
}

const createTask = async () => {
  if (!createForm.name.trim()) {
    ElMessage.warning('请输入任务名称')
    return
  }
  try {
    if (createForm.type === 'script') {
      await api.post('/data-development/script/save', {
        scriptName: createForm.name,
        taskLayer: createForm.layer,
        folderId: createForm.folderId || 1,
        description: createForm.description,
        content: `-- ${createForm.name}\n-- ${createForm.layer}层任务\n\nSELECT \nFROM \nWHERE \n`,
        scriptType: 'sql',
        taskStatus: 'draft',
        scheduleStatus: 'offline',
        environment: 'dev'
      })
      ElMessage.success('SQL脚本创建成功')
    }
    showCreateDialog.value = false
    createForm.name = ''
    createForm.description = ''
    await loadTasks()
  } catch (e) {
    ElMessage.error('创建失败')
  }
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  loadTasks()
  loadDatasources()
  loadFolders()
})
</script>

<style scoped>
.data-dev-container {
  display: flex;
  height: calc(100vh - 120px);
  background: #fff;
  border-radius: 4px;
  overflow: hidden;
}

.left-panel {
  width: 280px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  background: #fafafa;
  flex-shrink: 0;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
}

.panel-header h4 {
  margin: 0;
  font-size: 14px;
}

.env-switcher {
  padding: 8px 12px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: center;
}

.panel-toolbar {
  padding: 8px 12px;
  border-bottom: 1px solid #e4e7ed;
}

.task-tree {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

.task-tree :deep(.el-collapse) { border: none; }
.task-tree :deep(.el-collapse-item__header) {
  padding: 0 12px;
  height: 36px;
  line-height: 36px;
  font-size: 13px;
  border: none;
  background: #f5f7fa;
}
.task-tree :deep(.el-collapse-item__wrap) { border: none; }
.task-tree :deep(.el-collapse-item__content) { padding: 0; }

.layer-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.layer-label { font-size: 12px; color: #909399; }
.layer-count { font-size: 11px; color: #c0c4cc; }

.task-item {
  padding: 8px 16px 8px 24px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.task-item:hover { background: #ecf5ff; }
.task-item.active {
  background: #d9ecff;
  border-left: 3px solid #1890ff;
  padding-left: 21px;
}

.task-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-name {
  font-size: 13px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 160px;
}

.task-type-tag {
  font-size: 10px;
  color: #909399;
  background: #f0f2f5;
  padding: 1px 6px;
  border-radius: 3px;
}

.task-meta {
  margin-top: 4px;
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.empty-tip {
  padding: 20px;
  text-align: center;
  color: #c0c4cc;
  font-size: 13px;
}

.center-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
  flex-shrink: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.current-task-name {
  font-weight: 600;
  font-size: 14px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.welcome-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.welcome-content {
  text-align: center;
  color: #909399;
}

.welcome-content h2 {
  margin: 16px 0 8px;
  color: #606266;
}

.editor-area {
  flex: 1;
  min-height: 200px;
}

.sql-editor {
  width: 100%;
  height: 100%;
  border: none;
  padding: 16px;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 14px;
  line-height: 1.6;
  resize: none;
  outline: none;
  background: #1e1e1e;
  color: #d4d4d4;
  tab-size: 2;
}

.sql-editor:read-only {
  background: #2d2d2d;
  color: #888;
}

.sql-editor::placeholder { color: #6a6a6a; }

.result-panel {
  border-top: 1px solid #e4e7ed;
  max-height: 300px;
  overflow-y: auto;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: #f0f9eb;
  border-bottom: 1px solid #e1f3d8;
  font-size: 13px;
  font-weight: 500;
}

.result-meta { font-size: 12px; color: #909399; flex: 1; }
.result-item { padding: 8px 16px; border-bottom: 1px solid #f0f0f0; }
.update-result, .query-result { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

.exec-sql {
  width: 100%;
  font-size: 12px;
  color: #909399;
  font-family: monospace;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.error-panel {
  border-top: 1px solid #e4e7ed;
  max-height: 200px;
  overflow-y: auto;
}

.error-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #fef0f0;
  border-bottom: 1px solid #fde2e2;
  font-size: 13px;
  color: #f56c6c;
}

.error-msg {
  padding: 12px 16px;
  margin: 0;
  font-size: 12px;
  color: #f56c6c;
  white-space: pre-wrap;
  word-break: break-all;
}

.right-panel {
  width: 340px;
  border-left: 1px solid #e4e7ed;
  background: #fafafa;
  flex-shrink: 0;
  overflow-y: auto;
}

.right-panel :deep(.el-tabs__header) { margin: 0; padding: 0 12px; }
.right-panel :deep(.el-tabs__content) { padding: 12px; }

/* 发布管理 */
.publish-section { margin-bottom: 8px; }
.publish-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 500;
}

.publish-history-section { }
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 500;
}

.publish-item {
  padding: 8px 10px;
  margin-bottom: 4px;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.publish-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.publish-version {
  font-weight: 600;
  font-size: 13px;
}

.publish-meta {
  margin-top: 4px;
  display: flex;
  gap: 8px;
  font-size: 11px;
  color: #909399;
}

.publish-comment {
  margin-top: 4px;
  font-size: 12px;
  color: #606266;
}

/* 发布对话框 */
.publish-flow { }
.publish-step { margin-bottom: 8px; }
.step-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}

.form-tip {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}

.grayscale-result { }

/* 依赖 */
.dep-section { margin-bottom: 8px; }
.dep-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 500;
}

.dep-item {
  padding: 6px 10px;
  margin-bottom: 4px;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}
.dep-item.upstream { border-left: 3px solid #1890ff; }
.dep-item.downstream { border-left: 3px solid #67c23a; }
.dep-name { font-weight: 500; }
.dep-table { color: #909399; font-size: 11px; }

/* 执行历史 */
.history-item {
  padding: 8px 10px;
  margin-bottom: 4px;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}
.history-header { display: flex; align-items: center; gap: 8px; }
.history-time { font-size: 12px; color: #606266; }
.history-meta { margin-top: 4px; font-size: 11px; color: #909399; }
</style>
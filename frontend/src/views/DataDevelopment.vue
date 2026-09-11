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
        </div>
        <div class="toolbar-right">
          <el-select v-model="selectedDatasourceId" placeholder="选择数据源" size="small" style="width: 180px;" clearable>
            <el-option v-for="ds in datasources" :key="ds.id" :label="ds.name" :value="ds.id" />
          </el-select>
          <el-button type="primary" size="small" @click="executeSql" :loading="executing">
            <el-icon><VideoPlay /></el-icon>运行
          </el-button>
          <el-button size="small" @click="saveScript">
            <el-icon><Check /></el-icon>保存
          </el-button>
          <el-button size="small" @click="formatSql">
            <el-icon><Operation /></el-icon>格式化
          </el-button>
        </div>
      </div>
      <div v-else class="welcome-panel">
        <div class="welcome-content">
          <el-icon :size="64" color="#c0c4cc"><EditPen /></el-icon>
          <h2>数据开发工作台</h2>
          <p>从左侧选择一个任务开始编辑，或创建新任务</p>
        </div>
      </div>

      <!-- SQL编辑器 -->
      <div v-if="selectedTask" class="editor-area">
        <textarea
          ref="editorRef"
          v-model="sqlContent"
          class="sql-editor"
          placeholder="输入SQL语句..."
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
              <el-input v-model="taskForm.name" />
            </el-form-item>
            <el-form-item label="数据分层">
              <el-select v-model="taskForm.layer" style="width: 100%;">
                <el-option v-for="l in layers" :key="l.key" :label="l.label" :value="l.key" />
              </el-select>
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="taskForm.description" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item label="负责人">
              <el-input v-model="taskForm.owner" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="small" @click="updateBasicInfo">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="调度配置" name="schedule">
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
                  <el-tooltip content="每秒: * * * * * ? | 每天2点: 0 0 2 * * ? | 每小时: 0 0 * * * ?">
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
import { ElMessage } from 'element-plus'
import { Plus, VideoPlay, Check, Operation, EditPen, QuestionFilled, CircleCloseFilled, Refresh, Lock, Upload, Search, DataAnalysis, DataBoard } from '@element-plus/icons-vue'
import { dataDevelopmentApi, datasourceApi } from '@/api/dataDevelopment'

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

const createForm = reactive({
  name: '',
  layer: 'ODS',
  type: 'script',
  folderId: null,
  description: ''
})

const loadTasks = async () => {
  try {
    const res = await dataDevelopmentApi.getTasks({ keyword: searchKeyword.value || undefined })
    tasksByLayer.value = res.data.tasksByLayer || {}
  } catch (e) {
    console.error('加载任务列表失败', e)
  }
}

const loadDatasources = async () => {
  try {
    const res = await datasourceApi.list()
    datasources.value = res.data || []
  } catch (e) {
    console.error('加载数据源失败', e)
  }
}

const loadFolders = async () => {
  try {
    const res = await dataDevelopmentApi.getFolders()
    // 提取所有文件夹节点
    const extractFolders = (nodes) => {
      let result = []
      for (const node of nodes) {
        if (node.type === 'folder') {
          result.push(node)
          if (node.children) {
            result = result.concat(extractFolders(node.children))
          }
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

const getLayerTaskCount = (layer) => {
  return (tasksByLayer.value[layer] || []).length
}

const getLayerTagType = (layer) => {
  const found = layers.find(l => l.key === layer)
  return found ? found.tagType : 'info'
}

const selectTask = async (task) => {
  selectedTask.value = task
  execResults.value = []
  execError.value = ''

  // 加载基本信息
  taskForm.name = task.name
  taskForm.layer = task.layer
  taskForm.description = task.description || ''
  taskForm.owner = task.owner || ''

  // 加载调度配置
  scheduleForm.scheduleStatus = task.scheduleStatus || 'offline'
  scheduleForm.scheduleCron = task.scheduleCron || '0 0 2 * * ?'
  scheduleForm.timeoutSeconds = task.timeoutSeconds || 3600
  scheduleForm.retryTimes = task.retryTimes || 1
  scheduleForm.retryInterval = task.retryInterval || 60
  scheduleForm.warningType = task.warningType || 'FAILURE'

  if (task.type === 'script') {
    try {
      const res = await dataDevelopmentApi.getScript(task.id)
      sqlContent.value = res.data.content || ''
      // 自动选择数据源
      if (res.data.databaseName && datasources.value.length > 0) {
        const ds = datasources.value.find(d => d.databaseName === res.data.databaseName)
        if (ds) selectedDatasourceId.value = ds.id
      }
    } catch (e) {
      sqlContent.value = ''
    }
    // 加载依赖
    loadDependencies(task.id, 'script')
    // 加载执行历史
    loadExecHistory(task.id, 'script')
  } else {
    sqlContent.value = `-- 同步任务: ${task.name}\n-- 源表: ${task.sourceDb}.${task.sourceTable}\n-- 目标表: ${task.targetTable}`
    loadDependencies(task.id, 'syncTask')
    loadExecHistory(task.id, 'syncTask')
  }
}

const loadDependencies = async (taskId, taskType) => {
  try {
    const res = await dataDevelopmentApi.getDependencies(taskId, taskType)
    dependencies.upstream = res.data.upstream || []
    dependencies.downstream = res.data.downstream || []
  } catch (e) {
    console.error('加载依赖失败', e)
  }
}

const loadExecHistory = async (taskId, taskType) => {
  try {
    const res = await dataDevelopmentApi.getExecutions(taskId, taskType, 20)
    execHistory.value = res.data || []
  } catch (e) {
    console.error('加载执行历史失败', e)
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
    const res = await dataDevelopmentApi.execute({
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

    // 刷新执行历史
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
    await dataDevelopmentApi.saveScript({
      id: selectedTask.value.id,
      scriptName: taskForm.name,
      content: sqlContent.value,
      taskLayer: taskForm.layer,
      description: taskForm.description,
      owner: taskForm.owner
    })
    ElMessage.success('保存成功')
    loadTasks()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const formatSql = () => {
  // 简单的SQL格式化：关键字大写、缩进
  const keywords = ['SELECT', 'FROM', 'WHERE', 'AND', 'OR', 'JOIN', 'LEFT JOIN', 'RIGHT JOIN',
    'INNER JOIN', 'ON', 'GROUP BY', 'ORDER BY', 'HAVING', 'LIMIT', 'INSERT INTO',
    'UPDATE', 'DELETE FROM', 'CREATE TABLE', 'ALTER TABLE', 'DROP TABLE',
    'UNION ALL', 'UNION', 'CASE', 'WHEN', 'THEN', 'ELSE', 'END', 'AS']

  let formatted = sqlContent.value
  // 简单关键字大写
  keywords.forEach(kw => {
    const regex = new RegExp('\\b' + kw.replace(/ /g, '\\s+') + '\\b', 'gi')
    formatted = formatted.replace(regex, kw)
  })
  // 在关键字前换行
  const breakKeywords = ['FROM', 'WHERE', 'AND', 'OR', 'JOIN', 'LEFT JOIN', 'RIGHT JOIN',
    'INNER JOIN', 'ON', 'GROUP BY', 'ORDER BY', 'HAVING', 'LIMIT', 'UNION ALL', 'UNION']
  breakKeywords.forEach(kw => {
    const regex = new RegExp('\\s+(' + kw.replace(/ /g, '\\s+') + ')\\b', 'gi')
    formatted = formatted.replace(regex, '\n$1')
  })

  sqlContent.value = formatted
}

const handleEditorKeydown = (e) => {
  // Ctrl+Enter 执行
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    executeSql()
  }
  // Tab 缩进
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
  if (!selectedTask.value) return
  try {
    if (selectedTask.value.type === 'script') {
      await dataDevelopmentApi.saveScript({
        id: selectedTask.value.id,
        scriptName: taskForm.name,
        content: sqlContent.value,
        taskLayer: taskForm.layer,
        description: taskForm.description,
        owner: taskForm.owner
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
    await dataDevelopmentApi.updateSchedule(selectedTask.value.id, scheduleForm, selectedTask.value.type)
    ElMessage.success('调度配置更新成功')
    loadTasks()
  } catch (e) {
    ElMessage.error('调度配置更新失败')
  }
}

const refreshDependencies = async () => {
  try {
    const res = await dataDevelopmentApi.refreshDependencies()
    ElMessage.success(`依赖刷新完成，更新 ${res.data.updatedCount} 条`)
    if (selectedTask.value) {
      loadDependencies(selectedTask.value.id, selectedTask.value.type)
    }
  } catch (e) {
    ElMessage.error('依赖刷新失败')
  }
}

const createTask = async () => {
  if (!createForm.name.trim()) {
    ElMessage.warning('请输入任务名称')
    return
  }
  try {
    if (createForm.type === 'script') {
      await dataDevelopmentApi.saveScript({
        scriptName: createForm.name,
        taskLayer: createForm.layer,
        folderId: createForm.folderId || 1,
        description: createForm.description,
        content: `-- ${createForm.name}\n-- ${createForm.layer}层任务\n\nSELECT \nFROM \nWHERE \n`,
        scriptType: 'sql',
        taskStatus: 'draft',
        scheduleStatus: 'offline'
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

.panel-toolbar {
  padding: 8px 12px;
  border-bottom: 1px solid #e4e7ed;
}

.task-tree {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

.task-tree :deep(.el-collapse) {
  border: none;
}

.task-tree :deep(.el-collapse-item__header) {
  padding: 0 12px;
  height: 36px;
  line-height: 36px;
  font-size: 13px;
  border: none;
  background: #f5f7fa;
}

.task-tree :deep(.el-collapse-item__wrap) {
  border: none;
}

.task-tree :deep(.el-collapse-item__content) {
  padding: 0;
}

.layer-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.layer-label {
  font-size: 12px;
  color: #909399;
}

.layer-count {
  font-size: 11px;
  color: #c0c4cc;
}

.task-item {
  padding: 8px 16px 8px 24px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.task-item:hover {
  background: #ecf5ff;
}

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

.sql-editor::placeholder {
  color: #6a6a6a;
}

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

.result-meta {
  font-size: 12px;
  color: #909399;
  flex: 1;
}

.result-item {
  padding: 8px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.update-result, .query-result {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

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
  width: 320px;
  border-left: 1px solid #e4e7ed;
  background: #fafafa;
  flex-shrink: 0;
  overflow-y: auto;
}

.right-panel :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 12px;
}

.right-panel :deep(.el-tabs__content) {
  padding: 12px;
}

.dep-section {
  margin-bottom: 8px;
}

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

.dep-item.upstream {
  border-left: 3px solid #1890ff;
}

.dep-item.downstream {
  border-left: 3px solid #67c23a;
}

.dep-name {
  font-weight: 500;
}

.dep-table {
  color: #909399;
  font-size: 11px;
}

.history-item {
  padding: 8px 10px;
  margin-bottom: 4px;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.history-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.history-time {
  font-size: 12px;
  color: #606266;
}

.history-meta {
  margin-top: 4px;
  font-size: 11px;
  color: #909399;
}
</style>
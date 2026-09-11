<template>
  <el-card>
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新增同步任务
      </el-button>
    </div>

    <el-table :data="tasks" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="taskName" label="任务名称" width="200" />
      <el-table-column prop="sourceDb" label="源库" width="120" />
      <el-table-column prop="sourceTable" label="源表" width="150" />
      <el-table-column prop="targetDb" label="目标库" width="120" />
      <el-table-column prop="targetTable" label="目标表" width="180" />
      <el-table-column prop="syncMode" label="同步模式" width="120">
        <template #default="{ row }">
          <el-tag :type="row.syncMode === 'full' ? '' : 'warning'">
            {{ row.syncMode === 'full' ? '全量' : '增量' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" fixed="right" width="260">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="handleRun(row)">执行</el-button>
          <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除?" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="任务名称">
          <el-input v-model="form.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="源数据源">
          <el-select v-model="form.sourceDsId" placeholder="请选择数据源" style="width: 100%;" @change="onSourceChange">
            <el-option v-for="ds in datasources" :key="ds.id" :label="ds.name" :value="ds.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="源数据库">
          <el-input v-model="form.sourceDb" placeholder="请输入源数据库名" />
        </el-form-item>
        <el-form-item label="源表名">
          <el-input v-model="form.sourceTable" placeholder="请输入源表名" />
        </el-form-item>
        <el-form-item label="目标数据库">
          <el-input v-model="form.targetDb" placeholder="默认 ods" />
        </el-form-item>
        <el-form-item label="目标表名">
          <el-input v-model="form.targetTable" placeholder="例如：ods_business_orders_full" />
        </el-form-item>
        <el-form-item label="同步模式">
          <el-select v-model="form.syncMode" style="width: 100%;">
            <el-option label="全量同步" value="full" />
            <el-option label="增量同步" value="incr" />
          </el-select>
        </el-form-item>
        <el-form-item label="分区字段">
          <el-input v-model="form.partitionField" placeholder="默认 dt" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { syncTaskApi, datasourceApi } from '@/api/dataDevelopment'

const tasks = ref([])
const datasources = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增同步任务')
const isEdit = ref(false)

const defaultForm = {
  id: null,
  taskName: '',
  sourceDsId: null,
  sourceDb: '',
  sourceTable: '',
  targetDb: 'ods',
  targetTable: '',
  syncMode: 'full',
  partitionField: 'dt',
  status: 1
}
const form = ref({ ...defaultForm })

const loadTasks = async () => {
  loading.value = true
  try {
    const res = await syncTaskApi.list()
    tasks.value = res.data || []
  } catch (e) {
    ElMessage.error('加载同步任务失败')
  } finally {
    loading.value = false
  }
}

const loadDatasources = async () => {
  try {
    const res = await datasourceApi.list()
    datasources.value = (res.data || []).filter(ds => ds.status === 1)
  } catch (e) {
    console.error('加载数据源失败', e)
  }
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增同步任务'
  form.value = { ...defaultForm }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑同步任务'
  form.value = { ...row }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.value.taskName || !form.value.sourceTable) {
    ElMessage.warning('请填写必填项')
    return
  }
  try {
    if (isEdit.value) {
      await syncTaskApi.update(form.value)
    } else {
      await syncTaskApi.create(form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadTasks()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const handleRun = async (row) => {
  try {
    await ElMessageBox.confirm(`确认执行同步任务 "${row.taskName}"？`, '提示', { type: 'warning' })
    await syncTaskApi.run(row.id)
    ElMessage.success('任务已提交执行')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('执行失败')
  }
}

const handleDelete = async (id) => {
  try {
    await syncTaskApi.delete(id)
    ElMessage.success('删除成功')
    loadTasks()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const onSourceChange = (dsId) => {
  const ds = datasources.value.find(d => d.id === dsId)
  if (ds) {
    form.value.sourceDb = ds.databaseName || ''
  }
}

onMounted(() => {
  loadTasks()
  loadDatasources()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}
</style>
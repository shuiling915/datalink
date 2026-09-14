<template>
  <el-card>
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新增数据源
      </el-button>
      <el-input
        v-model="searchText" placeholder="搜索数据源名称" class="search-input"
        clearable @clear="loadData" @keyup.enter="loadData"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" width="180" />
      <el-table-column prop="type" label="类型" width="120">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.type)">{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="host" label="主机" width="180" />
      <el-table-column prop="port" label="端口" width="100" />
      <el-table-column prop="databaseName" label="数据库" width="180" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" fixed="right" width="220">
        <template #default="{ row }">
          <el-button size="small" @click="handleTest(row)">测试连接</el-button>
          <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除?" @confirm="handleDelete(row.id)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑数据源' : '新增数据源'" width="600px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="数据源名称"><el-input v-model="form.name" placeholder="请输入数据源名称" /></el-form-item>
        <el-form-item label="数据源类型">
          <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%;">
            <el-option label="MySQL" value="mysql" />
            <el-option label="PostgreSQL" value="postgresql" />
            <el-option label="ClickHouse" value="clickhouse" />
            <el-option label="Oracle" value="oracle" />
            <el-option label="Hive" value="hive" />
          </el-select>
        </el-form-item>
        <el-form-item label="主机"><el-input v-model="form.host" placeholder="例如：127.0.0.1" /></el-form-item>
        <el-form-item label="端口"><el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%;" /></el-form-item>
        <el-form-item label="数据库名"><el-input v-model="form.databaseName" placeholder="请输入数据库名称" /></el-form-item>
        <el-form-item label="用户名"><el-input v-model="form.username" placeholder="请输入用户名" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" placeholder="请输入密码" show-password /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
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
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { datasourceApi } from '@/api/dataDevelopment'

const tableData = ref([])
const loading = ref(false)
const searchText = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({ id: null, name: '', type: 'mysql', host: '', port: 3306, databaseName: '', username: '', password: '', status: 1 })

const typeTag = (type) => ({ mysql: '', postgresql: 'success', clickhouse: 'warning', oracle: 'danger', hive: 'warning' }[type] || 'info')

const loadData = async () => {
  loading.value = true
  try {
    const res = await datasourceApi.list()
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  form.value = { id: null, name: '', type: 'mysql', host: '', port: 3306, databaseName: '', username: '', password: '', status: 1 }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  form.value = { ...row, password: '' }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.value.name || !form.value.host) { ElMessage.warning('请填写必填项'); return }
  try {
    await datasourceApi.save(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (e) { ElMessage.error('保存失败') }
}

const handleTest = async (row) => {
  try {
    await datasourceApi.test({ id: row.id, password: '' })
    ElMessage.success('连接测试成功')
  } catch (e) { ElMessage.error('连接测试失败') }
}

const handleDelete = async (id) => {
  await datasourceApi.delete(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.search-input { width: 300px; }
</style>
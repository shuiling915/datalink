<template>
  <el-card>
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="domainId" placeholder="选择数据域" clearable class="domain-select" @change="loadData">
          <el-option v-for="d in domains" :key="d.id" :label="d.domainName" :value="d.id" />
        </el-select>
        <el-input
          v-model="keyword" placeholder="搜索汇总表" class="search-input"
          clearable @clear="loadData" @keyup.enter="loadData"
        >
          <template #append><el-button @click="loadData"><el-icon><Search /></el-icon></el-button></template>
        </el-input>
      </div>
      <el-button type="primary" @click="resetDialog"><el-icon><Plus /></el-icon>新建汇总表</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="summaryCode" label="汇总表编码" width="200" />
      <el-table-column prop="summaryName" label="汇总表名称" width="200" />
      <el-table-column prop="layer" label="层级" width="80" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'published' ? 'success' : row.status === 'deprecated' ? 'info' : ''">
            {{ row.status === 'published' ? '已发布' : row.status === 'deprecated' ? '已下线' : '开发中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="publishVersion" label="版本" width="80" />
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/summarytables/${row.id}`)">详情</el-button>
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="success" :disabled="row.status === 'published'" @click="handlePublish(row)">发布</el-button>
          <el-popconfirm title="确定删除?" @confirm="handleDelete(row.id)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pagination"
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @current-change="loadData"
      layout="total, prev, pager, next"
    />
  </el-card>

  <el-dialog v-model="showDialog" :title="isEdit ? '编辑汇总表' : '新建汇总表'" width="600px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="汇总表编码" required><el-input v-model="form.summaryCode" placeholder="如: dws_user_order_1d" /></el-form-item>
      <el-form-item label="汇总表名称" required><el-input v-model="form.summaryName" placeholder="如: 用户订单1天汇总表" /></el-form-item>
      <el-form-item label="所属数据域" required>
        <el-select v-model="form.domainId" placeholder="选择数据域" style="width: 100%;">
          <el-option v-for="d in domains" :key="d.id" :label="d.domainName" :value="d.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="层级">
        <el-select v-model="form.layer" style="width: 100%;">
          <el-option label="DWS" value="DWS" /><el-option label="ADS" value="ADS" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述"><el-input v-model="form.description" type="textarea" rows="3" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showDialog = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { summaryTableApi, dataDomainApi } from '@/api/datamodeling'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const domains = ref([])
const domainId = ref(null)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const showDialog = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const defaultForm = { summaryCode: '', summaryName: '', domainId: null, description: '', layer: 'DWS' }
const form = ref({ ...defaultForm })

const loadData = async () => {
  loading.value = true
  try {
    const res = await summaryTableApi.list({ pageNum: pageNum.value, pageSize: pageSize.value, domainId: domainId.value, keyword: keyword.value })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const loadDomains = async () => {
  const res = await dataDomainApi.listAll()
  domains.value = res.data
}

const resetDialog = () => {
  showDialog.value = true
  isEdit.value = false
  editId.value = null
  form.value = { ...defaultForm }
}

const handleEdit = (row) => {
  isEdit.value = true
  editId.value = row.id
  form.value = { ...row }
  showDialog.value = true
}

const handleDelete = async (id) => {
  await summaryTableApi.delete(id)
  ElMessage.success('删除成功')
  loadData()
}

const handlePublish = async (row) => {
  try {
    await summaryTableApi.publish(row.id, 'admin')
    ElMessage.success('发布成功')
    loadData()
  } catch (e) {
    ElMessage.error('发布失败: ' + (e.response?.data?.msg || e.message))
  }
}

const handleSubmit = async () => {
  if (isEdit.value) {
    await summaryTableApi.update(editId.value, form.value)
    ElMessage.success('更新成功')
  } else {
    await summaryTableApi.create(form.value)
    ElMessage.success('创建成功')
  }
  showDialog.value = false
  form.value = { ...defaultForm }
  isEdit.value = false
  loadData()
}

onMounted(() => {
  loadDomains()
  loadData()
})
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.toolbar-left { display: flex; gap: 10px; }
.domain-select { width: 200px; }
.search-input { width: 300px; }
.pagination { margin-top: 20px; justify-content: flex-end; }
</style>
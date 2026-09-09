<template>
  <el-card>
    <div style="margin-bottom: 20px; display: flex; justify-content: space-between;">
      <div style="display: flex; gap: 10px;">
        <el-select v-model="domainId" placeholder="选择数据域" clearable style="width: 200px;" @change="loadData">
          <el-option v-for="d in domains" :key="d.id" :label="d.domainName" :value="d.id" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索业务过程" style="width: 300px;" clearable @clear="loadData" @keyup.enter="loadData">
          <template #append><el-button @click="loadData"><el-icon><Search /></el-icon></el-button></template>
        </el-input>
      </div>
      <el-button type="primary" @click="showDialog = true"><el-icon><Plus /></el-icon>新建业务过程</el-button>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="processCode" label="业务过程编码" width="200" />
      <el-table-column prop="processName" label="业务过程名称" width="200" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除?" @confirm="handleDelete(row.id)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top: 20px; justify-content: flex-end;" v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" @current-change="loadData" layout="total, prev, pager, next" />
  </el-card>

  <el-dialog v-model="showDialog" :title="isEdit ? '编辑业务过程' : '新建业务过程'" width="600px">
    <el-form :model="form" label-width="120px">
      <el-form-item label="所属数据域" required>
        <el-select v-model="form.domainId" placeholder="选择数据域" style="width: 100%;">
          <el-option v-for="d in domains" :key="d.id" :label="d.domainName" :value="d.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="业务过程编码" required>
        <el-input v-model="form.processCode" placeholder="如: order_create, pay_success" />
      </el-form-item>
      <el-form-item label="业务过程名称" required>
        <el-input v-model="form.processName" placeholder="如: 创建订单, 支付成功" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" rows="3" />
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showDialog = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { businessProcessApi, dataDomainApi } from '@/api/datamodeling'
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
const form = ref({ domainId: null, processCode: '', processName: '', description: '', status: 1 })

const loadData = async () => {
  loading.value = true
  try {
    const res = await businessProcessApi.list({ pageNum: pageNum.value, pageSize: pageSize.value, domainId: domainId.value, keyword: keyword.value })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

const loadDomains = async () => {
  const res = await dataDomainApi.listAll()
  domains.value = res.data
}

const handleEdit = (row) => {
  isEdit.value = true
  editId.value = row.id
  form.value = { ...row }
  showDialog.value = true
}

const handleDelete = async (id) => {
  await businessProcessApi.delete(id)
  ElMessage.success('删除成功')
  loadData()
}

const handleSubmit = async () => {
  if (isEdit.value) {
    await businessProcessApi.update(editId.value, form.value)
    ElMessage.success('更新成功')
  } else {
    await businessProcessApi.create(form.value)
    ElMessage.success('创建成功')
  }
  showDialog.value = false
  form.value = { domainId: null, processCode: '', processName: '', description: '', status: 1 }
  isEdit.value = false
  loadData()
}

onMounted(() => { loadDomains(); loadData() })
</script>
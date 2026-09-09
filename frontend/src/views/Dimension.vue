<template>
  <el-card>
    <div style="margin-bottom: 20px; display: flex; justify-content: space-between;">
      <div style="display: flex; gap: 10px;">
        <el-select v-model="domainId" placeholder="选择数据域" clearable style="width: 200px;" @change="loadData">
          <el-option v-for="d in domains" :key="d.id" :label="d.domainName" :value="d.id" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索维度表" style="width: 300px;" clearable @clear="loadData" @keyup.enter="loadData">
          <template #append><el-button @click="loadData"><el-icon><Search /></el-icon></el-button></template>
        </el-input>
      </div>
      <el-button type="primary" @click="showDialog = true"><el-icon><Plus /></el-icon>新建维度表</el-button>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="dimCode" label="维度编码" width="200" />
      <el-table-column prop="dimName" label="维度名称" width="200" />
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
          <el-button link type="primary" @click="$router.push(`/dimensions/${row.id}`)">详情</el-button>
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="success" :disabled="row.status === 'published'" @click="handlePublish(row)">发布</el-button>
          <el-popconfirm title="确定删除?" @confirm="handleDelete(row.id)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top: 20px; justify-content: flex-end;" v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" @current-change="loadData" layout="total, prev, pager, next" />
  </el-card>

  <el-dialog v-model="showDialog" :title="isEdit ? '编辑维度表' : '新建维度表'" width="600px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="维度编码" required><el-input v-model="form.dimCode" placeholder="如: dim_user, dim_product" /></el-form-item>
      <el-form-item label="维度名称" required><el-input v-model="form.dimName" placeholder="如: 用户维度, 商品维度" /></el-form-item>
      <el-form-item label="所属数据域" required>
        <el-select v-model="form.domainId" placeholder="选择数据域" style="width: 100%;">
          <el-option v-for="d in domains" :key="d.id" :label="d.domainName" :value="d.id" />
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
import { dimensionApi, dataDomainApi } from '@/api/datamodeling'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false), tableData = ref([]), domains = ref([]), domainId = ref(null), keyword = ref('')
const pageNum = ref(1), pageSize = ref(20), total = ref(0)
const showDialog = ref(false), isEdit = ref(false), editId = ref(null)
const form = ref({ dimCode: '', dimName: '', domainId: null, description: '' })

const loadData = async () => {
  loading.value = true
  try { const res = await dimensionApi.list({ pageNum: pageNum.value, pageSize: pageSize.value, domainId: domainId.value, keyword: keyword.value }); tableData.value = res.data.records; total.value = res.data.total }
  finally { loading.value = false }
}
const loadDomains = async () => { const res = await dataDomainApi.listAll(); domains.value = res.data }
const handleEdit = (row) => { isEdit.value = true; editId.value = row.id; form.value = { ...row }; showDialog.value = true }
const handleDelete = async (id) => { await dimensionApi.delete(id); ElMessage.success('删除成功'); loadData() }
const handlePublish = async (row) => {
  try { await dimensionApi.publish(row.id, 'admin'); ElMessage.success('发布成功'); loadData() }
  catch (e) { ElMessage.error('发布失败: ' + (e.response?.data?.msg || e.message)) }
}
const handleSubmit = async () => {
  if (isEdit.value) { await dimensionApi.update(editId.value, form.value); ElMessage.success('更新成功') }
  else { await dimensionApi.create(form.value); ElMessage.success('创建成功') }
  showDialog.value = false; form.value = { dimCode: '', dimName: '', domainId: null, description: '' }; isEdit.value = false; loadData()
}
onMounted(() => { loadDomains(); loadData() })
</script>
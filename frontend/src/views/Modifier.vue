<template>
  <el-card>
    <div style="margin-bottom: 20px; display: flex; justify-content: space-between;">
      <el-input v-model="keyword" placeholder="搜索修饰词" style="width: 300px;" clearable @clear="loadData" @keyup.enter="loadData">
        <template #append><el-button @click="loadData"><el-icon><Search /></el-icon></el-button></template>
      </el-input>
      <el-button type="primary" @click="showDialog = true"><el-icon><Plus /></el-icon>新建修饰词</el-button>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="modifierCode" label="修饰词编码" width="150" />
      <el-table-column prop="modifierName" label="修饰词名称" width="150" />
      <el-table-column prop="modifierType" label="修饰词类型" width="120" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除?" @confirm="handleDelete(row.id)"><template #reference><el-button link type="danger">删除</el-button></template></el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top: 20px; justify-content: flex-end;" v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" @current-change="loadData" layout="total, prev, pager, next" />
  </el-card>

  <el-dialog v-model="showDialog" :title="isEdit ? '编辑修饰词' : '新建修饰词'" width="600px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="修饰词编码" required><el-input v-model="form.modifierCode" placeholder="如: online, app, new_user" /></el-form-item>
      <el-form-item label="修饰词名称" required><el-input v-model="form.modifierName" placeholder="如: 线上, APP端, 新用户" /></el-form-item>
      <el-form-item label="修饰词类型">
        <el-select v-model="form.modifierType" placeholder="选择类型" style="width: 100%;">
          <el-option label="渠道" value="渠道" /><el-option label="地区" value="地区" /><el-option label="用户类型" value="用户类型" /><el-option label="终端" value="终端" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述"><el-input v-model="form.description" type="textarea" rows="3" /></el-form-item>
      <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showDialog = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { modifierApi } from '@/api/datamodeling'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'

const loading = ref(false), tableData = ref([]), keyword = ref(''), pageNum = ref(1), pageSize = ref(20), total = ref(0)
const showDialog = ref(false), isEdit = ref(false), editId = ref(null)
const form = ref({ modifierCode: '', modifierName: '', modifierType: '', description: '', status: 1 })

const loadData = async () => {
  loading.value = true
  try { const res = await modifierApi.list({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value }); tableData.value = res.data.records; total.value = res.data.total }
  finally { loading.value = false }
}
const handleEdit = (row) => { isEdit.value = true; editId.value = row.id; form.value = { ...row }; showDialog.value = true }
const handleDelete = async (id) => { await modifierApi.delete(id); ElMessage.success('删除成功'); loadData() }
const handleSubmit = async () => {
  if (isEdit.value) { await modifierApi.update(editId.value, form.value); ElMessage.success('更新成功') }
  else { await modifierApi.create(form.value); ElMessage.success('创建成功') }
  showDialog.value = false; form.value = { modifierCode: '', modifierName: '', modifierType: '', description: '', status: 1 }; isEdit.value = false; loadData()
}
onMounted(loadData)
</script>
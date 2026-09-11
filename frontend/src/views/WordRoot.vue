<template>
  <el-card>
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="keyword" placeholder="搜索词根" class="search-input"
          clearable @clear="loadData" @keyup.enter="loadData"
        >
          <template #append><el-button @click="loadData"><el-icon><Search /></el-icon></el-button></template>
        </el-input>
      </div>
      <el-button type="primary" @click="resetDialog"><el-icon><Plus /></el-icon>新建词根</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="wordCode" label="词根编码" width="150" />
      <el-table-column prop="wordName" label="词根名称" width="150" />
      <el-table-column prop="wordType" label="词根类型" width="120" />
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

    <el-pagination class="pagination" v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" @current-change="loadData" layout="total, prev, pager, next" />
  </el-card>

  <el-dialog v-model="showDialog" :title="isEdit ? '编辑词根' : '新建词根'" width="600px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="词根编码" required><el-input v-model="form.wordCode" placeholder="如: amt, cnt, date" /></el-form-item>
      <el-form-item label="词根名称" required><el-input v-model="form.wordName" placeholder="如: 金额, 数量, 日期" /></el-form-item>
      <el-form-item label="词根类型">
        <el-select v-model="form.wordType" placeholder="选择类型" style="width: 100%;">
          <el-option label="时间" value="时间" /><el-option label="状态" value="状态" /><el-option label="类型" value="类型" /><el-option label="金额" value="金额" /><el-option label="数量" value="数量" />
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
import { onMounted } from 'vue'
import { wordRootApi } from '@/api/datamodeling'
import { useCrud } from '@/composables/useCrud'
import { Search, Plus } from '@element-plus/icons-vue'

const defaultForm = { wordCode: '', wordName: '', wordType: '', description: '', status: 1 }
const { loading, tableData, keyword, pageNum, pageSize, total, showDialog, isEdit, form, loadData, handleEdit, handleDelete, handleSubmit, resetDialog } = useCrud(wordRootApi, defaultForm)

onMounted(loadData)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.search-input { width: 300px; }
.pagination { margin-top: 20px; justify-content: flex-end; }
</style>
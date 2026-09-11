<template>
  <el-card>
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="keyword" placeholder="搜索数据域" class="search-input"
          clearable @clear="loadData" @keyup.enter="loadData"
        >
          <template #append>
            <el-button @click="loadData"><el-icon><Search /></el-icon></el-button>
          </template>
        </el-input>
      </div>
      <el-button type="primary" @click="resetDialog"><el-icon><Plus /></el-icon>新建数据域</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="domainCode" label="数据域编码" width="200" />
      <el-table-column prop="domainName" label="数据域名称" width="200" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="owner" label="负责人" width="120" />
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
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
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

  <el-dialog v-model="showDialog" :title="isEdit ? '编辑数据域' : '新建数据域'" width="600px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="数据域编码" required>
        <el-input v-model="form.domainCode" placeholder="如: trad, user, product" />
      </el-form-item>
      <el-form-item label="数据域名称" required>
        <el-input v-model="form.domainName" placeholder="如: 交易域, 用户域" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" rows="3" />
      </el-form-item>
      <el-form-item label="负责人">
        <el-input v-model="form.owner" />
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
import { onMounted } from 'vue'
import { dataDomainApi } from '@/api/datamodeling'
import { useCrud } from '@/composables/useCrud'
import { Search, Plus } from '@element-plus/icons-vue'

const defaultForm = { domainCode: '', domainName: '', description: '', owner: '', status: 1 }
const { loading, tableData, keyword, pageNum, pageSize, total, showDialog, isEdit, form, loadData, handleEdit, handleDelete, handleSubmit, resetDialog } = useCrud(dataDomainApi, defaultForm)

onMounted(loadData)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.search-input { width: 300px; }
.pagination { margin-top: 20px; justify-content: flex-end; }
</style>
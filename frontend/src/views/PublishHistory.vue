<template>
  <el-card>
    <div style="margin-bottom: 20px; display: flex; gap: 10px;">
      <el-select v-model="modelType" placeholder="模型类型" clearable style="width: 150px;" @change="loadData">
        <el-option label="维度表" value="dimension" /><el-option label="事实表" value="fact" /><el-option label="汇总表" value="summary" />
      </el-select>
      <el-button @click="loadData"><el-icon><Refresh /></el-icon>刷新</el-button>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="modelType" label="模型类型" width="100">
        <template #default="{ row }">
          <el-tag>{{ row.modelType === 'dimension' ? '维度表' : row.modelType === 'fact' ? '事实表' : '汇总表' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="modelCode" label="模型编码" width="200" />
      <el-table-column prop="modelName" label="模型名称" width="200" />
      <el-table-column prop="version" label="版本" width="80" />
      <el-table-column prop="publishStatus" label="发布状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.publishStatus === 'success' ? 'success' : 'danger'">
            {{ row.publishStatus === 'success' ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="publishMsg" label="发布消息" show-overflow-tooltip />
      <el-table-column prop="publishedBy" label="发布人" width="100" />
      <el-table-column prop="publishedAt" label="发布时间" width="180" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="primary" @click="showDDL(row)">查看DDL</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top: 20px; justify-content: flex-end;" v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" @current-change="loadData" layout="total, prev, pager, next" />
  </el-card>

  <el-dialog v-model="showDDLDialog" title="DDL内容" width="800px">
    <el-input v-model="currentDDL" type="textarea" :rows="20" readonly style="font-family: monospace;" />
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { publishHistoryApi } from '@/api/datamodeling'
import { Refresh } from '@element-plus/icons-vue'

const loading = ref(false), tableData = ref([]), modelType = ref(null)
const pageNum = ref(1), pageSize = ref(20), total = ref(0)
const showDDLDialog = ref(false), currentDDL = ref('')

const loadData = async () => {
  loading.value = true
  try { const res = await publishHistoryApi.list({ pageNum: pageNum.value, pageSize: pageSize.value, modelType: modelType.value }); tableData.value = res.data.records; total.value = res.data.total }
  finally { loading.value = false }
}

const showDDL = (row) => {
  currentDDL.value = row.ddlContent || '无DDL内容'
  showDDLDialog.value = true
}

onMounted(loadData)
</script>
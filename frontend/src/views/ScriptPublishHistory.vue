<template>
  <el-card>
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="filterEnv" placeholder="环境" clearable class="filter-select" @change="loadData">
          <el-option label="开发" value="dev" />
          <el-option label="生产" value="prod" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" clearable class="filter-select" @change="loadData">
          <el-option label="已发布" value="published" />
          <el-option label="已回滚" value="rolled_back" />
          <el-option label="灰度测试中" value="grayscale_testing" />
          <el-option label="失败" value="failed" />
        </el-select>
        <el-select v-model="filterType" placeholder="类型" clearable class="filter-select" @change="loadData">
          <el-option label="正式发布" value="normal" />
          <el-option label="回滚" value="rollback" />
        </el-select>
      </div>
      <el-button @click="loadData"><el-icon><Refresh /></el-icon>刷新</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="publishVersion" label="版本号" width="120" />
      <el-table-column prop="scriptName" label="脚本名称" min-width="180" show-overflow-tooltip />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.publishType === 'normal' ? 'success' : 'warning'" size="small">
            {{ row.publishType === 'normal' ? '正式发布' : '回滚' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag
            :type="row.publishStatus === 'published' ? 'success' : row.publishStatus === 'rolled_back' ? 'info' : row.publishStatus === 'grayscale_testing' ? 'warning' : 'danger'"
            size="small"
          >
            {{ statusLabel(row.publishStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="灰度测试" width="100">
        <template #default="{ row }">
          <template v-if="row.grayscaleEnabled">
            <el-tag v-if="row.grayscaleMatch === 1" type="success" size="small">一致</el-tag>
            <el-tag v-else-if="row.grayscaleMatch === 0" type="danger" size="small">不一致</el-tag>
            <el-tag v-else type="info" size="small">跳过</el-tag>
          </template>
          <span v-else class="text-disabled">未启用</span>
        </template>
      </el-table-column>
      <el-table-column prop="publishedBy" label="发布人" width="100" />
      <el-table-column prop="publishComment" label="发布说明" min-width="150" show-overflow-tooltip />
      <el-table-column prop="publishedAt" label="发布时间" width="170" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="showDetail(row)">详情</el-button>
          <el-popconfirm
            v-if="row.publishType === 'normal' && row.publishStatus === 'published'"
            title="确定回滚到此版本?"
            @confirm="handleRollback(row)"
          >
            <template #reference>
              <el-button link type="danger" size="small">回滚</el-button>
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

  <el-dialog v-model="detailVisible" title="发布详情" width="700px">
    <el-descriptions v-if="currentRow" :column="2" border size="small">
      <el-descriptions-item label="版本号">{{ currentRow.publishVersion }}</el-descriptions-item>
      <el-descriptions-item label="发布类型">
        <el-tag :type="currentRow.publishType === 'normal' ? 'success' : 'warning'" size="small">
          {{ currentRow.publishType === 'normal' ? '正式发布' : '回滚' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="currentRow.publishStatus === 'published' ? 'success' : 'info'" size="small">
          {{ statusLabel(currentRow.publishStatus) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="发布人">{{ currentRow.publishedBy }}</el-descriptions-item>
      <el-descriptions-item label="发布时间">{{ currentRow.publishedAt }}</el-descriptions-item>
      <el-descriptions-item label="发布说明">{{ currentRow.publishComment || '-' }}</el-descriptions-item>
      <el-descriptions-item label="灰度测试" :span="2">
        <template v-if="currentRow.grayscaleEnabled">
          <div>Dev行数: {{ currentRow.grayscaleDevRows }} | Prod行数: {{ currentRow.grayscaleProdRows }}</div>
          <div>Dev耗时: {{ currentRow.grayscaleDevTime }}ms | Prod耗时: {{ currentRow.grayscaleProdTime }}ms</div>
          <div>结果: {{ currentRow.grayscaleMatch === 1 ? '一致' : currentRow.grayscaleMatch === 0 ? '不一致' : '跳过' }}</div>
        </template>
        <span v-else>未启用</span>
      </el-descriptions-item>
      <el-descriptions-item label="开发环境脚本" :span="2">
        <el-input v-model="currentRow.devContent" type="textarea" :rows="6" readonly class="code-textarea" />
      </el-descriptions-item>
      <el-descriptions-item label="生产环境脚本(发布前)" :span="2" v-if="currentRow.prodContentBefore">
        <el-input v-model="currentRow.prodContentBefore" type="textarea" :rows="6" readonly class="code-textarea" />
      </el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { dataDevelopmentApi } from '@/api/dataDevelopment'

const loading = ref(false)
const tableData = ref([])
const filterEnv = ref(null)
const filterStatus = ref(null)
const filterType = ref(null)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const detailVisible = ref(false)
const currentRow = ref(null)

const statusLabel = (s) => {
  const map = { pending: '待发布', grayscale_testing: '灰度测试中', published: '已发布', failed: '失败', rolled_back: '已回滚' }
  return map[s] || s
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await dataDevelopmentApi.getAllPublishHistory({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      environment: filterEnv.value,
      publishStatus: filterStatus.value,
      publishType: filterType.value
    })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('加载发布历史失败', e)
  } finally {
    loading.value = false
  }
}

const showDetail = (row) => {
  currentRow.value = row
  detailVisible.value = true
}

const handleRollback = async (row) => {
  try {
    await dataDevelopmentApi.rollback(row.id, {
      publishedBy: 'admin',
      rollbackComment: `回滚到 ${row.publishVersion}`
    })
    ElMessage.success('回滚成功')
    loadData()
  } catch (e) {
    ElMessage.error('回滚失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.toolbar-left { display: flex; gap: 10px; }
.filter-select { width: 120px; }
.pagination { margin-top: 20px; justify-content: flex-end; }
.text-disabled { color: #c0c4cc; }
.code-textarea { font-family: monospace; }
</style>
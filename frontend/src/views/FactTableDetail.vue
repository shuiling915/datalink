<template>
  <div>
    <el-page-header @back="$router.back()" :title="'返回'" style="margin-bottom: 20px;">
      <template #content>
        <span style="font-size: 16px; font-weight: 500;">事实表详情 - {{ factTable?.factName }}</span>
      </template>
    </el-page-header>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card style="margin-bottom: 20px;">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span>字段设计</span>
              <el-button type="primary" size="small" @click="addField"><el-icon><Plus /></el-icon>添加字段</el-button>
            </div>
          </template>
          <el-table :data="fields" stripe>
            <el-table-column prop="fieldName" label="字段名" width="180">
              <template #default="{ row }"><el-input v-model="row.fieldName" size="small" /></template>
            </el-table-column>
            <el-table-column prop="fieldType" label="字段类型" width="120">
              <template #default="{ row }">
                <el-select v-model="row.fieldType" size="small">
                  <el-option label="STRING" value="string" /><el-option label="INT" value="int" />
                  <el-option label="BIGINT" value="bigint" /><el-option label="DECIMAL" value="decimal" />
                  <el-option label="DOUBLE" value="double" /><el-option label="DATETIME" value="datetime" />
                  <el-option label="BOOLEAN" value="boolean" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column prop="fieldCategory" label="字段类别" width="120">
              <template #default="{ row }">
                <el-select v-model="row.fieldCategory" size="small">
                  <el-option label="维度" value="dimension" /><el-option label="度量" value="measure" />
                  <el-option label="属性" value="attribute" /><el-option label="分区" value="partition" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column prop="fieldComment" label="字段注释">
              <template #default="{ row }"><el-input v-model="row.fieldComment" size="small" /></template>
            </el-table-column>
            <el-table-column prop="isPrimary" label="主键" width="80">
              <template #default="{ row }"><el-checkbox v-model="row.isPrimary" :true-value="1" :false-value="0" /></template>
            </el-table-column>
            <el-table-column prop="isPartition" label="分区" width="80">
              <template #default="{ row }"><el-checkbox v-model="row.isPartition" :true-value="1" :false-value="0" /></template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ $index }"><el-button link type="danger" size="small" @click="removeField($index)">删除</el-button></template>
            </el-table-column>
          </el-table>
          <div style="margin-top: 15px;">
            <el-button type="primary" @click="handleSaveFields">保存字段</el-button>
            <el-button @click="handleGenerateDDL">生成DDL</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <template #header>事实表信息</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="事实表编码">{{ factTable?.factCode }}</el-descriptions-item>
            <el-descriptions-item label="事实表名称">{{ factTable?.factName }}</el-descriptions-item>
            <el-descriptions-item label="事实表类型">
              {{ factTable?.factType === 'transaction' ? '事务事实表' : factTable?.factType === 'periodic_snapshot' ? '周期快照事实表' : '累积快照事实表' }}
            </el-descriptions-item>
            <el-descriptions-item label="层级">{{ factTable?.layer }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="factTable?.status === 'published' ? 'success' : ''">
                {{ factTable?.status === 'published' ? '已发布' : factTable?.status === 'deprecated' ? '已下线' : '开发中' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="版本">v{{ factTable?.publishVersion }}</el-descriptions-item>
            <el-descriptions-item label="描述">{{ factTable?.description }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="showDDLDialog" title="DDL预览" width="800px">
      <el-input v-model="ddlContent" type="textarea" :rows="20" readonly style="font-family: monospace;" />
      <template #footer>
        <el-button @click="showDDLDialog = false">关闭</el-button>
        <el-button type="primary" @click="handlePublish">发布到Hive</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { factTableApi } from '@/api/datamodeling'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const route = useRoute()
const factTable = ref(null)
const fields = ref([])
const showDDLDialog = ref(false)
const ddlContent = ref('')

const loadData = async () => {
  const res = await factTableApi.get(route.params.id)
  factTable.value = res.data.factTable
  fields.value = res.data.fields || []
}

const addField = () => {
  fields.value.push({ fieldName: '', fieldType: 'string', fieldComment: '', fieldCategory: 'dimension', isPrimary: 0, isPartition: 0 })
}

const removeField = (index) => {
  fields.value.splice(index, 1)
}

const handleSaveFields = async () => {
  await factTableApi.saveFields(route.params.id, fields.value)
  ElMessage.success('字段保存成功')
}

const handleGenerateDDL = async () => {
  const res = await factTableApi.generateDDL(route.params.id)
  ddlContent.value = res.data
  showDDLDialog.value = true
}

const handlePublish = async () => {
  try {
    await factTableApi.publish(route.params.id, 'admin')
    ElMessage.success('发布成功')
    showDDLDialog.value = false
    loadData()
  } catch (e) {
    ElMessage.error('发布失败: ' + (e.response?.data?.msg || e.message))
  }
}

onMounted(loadData)
</script>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * 标准CRUD Composable — 统一数据建模类页面的状态管理
 * @param {Object} api - 必须有 list, create, update, delete 方法
 * @param {Object} defaultForm - 表单默认值
 */
export function useCrud(api, defaultForm = {}) {
  const loading = ref(false)
  const tableData = ref([])
  const keyword = ref('')
  const pageNum = ref(1)
  const pageSize = ref(20)
  const total = ref(0)
  const showDialog = ref(false)
  const isEdit = ref(false)
  const editId = ref(null)
  const form = ref({ ...defaultForm })

  const loadData = async () => {
    loading.value = true
    try {
      const res = await api.list({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value })
      tableData.value = res.data.records || res.data || []
      total.value = res.data.total || 0
    } finally {
      loading.value = false
    }
  }

  const handleEdit = (row) => {
    isEdit.value = true
    editId.value = row.id
    form.value = { ...row }
    showDialog.value = true
  }

  const handleDelete = async (id) => {
    await api.delete(id)
    ElMessage.success('删除成功')
    loadData()
  }

  const handleSubmit = async () => {
    if (isEdit.value) {
      await api.update(editId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await api.create(form.value)
      ElMessage.success('创建成功')
    }
    showDialog.value = false
    form.value = { ...defaultForm }
    isEdit.value = false
    loadData()
  }

  const resetDialog = () => {
    showDialog.value = true
    isEdit.value = false
    editId.value = null
    form.value = { ...defaultForm }
  }

  return {
    loading, tableData, keyword, pageNum, pageSize, total,
    showDialog, isEdit, editId, form,
    loadData, handleEdit, handleDelete, handleSubmit, resetDialog
  }
}
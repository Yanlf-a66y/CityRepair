<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">类别管理</h1>
        <p class="muted">管理报修类别，新增、修改、启停和删除类别。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增类别</el-button>
    </div>

    <div class="panel" v-loading="loading">
      <el-table :data="categories" stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="categoryCode" label="类别编码" width="140" />
        <el-table-column prop="categoryName" label="类别名称" width="140" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序号" width="80" align="center" />
        <el-table-column label="启用状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled === 1"
              @change="(val: boolean) => handleToggle(row.id, val)"
              active-text="启用"
              inactive-text="停用"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-popconfirm
              title="确定删除该类别吗？"
              confirm-button-text="删除"
              cancel-button-text="取消"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && categories.length === 0" description="暂无比别数据" />
    </div>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑类别' : '新增类别'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        @submit.prevent
      >
        <el-form-item label="类别编码" prop="categoryCode">
          <el-input
            v-model="form.categoryCode"
            placeholder="如 ROAD, LIGHTING"
            :disabled="isEdit"
            maxlength="40"
          />
        </el-form-item>
        <el-form-item label="类别名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="如 道路破损" maxlength="80" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="类别描述（选填）"
            maxlength="255"
          />
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getCategories,
  createCategory,
  updateCategory,
  toggleCategoryStatus,
  deleteCategory,
  type RepairCategory,
} from '@/api/systemApi'

// ===== 列表数据 =====

const loading = ref(false)
const categories = ref<RepairCategory[]>([])

async function loadCategories() {
  loading.value = true
  try {
    const res = await getCategories()
    categories.value = res.data.data
  } finally {
    loading.value = false
  }
}

// ===== 启停 =====

async function handleToggle(id: number, enabled: boolean) {
  try {
    await toggleCategoryStatus(id)
    ElMessage.success(enabled ? '已启用' : '已停用')
    await loadCategories()
  } catch {
    // 错误已在拦截器中提示
  }
}

// ===== 删除 =====

async function handleDelete(id: number) {
  try {
    await deleteCategory(id)
    ElMessage.success('删除成功')
    await loadCategories()
  } catch {
    // 错误已在拦截器中提示（如有关联工单）
  }
}

// ===== 新增 / 编辑弹窗 =====

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const editId = ref<number | null>(null)

const form = reactive({
  categoryCode: '',
  categoryName: '',
  description: '',
  sortOrder: 0,
})

const rules: FormRules = {
  categoryCode: [
    { required: true, message: '请输入类别编码', trigger: 'blur' },
    { pattern: /^[A-Z_]+$/, message: '编码只能包含大写字母和下划线', trigger: 'blur' },
  ],
  categoryName: [{ required: true, message: '请输入类别名称', trigger: 'blur' }],
}

function openDialog(row?: RepairCategory) {
  if (row) {
    isEdit.value = true
    editId.value = row.id!
    form.categoryCode = row.categoryCode
    form.categoryName = row.categoryName
    form.description = row.description || ''
    form.sortOrder = row.sortOrder
  } else {
    isEdit.value = false
    editId.value = null
    form.categoryCode = ''
    form.categoryName = ''
    form.description = ''
    form.sortOrder = 0
  }
  formRef.value?.resetFields()
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const data = { ...form }
    if (isEdit.value && editId.value != null) {
      await updateCategory(editId.value, data)
      ElMessage.success('修改成功')
    } else {
      await createCategory(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await loadCategories()
  } catch {
    // 错误已在拦截器中提示
  } finally {
    saving.value = false
  }
}

// ===== 初始化 =====

onMounted(loadCategories)
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
</style>

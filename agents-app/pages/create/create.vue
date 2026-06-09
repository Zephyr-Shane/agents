<template>
  <view class="create-page">
    <!-- 空列表状态 -->
    <view v-if="agentStore.agents.length === 0" class="empty-state">
      <view class="empty-icon">🤖</view>
      <text class="empty-title">还没有创建智能体</text>
      <text class="empty-desc">点击下方按钮创建你的第一个 AI 智能体</text>
    </view>

    <!-- 智能体列表 -->
    <scroll-view v-else class="agent-list" scroll-y>
      <view v-for="agent in agentStore.agents" :key="agent.id" class="agent-card">
        <view class="card-avatar">{{ agent.name ? agent.name[0] : '?' }}</view>
        <view class="card-body">
          <view class="card-top">
            <text class="card-name">{{ agent.name }}</text>
            <text class="card-badge" :class="{ public: agent.isPublic === 1, private: agent.isPublic !== 1 }">
              {{ agent.isPublic === 1 ? '公开' : '私有' }}
            </text>
          </view>
          <text class="card-desc">{{ agent.description || '暂无描述' }}</text>
          <text class="card-time">{{ formatTime(agent.createTime) }}</text>
        </view>
        <view class="card-actions">
          <view class="ca-btn" @tap.stop="showEdit(agent)">✏️</view>
          <view class="ca-btn" @tap.stop="confirmDelete(agent.id)">🗑️</view>
        </view>
      </view>
    </scroll-view>

    <!-- 创建 FAB -->
    <view class="fab-btn" @tap="showCreateForm = true">
      <text class="fab-icon">＋</text>
    </view>

    <!-- ======== 创建智能体弹窗 ======== -->
    <view v-if="showCreateForm" class="dialog-mask" @tap="showCreateForm = false">
      <view class="dialog-box" @tap.stop>
        <text class="dialog-title">创建智能体</text>

        <view class="form-field">
          <text class="field-label">智能体名称 <text class="required">*</text></text>
          <input class="field-input" v-model="form.name" placeholder="给你的智能体取个名字" maxlength="30" />
        </view>
        <view class="form-field">
          <text class="field-label">功能描述 <text class="required">*</text></text>
          <textarea class="field-textarea" v-model="form.agentDescription" placeholder="描述功能和用途" maxlength="500" />
        </view>

        <!-- 公开开关 -->
        <view class="public-toggle">
          <view class="toggle-left">
            <text class="toggle-title">🌐 公开智能体</text>
            <text class="toggle-desc">开启后其他人可以搜索并使用</text>
          </view>
          <switch :checked="form.isPublic === 1" @change="form.isPublic = $event ? 1 : 0" color="#07C160" />
        </view>

        <!-- 高级设定折叠 -->
        <view class="advanced-toggle" @tap="showAdvanced = !showAdvanced">
          <text>⚙️ 更多高级设定</text>
          <text>{{ showAdvanced ? '▲' : '▼' }}</text>
        </view>
        <view v-if="showAdvanced" class="advanced-fields">
          <view class="form-field">
            <text class="field-label">介绍</text>
            <textarea class="field-textarea" v-model="form.introduction" placeholder="智能体的详细介绍" maxlength="1000" />
          </view>
          <view class="form-field">
            <text class="field-label">开场白</text>
            <textarea class="field-textarea" v-model="form.openingLine" placeholder="首次对话时智能体发送的欢迎语" maxlength="300" />
          </view>
        </view>

        <view class="dialog-actions">
          <view class="dbtn cancel" @tap="showCreateForm = false">取消</view>
          <view class="dbtn primary" :class="{ disabled: !isFormValid || submitting }" @tap="doCreate">
            {{ submitting ? '创建中...' : '开始创建' }}
          </view>
        </view>
      </view>
    </view>

    <!-- ======== 编辑弹窗 ======== -->
    <view v-if="editDialog.show" class="dialog-mask" @tap="editDialog.show = false">
      <view class="dialog-box" @tap.stop>
        <text class="dialog-title">编辑智能体</text>

        <view class="form-field">
          <text class="field-label">名称</text>
          <input class="field-input" v-model="editDialog.name" maxlength="30" />
        </view>
        <view class="form-field">
          <text class="field-label">描述</text>
          <textarea class="field-textarea" v-model="editDialog.agentDescription" maxlength="500" />
        </view>
        <view class="form-field">
          <text class="field-label">介绍</text>
          <textarea class="field-textarea" v-model="editDialog.introduction" maxlength="1000" />
        </view>
        <view class="form-field">
          <text class="field-label">开场白</text>
          <textarea class="field-textarea" v-model="editDialog.openingLine" maxlength="300" />
        </view>

        <view class="public-toggle" style="padding:16rpx 0">
          <view class="toggle-left"><text class="toggle-title">🌐 公开</text></view>
          <switch :checked="editDialog.isPublic === 1" @change="editDialog.isPublic = $event ? 1 : 0" color="#07C160" />
        </view>

        <view class="dialog-actions">
          <view class="dbtn cancel" @tap="editDialog.show = false">取消</view>
          <view class="dbtn primary" @tap="doUpdate">保存</view>
        </view>
      </view>
    </view>

    <!-- 创建成功弹窗 -->
    <view v-if="showSuccess" class="dialog-mask" @tap="showSuccess=false">
      <view class="dialog-box" @tap.stop>
        <text style="font-size:64rpx">🎉</text>
        <text class="dialog-title">创建完成</text>
        <text class="dialog-desc">智能体「{{ createdName }}」已创建成功！</text>
        <view class="dialog-actions">
          <view class="dbtn primary" @tap="goToChat">去对话</view>
          <view class="dbtn cancel" @tap="showSuccess=false">继续创建</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAgentStore } from '@/stores/agent'
import { useAuthStore } from '@/stores/auth'

const agentStore = useAgentStore()
const authStore = useAuthStore()

const showCreateForm = ref(false)
const showAdvanced = ref(false)
const submitting = ref(false)
const showSuccess = ref(false)
const createdName = ref('')

const form = reactive({
  name: '', agentDescription: '', introduction: '',
  openingLine: '', avatar: '', isPublic: 0
})

const editDialog = reactive({
  show: false, agentId: null,
  name: '', agentDescription: '', introduction: '', openingLine: '', isPublic: 0
})

const isFormValid = computed(() => form.name.trim().length > 0 && form.agentDescription.trim().length > 0)

onMounted(() => loadAgents())
onShow(() => {
  if (authStore.ready && !authStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  loadAgents()
})

async function loadAgents() {
  if (authStore.isLoggedIn) await agentStore.loadAgents()
}

async function doCreate() {
  if (!isFormValid.value || submitting.value) return
  submitting.value = true
  try {
    const agent = await agentStore.createAgent({
      name: form.name.trim(),
      agentDescription: form.agentDescription.trim(),
      introduction: form.introduction.trim(),
      openingLine: form.openingLine.trim(),
      avatar: form.avatar, isPublic: form.isPublic
    })
    createdName.value = agent.name || '新智能体'
    showSuccess.value = true
    showCreateForm.value = false
    showAdvanced.value = false
    form.name = ''; form.agentDescription = ''; form.introduction = ''
    form.openingLine = ''; form.avatar = ''; form.isPublic = 0
  } catch (e) {
    uni.showToast({ title: e.message || '创建失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

function showEdit(agent) {
  editDialog.agentId = agent.id
  editDialog.name = agent.name || ''
  editDialog.agentDescription = agent.description || ''
  editDialog.introduction = agent.introduction || ''
  editDialog.openingLine = agent.openingLine || ''
  editDialog.isPublic = agent.isPublic || 0
  editDialog.show = true
}

async function doUpdate() {
  try {
    await agentStore.updateAgent(editDialog.agentId, {
      name: editDialog.name.trim(),
      agentDescription: editDialog.agentDescription.trim(),
      introduction: editDialog.introduction.trim(),
      openingLine: editDialog.openingLine.trim(),
      isPublic: editDialog.isPublic
    })
    editDialog.show = false
    uni.showToast({ title: '保存成功', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e.message || '保存失败', icon: 'none' })
  }
}

function confirmDelete(agentId) {
  uni.showModal({
    title: '提示',
    content: '确定要删除这个智能体吗？删除后不可恢复。',
    success: (res) => {
      if (res.confirm) {
        agentStore.removeAgent(agentId)
        uni.showToast({ title: '已删除', icon: 'success' })
      }
    }
  })
}

function goToChat() {
  showSuccess.value = false
  uni.switchTab({ url: '/pages/dialogue/dialogue' })
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t); const now = new Date()
  const diff = now - d
  if (diff < 86400000 && d.getDate() === now.getDate())
    return `今天 ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (d.toDateString() === yesterday.toDateString()) return '昨天'
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}
</script>

<style scoped>
.create-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #F5F7FA;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 240rpx;
}
.empty-icon { font-size: 120rpx; margin-bottom: 24rpx; opacity: 0.6; }
.empty-title { font-size: 32rpx; font-weight: 600; color: #333; margin-bottom: 12rpx; }
.empty-desc { font-size: 26rpx; color: #999; }

/* 列表 */
.agent-list {
  flex: 1;
  padding: 24rpx;
  padding-bottom: 160rpx;
  box-sizing: border-box;
}
.agent-card {
  display: flex;
  align-items: flex-start;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  gap: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
}
.card-avatar {
  width: 80rpx; height: 80rpx; border-radius: 18rpx;
  background: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);
  display: flex; align-items: center; justify-content: center;
  font-size: 36rpx; color: #fff; font-weight: 600; flex-shrink: 0;
}
.card-body { flex: 1; min-width: 0; }
.card-top { display: flex; align-items: center; gap: 12rpx; margin-bottom: 8rpx; }
.card-name { font-size: 30rpx; font-weight: 600; color: #1A1A1A; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 70%; }
.card-badge { font-size: 20rpx; padding: 4rpx 14rpx; border-radius: 20rpx; flex-shrink: 0; }
.card-badge.public { background: #EDE7F6; color: #5E35B1; }
.card-badge.private { background: #F5F5F5; color: #999; }
.card-desc { font-size: 24rpx; color: #666; line-height: 1.4; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 4rpx; }
.card-time { font-size: 20rpx; color: #BBB; }
.card-actions { display: flex; flex-direction: column; gap: 12rpx; flex-shrink: 0; }
.ca-btn { width: 52rpx; height: 52rpx; display: flex; align-items: center; justify-content: center; font-size: 28rpx; background: #F5F7FA; border-radius: 14rpx; }

/* FAB */
.fab-btn {
  position: fixed; bottom: 140rpx; left: 50%; transform: translateX(-50%);
  width: 120rpx; height: 120rpx; border-radius: 60rpx;
  background: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.4); z-index: 999;
}
.fab-icon { font-size: 56rpx; color: #FFFFFF; font-weight: 300; line-height: 1; }

/* 弹窗通用 */
.dialog-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.dialog-box {
  background: #fff; border-radius: 28rpx; padding: 40rpx 32rpx 32rpx;
  width: 620rpx; max-height: 80vh; overflow-y: auto;
}
.dialog-title {
  font-size: 34rpx; font-weight: 700; color: #1A1A1A;
  text-align: center; margin-bottom: 28rpx; display: block;
}
.dialog-desc { font-size: 28rpx; color: #666; margin: 12rpx 0 32rpx; text-align: center; display: block; }
.dialog-actions { display: flex; gap: 20rpx; margin-top: 32rpx; }
.dbtn { flex: 1; height: 76rpx; border-radius: 38rpx; display: flex; align-items: center; justify-content: center; font-size: 28rpx; font-weight: 500; }
.dbtn.cancel { background: #F5F7FA; color: #666; }
.dbtn.primary { background: linear-gradient(135deg, #667EEA 0%, #764BA2 100%); color: #fff; }
.dbtn.primary.disabled { opacity: 0.5; }

/* 表单字段 */
.form-field { margin-bottom: 24rpx; }
.field-label { font-size: 26rpx; color: #333; display: block; margin-bottom: 12rpx; }
.required { color: #FF3B30; }
.field-input { height: 76rpx; background: #F5F7FA; border-radius: 16rpx; padding: 0 24rpx; font-size: 28rpx; width: 100%; box-sizing: border-box; }
.field-textarea { width: 100%; min-height: 120rpx; background: #F5F7FA; border-radius: 16rpx; padding: 16rpx 24rpx; font-size: 26rpx; box-sizing: border-box; line-height: 1.5; }
.public-toggle { display: flex; align-items: center; justify-content: space-between; padding: 8rpx 0; }
.toggle-left { flex: 1; }
.toggle-title { font-size: 28rpx; font-weight: 500; color: #1A1A1A; display: block; }
.toggle-desc { font-size: 22rpx; color: #999; margin-top: 4rpx; display: block; }
.advanced-toggle { display: flex; justify-content: space-between; align-items: center; padding: 16rpx 0; font-size: 26rpx; color: #666; }
.advanced-fields { padding-top: 8rpx; }
</style>

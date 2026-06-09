import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getMyAgents, deleteAgent, createAgent as createAgentApi, updateAgent as updateAgentApi } from '@/api/agent'

export const useAgentStore = defineStore('agent', () => {
  const agents = ref([])
  const currentAgentId = ref(null)
  const currentAgent = computed(() => agents.value.find(a => a.id === currentAgentId.value) || null)

  async function loadAgents() {
    const res = await getMyAgents()
    if (res.code === 0) agents.value = res.data
  }

  function selectAgent(agentId) { currentAgentId.value = agentId }

  async function removeAgent(agentId) {
    await deleteAgent(agentId)
    agents.value = agents.value.filter(a => a.id !== agentId)
    if (currentAgentId.value === agentId) currentAgentId.value = null
  }

  /**
   * 结构化创建智能体
   * @param {Object} data - { name, agentDescription, introduction, openingLine, avatar, isPublic }
   */
  async function createAgent(data) {
    const res = await createAgentApi(data)
    if (res.code === 0) {
      agents.value.unshift(res.data)
      return res.data
    }
    throw new Error(res.message || '创建失败')
  }

  /**
   * 更新智能体设置
   */
  async function updateAgent(agentId, data) {
    const res = await updateAgentApi(agentId, data)
    if (res.code === 0) {
      const idx = agents.value.findIndex(a => a.id === agentId)
      if (idx >= 0) agents.value[idx] = { ...agents.value[idx], ...res.data }
      return res.data
    }
    throw new Error(res.message || '更新失败')
  }

  return {
    agents, currentAgentId, currentAgent,
    loadAgents, selectAgent, removeAgent, createAgent, updateAgent
  }
})

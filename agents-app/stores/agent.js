import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getMyAgents, deleteAgent } from '@/api/agent'

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

  return { agents, currentAgentId, currentAgent, loadAgents, selectAgent, removeAgent }
})

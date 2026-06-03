import { get, post, del } from './request'

export function getMyAgents() {
  return get('/agents')
}

export function getAgentDetail(id) {
  return get(`/agents/${id}`)
}

export function createAgentFromNL(description) {
  return post('/agents/create-from-nl', { description })
}

export function updateAgentFromNL(agentId, description) {
  return post('/agents/update-from-nl', { agentId, description })
}

export function deleteAgent(id) {
  return del(`/agents/${id}`)
}

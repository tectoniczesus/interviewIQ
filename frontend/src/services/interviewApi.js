import api from './api'

export async function createInterviewSession(resumeId, jobId, role) {
  const res = await api.post('/api/interviews', { resumeId, jobId, role })
  return res.data.data
}

export async function listInterviewSessions() {
  const res = await api.get('/api/interviews')
  return res.data.data
}

export async function getInterviewSession(id) {
  const res = await api.get(`/api/interviews/${id}`)
  return res.data.data
}

export async function getInterviewQuestions(id, category) {
  const res = await api.get(`/api/interviews/${id}/questions`, {
    params: category ? { category } : {},
  })
  return res.data.data
}

export async function regenerateInterviewQuestions(id) {
  const res = await api.post(`/api/interviews/${id}/regenerate`)
  return res.data.data
}

export async function deleteInterviewSession(id) {
  await api.delete(`/api/interviews/${id}`)
}
import api from './api'

export async function createJobMatch(resumeId, jobId) {
  const res = await api.post('/api/job-matches', { resumeId, jobId })
  return res.data.data
}

export async function listJobMatches() {
  const res = await api.get('/api/job-matches')
  return res.data.data
}

export async function getJobMatch(id) {
  const res = await api.get(`/api/job-matches/${id}`)
  return res.data.data
}
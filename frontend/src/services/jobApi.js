import api from './api'

export async function createJob(payload) {
  const res = await api.post('/api/jobs', payload)
  return res.data.data
}

export async function createJobFromUrl(payload) {
  const res = await api.post('/api/jobs/from-url', payload)
  return res.data.data
}

export async function listJobs() {
  const res = await api.get('/api/jobs')
  return res.data.data
}

export async function getJob(id) {
  const res = await api.get(`/api/jobs/${id}`)
  return res.data.data
}

export async function deleteJob(id) {
  await api.delete(`/api/jobs/${id}`)
}
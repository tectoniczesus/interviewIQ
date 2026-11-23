import api from './api'

// Applications are keyed by job id: every saved job has exactly one.
export async function listApplications() {
  const res = await api.get('/api/applications')
  return res.data.data
}

export async function getApplication(jobId) {
  const res = await api.get(`/api/applications/${jobId}`)
  return res.data.data
}

/** Full replacement of { status, appliedOn, followUpOn, notes }. */
export async function updateApplication(jobId, payload) {
  const res = await api.put(`/api/applications/${jobId}`, payload)
  return res.data.data
}

export async function listApplicationEvents(jobId) {
  const res = await api.get(`/api/applications/${jobId}/events`)
  return res.data.data
}

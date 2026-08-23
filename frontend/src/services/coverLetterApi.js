import api from './api'

export async function createCoverLetter(resumeId, jobId, candidateName) {
  const res = await api.post('/api/cover-letters', { resumeId, jobId, candidateName })
  return res.data.data
}

export async function listCoverLetters() {
  const res = await api.get('/api/cover-letters')
  return res.data.data
}

export async function getCoverLetter(id) {
  const res = await api.get(`/api/cover-letters/${id}`)
  return res.data.data
}

export async function deleteCoverLetter(id) {
  await api.delete(`/api/cover-letters/${id}`)
}

export async function regenerateCoverLetter(id) {
  const res = await api.post(`/api/cover-letters/${id}/regenerate`)
  return res.data.data
}
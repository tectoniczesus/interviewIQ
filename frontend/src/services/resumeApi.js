import api from './api'

export async function uploadResume(file) {
  const formData = new FormData()
  formData.append('file', file)

  const res = await api.post('/api/resumes', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return res.data.data
}

export async function listResumes() {
  const res = await api.get('/api/resumes')
  return res.data.data
}

export async function getResume(id) {
  const res = await api.get(`/api/resumes/${id}`)
  return res.data.data
}

export async function deleteResume(id) {
  await api.delete(`/api/resumes/${id}`)
}

export async function analyzeResume(id) {
  const res = await api.post(`/api/resumes/${id}/analyze`)
  return res.data.data
}

export async function getResumeAnalysis(id) {
  const res = await api.get(`/api/resumes/${id}/analysis`)
  return res.data.data
}
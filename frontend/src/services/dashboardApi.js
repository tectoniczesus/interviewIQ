import api from './api'

export async function getDashboardSummary() {
  const res = await api.get('/api/dashboard')
  return res.data.data
}
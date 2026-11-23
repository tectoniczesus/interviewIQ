import api from './api'

/** Generates (or regenerates, which resets progress) the roadmap for a job match. */
export async function generateRoadmap(jobMatchId, hoursPerWeek) {
  const res = await api.post(`/api/job-matches/${jobMatchId}/roadmap`, { hoursPerWeek })
  return res.data.data
}

export async function listRoadmaps() {
  const res = await api.get('/api/roadmaps')
  return res.data.data
}

export async function getRoadmap(id) {
  const res = await api.get(`/api/roadmaps/${id}`)
  return res.data.data
}

export async function setRoadmapTaskCompleted(roadmapId, taskId, completed) {
  const res = await api.patch(
    `/api/roadmaps/${roadmapId}/tasks/${encodeURIComponent(taskId)}`,
    { completed }
  )
  return res.data.data
}

export async function deleteRoadmap(id) {
  await api.delete(`/api/roadmaps/${id}`)
}

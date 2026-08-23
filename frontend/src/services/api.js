import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

/**
 * The rest of the app should import `api` from here rather than creating
 * new axios instances - this keeps base URL, auth, and error handling in
 * one place (see section 24 of the spec: "centralized API client").
 */
export const api = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Clerk's getToken() is a hook-bound async function, so it can't be
// imported directly into this plain module. useAuthSync (called once near
// the root of the app) registers it here on every render, and the request
// interceptor below reads whatever was last registered.
let getAuthToken = async () => null

export function registerAuthTokenGetter(fn) {
  getAuthToken = fn
}

api.interceptors.request.use(async (config) => {
  const token = await getAuthToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Normalize backend error envelope: { timestamp, status, error, message, path }
    const message =
      error.response?.data?.message || error.message || 'Something went wrong'
    const wrapped = new Error(message)
    wrapped.status = error.response?.status
    return Promise.reject(wrapped)
  }
)

export default api
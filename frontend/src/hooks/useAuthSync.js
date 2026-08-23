import { useEffect } from 'react'
import { useAuth } from '@clerk/clerk-react'
import { registerAuthTokenGetter } from '../services/api'

/**
 * Call once near the root of the app (see App.jsx). Keeps the shared
 * axios instance able to attach a fresh Clerk session token to every
 * outgoing request without every page needing to know about Clerk.
 */
export default function useAuthSync() {
  const { getToken } = useAuth()

  useEffect(() => {
    registerAuthTokenGetter(() => getToken())
  }, [getToken])
}

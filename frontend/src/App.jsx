import { Routes, Route, Navigate } from 'react-router-dom'
import useAuthSync from './hooks/useAuthSync'
import ProtectedRoute from './routes/ProtectedRoute'
import MainLayout from './layouts/MainLayout'

import Login from './pages/Login'
import SignUpPage from './pages/SignUpPage'
import Dashboard from './pages/Dashboard'
import Resumes from './pages/Resumes'
import ResumeAnalysis from './pages/ResumeAnalysis'
import Jobs from './pages/Jobs'
import JobMatch from './pages/JobMatch'
import CoverLetters from './pages/CoverLetters'
import InterviewPreparation from './pages/InterviewPreparation'

export default function App() {
  // Keeps the shared axios client's bearer token in sync with Clerk on every render.
  useAuthSync()

  return (
    <Routes>
      <Route path="/login/*" element={<Login />} />
      <Route path="/sign-up/*" element={<SignUpPage />} />

      <Route
        element={
          <ProtectedRoute>
            <MainLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/resumes" element={<Resumes />} />
        <Route path="/resumes/:id/analysis" element={<ResumeAnalysis />} />
        <Route path="/jobs" element={<Jobs />} />
        <Route path="/jobs/:id/match" element={<JobMatch />} />
        <Route path="/cover-letters" element={<CoverLetters />} />
        <Route path="/interview-prep" element={<InterviewPreparation />} />
      </Route>

      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}

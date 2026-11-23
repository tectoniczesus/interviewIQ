import { Suspense, lazy } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import useAuthSync from './hooks/useAuthSync'
import ProtectedRoute from './routes/ProtectedRoute'
import MainLayout from './layouts/MainLayout'

import Dashboard from './pages/Dashboard'

// Dashboard is the landing route, so it ships in the main chunk. Every other page
// loads on demand, which keeps the first paint (and Clerk's login screen) light.
const Login = lazy(() => import('./pages/Login'))
const SignUpPage = lazy(() => import('./pages/SignUpPage'))
const Resumes = lazy(() => import('./pages/Resumes'))
const ResumeAnalysis = lazy(() => import('./pages/ResumeAnalysis'))
const Jobs = lazy(() => import('./pages/Jobs'))
const Applications = lazy(() => import('./pages/Applications'))
const JobMatch = lazy(() => import('./pages/JobMatch'))
const CoverLetters = lazy(() => import('./pages/CoverLetters'))
const InterviewPreparation = lazy(() => import('./pages/InterviewPreparation'))
const Roadmaps = lazy(() => import('./pages/Roadmaps'))
const LearningRoadmap = lazy(() => import('./pages/LearningRoadmap'))

export default function App() {
  // Keeps the shared axios client's bearer token in sync with Clerk on every render.
  useAuthSync()

  return (
    <Suspense fallback={null}>
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
        <Route path="/applications" element={<Applications />} />
        <Route path="/cover-letters" element={<CoverLetters />} />
        <Route path="/interview-prep" element={<InterviewPreparation />} />
        <Route path="/roadmaps" element={<Roadmaps />} />
        <Route path="/roadmaps/:id" element={<LearningRoadmap />} />
      </Route>

      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
    </Suspense>
  )
}

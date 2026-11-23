import { SignIn } from '@clerk/clerk-react'

export default function Login() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-paper px-4 py-8">
      <div className="mb-8 font-display text-2xl font-semibold tracking-tight text-ink">
        InterviewIQ
      </div>
      <SignIn routing="path" path="/login" signUpUrl="/sign-up" afterSignInUrl="/dashboard" />
    </div>
  )
}
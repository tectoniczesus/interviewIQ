import { SignUp } from '@clerk/clerk-react'

export default function SignUpPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-paper">
      <div className="mb-8 font-display text-2xl font-semibold tracking-tight text-ink">
        InterviewIQ
      </div>
      <SignUp routing="path" path="/sign-up" signInUrl="/login" afterSignUpUrl="/dashboard" />
    </div>
  )
}
"use client"

import { LoginForm } from "@/components/auth/login-form"
import { COMPONENT_TEMPLATES } from "@/lib/style-constants"

export default function LoginPage() {
  return (
    <div className={COMPONENT_TEMPLATES.pageContainer}>
      <div className="flex items-center justify-center min-h-screen p-4">
        <div className="w-full max-w-md">
          <LoginForm />
        </div>
      </div>
    </div>
  )
}

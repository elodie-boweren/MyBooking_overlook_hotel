"use client"

import { RegisterForm } from "@/components/auth/register-form"
import { COMPONENT_TEMPLATES } from "@/lib/style-constants"

export default function RegisterPage() {
  return (
    <div className={COMPONENT_TEMPLATES.pageContainer}>
      <div className="flex items-center justify-center min-h-screen p-4">
        <div className="w-full max-w-md">
          <RegisterForm />
        </div>
      </div>
    </div>
  )
}
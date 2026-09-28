"use client"

import type React from "react"
import { GeistSans } from "geist/font/sans"
import { GeistMono } from "geist/font/mono"
import { Analytics } from "@vercel/analytics/next"
import { AuthProvider } from "@/components/auth-provider"
import { Navigation } from "@/components/navigation"
import { Suspense } from "react"
import { usePathname } from "next/navigation"

function ConditionalNavigation() {
  const pathname = usePathname()

  // Don't show main navigation on admin, employee routes, start page, login page, and register page
  if (pathname?.startsWith("/employee") || 
      pathname?.startsWith("/admin") || 
      pathname === "/" || 
      pathname === "/login" ||
      pathname === "/register") {
    return null
  }

  return <Navigation />
}

export function ClientLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en">
      <body className={`font-sans ${GeistSans.variable} ${GeistMono.variable}`}>
        <Suspense fallback={<div>Loading...</div>}>
          <AuthProvider>
            <ConditionalNavigation />
            {children}
          </AuthProvider>
        </Suspense>
        <Analytics />
      </body>
    </html>
  )
}

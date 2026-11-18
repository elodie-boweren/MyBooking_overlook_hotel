"use client"

import { useState, useEffect } from 'react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

export default function DebugAuthPage() {
  const [email, setEmail] = useState('admin@example.com')
  const [password, setPassword] = useState('Pass123@')
  const [result, setResult] = useState<string>('')
  const [isLoading, setIsLoading] = useState(false)
  const [localStorageData, setLocalStorageData] = useState<{token: string | null, user: string | null}>({token: null, user: null})

  // Safely access localStorage on client side
  useEffect(() => {
    if (typeof window !== 'undefined') {
      setLocalStorageData({
        token: localStorage.getItem('token'),
        user: localStorage.getItem('user')
      })
    }
  }, [])

  const testLogin = async () => {
    setIsLoading(true)
    setResult('Testing login...')

    try {
      const response = await fetch('http://localhost:8080/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password })
      })

      const responseText = await response.text()
      let parsedResponse
      
      try {
        parsedResponse = JSON.parse(responseText)
      } catch {
        parsedResponse = responseText
      }
      
      setResult(`Status: ${response.status}\nResponse: ${JSON.stringify(parsedResponse, null, 2)}`)
    } catch (error: any) {
      setResult(`Error: ${error.message}`)
    } finally {
      setIsLoading(false)
    }
  }

  const testProfile = async () => {
    setIsLoading(true)
    setResult('Testing profile endpoint...')

    try {
      let token = null
      if (typeof window !== 'undefined') {
        token = localStorage.getItem('token')
        if (!token) {
          setResult('No token found in localStorage')
          return
        }
      }

      const response = await fetch('http://localhost:8080/api/auth/profile', {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        }
      })

      const responseText = await response.text()
      let parsedResponse
      
      try {
        parsedResponse = JSON.parse(responseText)
      } catch {
        parsedResponse = responseText
      }
      
      setResult(`Profile Status: ${response.status}\nResponse: ${JSON.stringify(parsedResponse, null, 2)}`)
    } catch (error: any) {
      setResult(`Profile Error: ${error.message}`)
    } finally {
      setIsLoading(false)
    }
  }

  const testTokenDecode = () => {
    let token = null
    if (typeof window !== 'undefined') {
      token = localStorage.getItem('token')
      if (!token) {
        setResult('No token found in localStorage')
        return
      }
    }

    try {
      // Decode JWT token (without verification)
      const parts = (token || '').split('.')
      if (parts.length !== 3) {
        setResult('Invalid JWT token format')
        return
      }

      const payload = JSON.parse(atob(parts[1]))
      setResult(`Token Payload: ${JSON.stringify(payload, null, 2)}`)
    } catch (error: any) {
      setResult(`Token decode error: ${error.message}`)
    }
  }

  const clearStorage = () => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      setLocalStorageData({token: null, user: null})
    }
    setResult('Storage cleared')
  }

  return (
    <div className="min-h-screen bg-background p-8">
      <div className="max-w-4xl mx-auto space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>Authentication Debug</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label className="block text-sm font-medium mb-2">Email</Label>
                <Input
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="admin@example.com"
                />
              </div>
              <div>
                <Label className="block text-sm font-medium mb-2">Password</Label>
                <Input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Password"
                />
              </div>
            </div>

            <div className="flex gap-4">
              <Button onClick={testLogin} disabled={isLoading}>
                Test Login
              </Button>
              <Button onClick={testProfile} disabled={isLoading} variant="outline">
                Test Profile
              </Button>
              <Button onClick={testTokenDecode} disabled={isLoading} variant="outline">
                Decode Token
              </Button>
              <Button onClick={clearStorage} disabled={isLoading} variant="destructive">
                Clear Storage
              </Button>
            </div>

            {result && (
              <Alert>
                <AlertDescription>
                  <pre className="whitespace-pre-wrap text-sm">{result}</pre>
                </AlertDescription>
              </Alert>
            )}

            <div className="text-sm text-muted-foreground">
              <p><strong>Current localStorage:</strong></p>
              <p>Token: {localStorageData.token ? 'Present' : 'Not found'}</p>
              <p>User: {localStorageData.user ? 'Present' : 'Not found'}</p>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

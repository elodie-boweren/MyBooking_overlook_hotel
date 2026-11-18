"use client"

import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { apiTester, runApiTests, testModule } from '@/lib/api-test'
import { CheckCircle, XCircle, Clock, AlertTriangle, Info } from 'lucide-react'

interface TestResult {
  success: boolean
  status: number
  data: any
  error: string | null
  endpoint: string
  method: string
}

export default function ApiTestPage() {
  const [results, setResults] = useState<TestResult[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [token, setToken] = useState('')
  const [selectedModule, setSelectedModule] = useState('all')
  const [testReport, setTestReport] = useState<any>(null)

  const runAllTests = async () => {
    setIsLoading(true)
    try {
      const testResults = await runApiTests(token || undefined)
      setResults(testResults.results)
      setTestReport(testResults)
    } catch (error) {
      console.error('Test execution failed:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const runModuleTests = async () => {
    if (selectedModule === 'all') {
      await runAllTests()
      return
    }

    setIsLoading(true)
    try {
      const testResults = await testModule(selectedModule, token || undefined)
      setResults(testResults.results)
      setTestReport(testResults)
    } catch (error) {
      console.error('Module test execution failed:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const getStatusIcon = (success: boolean, status: number) => {
    if (success) {
      return <CheckCircle className="h-4 w-4 text-green-600" />
    } else if (status === 401) {
      return <AlertTriangle className="h-4 w-4 text-yellow-600" />
    } else if (status === 0) {
      return <XCircle className="h-4 w-4 text-red-600" />
    } else {
      return <XCircle className="h-4 w-4 text-red-600" />
    }
  }

  const getStatusBadge = (success: boolean, status: number) => {
    if (success) {
      return <Badge className="bg-green-100 text-green-800 border-green-200">Success</Badge>
    } else if (status === 401) {
      return <Badge className="bg-yellow-100 text-yellow-800 border-yellow-200">Unauthorized</Badge>
    } else if (status === 0) {
      return <Badge className="bg-red-100 text-red-800 border-red-200">Network Error</Badge>
    } else {
      return <Badge className="bg-red-100 text-red-800 border-red-200">Failed</Badge>
    }
  }

  const modules = [
    { value: 'all', label: 'All Modules' },
    { value: 'auth', label: 'Authentication' },
    { value: 'rooms', label: 'Rooms' },
    { value: 'reservations', label: 'Reservations' },
    { value: 'events', label: 'Events' },
    { value: 'feedback', label: 'Feedback' },
    { value: 'loyalty', label: 'Loyalty' },
    { value: 'employees', label: 'Employees' },
    { value: 'analytics', label: 'Analytics' },
  ]

  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto px-4 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-foreground mb-2">API Integration Tests</h1>
          <p className="text-muted-foreground">
            Test the connection between frontend and Spring Boot backend
          </p>
        </div>

        <Tabs defaultValue="test" className="space-y-6">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="test">Run Tests</TabsTrigger>
            <TabsTrigger value="results">Test Results</TabsTrigger>
          </TabsList>

          <TabsContent value="test" className="space-y-6">
            {/* Test Configuration */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Info className="h-5 w-5" />
                  Test Configuration
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <Label htmlFor="token">JWT Token (Optional)</Label>
                    <Input
                      id="token"
                      type="password"
                      placeholder="Enter JWT token for authenticated endpoints"
                      value={token}
                      onChange={(e) => setToken(e.target.value)}
                    />
                    <p className="text-sm text-muted-foreground mt-1">
                      Leave empty to test public endpoints only
                    </p>
                  </div>
                  <div>
                    <Label htmlFor="module">Test Module</Label>
                    <Select value={selectedModule} onValueChange={setSelectedModule}>
                      <SelectTrigger>
                        <SelectValue placeholder="Select module to test" />
                      </SelectTrigger>
                      <SelectContent>
                        {modules.map((module) => (
                          <SelectItem key={module.value} value={module.value}>
                            {module.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>

                <div className="flex gap-4">
                  <Button 
                    onClick={runModuleTests} 
                    disabled={isLoading}
                    className="flex items-center gap-2"
                  >
                    {isLoading ? (
                      <>
                        <Clock className="h-4 w-4 animate-spin" />
                        Running Tests...
                      </>
                    ) : (
                      <>
                        <CheckCircle className="h-4 w-4" />
                        Run Tests
                      </>
                    )}
                  </Button>
                  
                  <Button 
                    variant="outline" 
                    onClick={() => {
                      setResults([])
                      setTestReport(null)
                    }}
                  >
                    Clear Results
                  </Button>
                </div>
              </CardContent>
            </Card>

            {/* Test Instructions */}
            <Card>
              <CardHeader>
                <CardTitle>Test Instructions</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <h4 className="font-medium">How to use:</h4>
                  <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground">
                    <li>Make sure your Spring Boot backend is running on <code>http://localhost:8080</code></li>
                    <li>For authenticated endpoints, paste your JWT token in the token field</li>
                    <li>Select a specific module to test or &quot;All Modules&quot; for comprehensive testing</li>
                    <li>Click &quot;Run Tests&quot; to execute the API tests</li>
                    <li>View results in the &quot;Test Results&quot; tab</li>
                  </ul>
                </div>
                
                <div className="space-y-2">
                  <h4 className="font-medium">Expected Results:</h4>
                  <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground">
                    <li><span className="text-green-600">✅ Success</span> - Endpoint is working correctly</li>
                    <li><span className="text-yellow-600">⚠️ Unauthorized</span> - Endpoint requires authentication</li>
                    <li><span className="text-red-600">❌ Failed</span> - Endpoint has an error or is not available</li>
                  </ul>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="results" className="space-y-6">
            {/* Test Summary */}
            {testReport && (
              <Card>
                <CardHeader>
                  <CardTitle>Test Summary</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                    <div className="text-center">
                      <div className="text-2xl font-bold text-foreground">{testReport.total}</div>
                      <div className="text-sm text-muted-foreground">Total Tests</div>
                    </div>
                    <div className="text-center">
                      <div className="text-2xl font-bold text-green-600">{testReport.successful}</div>
                      <div className="text-sm text-muted-foreground">Successful</div>
                    </div>
                    <div className="text-center">
                      <div className="text-2xl font-bold text-red-600">{testReport.failed}</div>
                      <div className="text-sm text-muted-foreground">Failed</div>
                    </div>
                    <div className="text-center">
                      <div className="text-2xl font-bold text-blue-600">
                        {testReport.successRate.toFixed(1)}%
                      </div>
                      <div className="text-sm text-muted-foreground">Success Rate</div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* Test Results */}
            {results.length > 0 ? (
              <Card>
                <CardHeader>
                  <CardTitle>Test Results</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    {results.map((result, index) => (
                      <div
                        key={index}
                        className={`p-4 rounded-lg border ${
                          result.success 
                            ? 'bg-green-50 border-green-200' 
                            : result.status === 401
                            ? 'bg-yellow-50 border-yellow-200'
                            : 'bg-red-50 border-red-200'
                        }`}
                      >
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center gap-2">
                            {getStatusIcon(result.success, result.status)}
                            <span className="font-medium">
                              {result.method} {result.endpoint}
                            </span>
                          </div>
                          {getStatusBadge(result.success, result.status)}
                        </div>
                        
                        <div className="text-sm text-muted-foreground mb-2">
                          Status: {result.status}
                        </div>
                        
                        {result.error && (
                          <div className="text-sm text-red-600">
                            Error: {result.error}
                          </div>
                        )}
                        
                        {result.data && result.success && (
                          <details className="mt-2">
                            <summary className="text-sm text-muted-foreground cursor-pointer">
                              View Response Data
                            </summary>
                            <pre className="mt-2 p-2 bg-muted rounded text-xs overflow-auto">
                              {JSON.stringify(result.data, null, 2)}
                            </pre>
                          </details>
                        )}
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            ) : (
              <Card>
                <CardContent className="py-12 text-center">
                  <Info className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                  <h3 className="text-lg font-medium text-foreground mb-2">No Test Results</h3>
                  <p className="text-muted-foreground">
                    Run some tests first to see the results here.
                  </p>
                </CardContent>
              </Card>
            )}
          </TabsContent>
        </Tabs>
      </div>
    </div>
  )
}

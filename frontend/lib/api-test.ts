// API testing utilities for Spring Boot backend integration

import { API_ENDPOINTS } from './api'

export class ApiTester {
  private baseURL: string

  constructor(baseURL: string = "http://localhost:8080/api") {
    this.baseURL = baseURL
  }

  async testEndpoint(
    endpoint: string,
    method: 'GET' | 'POST' | 'PUT' | 'DELETE' = 'GET',
    data?: any,
    token?: string
  ) {
    const url = `${this.baseURL}${endpoint}`
    const config: RequestInit = {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(token && { Authorization: `Bearer ${token}` }),
      },
      ...(data && { body: JSON.stringify(data) }),
    }

    try {
      const response = await fetch(url, config)
      const result = await response.json()
      
      return {
        success: response.ok,
        status: response.status,
        data: result,
        error: response.ok ? null : result.message || 'Unknown error',
        endpoint,
        method
      }
    } catch (error) {
      return {
        success: false,
        status: 0,
        data: null,
        error: error instanceof Error ? error.message : 'Network error',
        endpoint,
        method
      }
    }
  }

  async testAllEndpoints(token?: string) {
    const results = []
    
    console.log('🧪 Starting API endpoint tests...')
    
    // Test authentication
    console.log('Testing authentication endpoints...')
    results.push(await this.testEndpoint(API_ENDPOINTS.AUTH.PROFILE, 'GET', undefined, token))
    
    // Test rooms
    console.log('Testing room endpoints...')
    results.push(await this.testEndpoint(API_ENDPOINTS.ROOMS.LIST, 'GET'))
    results.push(await this.testEndpoint(API_ENDPOINTS.ROOMS.GET('1'), 'GET'))
    
    // Test reservations
    console.log('Testing reservation endpoints...')
    results.push(await this.testEndpoint(API_ENDPOINTS.CLIENT_RESERVATIONS.MY, 'GET', undefined, token))
    results.push(await this.testEndpoint(API_ENDPOINTS.ADMIN_RESERVATIONS.ALL, 'GET', undefined, token))
    
    // Test events
    console.log('Testing event endpoints...')
    results.push(await this.testEndpoint(API_ENDPOINTS.EVENTS.LIST, 'GET'))
    results.push(await this.testEndpoint(API_ENDPOINTS.EVENT_BOOKINGS.MY, 'GET', undefined, token))
    
    // Test feedback
    console.log('Testing feedback endpoints...')
    results.push(await this.testEndpoint(API_ENDPOINTS.FEEDBACK.MY, 'GET', undefined, token))
    results.push(await this.testEndpoint(API_ENDPOINTS.ADMIN_FEEDBACK.ALL, 'GET', undefined, token))
    
    // Test loyalty
    console.log('Testing loyalty endpoints...')
    results.push(await this.testEndpoint(API_ENDPOINTS.LOYALTY.BALANCE, 'GET', undefined, token))
    results.push(await this.testEndpoint(API_ENDPOINTS.LOYALTY.TRANSACTIONS, 'GET', undefined, token))
    
    // Test employee endpoints
    console.log('Testing employee endpoints...')
    results.push(await this.testEndpoint(API_ENDPOINTS.EMPLOYEE.PROFILE, 'GET', undefined, token))
    results.push(await this.testEndpoint(API_ENDPOINTS.EMPLOYEE.TASKS, 'GET', undefined, token))
    results.push(await this.testEndpoint(API_ENDPOINTS.EMPLOYEE.SHIFTS, 'GET', undefined, token))
    
    // Test admin employee endpoints
    results.push(await this.testEndpoint(API_ENDPOINTS.ADMIN_EMPLOYEES.ALL, 'GET', undefined, token))
    results.push(await this.testEndpoint(API_ENDPOINTS.ADMIN_EMPLOYEES.STATISTICS, 'GET', undefined, token))
    
    // Test analytics endpoints
    console.log('Testing analytics endpoints...')
    results.push(await this.testEndpoint(API_ENDPOINTS.ANALYTICS.DASHBOARD, 'GET', undefined, token))
    results.push(await this.testEndpoint(API_ENDPOINTS.ANALYTICS.REVENUE, 'GET', undefined, token))
    results.push(await this.testEndpoint(API_ENDPOINTS.ANALYTICS.OCCUPANCY, 'GET', undefined, token))
    
    // Test announcements
    console.log('Testing announcement endpoints...')
    results.push(await this.testEndpoint(API_ENDPOINTS.ANNOUNCEMENTS.LIST, 'GET'))
    results.push(await this.testEndpoint(API_ENDPOINTS.ADMIN_ANNOUNCEMENTS.ALL, 'GET', undefined, token))
    
    console.log('✅ API endpoint tests completed!')
    return results
  }

  async testSpecificModule(module: string, token?: string) {
    const results = []
    
    switch (module.toLowerCase()) {
      case 'auth':
        results.push(await this.testEndpoint(API_ENDPOINTS.AUTH.PROFILE, 'GET', undefined, token))
        break
        
      case 'rooms':
        results.push(await this.testEndpoint(API_ENDPOINTS.ROOMS.LIST, 'GET'))
        results.push(await this.testEndpoint(API_ENDPOINTS.ROOMS.GET('1'), 'GET'))
        break
        
      case 'reservations':
        results.push(await this.testEndpoint(API_ENDPOINTS.CLIENT_RESERVATIONS.MY, 'GET', undefined, token))
        results.push(await this.testEndpoint(API_ENDPOINTS.ADMIN_RESERVATIONS.ALL, 'GET', undefined, token))
        break
        
      case 'events':
        results.push(await this.testEndpoint(API_ENDPOINTS.EVENTS.LIST, 'GET'))
        results.push(await this.testEndpoint(API_ENDPOINTS.EVENT_BOOKINGS.MY, 'GET', undefined, token))
        break
        
      case 'feedback':
        results.push(await this.testEndpoint(API_ENDPOINTS.FEEDBACK.MY, 'GET', undefined, token))
        results.push(await this.testEndpoint(API_ENDPOINTS.ADMIN_FEEDBACK.ALL, 'GET', undefined, token))
        break
        
      case 'loyalty':
        results.push(await this.testEndpoint(API_ENDPOINTS.LOYALTY.BALANCE, 'GET', undefined, token))
        results.push(await this.testEndpoint(API_ENDPOINTS.LOYALTY.TRANSACTIONS, 'GET', undefined, token))
        break
        
      case 'employees':
        results.push(await this.testEndpoint(API_ENDPOINTS.EMPLOYEE.PROFILE, 'GET', undefined, token))
        results.push(await this.testEndpoint(API_ENDPOINTS.EMPLOYEE.TASKS, 'GET', undefined, token))
        results.push(await this.testEndpoint(API_ENDPOINTS.ADMIN_EMPLOYEES.ALL, 'GET', undefined, token))
        break
        
      case 'analytics':
        results.push(await this.testEndpoint(API_ENDPOINTS.ANALYTICS.DASHBOARD, 'GET', undefined, token))
        results.push(await this.testEndpoint(API_ENDPOINTS.ANALYTICS.REVENUE, 'GET', undefined, token))
        break
        
      default:
        console.warn(`Unknown module: ${module}`)
    }
    
    return results
  }

  generateTestReport(results: any[]) {
    const total = results.length
    const successful = results.filter(r => r.success).length
    const failed = total - successful
    
    console.log('\n📊 API Test Report')
    console.log('==================')
    console.log(`Total Tests: ${total}`)
    console.log(`✅ Successful: ${successful}`)
    console.log(`❌ Failed: ${failed}`)
    console.log(`Success Rate: ${((successful / total) * 100).toFixed(1)}%`)
    
    if (failed > 0) {
      console.log('\n❌ Failed Tests:')
      results.filter(r => !r.success).forEach(result => {
        console.log(`  - ${result.method} ${result.endpoint}: ${result.error}`)
      })
    }
    
    return {
      total,
      successful,
      failed,
      successRate: (successful / total) * 100,
      results
    }
  }
}

// Create API tester instance
export const apiTester = new ApiTester()

// Utility function to run tests from browser console
export const runApiTests = async (token?: string) => {
  const results = await apiTester.testAllEndpoints(token)
  return apiTester.generateTestReport(results)
}

// Utility function to test specific module
export const testModule = async (module: string, token?: string) => {
  const results = await apiTester.testSpecificModule(module, token)
  return apiTester.generateTestReport(results)
}

"use client"

import { useState, useEffect } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { 
  Users, 
  Search, 
  Filter, 
  Plus,
  Edit,
  Trash2,
  User,
  Mail,
  Calendar,
  Briefcase,
  CheckCircle,
  XCircle,
  UserPlus
} from 'lucide-react'
import { COMPONENT_TEMPLATES } from '@/lib/style-constants'
import { apiClient, API_ENDPOINTS, Employee, CreateEmployeeRequest, UpdateEmployeeRequest } from '@/lib/api'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api"

export function EmployeeManagement() {
  const [employees, setEmployees] = useState<Employee[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedStatus, setSelectedStatus] = useState<string>('all')
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [showEditForm, setShowEditForm] = useState(false)
  const [creating, setCreating] = useState(false)
  const [editing, setEditing] = useState(false)
  const [selectedEmployee, setSelectedEmployee] = useState<Employee | null>(null)
  const [formData, setFormData] = useState({
    userId: '',
    jobTitle: ''
  })
  const [editFormData, setEditFormData] = useState({
    jobTitle: '',
    status: 'ACTIVE' as 'ACTIVE' | 'INACTIVE'
  })

  useEffect(() => {
    fetchEmployees()
  }, [])

  const fetchEmployees = async () => {
    try {
      setLoading(true)
      console.log('Fetching employees from:', API_ENDPOINTS.ADMIN_EMPLOYEES.ALL)
      
      // Use direct fetch to get better error handling
      const token = localStorage.getItem('token')
      if (!token) {
        throw new Error('No authentication token found')
      }
      
      const response = await fetch(`${API_BASE_URL}${API_ENDPOINTS.ADMIN_EMPLOYEES.ALL}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      })
      
      console.log('Response status:', response.status)
      console.log('Response headers:', response.headers)
      
      if (!response.ok) {
        const errorText = await response.text()
        console.error('HTTP Error:', response.status, errorText)
        throw new Error(`HTTP ${response.status}: ${errorText}`)
      }
      
      const data = await response.json()
      console.log('Employee API response:', data)
      
      // Handle paginated response from Spring Boot
      const employeesData = data.content || data
      const employeesArray = Array.isArray(employeesData) ? employeesData : []
      console.log('Processed employees:', employeesArray)
      
      if (employeesArray.length === 0) {
        console.warn('No employees found in response')
      }
      
      setEmployees(employeesArray)
    } catch (error) {
      console.error('Failed to fetch employees:', error)
      console.error('Error details:', error)
      
      // Show error message to user
      alert(`Failed to fetch employees from backend: ${error instanceof Error ? error.message : 'Unknown error'}`)
      
      // Fallback to mock data for development
      const mockEmployees: Employee[] = [
        {
          userId: 1,
          email: 'john.doe@hotel.com',
          firstName: 'John',
          lastName: 'Doe',
          status: 'ACTIVE',
          jobTitle: 'Receptionist',
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z'
        },
        {
          userId: 2,
          email: 'jane.smith@hotel.com',
          firstName: 'Jane',
          lastName: 'Smith',
          status: 'ACTIVE',
          jobTitle: 'Housekeeping Manager',
          createdAt: '2024-01-02T00:00:00Z',
          updatedAt: '2024-01-02T00:00:00Z'
        },
        {
          userId: 3,
          email: 'mike.wilson@hotel.com',
          firstName: 'Mike',
          lastName: 'Wilson',
          status: 'INACTIVE',
          jobTitle: 'Maintenance',
          createdAt: '2024-01-03T00:00:00Z',
          updatedAt: '2024-01-03T00:00:00Z'
        }
      ]
      setEmployees(mockEmployees)
    } finally {
      setLoading(false)
    }
  }

  const getStatusBadgeVariant = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'default'
      case 'INACTIVE':
        return 'destructive'
      default:
        return 'outline'
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return <CheckCircle className="h-4 w-4 text-green-600" />
      case 'INACTIVE':
        return <XCircle className="h-4 w-4 text-red-600" />
      default:
        return <User className="h-4 w-4 text-gray-600" />
    }
  }

  const handleCreateEmployee = async (e: React.FormEvent) => {
    e.preventDefault()
    if (creating) return

    try {
      setCreating(true)
      const createData: CreateEmployeeRequest = {
        userId: parseInt(formData.userId),
        jobTitle: formData.jobTitle
      }
      await apiClient.post(API_ENDPOINTS.ADMIN_EMPLOYEES.CREATE, createData)
      setFormData({ userId: '', jobTitle: '' })
      setShowCreateForm(false)
      fetchEmployees()
      alert('Employee created successfully!')
    } catch (error: any) {
      console.error('Failed to create employee:', error)
      const errorMessage = error?.response?.data?.message || error?.message || 'Unknown error'
      alert(`Failed to create employee: ${errorMessage}`)
    } finally {
      setCreating(false)
    }
  }

  const handleEditEmployee = (employee: Employee) => {
    setEditFormData({
      jobTitle: employee.jobTitle,
      status: employee.status
    })
    setSelectedEmployee(employee)
    setShowEditForm(true)
  }

  const handleUpdateEmployee = async (e: React.FormEvent) => {
    e.preventDefault()
    if (editing || !selectedEmployee) return

    try {
      setEditing(true)
      const updateData: UpdateEmployeeRequest = {
        jobTitle: editFormData.jobTitle,
        status: editFormData.status
      }
      await apiClient.put(API_ENDPOINTS.ADMIN_EMPLOYEES.UPDATE(selectedEmployee.userId.toString()), updateData)
      setEditFormData({ jobTitle: '', status: 'ACTIVE' })
      setShowEditForm(false)
      setSelectedEmployee(null)
      fetchEmployees()
      alert('Employee updated successfully!')
    } catch (error: any) {
      console.error('Failed to update employee:', error)
      const errorMessage = error?.response?.data?.message || error?.message || 'Unknown error'
      alert(`Failed to update employee: ${errorMessage}`)
    } finally {
      setEditing(false)
    }
  }

  const handleDeleteEmployee = async (employeeId: number) => {
    const employee = employees.find(e => e.userId === employeeId)
    if (!employee) return

    const confirmMessage = `Are you sure you want to delete ${employee.firstName} ${employee.lastName}?\n\nThis will permanently remove the employee from the system.`
    if (!confirm(confirmMessage)) {
      return
    }

    try {
      const response = await fetch(`${API_BASE_URL}/admin/employees/${employeeId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json',
        },
      })

      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(`HTTP ${response.status}: ${errorText}`)
      }
      
      alert(`Employee ${employee.firstName} ${employee.lastName} has been deleted successfully!`)
      fetchEmployees()
    } catch (error: any) {
      console.error('Failed to delete employee:', error)
      const errorMessage = error?.message || 'Network error occurred'
      alert(`Failed to delete employee: ${errorMessage}`)
    }
  }

  const handleFormChange = (field: string, value: string) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }))
  }

  const handleEditFormChange = (field: string, value: string) => {
    setEditFormData(prev => ({
      ...prev,
      [field]: value
    }))
  }

  const filteredEmployees = employees.filter(employee => {
    const matchesSearch = employee.firstName.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         employee.lastName.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         employee.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         employee.jobTitle.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesStatus = selectedStatus === 'all' || employee.status === selectedStatus
    return matchesSearch && matchesStatus
  })

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-foreground">Employee Management</h1>
            <p className="text-muted-foreground">
              Manage hotel employees and their information
            </p>
          </div>
        </div>
        <div className="flex items-center justify-center py-12">
          <div className="text-center">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
            <p className="text-muted-foreground">Loading employees...</p>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-foreground">Employee Management</h1>
          <p className="text-muted-foreground">
            Manage hotel employees and their information
          </p>
        </div>
        <Button onClick={() => setShowCreateForm(true)}>
          <UserPlus className="h-4 w-4 mr-2" />
          Add Employee
        </Button>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col md:flex-row gap-4">
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground h-4 w-4" />
                <Input
                  placeholder="Search by name, email, or job title..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>
            <div className="flex gap-4">
              <select
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                className="px-3 py-2 border rounded-md"
              >
                <option value="all">All Status</option>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
              </select>
              <Button variant="outline">
                <Filter className="h-4 w-4 mr-2" />
                Filter
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Employees Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredEmployees.map((employee) => (
          <Card key={employee.userId} className={COMPONENT_TEMPLATES.cardHover}>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className="h-10 w-10 rounded-full bg-primary flex items-center justify-center">
                    <User className="h-5 w-5 text-primary-foreground" />
                  </div>
                  <div>
                    <CardTitle className="text-lg">{employee.firstName} {employee.lastName}</CardTitle>
                    <p className="text-sm text-muted-foreground">{employee.jobTitle}</p>
                  </div>
                </div>
                <Badge variant={getStatusBadgeVariant(employee.status)}>
                  {employee.status}
                </Badge>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex items-center space-x-2">
                  <Mail className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm">{employee.email}</span>
                </div>
                <div className="flex items-center space-x-2">
                  <Briefcase className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm">{employee.jobTitle}</span>
                </div>
                <div className="flex items-center space-x-2">
                  <Calendar className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm">Joined {new Date(employee.createdAt).toLocaleDateString()}</span>
                </div>
                <div className="flex items-center space-x-2">
                  <span className="text-sm text-muted-foreground">Status:</span>
                  <div className="flex items-center space-x-1">
                    {getStatusIcon(employee.status)}
                    <span className="text-sm capitalize">{employee.status.toLowerCase()}</span>
                  </div>
                </div>
              </div>
              <div className="flex gap-2 mt-4">
                <Button 
                  variant="outline" 
                  size="sm" 
                  className="flex-1"
                  onClick={() => handleEditEmployee(employee)}
                >
                  <Edit className="h-4 w-4 mr-2" />
                  Edit
                </Button>
                <Button 
                  variant="outline"
                  size="sm" 
                  className="flex-1"
                  onClick={() => handleDeleteEmployee(employee.userId)}
                >
                  <Trash2 className="h-4 w-4 mr-2" />
                  Delete
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Empty State */}
      {filteredEmployees.length === 0 && (
        <Card>
          <CardContent className="text-center py-8">
            <Users className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
            <h3 className="text-lg font-medium text-foreground mb-2">No employees found</h3>
            <p className="text-muted-foreground">
              {searchTerm || selectedStatus !== 'all' 
                ? 'Try adjusting your search or filter criteria'
                : 'No employees have been added yet'
              }
            </p>
          </CardContent>
        </Card>
      )}

      {/* Create Employee Form Modal */}
      {showCreateForm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <Card className="w-full max-w-md mx-4">
            <CardHeader>
              <CardTitle>Add New Employee</CardTitle>
              <p className="text-sm text-muted-foreground">
                Create a new employee record
              </p>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleCreateEmployee} className="space-y-4">
                <div>
                  <label className="text-sm font-medium">User ID</label>
                  <Input
                    type="number"
                    value={formData.userId}
                    onChange={(e) => handleFormChange('userId', e.target.value)}
                    placeholder="Enter user ID"
                    required
                  />
                  <p className="text-xs text-muted-foreground mt-1">
                    The user must already exist in the system
                  </p>
                </div>

                <div>
                  <label className="text-sm font-medium">Job Title</label>
                  <Input
                    value={formData.jobTitle}
                    onChange={(e) => handleFormChange('jobTitle', e.target.value)}
                    placeholder="e.g., Receptionist, Housekeeping Manager"
                    required
                  />
                </div>

                <div className="flex gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => {
                      setShowCreateForm(false)
                      setFormData({ userId: '', jobTitle: '' })
                    }}
                    className="flex-1"
                    disabled={creating}
                  >
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    className="flex-1"
                    disabled={creating}
                  >
                    {creating ? 'Creating...' : 'Create Employee'}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Edit Employee Form Modal */}
      {showEditForm && selectedEmployee && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <Card className="w-full max-w-md mx-4">
            <CardHeader>
              <CardTitle>Edit Employee</CardTitle>
              <p className="text-sm text-muted-foreground">
                Update {selectedEmployee.firstName} {selectedEmployee.lastName}&apos;s information
              </p>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleUpdateEmployee} className="space-y-4">
                <div>
                  <label className="text-sm font-medium">Job Title</label>
                  <Input
                    value={editFormData.jobTitle}
                    onChange={(e) => handleEditFormChange('jobTitle', e.target.value)}
                    placeholder="e.g., Receptionist, Housekeeping Manager"
                    required
                  />
                </div>

                <div>
                  <label className="text-sm font-medium">Status</label>
                  <select
                    value={editFormData.status}
                    onChange={(e) => handleEditFormChange('status', e.target.value)}
                    className="w-full p-2 border rounded-md"
                    required
                  >
                    <option value="ACTIVE">Active</option>
                    <option value="INACTIVE">Inactive</option>
                  </select>
                </div>

                <div className="flex gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => {
                      setShowEditForm(false)
                      setSelectedEmployee(null)
                    }}
                    className="flex-1"
                    disabled={editing}
                  >
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    className="flex-1"
                    disabled={editing}
                  >
                    {editing ? 'Updating...' : 'Update Employee'}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  )
}

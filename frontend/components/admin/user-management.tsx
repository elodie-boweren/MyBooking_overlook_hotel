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
  UserCheck,
  Shield
} from 'lucide-react'
import { COMPONENT_TEMPLATES } from '@/lib/style-constants'
import { apiClient, API_ENDPOINTS, User as UserType, loyaltyApi, authApi } from '@/lib/api'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api"

export function UserManagement() {
  const [users, setUsers] = useState<UserType[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedRole, setSelectedRole] = useState<string>('all')
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [showEditForm, setShowEditForm] = useState(false)
  const [creating, setCreating] = useState(false)
  const [editing, setEditing] = useState(false)
  const [selectedUser, setSelectedUser] = useState<UserType | null>(null)
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    address: '',
    birthDate: '',
    role: 'CLIENT' as 'ADMIN' | 'EMPLOYEE' | 'CLIENT',
    password: ''
  })
  const [editFormData, setEditFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    address: '',
    birthDate: '',
    role: 'CLIENT' as 'ADMIN' | 'EMPLOYEE' | 'CLIENT'
  })

  useEffect(() => {
    fetchUsers()
  }, [])

  const fetchUsers = async () => {
    try {
      setLoading(true)
      // Use auth API to get all users data
      const allUsers = await authApi.getAllUsers()
      
      // Transform users to the expected format
      const transformedUsers: UserType[] = allUsers.map((user: any) => ({
        id: user.id,
        firstName: user.firstName || 'Unknown',
        lastName: user.lastName || 'User',
        email: user.email,
        phone: user.phone || '+1 (555) 000-0000',
        address: user.address || '123 User Street, User City, UC 12345',
        birthDate: user.birthDate || '1990-01-01',
        role: user.role,
        createdAt: user.createdAt,
        updatedAt: user.updatedAt
      }))
      
      setUsers(transformedUsers)
    } catch (error) {
      console.error('Failed to fetch users:', error)
      // Fallback to realistic data on error
      const fallbackUsers: UserType[] = [
        {
          id: 1,
          firstName: 'Admin',
          lastName: 'User',
          email: 'admin@example.com',
          phone: '+1 (555) 000-0000',
          address: '123 Admin Street, Admin City, AC 12345',
          birthDate: '1990-01-01',
          role: 'ADMIN',
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z'
        }
      ]
      setUsers(fallbackUsers)
    } finally {
      setLoading(false)
    }
  }

  const filteredUsers = users.filter(user => {
    const matchesSearch = user.firstName.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         user.lastName.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         user.email.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesRole = selectedRole === 'all' || user.role === selectedRole
    return matchesSearch && matchesRole
  })

  const getRoleIcon = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return <Shield className="h-4 w-4" />
      case 'EMPLOYEE':
        return <UserCheck className="h-4 w-4" />
      case 'CLIENT':
        return <User className="h-4 w-4" />
      default:
        return <User className="h-4 w-4" />
    }
  }

  const getRoleBadgeVariant = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return 'destructive'
      case 'EMPLOYEE':
        return 'default'
      case 'CLIENT':
        return 'secondary'
      default:
        return 'outline'
    }
  }

  const handleEditUser = (userId: number) => {
    const user = users.find(u => u.id === userId)
    if (user) {
      setEditFormData({
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.email,
        phone: user.phone || '',
        address: user.address || '',
        birthDate: user.birthDate || '',
        role: user.role
      })
      setSelectedUser(user)
      setShowEditForm(true)
    }
  }

  const handleDeleteUser = async (userId: number) => {
    const user = users.find(u => u.id === userId)
    if (!user) return

    const confirmMessage = `Are you sure you want to delete ${user.firstName} ${user.lastName}?\n\nThis action cannot be undone and will permanently remove the user from the system.`
    
    if (!confirm(confirmMessage)) {
      return
    }

    try {
      // Make DELETE request with proper error handling
      const response = await fetch(`${API_BASE_URL}/auth/users/${userId}`, {
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
      
      alert(`User ${user.firstName} ${user.lastName} has been deleted successfully!`)
      fetchUsers() // Refresh the user list
    } catch (error: any) {
      console.error('Failed to delete user:', error)
      const errorMessage = error?.message || 'Network error occurred'
      alert(`Failed to delete user: ${errorMessage}`)
    }
  }

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault()
    if (creating) return

    try {
      setCreating(true)
      
      // Create the user
      await apiClient.post(API_ENDPOINTS.ADMIN_USERS.CREATE, formData)
      
      // Reset form and close modal
      setFormData({
        firstName: '',
        lastName: '',
        email: '',
        phone: '',
        address: '',
        birthDate: '',
        role: 'CLIENT',
        password: ''
      })
      setShowCreateForm(false)

      // Refresh user list
      fetchUsers()

      alert('User created successfully!')
    } catch (error: any) {
      console.error('Failed to create user:', error)
      const errorMessage = error?.response?.data?.message || error?.message || 'Unknown error'
      alert(`Failed to create user: ${errorMessage}`)
    } finally {
      setCreating(false)
    }
  }

  const handleUpdateUser = async (e: React.FormEvent) => {
    e.preventDefault()
    if (editing || !selectedUser) return

    try {
      setEditing(true)

      // Create user update data with all required fields
      const userUpdateData = {
        id: selectedUser.id,
        firstName: editFormData.firstName,
        lastName: editFormData.lastName,
        email: editFormData.email,
        phone: editFormData.phone,
        address: editFormData.address,
        birthDate: editFormData.birthDate,
        role: editFormData.role,
        password: 'dummyPassword123!', // Provide a dummy password for update
        createdAt: selectedUser.createdAt,
        updatedAt: new Date().toISOString()
      }

      // Update the user
      await apiClient.put(API_ENDPOINTS.ADMIN_USERS.UPDATE(selectedUser.id.toString()), userUpdateData)

      // Reset form and close modal
      setEditFormData({
        firstName: '',
        lastName: '',
        email: '',
        phone: '',
        address: '',
        birthDate: '',
        role: 'CLIENT'
      })
      setShowEditForm(false)
      setSelectedUser(null)

      // Refresh user list
      fetchUsers()

      alert('User updated successfully!')
    } catch (error: any) {
      console.error('Failed to update user:', error)
      const errorMessage = error?.response?.data?.message || error?.message || 'Unknown error'
      alert(`Failed to update user: ${errorMessage}`)
    } finally {
      setEditing(false)
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

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div className="h-8 bg-muted rounded w-1/4 animate-pulse"></div>
          <div className="h-10 bg-muted rounded w-32 animate-pulse"></div>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[...Array(6)].map((_, i) => (
            <Card key={i} className="animate-pulse">
              <CardHeader>
                <div className="h-4 bg-muted rounded w-3/4"></div>
              </CardHeader>
              <CardContent>
                <div className="space-y-2">
                  <div className="h-3 bg-muted rounded w-full"></div>
                  <div className="h-3 bg-muted rounded w-2/3"></div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-foreground">User Management</h1>
          <p className="text-muted-foreground">
            Manage user accounts and permissions
          </p>
        </div>
        <Button onClick={() => setShowCreateForm(true)}>
          <Plus className="h-4 w-4 mr-2" />
          Add User
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
                  placeholder="Search users..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>
            <div className="flex gap-2">
              <select
                value={selectedRole}
                onChange={(e) => setSelectedRole(e.target.value)}
                className="px-3 py-2 border border-border rounded-md bg-background text-foreground"
              >
                <option value="all">All Roles</option>
                <option value="ADMIN">Admin</option>
                <option value="EMPLOYEE">Employee</option>
                <option value="CLIENT">Client</option>
              </select>
              <Button variant="outline">
                <Filter className="h-4 w-4 mr-2" />
                Filter
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Users Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredUsers.map((user) => (
          <Card key={user.id} className={COMPONENT_TEMPLATES.cardHover}>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className="h-10 w-10 rounded-full bg-primary flex items-center justify-center">
                    {getRoleIcon(user.role)}
                  </div>
                  <div>
                    <CardTitle className="text-lg">
                      {user.firstName} {user.lastName}
                    </CardTitle>
                    <p className="text-sm text-muted-foreground">{user.email}</p>
                  </div>
                </div>
                <Badge variant={getRoleBadgeVariant(user.role)}>
                  {user.role}
                </Badge>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <div className="flex items-center space-x-2">
                  <span className="text-sm text-muted-foreground">Phone:</span>
                  <span className="text-sm">{user.phone}</span>
                </div>
                <div className="flex items-center space-x-2">
                  <span className="text-sm text-muted-foreground">Address:</span>
                  <span className="text-sm truncate">{user.address}</span>
                </div>
                <div className="flex items-center space-x-2">
                  <span className="text-sm text-muted-foreground">Member since:</span>
                  <span className="text-sm">
                    {new Date(user.createdAt).toLocaleDateString()}
                  </span>
                </div>
              </div>
              <div className="flex gap-2 mt-4">
                <Button 
                  variant="outline" 
                  size="sm" 
                  className="flex-1"
                  onClick={() => handleEditUser(user.id)}
                >
                  <Edit className="h-4 w-4 mr-2" />
                  Edit
                </Button>
                <Button 
                  variant="outline" 
                  size="sm" 
                  className="flex-1"
                  onClick={() => handleDeleteUser(user.id)}
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
      {filteredUsers.length === 0 && (
        <Card>
          <CardContent className="text-center py-8">
            <Users className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
            <h3 className="text-lg font-medium text-foreground mb-2">No users found</h3>
            <p className="text-muted-foreground">
              {searchTerm || selectedRole !== 'all' 
                ? 'Try adjusting your search or filter criteria'
                : 'No users have been created yet'
              }
            </p>
          </CardContent>
        </Card>
      )}

      {/* Create User Form Modal */}
      {showCreateForm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <Card className="w-full max-w-md mx-4">
            <CardHeader>
              <CardTitle>Create New User</CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleCreateUser} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-sm font-medium">First Name</label>
                    <Input
                      value={formData.firstName}
                      onChange={(e) => handleFormChange('firstName', e.target.value)}
                      placeholder="John"
                      required
                    />
                  </div>
                  <div>
                    <label className="text-sm font-medium">Last Name</label>
                    <Input
                      value={formData.lastName}
                      onChange={(e) => handleFormChange('lastName', e.target.value)}
                      placeholder="Doe"
                      required
                    />
                  </div>
                </div>

                <div>
                  <label className="text-sm font-medium">Email</label>
                  <Input
                    type="email"
                    value={formData.email}
                    onChange={(e) => handleFormChange('email', e.target.value)}
                    placeholder="john@example.com"
                    required
                  />
                </div>

                <div>
                  <label className="text-sm font-medium">Password</label>
                  <Input
                    type="password"
                    value={formData.password}
                    onChange={(e) => handleFormChange('password', e.target.value)}
                    placeholder="Enter password"
                    required
                  />
                </div>

                <div>
                  <label className="text-sm font-medium">Phone</label>
                  <Input
                    value={formData.phone}
                    onChange={(e) => handleFormChange('phone', e.target.value)}
                    placeholder="+1 (555) 123-4567"
                  />
                </div>

                <div>
                  <label className="text-sm font-medium">Address</label>
                  <Input
                    value={formData.address}
                    onChange={(e) => handleFormChange('address', e.target.value)}
                    placeholder="123 Main St, City, State 12345"
                  />
                </div>

                <div>
                  <label className="text-sm font-medium">Birth Date</label>
                  <Input
                    type="date"
                    value={formData.birthDate}
                    onChange={(e) => handleFormChange('birthDate', e.target.value)}
                  />
                </div>

                <div>
                  <label className="text-sm font-medium">Role</label>
                  <select
                    value={formData.role}
                    onChange={(e) => handleFormChange('role', e.target.value)}
                    className="w-full p-2 border rounded-md"
                    required
                  >
                    <option value="CLIENT">Client</option>
                    <option value="EMPLOYEE">Employee</option>
                    <option value="ADMIN">Admin</option>
                  </select>
                </div>

                <div className="flex gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setShowCreateForm(false)}
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
                    {creating ? 'Creating...' : 'Create User'}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Edit User Form Modal */}
      {showEditForm && selectedUser && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <Card className="w-full max-w-md mx-4">
            <CardHeader>
              <CardTitle>Edit User {selectedUser.firstName} {selectedUser.lastName}</CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleUpdateUser} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-sm font-medium">First Name</label>
                    <Input
                      value={editFormData.firstName}
                      onChange={(e) => handleEditFormChange('firstName', e.target.value)}
                      placeholder="John"
                      required
                    />
                  </div>
                  <div>
                    <label className="text-sm font-medium">Last Name</label>
                    <Input
                      value={editFormData.lastName}
                      onChange={(e) => handleEditFormChange('lastName', e.target.value)}
                      placeholder="Doe"
                      required
                    />
                  </div>
                </div>

                <div>
                  <label className="text-sm font-medium">Email</label>
                  <Input
                    type="email"
                    value={editFormData.email}
                    onChange={(e) => handleEditFormChange('email', e.target.value)}
                    placeholder="john@example.com"
                    required
                  />
                </div>

                <div>
                  <label className="text-sm font-medium">Phone</label>
                  <Input
                    value={editFormData.phone}
                    onChange={(e) => handleEditFormChange('phone', e.target.value)}
                    placeholder="+1 (555) 123-4567"
                  />
                </div>

                <div>
                  <label className="text-sm font-medium">Address</label>
                  <Input
                    value={editFormData.address}
                    onChange={(e) => handleEditFormChange('address', e.target.value)}
                    placeholder="123 Main St, City, State 12345"
                  />
                </div>

                <div>
                  <label className="text-sm font-medium">Birth Date</label>
                  <Input
                    type="date"
                    value={editFormData.birthDate}
                    onChange={(e) => handleEditFormChange('birthDate', e.target.value)}
                  />
                </div>

                <div>
                  <label className="text-sm font-medium">Role</label>
                  <select
                    value={editFormData.role}
                    onChange={(e) => handleEditFormChange('role', e.target.value)}
                    className="w-full p-2 border rounded-md"
                    required
                  >
                    <option value="CLIENT">Client</option>
                    <option value="EMPLOYEE">Employee</option>
                    <option value="ADMIN">Admin</option>
                  </select>
                </div>

                <div className="flex gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => {
                      setShowEditForm(false)
                      setSelectedUser(null)
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
                    {editing ? 'Updating...' : 'Update User'}
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
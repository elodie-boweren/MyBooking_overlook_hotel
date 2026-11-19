"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { 
  CheckCircle, 
  Clock, 
  AlertCircle,
  Filter,
  Search,
  RefreshCw,
  ArrowLeft,
  Edit,
  MessageSquare,
  Calendar,
  User
} from "lucide-react"
import { employeeApi, EmployeeTask, UpdateTaskRequest } from "@/lib/api"

interface TaskUpdateFormData {
  status: "TODO" | "IN_PROGRESS" | "DONE"
  note: string
}

export default function EmployeeTasks() {
  const [tasks, setTasks] = useState<EmployeeTask[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [showUpdateDialog, setShowUpdateDialog] = useState(false)
  const [selectedTask, setSelectedTask] = useState<EmployeeTask | null>(null)
  const [isUpdating, setIsUpdating] = useState(false)
  const [filterStatus, setFilterStatus] = useState<string>("all")
  const [searchTerm, setSearchTerm] = useState("")
  
  const [updateForm, setUpdateForm] = useState<TaskUpdateFormData>({
    status: "TODO",
    note: ""
  })

  // Fetch tasks on component mount
  useEffect(() => {
    fetchTasks()
  }, [])

  const fetchTasks = async () => {
    try {
      setIsLoading(true)
      setError(null)
      
      const response = await employeeApi.getTasks()
      setTasks(response.content)
    } catch (error) {
      console.error('Error fetching tasks:', error)
      setError('Failed to load tasks. Please try again.')
    } finally {
      setIsLoading(false)
    }
  }

  const handleUpdateTask = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!selectedTask) return

    try {
      setIsUpdating(true)
      setError(null)
      
      const request: UpdateTaskRequest = {
        status: updateForm.status,
        note: updateForm.note.trim() || undefined
      }
      
      await employeeApi.updateTask(selectedTask.id, request)
      
      // Update local state
      setTasks(prev => prev.map(task => 
        task.id === selectedTask.id 
          ? { ...task, status: updateForm.status, note: updateForm.note.trim() || task.note }
          : task
      ))
      
      // Reset form and close dialog
      setUpdateForm({ status: "TODO", note: "" })
      setShowUpdateDialog(false)
      setSelectedTask(null)
    } catch (error) {
      console.error('Error updating task:', error)
      setError('Failed to update task. Please try again.')
    } finally {
      setIsUpdating(false)
    }
  }

  const openUpdateDialog = (task: EmployeeTask) => {
    setSelectedTask(task)
    setUpdateForm({
      status: task.status,
      note: task.note || ""
    })
    setShowUpdateDialog(true)
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'DONE': return 'bg-green-100 text-green-800'
      case 'IN_PROGRESS': return 'bg-blue-100 text-blue-800'
      case 'TODO': return 'bg-yellow-100 text-yellow-800'
      default: return 'bg-gray-100 text-gray-800'
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'DONE': return <CheckCircle className="h-4 w-4 text-green-600" />
      case 'IN_PROGRESS': return <Clock className="h-4 w-4 text-blue-600" />
      case 'TODO': return <AlertCircle className="h-4 w-4 text-yellow-600" />
      default: return <AlertCircle className="h-4 w-4 text-gray-600" />
    }
  }

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'HIGH': return 'bg-red-100 text-red-800'
      case 'MEDIUM': return 'bg-yellow-100 text-yellow-800'
      case 'LOW': return 'bg-green-100 text-green-800'
      default: return 'bg-gray-100 text-gray-800'
    }
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    })
  }

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  // Filter and search tasks
  const filteredTasks = tasks.filter(task => {
    const matchesStatus = filterStatus === "all" || task.status === filterStatus
    const matchesSearch = searchTerm === "" || 
      task.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      task.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (task.assignedToName || "").toLowerCase().includes(searchTerm.toLowerCase())
    
    return matchesStatus && matchesSearch
  })

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button 
            variant="outline" 
            size="sm"
            onClick={() => window.location.href = '/employee/dashboard'}
            className="flex items-center gap-2"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Dashboard
          </Button>
          <div>
            <h2 className="text-2xl font-bold text-gray-900">My Tasks</h2>
            <p className="text-gray-600 mt-1">Manage your assigned tasks and track progress</p>
          </div>
        </div>
        <Button variant="outline" size="sm" onClick={fetchTasks} disabled={isLoading}>
          <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
        </Button>
      </div>

      {/* Filters and Search */}
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-2">
          <Filter className="h-4 w-4" />
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-md text-sm"
          >
            <option value="all">All Status</option>
            <option value="TODO">To Do</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="DONE">Done</option>
          </select>
        </div>
        
        <div className="flex items-center gap-2 flex-1">
          <Search className="h-4 w-4" />
          <Input
            placeholder="Search tasks..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="max-w-sm"
          />
        </div>
      </div>

      {/* Tasks List */}
      {isLoading ? (
        <div className="text-center py-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading tasks...</p>
        </div>
      ) : error ? (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      ) : filteredTasks.length === 0 ? (
        <div className="text-center py-8 text-gray-500">
          <CheckCircle className="h-12 w-12 mx-auto mb-4 opacity-50" />
          <p>No tasks found</p>
          {searchTerm && <p className="text-sm mt-2">Try adjusting your search criteria</p>}
        </div>
      ) : (
        <div className="space-y-4">
          {filteredTasks.map((task) => (
            <Card key={task.id} className="hover:shadow-md transition-shadow">
              <CardContent className="p-6">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <h3 className="font-semibold text-lg">{task.title}</h3>
                      <Badge className={getStatusColor(task.status)}>
                        {getStatusIcon(task.status)}
                        <span className="ml-1">{task.status.replace('_', ' ')}</span>
                      </Badge>
                      <Badge className={getPriorityColor(task.priority || "")}>
                        {task.priority}
                      </Badge>
                    </div>
                    
                    <p className="text-gray-700 mb-4">{task.description}</p>
                    
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm text-gray-600 mb-4">
                      <div className="flex items-center gap-2">
                        <Calendar className="h-4 w-4" />
                        <span>Due: {formatDate(task.dueDate || "")}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <User className="h-4 w-4" />
                        <span>Assigned to: {task.assignedToName}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <MessageSquare className="h-4 w-4" />
                        <span>Updated: {formatDateTime(task.updatedAt)}</span>
                      </div>
                    </div>
                    
                    {task.note && (
                      <div className="bg-gray-50 p-3 rounded-lg">
                        <span className="font-medium text-sm">Note:</span>
                        <p className="text-sm text-gray-700 mt-1">{task.note}</p>
                      </div>
                    )}
                  </div>
                  
                  <div className="ml-4">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => openUpdateDialog(task)}
                      className="flex items-center gap-2"
                    >
                      <Edit className="h-4 w-4" />
                      Update
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Update Task Dialog */}
      <Dialog open={showUpdateDialog} onOpenChange={setShowUpdateDialog}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Update Task Status</DialogTitle>
            <DialogDescription>
              Update the status and add notes for: {selectedTask?.title}
            </DialogDescription>
          </DialogHeader>
          
          <form onSubmit={handleUpdateTask} className="space-y-4">
            {error && (
              <Alert variant="destructive">
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            
            <div className="space-y-2">
              <Label htmlFor="status">Status</Label>
              <select
                id="status"
                value={updateForm.status}
                onChange={(e) => setUpdateForm(prev => ({ ...prev, status: e.target.value as "TODO" | "IN_PROGRESS" | "DONE" }))}
                className="w-full px-3 py-2 border border-gray-300 rounded-md"
                required
              >
                <option value="TODO">To Do</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="DONE">Done</option>
              </select>
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="note">Note (Optional)</Label>
              <Textarea
                id="note"
                placeholder="Add a note about this task update..."
                value={updateForm.note}
                onChange={(e) => setUpdateForm(prev => ({ ...prev, note: e.target.value }))}
                rows={3}
              />
            </div>
            
            <div className="flex justify-end gap-2">
              <Button 
                type="button" 
                variant="outline" 
                onClick={() => setShowUpdateDialog(false)}
                disabled={isUpdating}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={isUpdating}>
                {isUpdating ? (
                  <>
                    <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                    Updating...
                  </>
                ) : (
                  'Update Task'
                )}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}

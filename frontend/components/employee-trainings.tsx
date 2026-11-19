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
  BookOpen,
  Calendar,
  User,
  Award
} from "lucide-react"
import { employeeApi, EmployeeTrainingAssignment, UpdateTrainingStatusRequest } from "@/lib/api"

interface TrainingUpdateFormData {
  status: "ASSIGNED" | "IN_PROGRESS" | "COMPLETED"
  note: string
}

export default function EmployeeTrainings() {
  const [trainings, setTrainings] = useState<EmployeeTrainingAssignment[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [showUpdateDialog, setShowUpdateDialog] = useState(false)
  const [selectedTraining, setSelectedTraining] = useState<EmployeeTrainingAssignment | null>(null)
  const [isUpdating, setIsUpdating] = useState(false)
  const [filterStatus, setFilterStatus] = useState<string>("all")
  const [searchTerm, setSearchTerm] = useState("")
  
  const [updateForm, setUpdateForm] = useState<TrainingUpdateFormData>({
    status: "ASSIGNED",
    note: ""
  })

  // Fetch trainings on component mount
  useEffect(() => {
    fetchTrainings()
  }, [])

  const fetchTrainings = async () => {
    try {
      setIsLoading(true)
      setError(null)
      
      const response = await employeeApi.getTrainings()
      setTrainings(response.content)
    } catch (error) {
      console.error('Error fetching trainings:', error)
      setError('Failed to load trainings. Please try again.')
    } finally {
      setIsLoading(false)
    }
  }

  const handleUpdateTraining = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!selectedTraining) return

    try {
      setIsUpdating(true)
      setError(null)
      
      const request: UpdateTrainingStatusRequest = {
        status: updateForm.status,
        note: updateForm.note.trim() || undefined
      }
      
      await employeeApi.updateTrainingStatus(selectedTraining.id, request)
      
      // Update local state
      setTrainings(prev => prev.map(training => 
        training.id === selectedTraining.id 
          ? { ...training, status: updateForm.status, note: updateForm.note.trim() || training.note }
          : training
      ))
      
      // Reset form and close dialog
      setUpdateForm({ status: "ASSIGNED", note: "" })
      setShowUpdateDialog(false)
      setSelectedTraining(null)
    } catch (error) {
      console.error('Error updating training:', error)
      setError('Failed to update training. Please try again.')
    } finally {
      setIsUpdating(false)
    }
  }

  const openUpdateDialog = (training: EmployeeTrainingAssignment) => {
    setSelectedTraining(training)
    setUpdateForm({
      status: training.status,
      note: training.note || ""
    })
    setShowUpdateDialog(true)
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED': return 'bg-green-100 text-green-800'
      case 'IN_PROGRESS': return 'bg-blue-100 text-blue-800'
      case 'ASSIGNED': return 'bg-yellow-100 text-yellow-800'
      default: return 'bg-gray-100 text-gray-800'
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'COMPLETED': return <CheckCircle className="h-4 w-4 text-green-600" />
      case 'IN_PROGRESS': return <Clock className="h-4 w-4 text-blue-600" />
      case 'ASSIGNED': return <AlertCircle className="h-4 w-4 text-yellow-600" />
      default: return <AlertCircle className="h-4 w-4 text-gray-600" />
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

  const getTrainingProgress = (training: EmployeeTrainingAssignment) => {
    const startDate = new Date(training.assignedDate || training.assignedAt)
    const endDate = new Date(training.trainingEndDate)
    const today = new Date()
    
    if (training.status === 'COMPLETED') return 100
    if (training.status === 'ASSIGNED') return 0
    
    const totalDays = Math.ceil((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24))
    const daysPassed = Math.ceil((today.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24))
    
    if (daysPassed <= 0) return 0
    if (daysPassed >= totalDays) return 100
    
    return Math.round((daysPassed / totalDays) * 100)
  }

  // Filter and search trainings
  const filteredTrainings = trainings.filter(training => {
    const matchesStatus = filterStatus === "all" || training.status === filterStatus
    const matchesSearch = searchTerm === "" || 
      training.trainingTitle.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (training.trainingDescription || "").toLowerCase().includes(searchTerm.toLowerCase())
    
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
            <h2 className="text-2xl font-bold text-gray-900">My Trainings</h2>
            <p className="text-gray-600 mt-1">Manage your training assignments and track progress</p>
          </div>
        </div>
        <Button variant="outline" size="sm" onClick={fetchTrainings} disabled={isLoading}>
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
            <option value="ASSIGNED">Assigned</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="COMPLETED">Completed</option>
          </select>
        </div>
        
        <div className="flex items-center gap-2 flex-1">
          <Search className="h-4 w-4" />
          <Input
            placeholder="Search trainings..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="max-w-sm"
          />
        </div>
      </div>

      {/* Trainings List */}
      {isLoading ? (
        <div className="text-center py-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading trainings...</p>
        </div>
      ) : error ? (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      ) : filteredTrainings.length === 0 ? (
        <div className="text-center py-8 text-gray-500">
          <BookOpen className="h-12 w-12 mx-auto mb-4 opacity-50" />
          <p>No trainings found</p>
          {searchTerm && <p className="text-sm mt-2">Try adjusting your search criteria</p>}
        </div>
      ) : (
        <div className="space-y-4">
          {filteredTrainings.map((training) => (
            <Card key={training.id} className="hover:shadow-md transition-shadow">
              <CardContent className="p-6">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <h3 className="font-semibold text-lg">{training.trainingTitle}</h3>
                      <Badge className={getStatusColor(training.status)}>
                        {getStatusIcon(training.status)}
                        <span className="ml-1">{training.status.replace('_', ' ')}</span>
                      </Badge>
                    </div>
                    
                    <p className="text-gray-700 mb-4">{training.trainingDescription}</p>
                    
                    {/* Progress Bar */}
                    {training.status === 'IN_PROGRESS' && (
                      <div className="mb-4">
                        <div className="flex justify-between text-sm text-gray-600 mb-1">
                          <span>Progress</span>
                          <span>{getTrainingProgress(training)}%</span>
                        </div>
                        <div className="w-full bg-gray-200 rounded-full h-2">
                          <div 
                            className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                            style={{ width: `${getTrainingProgress(training)}%` }}
                          ></div>
                        </div>
                      </div>
                    )}
                    
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm text-gray-600 mb-4">
                      <div className="flex items-center gap-2">
                        <Calendar className="h-4 w-4" />
                        <span>Assigned: {formatDate(training.assignedDate || training.assignedAt)}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <Calendar className="h-4 w-4" />
                        <span>Due: {formatDate(training.trainingEndDate)}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <Award className="h-4 w-4" />
                        <span>Updated: {formatDateTime(training.updatedAt)}</span>
                      </div>
                    </div>
                    
                    {training.note && (
                      <div className="bg-gray-50 p-3 rounded-lg">
                        <span className="font-medium text-sm">Note:</span>
                        <p className="text-sm text-gray-700 mt-1">{training.note}</p>
                      </div>
                    )}
                  </div>
                  
                  <div className="ml-4">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => openUpdateDialog(training)}
                      className="flex items-center gap-2"
                      disabled={training.status === 'COMPLETED'}
                    >
                      <Edit className="h-4 w-4" />
                      {training.status === 'COMPLETED' ? 'Completed' : 'Update'}
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Update Training Dialog */}
      <Dialog open={showUpdateDialog} onOpenChange={setShowUpdateDialog}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Update Training Status</DialogTitle>
            <DialogDescription>
              Update the status and add notes for: {selectedTraining?.trainingTitle}
            </DialogDescription>
          </DialogHeader>
          
          <form onSubmit={handleUpdateTraining} className="space-y-4">
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
                onChange={(e) => setUpdateForm(prev => ({ ...prev, status: e.target.value as "ASSIGNED" | "IN_PROGRESS" | "COMPLETED" }))}
                className="w-full px-3 py-2 border border-gray-300 rounded-md"
                required
              >
                <option value="ASSIGNED">Assigned</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="COMPLETED">Completed</option>
              </select>
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="note">Note (Optional)</Label>
              <Textarea
                id="note"
                placeholder="Add a note about your training progress..."
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
                  'Update Training'
                )}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}

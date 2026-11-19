"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { 
  ArrowLeft, 
  RefreshCw, 
  CheckCircle, 
  XCircle, 
  Clock, 
  AlertCircle,
  Calendar,
  User,
  Filter,
  Search
} from "lucide-react"
import { adminLeaveApi, LeaveRequest } from "@/lib/api"
import { toast } from "sonner"

export default function AdminLeaveManagement() {
  const [leaveRequests, setLeaveRequests] = useState<LeaveRequest[]>([])
  const [allLeaveRequests, setAllLeaveRequests] = useState<LeaveRequest[]>([])
  const [loading, setLoading] = useState(false)
  const [filterStatus, setFilterStatus] = useState<string>("all")
  const [searchTerm, setSearchTerm] = useState("")

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      setLoading(true)
      
      // Load all leave requests for statistics
      const allRequestsResponse = await adminLeaveApi.getAllLeaveRequests()
      setAllLeaveRequests(allRequestsResponse.content)
      
      // Load pending requests for the main list
      const pendingRequestsResponse = await adminLeaveApi.getPendingLeaveRequests()
      setLeaveRequests(pendingRequestsResponse.content)
      
    } catch (error) {
      console.error("Error loading leave requests:", error)
      toast.error("Failed to load leave requests")
    } finally {
      setLoading(false)
    }
  }

  const handleApproveLeaveRequest = async (requestId: number) => {
    try {
      await adminLeaveApi.approveLeaveRequest(requestId)
      toast.success("Leave request approved successfully")
      loadData() // Refresh data
    } catch (error: any) {
      console.error("Error approving leave request:", error)
      if (error.status === 401 || error.status === 403) {
        toast.error("Session expired. Please log in again.")
        // Redirect to login after a short delay
        setTimeout(() => {
          window.location.href = "/login"
        }, 2000)
      } else {
        toast.error("Failed to approve leave request")
      }
    }
  }

  const handleRejectLeaveRequest = async (requestId: number) => {
    try {
      await adminLeaveApi.rejectLeaveRequest(requestId)
      toast.success("Leave request rejected successfully")
      loadData() // Refresh data
    } catch (error: any) {
      console.error("Error rejecting leave request:", error)
      if (error.status === 401 || error.status === 403) {
        toast.error("Session expired. Please log in again.")
        // Redirect to login after a short delay
        setTimeout(() => {
          window.location.href = "/login"
        }, 2000)
      } else {
        toast.error("Failed to reject leave request")
      }
    }
  }

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "PENDING":
        return <Badge variant="outline" className="bg-yellow-50 text-yellow-700 border-yellow-200"><Clock className="h-3 w-3 mr-1" />Pending</Badge>
      case "APPROVED":
        return <Badge variant="outline" className="bg-green-50 text-green-700 border-green-200"><CheckCircle className="h-3 w-3 mr-1" />Approved</Badge>
      case "REJECTED":
        return <Badge variant="outline" className="bg-red-50 text-red-700 border-red-200"><XCircle className="h-3 w-3 mr-1" />Rejected</Badge>
      default:
        return <Badge variant="outline">{status}</Badge>
    }
  }

  const filteredRequests = leaveRequests.filter(request => {
    const matchesStatus = filterStatus === "all" || request.status === filterStatus
    const matchesSearch = searchTerm === "" || 
      request.employee?.firstName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      request.employee?.lastName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      request.employee?.email?.toLowerCase().includes(searchTerm.toLowerCase())
    return matchesStatus && matchesSearch
  })

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-foreground">Leave Management</h1>
          <p className="text-muted-foreground">Manage employee leave requests and approvals</p>
        </div>
        
        <div className="flex gap-2">
          <Button 
            variant="outline" 
            onClick={loadData}
            className="flex items-center gap-2"
          >
            <RefreshCw className="h-4 w-4" />
            Refresh Data
          </Button>
        </div>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Requests</CardTitle>
            <AlertCircle className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{allLeaveRequests.length}</div>
            <p className="text-xs text-muted-foreground">All time requests</p>
          </CardContent>
        </Card>
        
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Pending</CardTitle>
            <Clock className="h-4 w-4 text-yellow-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {allLeaveRequests.filter(r => r.status === "PENDING").length}
            </div>
            <p className="text-xs text-muted-foreground">Awaiting approval</p>
          </CardContent>
        </Card>
        
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Approved</CardTitle>
            <CheckCircle className="h-4 w-4 text-green-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {allLeaveRequests.filter(r => r.status === "APPROVED").length}
            </div>
            <p className="text-xs text-muted-foreground">Approved requests</p>
          </CardContent>
        </Card>
        
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Rejected</CardTitle>
            <XCircle className="h-4 w-4 text-red-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {allLeaveRequests.filter(r => r.status === "REJECTED").length}
            </div>
            <p className="text-xs text-muted-foreground">Rejected requests</p>
          </CardContent>
        </Card>
      </div>

      {/* Filters and Search */}
      <Card>
        <CardHeader>
          <CardTitle>Leave Requests</CardTitle>
          <CardDescription>Manage and review employee leave requests</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col sm:flex-row gap-4 mb-6">
            <div className="flex-1">
              <Label htmlFor="search">Search</Label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
                <Input
                  id="search"
                  placeholder="Search by employee name or email..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>
            <div className="sm:w-48">
              <Label htmlFor="status-filter">Status</Label>
              <select
                id="status-filter"
                value={filterStatus}
                onChange={(e) => setFilterStatus(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="all">All Status</option>
                <option value="PENDING">Pending</option>
                <option value="APPROVED">Approved</option>
                <option value="REJECTED">Rejected</option>
              </select>
            </div>
          </div>

          {/* Leave Requests List */}
          {loading ? (
            <div className="text-center py-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
              <p className="mt-2 text-muted-foreground">Loading leave requests...</p>
            </div>
          ) : filteredRequests.length === 0 ? (
            <div className="text-center py-8">
              <AlertCircle className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
              <h3 className="text-lg font-medium text-foreground mb-2">No leave requests found</h3>
              <p className="text-muted-foreground">
                {searchTerm || filterStatus !== "all" 
                  ? "Try adjusting your search or filter criteria"
                  : "No leave requests have been submitted yet"
                }
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {filteredRequests.map((request) => (
                <Card key={request.id} className="p-4">
                  <div className="flex items-center justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-4 mb-2">
                        <div className="flex items-center gap-2">
                          <User className="h-4 w-4 text-muted-foreground" />
                          <span className="font-medium">
                            {request.employee?.firstName} {request.employee?.lastName}
                          </span>
                          <span className="text-muted-foreground">({request.employee?.email})</span>
                        </div>
                        {getStatusBadge(request.status)}
                      </div>
                      
                      <div className="flex items-center gap-6 text-sm text-muted-foreground">
                        <div className="flex items-center gap-1">
                          <Calendar className="h-4 w-4" />
                          <span>From: {new Date(request.fromDate || request.startDate).toLocaleDateString()}</span>
                        </div>
                        <div className="flex items-center gap-1">
                          <Calendar className="h-4 w-4" />
                          <span>To: {new Date(request.toDate || request.endDate).toLocaleDateString()}</span>
                        </div>
                        <div className="flex items-center gap-1">
                          <Clock className="h-4 w-4" />
                          <span>
                            {Math.ceil((new Date(request.toDate || request.endDate).getTime() - new Date(request.fromDate || request.startDate).getTime()) / (1000 * 60 * 60 * 24)) + 1} days
                          </span>
                        </div>
                      </div>
                      
                      {request.reason && (
                        <div className="mt-2">
                          <p className="text-sm text-muted-foreground">
                            <strong>Reason:</strong> {request.reason}
                          </p>
                        </div>
                      )}
                    </div>
                    
                    {request.status === "PENDING" && (
                      <div className="flex gap-2">
                        <Button
                          size="sm"
                          onClick={() => handleApproveLeaveRequest(request.id)}
                          className="bg-green-600 hover:bg-green-700"
                        >
                          <CheckCircle className="h-4 w-4 mr-1" />
                          Approve
                        </Button>
                        <Button
                          size="sm"
                          variant="destructive"
                          onClick={() => handleRejectLeaveRequest(request.id)}
                        >
                          <XCircle className="h-4 w-4 mr-1" />
                          Reject
                        </Button>
                      </div>
                    )}
                  </div>
                </Card>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

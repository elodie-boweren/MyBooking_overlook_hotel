"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { 
  ArrowLeft, 
  Star, 
  MessageSquare, 
  Search, 
  Reply, 
  CheckCircle,
  Clock,
  User,
  Calendar,
  Hotel
} from "lucide-react"
import Link from "next/link"
import { feedbackApi } from "@/lib/api"
import type { Feedback } from "@/lib/api"
import { toast } from "sonner"
import { format } from "date-fns"

export default function AdminFeedbackPage() {
  const [feedbacks, setFeedbacks] = useState<Feedback[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedRating, setSelectedRating] = useState("all")
  const [selectedStatus, setSelectedStatus] = useState("all")
  const [selectedFeedback, setSelectedFeedback] = useState<Feedback | null>(null)
  const [isReplyModalOpen, setIsReplyModalOpen] = useState(false)
  const [replyText, setReplyText] = useState("")
  const [submittingReply, setSubmittingReply] = useState(false)

  useEffect(() => {
    const fetchFeedbacks = async () => {
      try {
        setLoading(true)
        const response = await feedbackApi.getAllFeedbacks()
        setFeedbacks(response.content || [])
      } catch (error) {
        console.error('Failed to fetch feedbacks:', error)
        toast.error("Failed to load feedbacks")
        setFeedbacks([])
      } finally {
        setLoading(false)
      }
    }

    fetchFeedbacks()
  }, [])

  const filteredFeedbacks = feedbacks.filter(feedback => {
    const matchesSearch = 
      feedback.userName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      feedback.userEmail.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (feedback.comment || "").toLowerCase().includes(searchTerm.toLowerCase())
    
    const matchesRating = selectedRating === "all" || feedback.rating.toString() === selectedRating
    const matchesStatus = selectedStatus === "all" || 
      (selectedStatus === "replied" && feedback.replies && feedback.replies.length > 0) ||
      (selectedStatus === "pending" && (!feedback.replies || feedback.replies.length === 0))
    
    return matchesSearch && matchesRating && matchesStatus
  })

  const getRatingBadge = (rating: number) => {
    const colors = {
      5: "bg-green-100 text-green-800",
      4: "bg-blue-100 text-blue-800", 
      3: "bg-yellow-100 text-yellow-800",
      2: "bg-orange-100 text-orange-800",
      1: "bg-red-100 text-red-800"
    }
    return colors[rating as keyof typeof colors] || "bg-gray-100 text-gray-800"
  }

  const getStatusBadge = (feedback: Feedback) => {
    if (feedback.replies && feedback.replies.length > 0) {
      return <Badge className="bg-green-100 text-green-800">Replied</Badge>
    }
    return <Badge variant="secondary">Pending</Badge>
  }

  const handleReplySubmit = async () => {
    if (!selectedFeedback || !replyText.trim()) return

    setSubmittingReply(true)
    try {
      await feedbackApi.replyToFeedback(selectedFeedback.id, replyText.trim())
      
      toast.success("Reply sent successfully!")
      setIsReplyModalOpen(false)
      setReplyText("")
      setSelectedFeedback(null)
      
      // Refresh feedbacks
      const response = await feedbackApi.getAllFeedbacks()
      setFeedbacks(response.content || [])
    } catch (error) {
      console.error("Failed to send reply:", error)
      toast.error("Failed to send reply. Please try again.")
    } finally {
      setSubmittingReply(false)
    }
  }

  const openReplyModal = (feedback: Feedback) => {
    setSelectedFeedback(feedback)
    setReplyText("") // Always start with empty text for new replies
    setIsReplyModalOpen(true)
  }

  const totalFeedbacks = feedbacks.length
  const averageRating = feedbacks.length > 0 
    ? (feedbacks.reduce((sum, f) => sum + f.rating, 0) / feedbacks.length).toFixed(1)
    : "0.0"
  const pendingReplies = feedbacks.filter(f => !f.replies || f.replies.length === 0).length
  const repliedCount = feedbacks.filter(f => f.replies && f.replies.length > 0).length

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-7xl">
        <div className="text-center py-8">Loading feedbacks...</div>
      </div>
    )
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-7xl">
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-4">
          <Link href="/admin">
            <Button variant="outline" size="sm">
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Admin
            </Button>
          </Link>
          <div>
            <h1 className="text-3xl font-bold text-foreground">Feedback Management</h1>
            <p className="text-muted-foreground mt-2">Manage client feedback and respond to reviews</p>
          </div>
        </div>
      </div>

      {/* Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <Card>
          <CardHeader className="text-center">
            <div className="mx-auto w-12 h-12 bg-primary/10 rounded-full flex items-center justify-center mb-2">
              <MessageSquare className="h-6 w-6 text-primary" />
            </div>
            <CardTitle className="text-2xl">{totalFeedbacks}</CardTitle>
            <CardDescription>Total Feedbacks</CardDescription>
          </CardHeader>
        </Card>

        <Card>
          <CardHeader className="text-center">
            <div className="mx-auto w-12 h-12 bg-yellow-100 rounded-full flex items-center justify-center mb-2">
              <Star className="h-6 w-6 text-yellow-600" />
            </div>
            <CardTitle className="text-2xl">{averageRating}</CardTitle>
            <CardDescription>Average Rating</CardDescription>
          </CardHeader>
        </Card>

        <Card>
          <CardHeader className="text-center">
            <div className="mx-auto w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mb-2">
              <Clock className="h-6 w-6 text-blue-600" />
            </div>
            <CardTitle className="text-2xl">{pendingReplies}</CardTitle>
            <CardDescription>Pending Replies</CardDescription>
          </CardHeader>
        </Card>

        <Card>
          <CardHeader className="text-center">
            <div className="mx-auto w-12 h-12 bg-green-100 rounded-full flex items-center justify-center mb-2">
              <CheckCircle className="h-6 w-6 text-green-600" />
            </div>
            <CardTitle className="text-2xl">{repliedCount}</CardTitle>
            <CardDescription>Replied</CardDescription>
          </CardHeader>
        </Card>
      </div>

      {/* Search and Filters */}
      <div className="flex gap-4 mb-6">
        <div className="flex-1">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search by client name, email, or feedback content..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10"
            />
          </div>
        </div>
        
        <Select value={selectedRating} onValueChange={setSelectedRating}>
          <SelectTrigger className="w-48">
            <SelectValue placeholder="Filter by Rating" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All Ratings</SelectItem>
            <SelectItem value="5">5 Stars</SelectItem>
            <SelectItem value="4">4 Stars</SelectItem>
            <SelectItem value="3">3 Stars</SelectItem>
            <SelectItem value="2">2 Stars</SelectItem>
            <SelectItem value="1">1 Star</SelectItem>
          </SelectContent>
        </Select>

        <Select value={selectedStatus} onValueChange={setSelectedStatus}>
          <SelectTrigger className="w-48">
            <SelectValue placeholder="Filter by Status" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All Status</SelectItem>
            <SelectItem value="pending">Pending Reply</SelectItem>
            <SelectItem value="replied">Replied</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Feedbacks List */}
      <Card>
        <CardHeader>
          <CardTitle>Client Feedbacks</CardTitle>
          <CardDescription>
            {filteredFeedbacks.length} feedbacks found
          </CardDescription>
        </CardHeader>
        <CardContent>
          {filteredFeedbacks.length === 0 ? (
            <div className="text-center py-8">
              <MessageSquare className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
              <h3 className="text-lg font-semibold">No feedbacks found</h3>
              <p className="text-muted-foreground">No feedbacks match your search criteria.</p>
            </div>
          ) : (
            <div className="space-y-4">
              {filteredFeedbacks.map((feedback) => (
                <div key={feedback.id} className="p-6 border rounded-lg">
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 bg-primary/10 rounded-full flex items-center justify-center">
                        <span className="text-sm font-semibold text-primary">
                          {feedback.userName.charAt(0).toUpperCase()}
                        </span>
                      </div>
                      <div>
                        <p className="font-medium">{feedback.userName}</p>
                        <p className="text-sm text-muted-foreground">{feedback.userEmail}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <Badge className={getRatingBadge(feedback.rating)}>
                        {feedback.rating} Stars
                      </Badge>
                      {getStatusBadge(feedback)}
                    </div>
                  </div>

                  <div className="space-y-3">
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <Hotel className="h-4 w-4" />
                      <span>Room {feedback.reservation?.roomNumber}</span>
                      <Calendar className="h-4 w-4 ml-4" />
                      <span>{format(new Date(feedback.createdAt), "MMM dd, yyyy")}</span>
                    </div>

                    {feedback.comment && (
                      <div className="bg-gray-50 p-4 rounded-lg">
                        <p className="text-sm">{feedback.comment}</p>
                      </div>
                    )}

                    {feedback.replies && feedback.replies.length > 0 && (
                      <div className="space-y-2">
                        {feedback.replies.map((reply, index) => (
                          <div key={reply.id} className="bg-blue-50 p-4 rounded-lg">
                            <div className="flex items-center gap-2 mb-2">
                              <Reply className="h-4 w-4 text-blue-600" />
                              <span className="text-sm font-medium text-blue-800">
                                Admin Reply {(feedback.replies?.length ?? 0) > 1 ? `#${index + 1}` : ''}:
                              </span>
                              <span className="text-xs text-blue-600">
                                by {reply.adminUserName}
                              </span>
                            </div>
                            <p className="text-sm text-blue-700">{reply.message}</p>
                            <p className="text-xs text-blue-600 mt-1">
                              {format(new Date(reply.createdAt), "MMM dd, yyyy 'at' HH:mm")}
                            </p>
                          </div>
                        ))}
                      </div>
                    )}

                    <div className="flex justify-end pt-3 border-t">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => openReplyModal(feedback)}
                      >
                        <Reply className="h-4 w-4 mr-2" />
                        {feedback.replies && feedback.replies.length > 0 ? "Add Reply" : "Reply"}
                      </Button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Reply Modal */}
      <Dialog open={isReplyModalOpen} onOpenChange={setIsReplyModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Reply className="h-5 w-5 text-blue-600" />
              Reply to Feedback
            </DialogTitle>
            <DialogDescription>
              Respond to {selectedFeedback?.userName}&apos;s feedback
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="reply">Admin Reply</Label>
              <Textarea
                id="reply"
                placeholder="Write your reply to the client..."
                value={replyText}
                onChange={(e) => setReplyText(e.target.value)}
                className="min-h-[120px]"
              />
            </div>

            <div className="flex gap-3 pt-4">
              <Button
                variant="outline"
                onClick={() => setIsReplyModalOpen(false)}
                className="flex-1"
                disabled={submittingReply}
              >
                Cancel
              </Button>
              <Button
                onClick={handleReplySubmit}
                className="flex-1"
                disabled={submittingReply || !replyText.trim()}
              >
                {submittingReply ? "Sending..." : "Send Reply"}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}

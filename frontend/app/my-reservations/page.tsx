"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { 
  ArrowLeft, 
  CalendarIcon, 
  Users, 
  MapPin, 
  Clock, 
  Star,
  MessageSquare,
  CheckCircle,
  XCircle,
  Eye,
  Reply
} from "lucide-react"
import Link from "next/link"
import { reservationApi, feedbackApi } from "@/lib/api"
import type { ReservationResponse, FeedbackResponse } from "@/lib/api"
import { toast } from "sonner"
import { format } from "date-fns"

export default function MyReservationsPage() {
  const [reservations, setReservations] = useState<ReservationResponse[]>([])
  const [feedbacks, setFeedbacks] = useState<FeedbackResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [statusFilter, setStatusFilter] = useState<string>("all")
  const [selectedReservation, setSelectedReservation] = useState<ReservationResponse | null>(null)
  const [isFeedbackModalOpen, setIsFeedbackModalOpen] = useState(false)
  const [feedbackData, setFeedbackData] = useState({
    rating: 5,
    comment: ""
  })
  const [submittingFeedback, setSubmittingFeedback] = useState(false)

  const loadReservations = async () => {
    setLoading(true)
    try {
      console.log('🔍 DEBUG: Loading reservations...')
      console.log('🔍 DEBUG: Current user from localStorage:', localStorage.getItem('user'))
      console.log('🔍 DEBUG: Current token from localStorage:', localStorage.getItem('token'))
      
      // Test direct API call to see what's happening
      try {
        console.log('🧪 TESTING: Direct API call to /client/reservations/my...')
        const token = localStorage.getItem('token')
        const user = JSON.parse(localStorage.getItem('user') || '{}')
        console.log('🧪 USER ID FROM LOCALSTORAGE:', user.id)
        console.log('🧪 USER EMAIL FROM LOCALSTORAGE:', user.email)
        
        const testResponse = await fetch('http://localhost:8080/api/client/reservations/my?page=0&size=100', {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        })
        console.log('🧪 TEST RESPONSE STATUS:', testResponse.status)
        if (testResponse.ok) {
          const testData = await testResponse.json()
          console.log('🧪 TEST DATA:', testData)
          console.log('🧪 TEST CONTENT LENGTH:', testData.content?.length || 0)
          console.log('🧪 TEST SAMPLE RESERVATION:', testData.content?.[0])
          
          // Check if the reservation we know exists (ID 38) is in the response
          const reservation38 = testData.content?.find((r: any) => r.id === 38)
          if (reservation38) {
            console.log('✅ FOUND RESERVATION 38:', reservation38)
          } else {
            console.log('❌ RESERVATION 38 NOT FOUND in API response')
            console.log('❌ Available reservation IDs:', testData.content?.map((r: any) => r.id) || [])
          }
        } else {
          console.log('🧪 TEST FAILED:', testResponse.status, testResponse.statusText)
          const errorText = await testResponse.text()
          console.log('🧪 ERROR RESPONSE:', errorText)
        }
      } catch (testError) {
        console.log('🧪 TEST ERROR:', testError)
      }
      
      // Test direct call to the specific reservation we know exists
      try {
        console.log('🧪 TESTING: Direct API call to /client/reservations/38...')
        const token = localStorage.getItem('token')
        const testResponse38 = await fetch('http://localhost:8080/api/client/reservations/38', {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        })
        console.log('🧪 TEST RESPONSE 38 STATUS:', testResponse38.status)
        if (testResponse38.ok) {
          const testData38 = await testResponse38.json()
          console.log('🧪 TEST DATA 38:', testData38)
        } else {
          console.log('🧪 TEST 38 FAILED:', testResponse38.status, testResponse38.statusText)
          const errorText38 = await testResponse38.text()
          console.log('🧪 ERROR RESPONSE 38:', errorText38)
        }
      } catch (testError38) {
        console.log('🧪 TEST ERROR 38:', testError38)
      }
      
      const [reservationResponse, feedbackResponse] = await Promise.all([
        reservationApi.getMyReservations(0, 100) // Load more reservations to get all pages
          .then(data => {
            console.log('✅ My Reservations API SUCCESS:', data)
            console.log('✅ Reservations count:', data.content?.length || 0)
            console.log('✅ Total elements:', data.totalElements)
            console.log('✅ Sample reservation:', data.content?.[0])
            
            // Check if reservation 38 is now in the response
            const reservation38 = data.content?.find((r: any) => r.id === 38)
            if (reservation38) {
              console.log('✅ FOUND RESERVATION 38 in API response:', reservation38)
            } else {
              console.log('❌ RESERVATION 38 STILL NOT FOUND in API response')
              console.log('❌ Available reservation IDs:', data.content?.map((r: any) => r.id) || [])
            }
            
            return data
          })
          .catch(error => {
            console.error('❌ My Reservations API FAILED:', error)
            throw error
          }),
        feedbackApi.getUserFeedbacks()
          .then(data => {
            console.log('✅ My Feedbacks API SUCCESS:', data)
            return data
          })
          .catch(error => {
            console.error('❌ My Feedbacks API FAILED:', error)
            throw error
          })
      ])
      
      console.log('🔍 DEBUG: Setting reservations:', reservationResponse.content?.length || 0)
      setReservations(reservationResponse.content || [])
      setFeedbacks(feedbackResponse.content || [])
    } catch (error) {
      console.error("❌ CRITICAL: Failed to fetch reservations:", error)
      toast.error("Failed to load your reservations.")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadReservations()
  }, [])

  const filteredReservations = reservations.filter(reservation => {
    if (statusFilter !== "all" && reservation.status !== statusFilter) {
      return false
    }
    return true
  })

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "CONFIRMED":
        return <Badge className="bg-green-100 text-green-800">Confirmed</Badge>
      case "CANCELLED":
        return <Badge variant="destructive">Cancelled</Badge>
      default:
        return <Badge variant="outline">{status}</Badge>
    }
  }

  const isPastReservation = (checkOut: string) => {
    return new Date(checkOut) < new Date()
  }

  const canLeaveFeedback = (reservation: ReservationResponse) => {
    return isPastReservation(reservation.checkOut) && reservation.status === "CONFIRMED"
  }

  const getFeedbackForReservation = (reservationId: number) => {
    return feedbacks.find(feedback => feedback.reservationId === reservationId)
  }

  const hasFeedback = (reservation: ReservationResponse) => {
    return getFeedbackForReservation(reservation.id) !== undefined
  }

  const handleFeedbackSubmit = async () => {
    if (!selectedReservation) return

    setSubmittingFeedback(true)
    try {
      await feedbackApi.createFeedback({
        reservationId: selectedReservation.id,
        rating: feedbackData.rating,
        comment: feedbackData.comment
      })
      
      toast.success("Feedback submitted successfully!")
      setIsFeedbackModalOpen(false)
      setFeedbackData({ rating: 5, comment: "" })
      setSelectedReservation(null)
      
      // Refresh both reservations and feedback data
      const [reservationResponse, feedbackResponse] = await Promise.all([
        reservationApi.getMyReservations(),
        feedbackApi.getUserFeedbacks()
      ])
      setReservations(reservationResponse.content || [])
      setFeedbacks(feedbackResponse.content || [])
    } catch (error: any) {
      console.error("Failed to submit feedback:", error)
      if (error.message && error.message.includes("already exists")) {
        toast.error("You have already submitted feedback for this reservation")
      } else {
        toast.error("Failed to submit feedback. Please try again.")
      }
    } finally {
      setSubmittingFeedback(false)
    }
  }

  const openFeedbackModal = (reservation: ReservationResponse) => {
    setSelectedReservation(reservation)
    setIsFeedbackModalOpen(true)
  }

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-6xl">
        <div className="text-center py-8">Loading your reservations...</div>
      </div>
    )
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-6xl">
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-4">
          <Link href="/rooms">
            <Button variant="outline" size="sm">
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Dashboard
            </Button>
          </Link>
          <div>
            <h1 className="text-3xl font-bold text-foreground">My Reservations</h1>
            <p className="text-muted-foreground mt-2">View and manage your room reservations</p>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="flex gap-4 mb-6">
        <Select value={statusFilter} onValueChange={setStatusFilter}>
          <SelectTrigger className="w-48">
            <SelectValue placeholder="Filter by Status" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All Statuses</SelectItem>
            <SelectItem value="CONFIRMED">Confirmed</SelectItem>
            <SelectItem value="CANCELLED">Cancelled</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {filteredReservations.length === 0 ? (
        <div className="text-center py-16">
          <CalendarIcon className="h-16 w-16 mx-auto text-muted-foreground mb-4" />
          <h3 className="text-xl font-semibold">No reservations found</h3>
          <p className="text-muted-foreground mt-2">You haven&apos;t made any reservations yet. <Link href="/rooms" className="text-primary hover:underline">Browse our rooms</Link> to get started!</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredReservations.map((reservation) => (
            <Card key={reservation.id} className="flex flex-col">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle className="text-lg">Room {reservation.roomNumber}</CardTitle>
                  {getStatusBadge(reservation.status)}
                </div>
                <CardDescription className="text-sm text-muted-foreground">
                  {reservation.roomType}
                </CardDescription>
              </CardHeader>
              <CardContent className="flex-grow space-y-3 text-sm">
                <div className="flex items-center gap-2 text-muted-foreground">
                  <CalendarIcon className="h-4 w-4 text-primary" />
                  <span>{format(new Date(reservation.checkIn), "MMM dd, yyyy")} - {format(new Date(reservation.checkOut), "MMM dd, yyyy")}</span>
                </div>
                <div className="flex items-center gap-2 text-muted-foreground">
                  <Users className="h-4 w-4 text-primary" />
                  <span>{reservation.numberOfGuests} guest{reservation.numberOfGuests > 1 ? 's' : ''}</span>
                </div>
                <div className="flex items-center gap-2 text-muted-foreground">
                  <MapPin className="h-4 w-4 text-primary" />
                  <span>{(reservation as any).installationName}</span>
                </div>
                <div className="flex items-center justify-between pt-2 border-t border-border">
                  <span className="text-muted-foreground">Total Price:</span>
                  <span className="font-semibold text-lg text-foreground">{reservation.totalPrice} {reservation.currency}</span>
                </div>
                
                {/* Feedback Section */}
                {canLeaveFeedback(reservation) && (
                  <div className="pt-3 border-t border-border">
                    {hasFeedback(reservation) ? (
                      (() => {
                        const feedback = getFeedbackForReservation(reservation.id)!
                        return (
                          <div className="space-y-2">
                            <div className="flex items-center gap-2">
                              <Star className="h-4 w-4 text-yellow-500" />
                              <span className="text-sm font-medium">Your Rating: {feedback.rating}/5</span>
                            </div>
                            {feedback.comment && (
                              <p className="text-sm text-muted-foreground">{feedback.comment}</p>
                            )}
                            {feedback.replies && feedback.replies.length > 0 && (
                              <div className="space-y-2">
                                {feedback.replies.map((reply, index) => (
                                  <div key={reply.id} className="bg-blue-50 p-3 rounded text-sm">
                                    <div className="flex items-center gap-2 mb-1">
                                      <Reply className="h-3 w-3 text-blue-600" />
                                      <span className="font-medium text-blue-800">
                                        Admin Response {(feedback.replies?.length ?? 0) > 1 ? `#${index + 1}` : ''}:
                                      </span>
                                      <span className="text-xs text-blue-600">
                                        by {reply.adminUserName}
                                      </span>
                                    </div>
                                    <p className="text-blue-700">{reply.message}</p>
                                    <p className="text-xs text-blue-600 mt-1">
                                      {format(new Date(reply.createdAt), "MMM dd, yyyy 'at' HH:mm")}
                                    </p>
                                  </div>
                                ))}
                              </div>
                            )}
                          </div>
                        )
                      })()
                    ) : (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => openFeedbackModal(reservation)}
                        className="w-full"
                      >
                        <Star className="h-4 w-4 mr-2" />
                        Leave Feedback
                      </Button>
                    )}
                  </div>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Feedback Modal */}
      <Dialog open={isFeedbackModalOpen} onOpenChange={setIsFeedbackModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Star className="h-5 w-5 text-yellow-500" />
              Leave Feedback
            </DialogTitle>
            <DialogDescription>
              Share your experience about your stay in Room {selectedReservation?.roomNumber}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="rating">Rating</Label>
              <div className="flex items-center gap-2">
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setFeedbackData(prev => ({ ...prev, rating: star }))}
                    className="text-2xl focus:outline-none"
                  >
                    <Star 
                      className={`h-8 w-8 ${
                        star <= feedbackData.rating 
                          ? 'text-yellow-500 fill-current' 
                          : 'text-gray-300'
                      }`} 
                    />
                  </button>
                ))}
                <span className="ml-2 text-sm text-muted-foreground">
                  {feedbackData.rating}/5
                </span>
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="comment">Comment (Optional)</Label>
              <Textarea
                id="comment"
                placeholder="Tell us about your experience..."
                value={feedbackData.comment}
                onChange={(e) => setFeedbackData(prev => ({ ...prev, comment: e.target.value }))}
                className="min-h-[100px]"
              />
            </div>

            <div className="flex gap-3 pt-4">
              <Button
                variant="outline"
                onClick={() => setIsFeedbackModalOpen(false)}
                className="flex-1"
                disabled={submittingFeedback}
              >
                Cancel
              </Button>
              <Button
                onClick={handleFeedbackSubmit}
                className="flex-1"
                disabled={submittingFeedback}
              >
                {submittingFeedback ? "Submitting..." : "Submit Feedback"}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}
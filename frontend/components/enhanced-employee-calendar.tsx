"use client"

import type React from "react"
import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent } from "@/components/ui/card"
import { Textarea } from "@/components/ui/textarea"
import { Input } from "@/components/ui/input"
import {
  ChevronLeft,
  ChevronRight,
  Clock,
  Users,
  BookOpen,
  Camera,
  MessageSquare,
  Send,
  Paperclip,
  CheckCircle,
  X,
  Calendar as CalendarIcon,
  Grid3X3,
  List,
} from "lucide-react"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { employeeDashboardApi, CalendarEvent } from "@/lib/api"

interface TaskReply {
  id: string
  content: string
  createdAt: string
  attachments?: string[]
}

// CalendarEvent interface is now imported from @/lib/api

// Fetch real calendar events from backend
const fetchCalendarEvents = async (startDate: string, endDate: string): Promise<CalendarEvent[]> => {
  try {
    return await employeeDashboardApi.getCalendarEvents(startDate, endDate)
  } catch (error) {
    console.error('Error fetching calendar events:', error)
    return []
  }
}

type ViewMode = "day" | "week" | "month"

export default function EnhancedEmployeeCalendar() {
  const [selectedDate, setSelectedDate] = useState(new Date())
  const [selectedEvent, setSelectedEvent] = useState<CalendarEvent | null>(null)
  const [showEventDialog, setShowEventDialog] = useState(false)
  const [events, setEvents] = useState<CalendarEvent[]>([])
  const [replyText, setReplyText] = useState("")
  const [attachments, setAttachments] = useState<File[]>([])
  const [viewMode, setViewMode] = useState<ViewMode>("week")
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Fetch events only once when component mounts
  useEffect(() => {
    const loadEvents = async () => {
      setIsLoading(true)
      setError(null)
      
      try {
        // Fetch events for a broader date range to avoid frequent API calls
        const today = new Date()
        const startDate = new Date(today.getFullYear(), today.getMonth() - 1, 1).toISOString().split('T')[0]
        const endDate = new Date(today.getFullYear(), today.getMonth() + 2, 0).toISOString().split('T')[0]
        
        const fetchedEvents = await fetchCalendarEvents(startDate, endDate)
        setEvents(fetchedEvents)
      } catch (error) {
        console.error('Error loading calendar events:', error)
        setError('Failed to load calendar events')
        setEvents([])
      } finally {
        setIsLoading(false)
      }
    }
    
    loadEvents()
  }, []) // Only run once on mount

  const getEventColor = (type: CalendarEvent["type"]) => {
    switch (type) {
      case "shift":
        return "bg-blue-100 text-blue-800 border-blue-200"
      case "task":
        return "bg-green-100 text-green-800 border-green-200"
      case "training":
        return "bg-red-100 text-red-800 border-red-200"
      default:
        return "bg-gray-100 text-gray-800 border-gray-200"
    }
  }

  const getEventIcon = (type: CalendarEvent["type"]) => {
    switch (type) {
      case "shift":
        return <Clock className="h-4 w-4" />
      case "task":
        return <Users className="h-4 w-4" />
      case "training":
        return <BookOpen className="h-4 w-4" />
      default:
        return null
    }
  }

  const handleEventClick = (event: CalendarEvent) => {
    setSelectedEvent(event)
    setShowEventDialog(true)
  }

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      setAttachments((prev) => [...prev, ...Array.from(e.target.files!)])
    }
  }

  const removeAttachment = (index: number) => {
    setAttachments((prev) => prev.filter((_, i) => i !== index))
  }

  const handleSubmitReply = () => {
    if (!selectedEvent || !replyText.trim()) return

    const newReply: TaskReply = {
      id: `reply_${Date.now()}`,
      content: replyText.trim(),
      createdAt: new Date().toISOString(),
      attachments: attachments.map((file) => file.name),
    }

    setEvents((prev) =>
      prev.map((event) =>
        event.id === selectedEvent.id ? { ...event, replies: [...(event.replies || []), newReply] } : event,
      ),
    )

    setReplyText("")
    setAttachments([])

    // Update selected event to show new reply
    setSelectedEvent((prev) =>
      prev
        ? {
            ...prev,
            replies: [...(prev.replies || []), newReply],
          }
        : null,
    )
  }

  const markTaskComplete = () => {
    if (!selectedEvent) return

    setEvents((prev) =>
      prev.map((event) => (event.id === selectedEvent.id ? { ...event, status: "completed" as const } : event)),
    )

    setSelectedEvent((prev) => (prev ? { ...prev, status: "completed" as const } : null))
  }

  const formatDate = (date: Date) => {
    return date.toLocaleDateString("en-US", {
      weekday: "long",
      year: "numeric",
      month: "long",
      day: "numeric",
    })
  }

  const formatReplyDate = (dateString: string) => {
    const date = new Date(dateString)
    return date.toLocaleString("en-US", {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    })
  }

  const navigateDate = (direction: 'prev' | 'next') => {
    const newDate = new Date(selectedDate)
    if (viewMode === 'day') {
      newDate.setDate(newDate.getDate() + (direction === 'next' ? 1 : -1))
    } else if (viewMode === 'week') {
      newDate.setDate(newDate.getDate() + (direction === 'next' ? 7 : -7))
    } else if (viewMode === 'month') {
      newDate.setMonth(newDate.getMonth() + (direction === 'next' ? 1 : -1))
    }
    setSelectedDate(newDate)
  }

  const goToToday = () => {
    setSelectedDate(new Date())
  }

  const getViewTitle = () => {
    const options: Intl.DateTimeFormatOptions = {
      year: 'numeric',
      month: 'long',
    }
    
    if (viewMode === 'day') {
      return selectedDate.toLocaleDateString("en-US", {
        weekday: "long",
        year: "numeric",
        month: "long",
        day: "numeric",
      })
    } else if (viewMode === 'week') {
      const startOfWeek = new Date(selectedDate)
      startOfWeek.setDate(selectedDate.getDate() - selectedDate.getDay())
      const endOfWeek = new Date(startOfWeek)
      endOfWeek.setDate(startOfWeek.getDate() + 6)
      
      return `${startOfWeek.toLocaleDateString("en-US", { month: "short", day: "numeric" })} - ${endOfWeek.toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })}`
    } else {
      return selectedDate.toLocaleDateString("en-US", options)
    }
  }

  // Filter events based on current view and selected date
  const getFilteredEvents = () => {
    const currentDate = new Date(selectedDate)
    
    if (viewMode === 'day') {
      // Show events for the selected day
      const targetDate = currentDate.toISOString().split('T')[0]
      const filtered = events.filter(event => {
        // For training events, check if they fall on the selected day
        if (event.type === 'training') {
          if (!event.date) return false
          // Check if the selected date falls within the training period
          if (event.startDate && event.endDate) {
            const selectedDate = new Date(targetDate)
            const startDate = new Date(event.startDate)
            const endDate = new Date(event.endDate)
            return selectedDate >= startDate && selectedDate <= endDate
          }
          // Fallback to single date check
          return event.date === targetDate
        }
        // For shifts and tasks, show if they're on the selected day
        return event.date === targetDate
      })
      
      
      return filtered
    } else if (viewMode === 'week') {
      // Show events for the selected week
      const startOfWeek = new Date(currentDate)
      startOfWeek.setDate(currentDate.getDate() - currentDate.getDay())
      const endOfWeek = new Date(startOfWeek)
      endOfWeek.setDate(startOfWeek.getDate() + 6)
      
      const filtered = events.filter(event => {
        if (event.type === 'training') {
          // For training events, check if they overlap with the week
          if (!event.date) return false
          if (event.startDate && event.endDate) {
            const trainingStart = new Date(event.startDate)
            const trainingEnd = new Date(event.endDate)
            // Check if training period overlaps with the week
            return trainingStart <= endOfWeek && trainingEnd >= startOfWeek
          }
          // Fallback to single date check
          const eventDate = new Date(event.date)
          return eventDate >= startOfWeek && eventDate <= endOfWeek
        }
        if (!event.date) return false
        
        const eventDate = new Date(event.date)
        return eventDate >= startOfWeek && eventDate <= endOfWeek
      })
      
      
      return filtered
    } else {
      // Month view - show all events for the month
      const monthStart = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1)
      const monthEnd = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 0)
      
      const filtered = events.filter(event => {
        if (event.type === 'training') {
          // For training events, check if they overlap with the month
          if (!event.date) return false
          if (event.startDate && event.endDate) {
            const trainingStart = new Date(event.startDate)
            const trainingEnd = new Date(event.endDate)
            // Check if training period overlaps with the month
            return trainingStart <= monthEnd && trainingEnd >= monthStart
          }
          // Fallback to single date check
          const eventDate = new Date(event.date)
          return eventDate >= monthStart && eventDate <= monthEnd
        }
        if (!event.date) return false
        
        const eventDate = new Date(event.date)
        return eventDate >= monthStart && eventDate <= monthEnd
      })
      
      
      return filtered
    }
  }

  return (
    <div className="space-y-4">
      {/* Enhanced Calendar Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <h3 className="text-lg font-semibold">{getViewTitle()}</h3>
          <div className="flex items-center gap-1">
            <Button
              variant={viewMode === 'day' ? 'default' : 'outline'}
              size="sm"
              onClick={() => setViewMode('day')}
            >
              <CalendarIcon className="h-4 w-4" />
            </Button>
            <Button
              variant={viewMode === 'week' ? 'default' : 'outline'}
              size="sm"
              onClick={() => setViewMode('week')}
            >
              <Grid3X3 className="h-4 w-4" />
            </Button>
            <Button
              variant={viewMode === 'month' ? 'default' : 'outline'}
              size="sm"
              onClick={() => setViewMode('month')}
            >
              <List className="h-4 w-4" />
            </Button>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" onClick={() => navigateDate('prev')}>
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <Button variant="outline" size="sm" onClick={goToToday}>
            Today
          </Button>
          <Button variant="outline" size="sm" onClick={() => navigateDate('next')}>
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {/* Legend */}
      <div className="flex items-center gap-4 text-sm">
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 bg-blue-500 rounded"></div>
          <span>Shifts</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 bg-green-500 rounded"></div>
          <span>Tasks</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 bg-red-500 rounded"></div>
          <span>Training</span>
        </div>
      </div>

      {/* Date Info */}
      <div className="text-sm text-gray-600 mb-4">
        {viewMode === 'day' && `Showing events for ${selectedDate.toLocaleDateString()}`}
        {viewMode === 'week' && `Showing events for week of ${selectedDate.toLocaleDateString()}`}
        {viewMode === 'month' && `Showing events for ${selectedDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })}`}
      </div>

      {/* Events List */}
      <div className="space-y-3">
        {isLoading ? (
          <div className="text-center py-8 text-gray-500">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
            <p>Loading calendar events...</p>
          </div>
        ) : error ? (
          <div className="text-center py-8 text-red-500">
            <CalendarIcon className="h-12 w-12 mx-auto mb-4 opacity-50" />
            <p>{error}</p>
            <Button 
              variant="outline" 
              size="sm" 
              onClick={() => window.location.reload()}
              className="mt-2"
            >
              Try Again
            </Button>
          </div>
        ) : getFilteredEvents().length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            <CalendarIcon className="h-12 w-12 mx-auto mb-4 opacity-50" />
            <p>No events scheduled for this {viewMode}</p>
          </div>
        ) : (
          getFilteredEvents().map((event) => (
          <Card
            key={event.id}
            className={`cursor-pointer transition-all hover:shadow-md ${getEventColor(event.type)} border`}
            onClick={() => handleEventClick(event)}
          >
            <CardContent className="p-4">
              <div className="flex items-start justify-between">
                <div className="flex items-start gap-3">
                  {getEventIcon(event.type)}
                  <div>
                    <h4 className="font-semibold">{event.title}</h4>
                    <p className="text-sm opacity-80">
                      {event.startTime} - {event.endTime}
                    </p>
                    {event.location && <p className="text-sm opacity-70 mt-1">{event.location}</p>}
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  {event.status && (
                    <Badge
                      variant={event.status === "completed" ? "default" : "secondary"}
                      className={`text-xs ${event.status === "completed" ? "bg-green-600" : ""}`}
                    >
                      {event.status}
                    </Badge>
                  )}
                  {event.canReply && <MessageSquare className="h-4 w-4 opacity-60" />}
                  {event.replies && event.replies.length > 0 && (
                    <Badge variant="outline" className="text-xs">
                      {event.replies.length} replies
                    </Badge>
                  )}
                </div>
              </div>
            </CardContent>
          </Card>
          ))
        )}
      </div>

      {/* Enhanced Event Details Dialog */}
      <Dialog open={showEventDialog} onOpenChange={setShowEventDialog}>
        <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              {selectedEvent && getEventIcon(selectedEvent.type)}
              {selectedEvent?.title}
            </DialogTitle>
            <DialogDescription>
              {selectedEvent?.startTime} - {selectedEvent?.endTime}
              {selectedEvent?.location && ` • ${selectedEvent.location}`}
            </DialogDescription>
          </DialogHeader>

          {selectedEvent && (
            <div className="space-y-6">
              {selectedEvent.description && (
                <div>
                  <h4 className="font-semibold text-sm mb-2">Description</h4>
                  <p className="text-sm text-gray-600">{selectedEvent.description}</p>
                </div>
              )}

              {selectedEvent.assignedBy && (
                <div>
                  <h4 className="font-semibold text-sm mb-2">Assigned By</h4>
                  <p className="text-sm text-gray-600">{selectedEvent.assignedBy}</p>
                </div>
              )}

              {selectedEvent.replies && selectedEvent.replies.length > 0 && (
                <div>
                  <h4 className="font-semibold text-sm mb-3">Task Updates</h4>
                  <div className="space-y-3">
                    {selectedEvent.replies.map((reply) => (
                      <div key={reply.id} className="bg-gray-50 p-4 rounded-lg">
                        <div className="flex items-start gap-3">
                          <Avatar className="h-8 w-8">
                            <AvatarFallback className="text-xs">JS</AvatarFallback>
                          </Avatar>
                          <div className="flex-1">
                            <div className="flex items-center gap-2 mb-2">
                              <span className="font-medium text-sm">John Smith</span>
                              <span className="text-xs text-gray-500">{formatReplyDate(reply.createdAt)}</span>
                            </div>
                            <p className="text-sm text-gray-700 mb-2">{reply.content}</p>
                            {reply.attachments && reply.attachments.length > 0 && (
                              <div className="flex flex-wrap gap-2">
                                {reply.attachments.map((attachment, index) => (
                                  <div
                                    key={index}
                                    className="flex items-center gap-1 bg-blue-100 text-blue-800 px-2 py-1 rounded text-xs"
                                  >
                                    <Paperclip className="h-3 w-3" />
                                    {attachment}
                                  </div>
                                ))}
                              </div>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {selectedEvent.canReply && selectedEvent.type === "task" && (
                <div className="border-t pt-4">
                  <div className="flex items-center justify-between mb-3">
                    <h4 className="font-semibold text-sm">Task Update</h4>
                    {selectedEvent.status !== "completed" && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={markTaskComplete}
                        className="flex items-center gap-2 bg-transparent"
                      >
                        <CheckCircle className="h-4 w-4" />
                        Mark Complete
                      </Button>
                    )}
                  </div>

                  <div className="space-y-3">
                    <Textarea
                      placeholder="Provide an update on this task..."
                      value={replyText}
                      onChange={(e) => setReplyText(e.target.value)}
                      rows={3}
                    />

                    {/* File Upload */}
                    <div className="flex items-center gap-2">
                      <Input
                        type="file"
                        multiple
                        accept="image/*,.pdf,.doc,.docx"
                        onChange={handleFileUpload}
                        className="hidden"
                        id="file-upload"
                      />
                      <label htmlFor="file-upload">
                        <Button variant="outline" size="sm" className="flex items-center gap-2 bg-transparent" asChild>
                          <span>
                            <Camera className="h-4 w-4" />
                            Add Photos
                          </span>
                        </Button>
                      </label>
                    </div>

                    {/* Attachment Preview */}
                    {attachments.length > 0 && (
                      <div className="flex flex-wrap gap-2">
                        {attachments.map((file, index) => (
                          <div
                            key={index}
                            className="flex items-center gap-1 bg-blue-100 text-blue-800 px-2 py-1 rounded text-xs"
                          >
                            <Paperclip className="h-3 w-3" />
                            {file.name}
                            <button onClick={() => removeAttachment(index)}>
                              <X className="h-3 w-3 hover:text-red-600" />
                            </button>
                          </div>
                        ))}
                      </div>
                    )}

                    <div className="flex justify-end">
                      <Button
                        onClick={handleSubmitReply}
                        disabled={!replyText.trim()}
                        className="flex items-center gap-2"
                      >
                        <Send className="h-4 w-4" />
                        Send Update
                      </Button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}

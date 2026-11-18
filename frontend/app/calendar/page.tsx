"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Calendar } from "@/components/ui/calendar"
import { Navigation } from "@/components/navigation"
import { CalendarView } from "@/components/calendar/calendar-view"
import { BookingDetails } from "@/components/calendar/booking-details"
import { Filter, Clock, MapPin } from "lucide-react"
import { format, startOfWeek, endOfWeek, eachDayOfInterval } from "date-fns"

interface CalendarBooking {
  id: string
  roomId: string
  roomName: string
  roomFloor: number
  title: string
  description?: string
  startTime: string
  endTime: string
  date: string
  userName: string
  userEmail: string
  attendees: number
  status: "confirmed" | "pending" | "cancelled"
  color: string
}

export default function CalendarPage() {
  const [selectedDate, setSelectedDate] = useState<Date>(new Date())
  const [viewMode, setViewMode] = useState<"day" | "week" | "month">("week")
  const [roomFilter, setRoomFilter] = useState("all")
  const [statusFilter, setStatusFilter] = useState("all")
  const [bookings, setBookings] = useState<CalendarBooking[]>([])
  const [selectedBooking, setSelectedBooking] = useState<CalendarBooking | null>(null)
  const [isDetailsOpen, setIsDetailsOpen] = useState(false)

  // Mock data - replace with API call to Spring Boot backend
  useEffect(() => {
    const mockBookings: CalendarBooking[] = [
      {
        id: "1",
        roomId: "1",
        roomName: "Conference Room A",
        roomFloor: 1,
        title: "Team Standup",
        description: "Daily team synchronization meeting",
        startTime: "09:00",
        endTime: "09:30",
        date: format(new Date(), "yyyy-MM-dd"),
        userName: "John Doe",
        userEmail: "john.doe@company.com",
        attendees: 8,
        status: "confirmed",
        color: "#0891b2",
      },
      {
        id: "2",
        roomId: "2",
        roomName: "Meeting Room B",
        roomFloor: 2,
        title: "Client Presentation",
        description: "Q4 results presentation to key clients",
        startTime: "14:00",
        endTime: "15:30",
        date: format(new Date(), "yyyy-MM-dd"),
        userName: "Jane Smith",
        userEmail: "jane.smith@company.com",
        attendees: 6,
        status: "confirmed",
        color: "#8b5cf6",
      },
      {
        id: "3",
        roomId: "3",
        roomName: "Executive Boardroom",
        roomFloor: 3,
        title: "Board Meeting",
        description: "Monthly board meeting",
        startTime: "10:00",
        endTime: "12:00",
        date: format(new Date(Date.now() + 86400000), "yyyy-MM-dd"), // Tomorrow
        userName: "Mike Johnson",
        userEmail: "mike.johnson@company.com",
        attendees: 12,
        status: "confirmed",
        color: "#f87171",
      },
      {
        id: "4",
        roomId: "4",
        roomName: "Creative Studio",
        roomFloor: 2,
        title: "Design Review",
        description: "Review new product designs",
        startTime: "15:00",
        endTime: "16:00",
        date: format(new Date(Date.now() - 86400000), "yyyy-MM-dd"), // Yesterday
        userName: "Sarah Wilson",
        userEmail: "sarah.wilson@company.com",
        attendees: 4,
        status: "confirmed",
        color: "#4b5563",
      },
      {
        id: "5",
        roomId: "1",
        roomName: "Conference Room A",
        roomFloor: 1,
        title: "Training Session",
        description: "New employee onboarding",
        startTime: "13:00",
        endTime: "17:00",
        date: format(new Date(Date.now() + 172800000), "yyyy-MM-dd"), // Day after tomorrow
        userName: "HR Team",
        userEmail: "hr@company.com",
        attendees: 15,
        status: "pending",
        color: "#0891b2",
      },
    ]
    setBookings(mockBookings)
  }, [])

  const rooms = [
    { id: "1", name: "Conference Room A", floor: 1 },
    { id: "2", name: "Meeting Room B", floor: 2 },
    { id: "3", name: "Executive Boardroom", floor: 3 },
    { id: "4", name: "Creative Studio", floor: 2 },
    { id: "5", name: "Phone Booth 1", floor: 1 },
    { id: "6", name: "Training Room", floor: 1 },
  ]

  const getFilteredBookings = () => {
    return bookings.filter((booking) => {
      const matchesRoom = roomFilter === "all" || booking.roomId === roomFilter
      const matchesStatus = statusFilter === "all" || booking.status === statusFilter
      return matchesRoom && matchesStatus
    })
  }

  const getBookingsForDate = (date: Date) => {
    const dateStr = format(date, "yyyy-MM-dd")
    return getFilteredBookings().filter((booking) => booking.date === dateStr)
  }

  const getBookingsForWeek = (date: Date) => {
    const weekStart = startOfWeek(date, { weekStartsOn: 1 })
    const weekEnd = endOfWeek(date, { weekStartsOn: 1 })
    const weekDays = eachDayOfInterval({ start: weekStart, end: weekEnd })

    return weekDays.map((day) => ({
      date: day,
      bookings: getBookingsForDate(day),
    }))
  }

  const handleBookingClick = (booking: CalendarBooking) => {
    setSelectedBooking(booking)
    setIsDetailsOpen(true)
  }

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "confirmed":
        return <Badge className="bg-green-100 text-green-800">Confirmed</Badge>
      case "pending":
        return <Badge variant="secondary">Pending</Badge>
      case "cancelled":
        return <Badge variant="destructive">Cancelled</Badge>
      default:
        return <Badge variant="outline">{status}</Badge>
    }
  }

  return (
    <div className="min-h-screen bg-background">
      <Navigation />

      <div className="container mx-auto px-4 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-foreground mb-2">Calendar</h1>
          <p className="text-muted-foreground">View and manage room bookings across your organization</p>
        </div>

        <div className="grid lg:grid-cols-4 gap-6">
          {/* Sidebar */}
          <div className="lg:col-span-1 space-y-6">
            {/* Mini Calendar */}
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Calendar</CardTitle>
              </CardHeader>
              <CardContent>
                <Calendar
                  mode="single"
                  selected={selectedDate}
                  onSelect={(date) => date && setSelectedDate(date)}
                  className="rounded-md border-0"
                />
              </CardContent>
            </Card>

            {/* Filters */}
            <Card>
              <CardHeader>
                <CardTitle className="text-lg flex items-center">
                  <Filter className="h-5 w-5 mr-2" />
                  Filters
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Room</label>
                  <Select value={roomFilter} onValueChange={setRoomFilter}>
                    <SelectTrigger>
                      <SelectValue placeholder="All Rooms" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All Rooms</SelectItem>
                      {rooms.map((room) => (
                        <SelectItem key={room.id} value={room.id}>
                          {room.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium">Status</label>
                  <Select value={statusFilter} onValueChange={setStatusFilter}>
                    <SelectTrigger>
                      <SelectValue placeholder="All Status" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All Status</SelectItem>
                      <SelectItem value="confirmed">Confirmed</SelectItem>
                      <SelectItem value="pending">Pending</SelectItem>
                      <SelectItem value="cancelled">Cancelled</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                {(roomFilter !== "all" || statusFilter !== "all") && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      setRoomFilter("all")
                      setStatusFilter("all")
                    }}
                    className="w-full"
                  >
                    Clear Filters
                  </Button>
                )}
              </CardContent>
            </Card>

            {/* Today's Summary */}
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Today&apos;s Summary</CardTitle>
                <CardDescription>{format(new Date(), "EEEE, MMMM dd")}</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {getBookingsForDate(new Date()).map((booking) => (
                    <div
                      key={booking.id}
                      className="p-3 rounded-lg border border-border cursor-pointer hover:bg-muted/50 transition-colors"
                      onClick={() => handleBookingClick(booking)}
                    >
                      <div className="flex items-center justify-between mb-1">
                        <span className="font-medium text-sm">{booking.title}</span>
                        {getStatusBadge(booking.status)}
                      </div>
                      <div className="text-xs text-muted-foreground space-y-1">
                        <div className="flex items-center">
                          <Clock className="h-3 w-3 mr-1" />
                          {booking.startTime} - {booking.endTime}
                        </div>
                        <div className="flex items-center">
                          <MapPin className="h-3 w-3 mr-1" />
                          {booking.roomName}
                        </div>
                      </div>
                    </div>
                  ))}
                  {getBookingsForDate(new Date()).length === 0 && (
                    <p className="text-sm text-muted-foreground">No bookings today</p>
                  )}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Main Calendar View */}
          <div className="lg:col-span-3">
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <div>
                    <CardTitle className="text-xl">
                      {viewMode === "day" && format(selectedDate, "EEEE, MMMM dd, yyyy")}
                      {viewMode === "week" &&
                        `Week of ${format(startOfWeek(selectedDate, { weekStartsOn: 1 }), "MMMM dd, yyyy")}`}
                      {viewMode === "month" && format(selectedDate, "MMMM yyyy")}
                    </CardTitle>
                    <CardDescription>Room booking schedule</CardDescription>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Select value={viewMode} onValueChange={(value: "day" | "week" | "month") => setViewMode(value)}>
                      <SelectTrigger className="w-[100px]">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="day">Day</SelectItem>
                        <SelectItem value="week">Week</SelectItem>
                        <SelectItem value="month">Month</SelectItem>
                      </SelectContent>
                    </Select>
                    <div className="flex space-x-1">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => {
                          const newDate = new Date(selectedDate)
                          if (viewMode === "day") newDate.setDate(newDate.getDate() - 1)
                          else if (viewMode === "week") newDate.setDate(newDate.getDate() - 7)
                          else newDate.setMonth(newDate.getMonth() - 1)
                          setSelectedDate(newDate)
                        }}
                      >
                        ←
                      </Button>
                      <Button variant="outline" size="sm" onClick={() => setSelectedDate(new Date())}>
                        Today
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => {
                          const newDate = new Date(selectedDate)
                          if (viewMode === "day") newDate.setDate(newDate.getDate() + 1)
                          else if (viewMode === "week") newDate.setDate(newDate.getDate() + 7)
                          else newDate.setMonth(newDate.getMonth() + 1)
                          setSelectedDate(newDate)
                        }}
                      >
                        →
                      </Button>
                    </div>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <CalendarView
                  viewMode={viewMode}
                  selectedDate={selectedDate}
                  bookings={getFilteredBookings()}
                  onBookingClick={handleBookingClick}
                />
              </CardContent>
            </Card>
          </div>
        </div>
      </div>

      {/* Booking Details Modal */}
      <BookingDetails
        booking={selectedBooking}
        isOpen={isDetailsOpen}
        onClose={() => {
          setIsDetailsOpen(false)
          setSelectedBooking(null)
        }}
      />
    </div>
  )
}

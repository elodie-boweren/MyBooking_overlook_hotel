"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { toast } from "sonner"
import { 
  Plus, 
  Search, 
  Edit, 
  Trash2, 
  Calendar, 
  Clock, 
  Users, 
  DollarSign,
  MapPin,
  RefreshCw,
  ArrowLeft
} from "lucide-react"

// Event type images mapping
const getEventTypeImage = (eventType: string) => {
  const imageMap: { [key: string]: string } = {
    SPA: "https://images.pexels.com/photos/3757942/pexels-photo-3757942.jpeg?auto=compress&cs=tinysrgb&w=400&h=300&fit=crop",
    CONFERENCE: "https://images.pexels.com/photos/1181406/pexels-photo-1181406.jpeg?auto=compress&cs=tinysrgb&w=400&h=300&fit=crop",
    YOGA_CLASS: "https://images.pexels.com/photos/1812964/pexels-photo-1812964.jpeg?auto=compress&cs=tinysrgb&w=400&h=300&fit=crop",
    FITNESS: "https://images.pexels.com/photos/1552242/pexels-photo-1552242.jpeg?auto=compress&cs=tinysrgb&w=400&h=300&fit=crop",
    WEDDING: "https://images.pexels.com/photos/2253870/pexels-photo-2253870.jpeg?auto=compress&cs=tinysrgb&w=400&h=300&fit=crop"
  }
  return imageMap[eventType] || "https://images.pexels.com/photos/1181406/pexels-photo-1181406.jpeg?auto=compress&cs=tinysrgb&w=400&h=300&fit=crop"
}
import { adminEventApi, installationApi } from "@/lib/api"
import type { Event, CreateEventRequest, UpdateEventRequest, Installation } from "@/lib/api"

export default function AdminEventManagement() {
  const [events, setEvents] = useState<Event[]>([])
  const [installations, setInstallations] = useState<Installation[]>([])
  const [loading, setLoading] = useState(false)
  const [searchTerm, setSearchTerm] = useState("")
  const [eventTypeFilter, setEventTypeFilter] = useState<string>("all")
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false)
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false)
  const [selectedEvent, setSelectedEvent] = useState<Event | null>(null)

  // Form state for create/edit
  const [formData, setFormData] = useState<CreateEventRequest>({
    name: "",
    description: "",
    eventType: "SPA",
    startAt: "",
    endAt: "",
    capacity: 1,
    price: 0,
    currency: "EUR",
    installationId: 0
  })

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    setLoading(true)
    try {
      const [eventsResponse, installationsResponse] = await Promise.all([
        adminEventApi.getAllEvents(),
        installationApi.getAllInstallations()
      ])
      
      setEvents(eventsResponse.content || [])
      setInstallations(installationsResponse || [])
    } catch (error) {
      console.error("Error loading data:", error)
      toast.error("Failed to load events and installations")
    } finally {
      setLoading(false)
    }
  }

  const handleCreateEvent = async () => {
    try {
      if (!formData.name || !formData.startAt || !formData.endAt || !formData.installationId) {
        toast.error("Please fill in all required fields")
        return
      }

      await adminEventApi.createEvent(formData)
      toast.success("Event created successfully")
      setIsCreateDialogOpen(false)
      resetForm()
      loadData()
    } catch (error: any) {
      console.error("Error creating event:", error)
      toast.error("Failed to create event")
    }
  }

  const handleUpdateEvent = async () => {
    if (!selectedEvent) return

    try {
      const updateData: UpdateEventRequest = {
        name: formData.name,
        description: formData.description,
        eventType: formData.eventType,
        startAt: formData.startAt,
        endAt: formData.endAt,
        capacity: formData.capacity,
        price: formData.price,
        currency: formData.currency,
        installationId: formData.installationId
      }

      await adminEventApi.updateEvent(selectedEvent.id, updateData)
      toast.success("Event updated successfully")
      setIsEditDialogOpen(false)
      setSelectedEvent(null)
      resetForm()
      loadData()
    } catch (error: any) {
      console.error("Error updating event:", error)
      toast.error("Failed to update event")
    }
  }

  const handleDeleteEvent = async (eventId: number) => {
    if (!confirm("Are you sure you want to delete this event?")) return

    try {
      await adminEventApi.deleteEvent(eventId)
      toast.success("Event deleted successfully")
      loadData()
    } catch (error: any) {
      console.error("Error deleting event:", error)
      toast.error("Failed to delete event")
    }
  }

  const handleEditEvent = (event: Event) => {
    setSelectedEvent(event)
    setFormData({
      name: event.name || event.title || "",
      description: event.description || "",
      eventType: (event.eventType || "SPA") as any,
      startAt: event.startAt || "",
      endAt: event.endAt || "",
      capacity: event.capacity ?? event.maxCapacity ?? 1,
      price: event.price,
      currency: event.currency,
      installationId: event.installationId ?? 0
    })
    setIsEditDialogOpen(true)
  }

  const resetForm = () => {
    setFormData({
      name: "",
      description: "",
      eventType: "SPA",
      startAt: "",
      endAt: "",
      capacity: 1,
      price: 0,
      currency: "EUR",
      installationId: 0
    })
  }

  const filteredEvents = events.filter(event => {
    const matchesSearch = (event.name || event.title || "").toLowerCase().includes(searchTerm.toLowerCase()) ||
                         event.description?.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesType = eventTypeFilter === "all" || event.eventType === eventTypeFilter
    return matchesSearch && matchesType
  })

  const getEventTypeBadge = (eventType: string) => {
    const colors = {
      SPA: "bg-pink-50 text-pink-700 border-pink-200",
      CONFERENCE: "bg-blue-50 text-blue-700 border-blue-200",
      YOGA_CLASS: "bg-green-50 text-green-700 border-green-200",
      FITNESS: "bg-orange-50 text-orange-700 border-orange-200",
      WEDDING: "bg-purple-50 text-purple-700 border-purple-200"
    }
    return colors[eventType as keyof typeof colors] || "bg-gray-50 text-gray-700 border-gray-200"
  }

  const formatDateTime = (dateTime: string) => {
    return new Date(dateTime).toLocaleString()
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <Button
            variant="ghost"
            onClick={() => window.history.back()}
            className="mb-4"
          >
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Dashboard
          </Button>
          <h1 className="text-3xl font-bold">Event Management</h1>
          <p className="text-muted-foreground">Manage hotel events and installations</p>
        </div>
        <Button onClick={loadData} variant="outline" size="sm">
          <RefreshCw className="h-4 w-4 mr-2" />
          Refresh Data
        </Button>
      </div>

      {/* Search and Filters */}
      <Card>
        <CardHeader>
          <CardTitle>Events</CardTitle>
          <CardDescription>Manage and review hotel events</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col sm:flex-row gap-4 mb-6">
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground h-4 w-4" />
                <Input
                  placeholder="Search by event name or description..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>
            <div className="w-full sm:w-48">
              <select
                value={eventTypeFilter}
                onChange={(e) => setEventTypeFilter(e.target.value)}
                className="w-full px-3 py-2 border border-input bg-background rounded-md"
              >
                <option value="all">All Types</option>
                <option value="SPA">SPA</option>
                <option value="CONFERENCE">Conference</option>
                <option value="YOGA_CLASS">Yoga Class</option>
                <option value="FITNESS">Fitness</option>
                <option value="WEDDING">Wedding</option>
              </select>
            </div>
            <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
              <DialogTrigger asChild>
                <Button>
                  <Plus className="h-4 w-4 mr-2" />
                  Create Event
                </Button>
              </DialogTrigger>
              <DialogContent className="max-w-2xl">
                <DialogHeader>
                  <DialogTitle>Create New Event</DialogTitle>
                  <DialogDescription>Add a new event to the hotel</DialogDescription>
                </DialogHeader>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="name">Event Name *</Label>
                    <Input
                      id="name"
                      value={formData.name}
                      onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                      placeholder="Enter event name"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="eventType">Event Type *</Label>
                    <select
                      id="eventType"
                      value={formData.eventType}
                      onChange={(e) => setFormData({ ...formData, eventType: e.target.value as any })}
                      className="w-full px-3 py-2 border border-input bg-background rounded-md"
                    >
                      <option value="SPA">SPA</option>
                      <option value="CONFERENCE">Conference</option>
                      <option value="YOGA_CLASS">Yoga Class</option>
                      <option value="FITNESS">Fitness</option>
                      <option value="WEDDING">Wedding</option>
                    </select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="startAt">Start Date & Time *</Label>
                    <Input
                      id="startAt"
                      type="datetime-local"
                      value={formData.startAt}
                      onChange={(e) => setFormData({ ...formData, startAt: e.target.value })}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="endAt">End Date & Time *</Label>
                    <Input
                      id="endAt"
                      type="datetime-local"
                      value={formData.endAt}
                      onChange={(e) => setFormData({ ...formData, endAt: e.target.value })}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="capacity">Capacity *</Label>
                    <Input
                      id="capacity"
                      type="number"
                      min="1"
                      max="100"
                      value={formData.capacity}
                      onChange={(e) => setFormData({ ...formData, capacity: parseInt(e.target.value) || 1 })}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="price">Price *</Label>
                    <Input
                      id="price"
                      type="number"
                      step="0.01"
                      min="0"
                      value={formData.price}
                      onChange={(e) => setFormData({ ...formData, price: parseFloat(e.target.value) || 0 })}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="currency">Currency *</Label>
                    <select
                      id="currency"
                      value={formData.currency}
                      onChange={(e) => setFormData({ ...formData, currency: e.target.value })}
                      className="w-full px-3 py-2 border border-input bg-background rounded-md"
                    >
                      <option value="EUR">EUR</option>
                      <option value="USD">USD</option>
                      <option value="GBP">GBP</option>
                    </select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="installationId">Installation *</Label>
                    <select
                      id="installationId"
                      value={formData.installationId.toString()}
                      onChange={(e) => setFormData({ ...formData, installationId: parseInt(e.target.value) })}
                      className="w-full px-3 py-2 border border-input bg-background rounded-md"
                    >
                      <option value="0">Select installation</option>
                      {installations.map((installation) => (
                        <option key={installation.id} value={installation.id.toString()}>
                          {installation.name} ({installation.installationType})
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="description">Description</Label>
                  <Textarea
                    id="description"
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    placeholder="Enter event description"
                    rows={3}
                  />
                </div>
                <div className="flex justify-end space-x-2">
                  <Button variant="outline" onClick={() => setIsCreateDialogOpen(false)}>
                    Cancel
                  </Button>
                  <Button onClick={handleCreateEvent}>
                    Create Event
                  </Button>
                </div>
              </DialogContent>
            </Dialog>
          </div>

          {/* Events List */}
          {loading ? (
            <div className="text-center py-8">Loading events...</div>
          ) : filteredEvents.length === 0 ? (
            <div className="text-center py-8">
              <Calendar className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
              <h3 className="text-lg font-semibold">No events found</h3>
              <p className="text-muted-foreground">No events match your search criteria.</p>
            </div>
          ) : (
            <div className="space-y-4">
              {filteredEvents.map((event) => (
                <Card key={event.id} className="overflow-hidden">
                  <div className="flex">
                    {/* Event Image */}
                    <div className="w-48 h-32 flex-shrink-0">
                      <img
                        src={getEventTypeImage(event.eventType)}
                        alt={`${event.eventType} event`}
                        className="w-full h-full object-cover"
                        onError={(e) => {
                          const target = e.target as HTMLImageElement;
                          target.src = "https://images.pexels.com/photos/1181406/pexels-photo-1181406.jpeg?auto=compress&cs=tinysrgb&w=400&h=300&fit=crop";
                        }}
                      />
                    </div>
                    
                    {/* Event Details */}
                    <CardContent className="p-6 flex-1">
                      <div className="flex items-start justify-between">
                        <div className="space-y-2">
                          <div className="flex items-center gap-2">
                            <h3 className="text-lg font-semibold">{event.name}</h3>
                            <Badge className={getEventTypeBadge(event.eventType)}>
                              {event.eventType}
                            </Badge>
                          </div>
                          {event.description && (
                            <p className="text-muted-foreground">{event.description}</p>
                          )}
                          <div className="flex items-center gap-4 text-sm text-muted-foreground">
                            <div className="flex items-center gap-1">
                              <Clock className="h-4 w-4" />
                              {formatDateTime(event.startAt || "")} - {formatDateTime(event.endAt || "")}
                            </div>
                            <div className="flex items-center gap-1">
                              <Users className="h-4 w-4" />
                              {event.capacity} people
                            </div>
                            <div className="flex items-center gap-1">
                              <DollarSign className="h-4 w-4" />
                              {event.price} {event.currency}
                            </div>
                            <div className="flex items-center gap-1">
                              <MapPin className="h-4 w-4" />
                              {event.installationName}
                            </div>
                          </div>
                        </div>
                        <div className="flex items-center gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleEditEvent(event)}
                          >
                            <Edit className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleDeleteEvent(event.id)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </div>
                    </CardContent>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Edit Dialog */}
      <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Edit Event</DialogTitle>
            <DialogDescription>Update event information</DialogDescription>
          </DialogHeader>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="edit-name">Event Name *</Label>
              <Input
                id="edit-name"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="Enter event name"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="edit-eventType">Event Type *</Label>
              <select
                id="edit-eventType"
                value={formData.eventType}
                onChange={(e) => setFormData({ ...formData, eventType: e.target.value as any })}
                className="w-full px-3 py-2 border border-input bg-background rounded-md"
              >
                <option value="SPA">SPA</option>
                <option value="CONFERENCE">Conference</option>
                <option value="YOGA_CLASS">Yoga Class</option>
                <option value="FITNESS">Fitness</option>
                <option value="WEDDING">Wedding</option>
              </select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="edit-startAt">Start Date & Time *</Label>
              <Input
                id="edit-startAt"
                type="datetime-local"
                value={formData.startAt}
                onChange={(e) => setFormData({ ...formData, startAt: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="edit-endAt">End Date & Time *</Label>
              <Input
                id="edit-endAt"
                type="datetime-local"
                value={formData.endAt}
                onChange={(e) => setFormData({ ...formData, endAt: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="edit-capacity">Capacity *</Label>
              <Input
                id="edit-capacity"
                type="number"
                min="1"
                max="100"
                value={formData.capacity}
                onChange={(e) => setFormData({ ...formData, capacity: parseInt(e.target.value) || 1 })}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="edit-price">Price *</Label>
              <Input
                id="edit-price"
                type="number"
                step="0.01"
                min="0"
                value={formData.price}
                onChange={(e) => setFormData({ ...formData, price: parseFloat(e.target.value) || 0 })}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="edit-currency">Currency *</Label>
              <select
                id="edit-currency"
                value={formData.currency}
                onChange={(e) => setFormData({ ...formData, currency: e.target.value })}
                className="w-full px-3 py-2 border border-input bg-background rounded-md"
              >
                <option value="EUR">EUR</option>
                <option value="USD">USD</option>
                <option value="GBP">GBP</option>
              </select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="edit-installationId">Installation *</Label>
              <select
                id="edit-installationId"
                value={formData.installationId.toString()}
                onChange={(e) => setFormData({ ...formData, installationId: parseInt(e.target.value) })}
                className="w-full px-3 py-2 border border-input bg-background rounded-md"
              >
                <option value="0">Select installation</option>
                {installations.map((installation) => (
                  <option key={installation.id} value={installation.id.toString()}>
                    {installation.name} ({installation.installationType})
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="space-y-2">
            <Label htmlFor="edit-description">Description</Label>
            <Textarea
              id="edit-description"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              placeholder="Enter event description"
              rows={3}
            />
          </div>
          <div className="flex justify-end space-x-2">
            <Button variant="outline" onClick={() => setIsEditDialogOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleUpdateEvent}>
              Update Event
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}

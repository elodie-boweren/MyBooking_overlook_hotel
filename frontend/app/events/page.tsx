"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { 
  PartyPopper, 
  CalendarIcon, 
  Users, 
  MapPin, 
  Clock, 
  Search, 
  Filter,
  DollarSign,
  RefreshCw
} from "lucide-react"
import { eventApi, installationApi } from "@/lib/api"
import type { Event, EventSearchCriteria, Installation } from "@/lib/api"
import { toast } from "sonner"
import { EventBookingModal } from "@/components/event-booking-modal"

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

const getEventTypeBadge = (eventType: string) => {
  const badgeMap: { [key: string]: string } = {
    SPA: "bg-pink-100 text-pink-800 border-pink-200",
    CONFERENCE: "bg-blue-100 text-blue-800 border-blue-200",
    YOGA_CLASS: "bg-green-100 text-green-800 border-green-200",
    FITNESS: "bg-orange-100 text-orange-800 border-orange-200",
    WEDDING: "bg-purple-100 text-purple-800 border-purple-200"
  }
  return badgeMap[eventType] || "bg-gray-100 text-gray-800 border-gray-200"
}

const formatDateTime = (dateTime: string) => {
  return new Date(dateTime).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

export default function EventsPage() {
  const [events, setEvents] = useState<Event[]>([])
  const [installations, setInstallations] = useState<Installation[]>([])
  const [loading, setLoading] = useState(false)
  const [searchTerm, setSearchTerm] = useState("")
  const [eventTypeFilter, setEventTypeFilter] = useState<string>("all")
  const [installationFilter, setInstallationFilter] = useState<string>("all")
  const [priceRange, setPriceRange] = useState<string>("all")

  // Search criteria
  const [searchCriteria, setSearchCriteria] = useState<EventSearchCriteria>({
    page: 0,
    size: 20
  })
  
  // Booking modal state
  const [selectedEvent, setSelectedEvent] = useState<Event | null>(null)
  const [isBookingModalOpen, setIsBookingModalOpen] = useState(false)

  // Load events and installations
  useEffect(() => {
    loadEvents()
    loadInstallations()
  }, [])

  // Load events when search criteria change
  useEffect(() => {
    loadEvents()
  }, [searchCriteria])

  const loadEvents = async () => {
    setLoading(true)
    try {
      const response = await eventApi.searchEvents(searchCriteria)
      setEvents(response.content)
    } catch (error) {
      console.error('Failed to load events:', error)
      toast.error('Failed to load events')
    } finally {
      setLoading(false)
    }
  }

  const loadInstallations = async () => {
    try {
      const response = await installationApi.getAllInstallations()
      setInstallations(response)
    } catch (error) {
      console.error('Failed to load installations:', error)
    }
  }

  const handleSearch = () => {
    const criteria: EventSearchCriteria = {
      name: searchTerm || undefined,
      eventType: eventTypeFilter !== "all" ? eventTypeFilter as any : undefined,
      installationId: installationFilter !== "all" ? parseInt(installationFilter) : undefined,
      minPrice: priceRange === "low" ? 0 : priceRange === "medium" ? 50 : priceRange === "high" ? 100 : undefined,
      maxPrice: priceRange === "low" ? 50 : priceRange === "medium" ? 100 : undefined,
      page: 0,
      size: 20
    }
    setSearchCriteria(criteria)
  }

  const handleClearFilters = () => {
    setSearchTerm("")
    setEventTypeFilter("all")
    setInstallationFilter("all")
    setPriceRange("all")
    setSearchCriteria({ page: 0, size: 20 })
  }

  const handleBookEvent = (event: Event) => {
    setSelectedEvent(event)
    setIsBookingModalOpen(true)
  }

  const handleBookingSuccess = () => {
    // Refresh events after successful booking
    loadEvents()
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold text-foreground mb-2">Hotel Events</h1>
            <p className="text-muted-foreground">Discover and book exciting events at our hotel</p>
          </div>
          <Button onClick={loadEvents} variant="outline" size="sm">
            <RefreshCw className="h-4 w-4 mr-2" />
            Refresh
          </Button>
        </div>

        {/* Search and Filters */}
        <div className="bg-card rounded-lg border p-6 mb-8">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
            {/* Search */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground h-4 w-4" />
              <Input
                placeholder="Search events..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>

            {/* Event Type Filter */}
            <Select value={eventTypeFilter} onValueChange={setEventTypeFilter}>
              <SelectTrigger>
                <SelectValue placeholder="Event Type" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Types</SelectItem>
                <SelectItem value="SPA">SPA</SelectItem>
                <SelectItem value="CONFERENCE">Conference</SelectItem>
                <SelectItem value="YOGA_CLASS">Yoga Class</SelectItem>
                <SelectItem value="FITNESS">Fitness</SelectItem>
                <SelectItem value="WEDDING">Wedding</SelectItem>
              </SelectContent>
            </Select>

            {/* Installation Filter */}
            <Select value={installationFilter} onValueChange={setInstallationFilter}>
              <SelectTrigger>
                <SelectValue placeholder="Location" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Locations</SelectItem>
                {installations.map((installation) => (
                  <SelectItem key={installation.id} value={installation.id.toString()}>
                    {installation.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            {/* Price Range Filter */}
            <Select value={priceRange} onValueChange={setPriceRange}>
              <SelectTrigger>
                <SelectValue placeholder="Price Range" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Prices</SelectItem>
                <SelectItem value="low">€0 - €50</SelectItem>
                <SelectItem value="medium">€50 - €100</SelectItem>
                <SelectItem value="high">€100+</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="flex gap-2">
            <Button onClick={handleSearch} className="flex items-center">
              <Filter className="h-4 w-4 mr-2" />
              Search
            </Button>
            <Button onClick={handleClearFilters} variant="outline">
              Clear Filters
            </Button>
          </div>
        </div>

        {/* Events Grid */}
        {loading ? (
          <div className="text-center py-8">
            <RefreshCw className="h-8 w-8 animate-spin mx-auto mb-4" />
            <p>Loading events...</p>
          </div>
        ) : events.length === 0 ? (
          <div className="text-center py-12">
            <PartyPopper className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
            <h3 className="text-lg font-semibold mb-2">No events found</h3>
            <p className="text-muted-foreground mb-4">No events match your search criteria.</p>
            <Button onClick={handleClearFilters} variant="outline">
              Clear Filters
            </Button>
          </div>
        ) : (
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
            {events.map((event) => (
              <Card key={event.id} className="overflow-hidden hover:shadow-xl transition-all duration-300 hover:scale-105">
                {/* Large Event Image */}
                <div className="relative h-48 w-full">
                  <img
                    src={getEventTypeImage(event.eventType)}
                    alt={`${event.eventType} event`}
                    className="w-full h-full object-cover"
                    onError={(e) => {
                      const target = e.target as HTMLImageElement;
                      target.src = "https://images.pexels.com/photos/1181406/pexels-photo-1181406.jpeg?auto=compress&cs=tinysrgb&w=600&h=400&fit=crop";
                    }}
                  />
                  {/* Event Type Badge Overlay */}
                  <div className="absolute top-4 right-4">
                    <Badge className={`${getEventTypeBadge(event.eventType)} shadow-lg`}>
                      {event.eventType}
                    </Badge>
                  </div>
                </div>

                {/* Event Details */}
                <CardContent className="p-6">
                  <div className="space-y-4">
                    <div>
                      <h3 className="text-xl font-bold text-foreground mb-2">{event.name}</h3>
                      {event.description && (
                        <p className="text-muted-foreground line-clamp-2">
                          {event.description}
                        </p>
                      )}
                    </div>

                    <div className="space-y-3 text-sm">
                      <div className="flex items-center gap-3 text-muted-foreground">
                        <Clock className="h-4 w-4 text-primary" />
                        <span>{formatDateTime(event.startAt || "")}</span>
                      </div>
                      <div className="flex items-center gap-3 text-muted-foreground">
                        <Users className="h-4 w-4 text-primary" />
                        <span>{event.capacity} people capacity</span>
                      </div>
                      <div className="flex items-center gap-3 text-muted-foreground">
                        <MapPin className="h-4 w-4 text-primary" />
                        <span>{event.installationName}</span>
                      </div>
                      <div className="flex items-center justify-between pt-2 border-t border-border">
                        <div className="flex items-center gap-2">
                          <DollarSign className="h-4 w-4 text-primary" />
                          <span className="text-muted-foreground">Price</span>
                        </div>
                        <span className="font-semibold text-lg text-foreground">{event.price} {event.currency}</span>
                      </div>
                    </div>

                    <Button 
                      onClick={() => handleBookEvent(event)}
                      className="w-full bg-primary hover:bg-primary/90 text-white font-semibold py-2"
                      size="lg"
                    >
                      Book Event
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* Event Booking Modal */}
      {selectedEvent && (
        <EventBookingModal
          isOpen={isBookingModalOpen}
          onClose={() => {
            setIsBookingModalOpen(false)
            setSelectedEvent(null)
          }}
          event={selectedEvent as any}
          onBookingSuccess={handleBookingSuccess}
        />
      )}
    </div>
  )
}
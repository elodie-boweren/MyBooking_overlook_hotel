import Link from "next/link"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { CalendarDays, Users, Shield } from "lucide-react"

export default function HomePage() {
  return (
    <div className="min-h-screen bg-background">

      {/* Hero Section */}
      <section className="py-20 px-4">
        <div className="container mx-auto text-center">
          <h2 className="text-4xl md:text-6xl font-bold text-foreground mb-6 text-balance">
            Streamline Your Hotel Management
          </h2>
          <p className="text-xl text-muted-foreground mb-8 max-w-2xl mx-auto text-pretty">
            Effortlessly book hotelrooms, manage schedules, and optimize your hotel processes with our comprehensive hotel management system.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link href="/register">
              <Button size="lg" className="w-full sm:w-auto">
                Start Booking Rooms
              </Button>
            </Link>
            <Link href="/login">
              <Button variant="outline" size="lg" className="w-full sm:w-auto bg-transparent">
                View available rooms now
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-16 px-4 bg-muted/50">
        <div className="container mx-auto">
          <h3 className="text-3xl font-bold text-center text-foreground mb-12">
            Everything You Need for Hotel Management
          </h3>
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
            <Card>
              <CardHeader>
                <CalendarDays className="h-12 w-12 text-primary mb-4" />
                <CardTitle>Easy Booking</CardTitle>
                <CardDescription>Book rooms instantly with our intuitive interface</CardDescription>
              </CardHeader>
              <CardContent>
                <p className="text-muted-foreground">
                  Select your preferred time slot, room capacity, and amenities with just a few clicks.
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <Users className="h-12 w-12 text-primary mb-4" />
                <CardTitle>Team Management</CardTitle>
                <CardDescription>Manage your team and their schedules</CardDescription>
              </CardHeader>
              <CardContent>
                <p className="text-muted-foreground">
                  Control who can book rooms, set approval workflows, and assign rapid tasks.
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <Shield className="h-12 w-12 text-primary mb-4" />
                <CardTitle>Admin Dashboard</CardTitle>
                <CardDescription>Comprehensive analytics and room utilization insights</CardDescription>
              </CardHeader>
              <CardContent>
                <p className="text-muted-foreground">
                  Track usage patterns, generate reports, and optimize your hotel space allocation.
                </p>
              </CardContent>
            </Card>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-border bg-card py-8 px-4">
        <div className="container mx-auto text-center">
          <p className="text-muted-foreground">© 2025 MyBooking. Built for modern hotel management.</p>
        </div>
      </footer>
    </div>
  )
}

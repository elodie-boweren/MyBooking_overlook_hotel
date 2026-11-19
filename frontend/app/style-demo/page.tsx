"use client"

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { 
  StatusBadge, 
  RoomStatusBadge, 
  ReservationStatusBadge, 
  TaskStatusBadge,
  LeaveRequestStatusBadge,
  TrainingStatusBadge,
  EmployeeStatusBadge,
  PriorityBadge
} from '@/components/ui/status-badge'
import { 
  STATUS_STYLES, 
  BADGE_VARIANTS, 
  COMPONENT_TEMPLATES, 
  UTILITY_CLASSES,
  BUSINESS_EFFECTS,
  CUSTOM_CLASSES
} from '@/lib/style-constants'

export default function StyleDemoPage() {
  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto px-4 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-foreground mb-2">Style Constants Demo</h1>
          <p className="text-muted-foreground">
            Demonstrating the corrected status badges that match your actual frontend colors
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Room Status Badges */}
          <Card>
            <CardHeader>
              <CardTitle>Room Status Badges</CardTitle>
              <p className="text-sm text-muted-foreground">
                These match your frontend exactly: Blue marine for Available, Purple for Occupied
              </p>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <h4 className="font-medium">Using StatusBadge Component:</h4>
                <div className="flex gap-2 flex-wrap">
                  <StatusBadge entity="room" status="AVAILABLE">Available</StatusBadge>
                  <StatusBadge entity="room" status="OCCUPIED">Occupied</StatusBadge>
                  <StatusBadge entity="room" status="OUT_OF_SERVICE">Out of Service</StatusBadge>
                </div>
              </div>
              
              <div className="space-y-2">
                <h4 className="font-medium">Using Convenience Component:</h4>
                <div className="flex gap-2 flex-wrap">
                  <RoomStatusBadge status="AVAILABLE">Available</RoomStatusBadge>
                  <RoomStatusBadge status="OCCUPIED">Occupied</RoomStatusBadge>
                  <RoomStatusBadge status="OUT_OF_SERVICE">Out of Service</RoomStatusBadge>
                </div>
              </div>

              <div className="space-y-2">
                <h4 className="font-medium">Direct Badge Usage (Your Current Method):</h4>
                <div className="flex gap-2 flex-wrap">
                  <Badge variant="default">Available</Badge>
                  <Badge variant="secondary">Occupied</Badge>
                  <Badge variant="outline">Out of Service</Badge>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Reservation Status Badges */}
          <Card>
            <CardHeader>
              <CardTitle>Reservation Status Badges</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex gap-2 flex-wrap">
                <ReservationStatusBadge status="CONFIRMED">Confirmed</ReservationStatusBadge>
                <ReservationStatusBadge status="CANCELLED">Cancelled</ReservationStatusBadge>
              </div>
            </CardContent>
          </Card>

          {/* Task Status Badges */}
          <Card>
            <CardHeader>
              <CardTitle>Task Status Badges</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex gap-2 flex-wrap">
                <TaskStatusBadge status="TODO">To Do</TaskStatusBadge>
                <TaskStatusBadge status="IN_PROGRESS">In Progress</TaskStatusBadge>
                <TaskStatusBadge status="DONE">Done</TaskStatusBadge>
              </div>
            </CardContent>
          </Card>

          {/* Leave Request Status Badges */}
          <Card>
            <CardHeader>
              <CardTitle>Leave Request Status Badges</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex gap-2 flex-wrap">
                <LeaveRequestStatusBadge status="PENDING">Pending</LeaveRequestStatusBadge>
                <LeaveRequestStatusBadge status="APPROVED">Approved</LeaveRequestStatusBadge>
                <LeaveRequestStatusBadge status="REJECTED">Rejected</LeaveRequestStatusBadge>
              </div>
            </CardContent>
          </Card>

          {/* Training Status Badges */}
          <Card>
            <CardHeader>
              <CardTitle>Training Status Badges</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex gap-2 flex-wrap">
                <TrainingStatusBadge status="SCHEDULED">Scheduled</TrainingStatusBadge>
                <TrainingStatusBadge status="IN_PROGRESS">In Progress</TrainingStatusBadge>
                <TrainingStatusBadge status="COMPLETED">Completed</TrainingStatusBadge>
                <TrainingStatusBadge status="CANCELLED">Cancelled</TrainingStatusBadge>
              </div>
            </CardContent>
          </Card>

          {/* Employee Status Badges */}
          <Card>
            <CardHeader>
              <CardTitle>Employee Status Badges</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex gap-2 flex-wrap">
                <EmployeeStatusBadge status="ACTIVE">Active</EmployeeStatusBadge>
                <EmployeeStatusBadge status="INACTIVE">Inactive</EmployeeStatusBadge>
              </div>
            </CardContent>
          </Card>

          {/* Priority Badges */}
          <Card>
            <CardHeader>
              <CardTitle>Priority Badges</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex gap-2 flex-wrap">
                <PriorityBadge priority="LOW">Low Priority</PriorityBadge>
                <PriorityBadge priority="MEDIUM">Medium Priority</PriorityBadge>
                <PriorityBadge priority="HIGH">High Priority</PriorityBadge>
              </div>
            </CardContent>
          </Card>

          {/* Component Templates Demo */}
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Component Templates</CardTitle>
              <p className="text-sm text-muted-foreground">
                Exact styling patterns from your frontend
              </p>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-4">
                  <h4 className="font-medium">Page Layouts</h4>
                  <div className="space-y-2 text-sm">
                    <div><code className="bg-muted px-2 py-1 rounded">{COMPONENT_TEMPLATES.pageContainer}</code></div>
                    <div><code className="bg-muted px-2 py-1 rounded">{COMPONENT_TEMPLATES.pageContent}</code></div>
                    <div><code className="bg-muted px-2 py-1 rounded">{COMPONENT_TEMPLATES.pageTitle}</code></div>
                  </div>
                  
                  <h4 className="font-medium">Card Styling</h4>
                  <div className="space-y-2 text-sm">
                    <div><code className="bg-muted px-2 py-1 rounded">{COMPONENT_TEMPLATES.cardHover}</code></div>
                    <div><code className="bg-muted px-2 py-1 rounded">{COMPONENT_TEMPLATES.cardHeader}</code></div>
                  </div>
                </div>
                
                <div className="space-y-4">
                  <h4 className="font-medium">Navigation</h4>
                  <div className="space-y-2 text-sm">
                    <div><code className="bg-muted px-2 py-1 rounded">{COMPONENT_TEMPLATES.navHeader}</code></div>
                    <div><code className="bg-muted px-2 py-1 rounded">{COMPONENT_TEMPLATES.navItemActive}</code></div>
                    <div><code className="bg-muted px-2 py-1 rounded">{COMPONENT_TEMPLATES.navItemInactive}</code></div>
                  </div>
                  
                  <h4 className="font-medium">Grid Layouts</h4>
                  <div className="space-y-2 text-sm">
                    <div><code className="bg-muted px-2 py-1 rounded">{COMPONENT_TEMPLATES.gridCards}</code></div>
                    <div><code className="bg-muted px-2 py-1 rounded">{COMPONENT_TEMPLATES.gridStats}</code></div>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Color Reference */}
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Color Reference</CardTitle>
              <p className="text-sm text-muted-foreground">
                Your frontend&apos;s actual color scheme
              </p>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="space-y-2">
                  <h4 className="font-medium">Primary Colors</h4>
                  <div className="space-y-2">
                    <div className="flex items-center gap-2">
                      <div className="w-4 h-4 rounded bg-primary"></div>
                      <span className="text-sm">Primary: #0891b2 (Cyan-600)</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <div className="w-4 h-4 rounded bg-secondary"></div>
                      <span className="text-sm">Secondary: #8b5cf6 (Purple)</span>
                    </div>
                  </div>
                </div>
                
                <div className="space-y-2">
                  <h4 className="font-medium">Status Colors</h4>
                  <div className="space-y-2">
                    <div className="flex items-center gap-2">
                      <div className="w-4 h-4 rounded bg-green-100 border border-green-200"></div>
                      <span className="text-sm">Success: Green</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <div className="w-4 h-4 rounded bg-yellow-100 border border-yellow-200"></div>
                      <span className="text-sm">Warning: Yellow</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <div className="w-4 h-4 rounded bg-red-100 border border-red-200"></div>
                      <span className="text-sm">Error: Red</span>
                    </div>
                  </div>
                </div>

                <div className="space-y-2">
                  <h4 className="font-medium">Usage Examples</h4>
                  <div className="space-y-2 text-sm">
                    <div>• Room Available → Primary (Blue marine)</div>
                    <div>• Room Occupied → Secondary (Purple)</div>
                    <div>• Confirmed → Green</div>
                    <div>• Pending → Yellow</div>
                    <div>• Cancelled → Red</div>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}

// API configuration and utility functions for Spring Boot backend integration

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api"

// Enhanced API response types matching Spring Boot structure
export interface ApiResponse<T> {
  data?: T
  message?: string
  success?: boolean
  timestamp?: string
  status?: number
  error?: string
  path?: string
}

export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  currentPage: number
  pageSize: number
  first: boolean
  last: boolean
  numberOfElements: number
  empty: boolean
  pageable: {
    pageNumber: number
    pageSize: number
    sort: {
      sorted: boolean
      unsorted: boolean
      empty: boolean
    }
    offset: number
    paged: boolean
    unpaged: boolean
  }
  sort: {
    sorted: boolean
    unsorted: boolean
    empty: boolean
  }
}

// Enhanced error handling for Spring Boot responses
export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public response?: any,
    public timestamp?: string,
    public path?: string
  ) {
    super(message)
    this.name = "ApiError"
  }
}

// Enhanced API client with better error handling
class ApiClient {
  private baseURL: string

  constructor(baseURL: string) {
    this.baseURL = baseURL
  }

  private async request<T>(endpoint: string, options: RequestInit = {}, retryCount = 0): Promise<T> {
    const url = `${this.baseURL}${endpoint}`

    // Get token and user ID from localStorage
    const token = typeof window !== "undefined" ? localStorage.getItem("token") : null
    const userStr = typeof window !== "undefined" ? localStorage.getItem("user") : null
    const user = userStr ? JSON.parse(userStr) : null

    const config: RequestInit = {
      headers: {
        "Content-Type": "application/json",
        ...(token && { Authorization: `Bearer ${token}` }),
        ...(user?.id && { "X-User-Id": user.id }),
        ...options.headers,
      },
      ...options,
    }

    try {
      const response = await fetch(url, config)

      if (!response.ok) {
        // Handle 401/403 errors with better user experience
        if (response.status === 401) {
          console.log("Authentication error, clearing session...")
          
          // Clear stored auth data only on 401 (unauthorized)
          if (typeof window !== "undefined") {
            localStorage.removeItem("token")
            localStorage.removeItem("user")
          }
        }
        
        const errorData = await response.json().catch(() => ({}))
        throw new ApiError(
          errorData.message || `HTTP error! status: ${response.status}`,
          response.status,
          errorData,
          errorData.timestamp,
          errorData.path
        )
      }

      return await response.json()
    } catch (error) {
      if (error instanceof ApiError) {
        throw error
      }
      throw new ApiError("Network error occurred", 0, error)
    }
  }

  async get<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: "GET" })
  }

  async post<T>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: "POST",
      body: data ? JSON.stringify(data) : undefined,
    })
  }

  async put<T>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: "PUT",
      body: data ? JSON.stringify(data) : undefined,
    })
  }

  async patch<T>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: "PATCH",
      body: data ? JSON.stringify(data) : undefined,
    })
  }

  async delete<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: "DELETE" })
  }
}

// Create API client instance
export const apiClient = new ApiClient(API_BASE_URL)

// API endpoints configuration matching Spring Boot backend
export const API_ENDPOINTS = {
  // Authentication
  AUTH: {
    LOGIN: "/auth/login",
    REGISTER: "/auth/register",
    PROFILE: "/auth/profile",
    CHANGE_PASSWORD: "/auth/change-password",
    NOTIFICATION_PREFERENCES: "/auth/notification-preferences",
  },

      // Admin User Management
      ADMIN_USERS: {
        ALL: "/auth/users",
        GET: (id: string) => `/auth/users/${id}`,
        CREATE: "/auth/users",
        UPDATE: (id: string) => `/auth/users/${id}`,
        DELETE: (id: string) => `/auth/users/${id}`,
        BY_ROLE: (role: string) => `/auth/users/role/${role}`,
  },

  // Rooms
  ROOMS: {
    LIST: "/rooms",
    GET: (id: string) => `/rooms/${id}`,
    AVAILABILITY: (id: string) => `/rooms/${id}/availability`,
    SEARCH: "/rooms",
  },

  // Admin Room Management
  ADMIN_ROOMS: {
    ALL: "/rooms",
    GET: (id: string) => `/rooms/${id}`,
    CREATE: "/rooms",
    UPDATE: (id: string) => `/rooms/${id}`,
    DELETE: (id: string) => `/rooms/${id}`,
    SEARCH: "/rooms",
  },

  // Room Photo Management
  ROOM_PHOTOS: {
    ADD: (roomId: string) => `/rooms/${roomId}/photos`,
    GET: (roomId: string) => `/rooms/${roomId}/photos`,
    DELETE: (photoId: string) => `/rooms/photos/${photoId}`,
    SET_PRIMARY: (photoId: string) => `/rooms/photos/${photoId}/primary`,
  },

  // Reservations - Client
  CLIENT_RESERVATIONS: {
    CREATE: "/client/reservations",
    MY: "/client/reservations/my",
    GET: (id: string) => `/client/reservations/${id}`,
    SEARCH: "/client/reservations/search",
    CANCEL: (id: string) => `/client/reservations/${id}/cancel`,
  },

  // Reservations - Admin
  ADMIN_RESERVATIONS: {
    ALL: "/admin/reservations",
    GET: (id: string) => `/admin/reservations/${id}`,
    SEARCH: "/admin/reservations/search",
    UPDATE: (id: string) => `/admin/reservations/${id}`,
    DELETE: (id: string) => `/admin/reservations/${id}`,
  },


  // Events - Public
  EVENTS: {
    LIST: "/events",
    GET: (id: string) => `/events/${id}`,
    SEARCH: "/events/search",
  },

  // Events - Admin
  ADMIN_EVENTS: {
    ALL: "/admin/events",
    GET: (id: string) => `/admin/events/${id}`,
    CREATE: "/admin/events",
    UPDATE: (id: string) => `/admin/events/${id}`,
    DELETE: (id: string) => `/admin/events/${id}`,
    STATISTICS: "/admin/events/statistics",
  },

  // Installations
  INSTALLATIONS: {
    ALL: "/installations",
    GET: (id: string) => `/installations/${id}`,
  },

  // Event Bookings
  EVENT_BOOKINGS: {
    CREATE: "/event-bookings",
    MY: "/event-bookings/my",
    GET: (id: string) => `/event-bookings/${id}`,
    CANCEL: (id: string) => `/event-bookings/${id}/cancel`,
  },

  // Admin Event Bookings
  ADMIN_EVENT_BOOKINGS: {
    ALL: "/admin/event-bookings",
    GET: (id: string) => `/admin/event-bookings/${id}`,
    SEARCH: "/admin/event-bookings/search",
    UPDATE: (id: string) => `/admin/event-bookings/${id}`,
  },

  // Feedback
  FEEDBACK: {
    CREATE: "/feedback",
    MY: "/feedback/my",
    GET: (id: string) => `/feedback/${id}`,
    UPDATE: (id: string) => `/feedback/${id}`,
    DELETE: (id: string) => `/feedback/${id}`,
  },

  // Admin Feedback
  ADMIN_FEEDBACK: {
    ALL: "/admin/feedback",
    GET: (id: string) => `/admin/feedback/${id}`,
    SEARCH: "/admin/feedback/search",
    STATISTICS: "/admin/feedback/statistics",
    REPLY: (id: string) => `/admin/feedback/${id}/reply`,
  },

  // Loyalty
  LOYALTY: {
    ACCOUNT: "/loyalty/account",
    BALANCE: "/loyalty/balance",
    TRANSACTIONS: "/loyalty/transactions",
    REDEEM: "/loyalty/redeem",
    STATISTICS: "/loyalty/statistics",
  },

  // Admin Loyalty
  ADMIN_LOYALTY: {
    ACCOUNTS: "/admin/loyalty/accounts",
    ACCOUNT: (id: string) => `/admin/loyalty/accounts/${id}`,
    ACCOUNT_BY_USER: (userId: string) => `/admin/loyalty/accounts/user/${userId}`,
    TRANSACTIONS: "/admin/loyalty/transactions",
    STATISTICS: "/admin/loyalty/statistics",
  },

  // Employee Dashboard APIs
  EMPLOYEE: {
    PROFILE: "/employee/profile",
    TASKS: "/employee/tasks",
    TASK_UPDATE: (id: string) => `/employee/tasks/${id}`,
    LEAVE_REQUESTS: "/employee/leave-requests",
    LEAVE_REQUEST_CREATE: "/employee/leave-requests",
    TRAININGS: "/employee/trainings",
    TRAINING_UPDATE: (id: string) => `/employee/trainings/${id}/status`,
    SHIFTS: "/employee/shifts",
  },

  // Admin Employees
  ADMIN_EMPLOYEES: {
    ALL: "/admin/employees",
    GET: (id: string) => `/admin/employees/${id}`,
    CREATE: "/admin/employees",
    UPDATE: (id: string) => `/admin/employees/${id}`,
    DELETE: (id: string) => `/admin/employees/${id}`,
    SEARCH: "/admin/employees/search",
    TASKS: "/admin/employees/tasks",
    TASK_BY_ID: (id: string) => `/admin/employees/tasks/${id}`,
    EMPLOYEE_TASKS: (employeeId: string) => `/admin/employees/${employeeId}/tasks`,
    SHIFTS: "/admin/employees/shifts",
    TRAININGS: "/admin/employees/trainings",
    TRAINING_ASSIGN: "/admin/employees/trainings/assign",
    TRAINING: "/admin/employees/training",
    LEAVE: "/admin/employees/leave",
    STATISTICS: "/admin/employees/statistics",
  },

  // Analytics
  ANALYTICS: {
    DASHBOARD: "/admin/analytics/dashboard",
    REVENUE: "/admin/analytics/revenue",
    REVENUE_TRENDS: "/admin/analytics/revenue/trends",
    OCCUPANCY: "/admin/analytics/occupancy",
    CUSTOMERS: "/admin/analytics/customers",
    EMPLOYEE_PERFORMANCE: "/admin/analytics/employees/performance",
    EMPLOYEE_PRODUCTIVITY: "/admin/analytics/employees/productivity",
    KPIS: "/admin/analytics/kpis",
    SYSTEM_PERFORMANCE: "/admin/analytics/system-performance",
  },

  // Announcements
  ANNOUNCEMENTS: {
    LIST: "/announcements",
    GET: (id: string) => `/announcements/${id}`,
  },

  // Admin Announcements
  ADMIN_ANNOUNCEMENTS: {
    ALL: "/admin/announcements",
    CREATE: "/admin/announcements",
    GET: (id: string) => `/admin/announcements/${id}`,
    UPDATE: (id: string) => `/admin/announcements/${id}`,
    DELETE: (id: string) => `/admin/announcements/${id}`,
  },
} as const

// ==================== CORRECTED TYPE DEFINITIONS ====================

// User interface - CORRECTED with phone, address, birthDate
export interface User {
  id: number
  firstName: string
  lastName: string
  email: string
  phone: string
  address: string
  birthDate: string // LocalDate from backend
  role: "CLIENT" | "EMPLOYEE" | "ADMIN"
  createdAt: string
  updatedAt: string
}

// Room interface - CORRECTED with proper status enum
export interface Room {
  id: number
  number: string // Changed from roomNumber to number
  roomType: "SINGLE" | "DOUBLE" | "DELUXE" | "FAMILY"
  capacity: number
  price: number
  currency: string
  status: "AVAILABLE" | "OCCUPIED" | "OUT_OF_SERVICE" // CORRECTED: OUT_OF_SERVICE instead of MAINTENANCE
  description?: string
  equipment?: string
  createdAt: string
  updatedAt: string
}

// Room Photo interface
export interface RoomPhoto {
  id: number
  photoUrl: string
  caption?: string
  roomId: number
  displayOrder: number
  isPrimary: boolean
  isActive: boolean
  photoType?: string
  fileSize?: number
  fileName?: string
  createdAt: string
  updatedAt: string
}

// Create Room Request interface
export interface CreateRoomRequest {
  number: string
  roomType: "SINGLE" | "DOUBLE" | "DELUXE" | "FAMILY"
  capacity: number
  price: number
  currency: string
  description?: string
}

// Add Room Photo Request interface
export interface AddRoomPhotoRequest {
  photoUrl: string
  caption?: string
  isPrimary?: boolean
}

// Reservation interfaces
export interface Reservation {
  id: number
  checkIn: string // LocalDate from backend
  checkOut: string // LocalDate from backend
  numberOfGuests: number
  totalPrice: number
  currency: string
  status: "CONFIRMED" | "CANCELLED"
  clientId: number
  clientName: string
  clientEmail: string
  roomId: number
  roomNumber: string
  roomType: "SINGLE" | "DOUBLE" | "DELUXE" | "FAMILY"
  pointsUsed?: number
  pointsDiscount?: number
  createdAt: string
  updatedAt: string
}

// Create Reservation Request interface
export interface CreateReservationRequest {
  roomId: number
  checkIn: string
  checkOut: string
  numberOfGuests: number
  pointsUsed?: number
}

// Update Reservation Request interface
export interface UpdateReservationRequest {
  checkIn?: string
  checkOut?: string
  numberOfGuests?: number
  status?: "CONFIRMED" | "CANCELLED"
}

// Event interfaces
export interface Event {
  id: number
  name: string
  description?: string
  eventType: "SPA" | "CONFERENCE" | "YOGA_CLASS" | "FITNESS" | "WEDDING"
  startAt: string // LocalDateTime from backend
  endAt: string // LocalDateTime from backend
  capacity: number
  price: number
  currency: string
  installationId: number
  installationName: string
  installationType: string
  createdAt: string
  updatedAt: string
}

// Create Event Request interface
export interface CreateEventRequest {
  name: string
  description?: string
  eventType: "SPA" | "CONFERENCE" | "YOGA_CLASS" | "FITNESS" | "WEDDING"
  startAt: string
  endAt: string
  capacity: number
  price: number
  currency: string
  installationId: number
}

// Event Search Criteria interface
export interface EventSearchCriteria {
  name?: string
  eventType?: "SPA" | "CONFERENCE" | "YOGA_CLASS" | "FITNESS" | "WEDDING"
  installationId?: number
  minPrice?: number
  maxPrice?: number
  minCapacity?: number
  page?: number
  size?: number
}

// Update Event Request interface
export interface UpdateEventRequest {
  name?: string
  description?: string
  eventType?: "SPA" | "CONFERENCE" | "YOGA_CLASS" | "FITNESS" | "WEDDING"
  startAt?: string
  endAt?: string
  capacity?: number
  price?: number
  currency?: string
  installationId?: number
}

// Installation interfaces
export interface Installation {
  id: number
  name: string
  description?: string
  installationType: "SPA_ROOM" | "CONFERENCE_ROOM" | "GYM" | "POOL" | "TENNIS_COURT" | "WEDDING_ROOM"
  capacity: number
  hourlyRate: number
  currency: string
  equipment?: string
  createdAt: string
  updatedAt: string
}

// Employee interfaces
export interface Employee {
  userId: number
  email: string
  firstName: string
  lastName: string
  status: "ACTIVE" | "INACTIVE"
  jobTitle: string
  createdAt: string
  updatedAt: string
}

// Create Employee Request interface
export interface CreateEmployeeRequest {
  userId: number
  jobTitle: string
}

// Update Employee Request interface
export interface UpdateEmployeeRequest {
  jobTitle?: string
  status?: "ACTIVE" | "INACTIVE"
}

// Event interface - matches backend exactly
export interface Event {
  id: number
  title: string
  description: string
  eventType: "CONFERENCE" | "WORKSHOP" | "MEETING" | "TRAINING" | "SOCIAL"
  maxCapacity: number
  price: number
  currency: string
  isActive: boolean
  createdAt: string
  updatedAt: string
}

// Event Booking interface - matches backend exactly
export interface EventBooking {
  id: number
  eventId: number
  eventTitle: string
  clientId: number
  clientName: string
  clientEmail: string
  bookingDate: string
  numberOfAttendees: number
  totalPrice: number
  currency: string
  status: "CONFIRMED" | "CANCELLED"
  createdAt: string
  updatedAt: string
}

// Feedback interface - CORRECTED with proper structure
export interface Feedback {
  id: number
  reservationId: number
  reservationNumber: string
  userId: number
  userName: string
  userEmail: string
  rating: number // 1-5
  comment?: string
  createdAt: string
  updatedAt: string
  replies?: FeedbackReply[] // Added feedback replies
  replyCount?: number
}

// Feedback Reply interface - NEW, matches backend exactly
export interface FeedbackReply {
  id: number
  feedbackId: number
  adminUserId: number
  adminUserName: string
  adminUserEmail: string
  message: string
  createdAt: string
  updatedAt: string
}

// Loyalty Account interface - matches backend exactly
export interface LoyaltyAccount {
  id: number
  userId: number
  userName: string
  userEmail: string
  balance: number
  createdAt: string
  updatedAt: string
}

// Loyalty Transaction interface - matches backend exactly
export interface LoyaltyTransaction {
  id: number
  accountId: number
  userId: number
  userName: string
  userEmail: string
  type: "EARN" | "REDEEM"
  points: number
  reservationId?: number
  reservationNumber?: string
  createdAt: string
}


// Task interface - matches backend exactly
export interface Task {
  id: number
  title: string
  description: string
  priority: "LOW" | "MEDIUM" | "HIGH"
  status: "TODO" | "IN_PROGRESS" | "DONE" // CORRECTED: matches TaskStatus enum
  assignedTo: number
  assignedToName: string
  dueDate: string
  createdAt: string
  updatedAt: string
}

// Shift interface - CORRECTED: No status field, matches backend exactly
export interface Shift {
  id: number
  employeeId: number
  employeeName: string
  employeeEmail: string
  startAt: string // LocalDateTime from backend
  endAt: string // LocalDateTime from backend
  // NO STATUS FIELD - matches backend Shift entity
}

// Leave Request interface - matches backend exactly
export interface LeaveRequest {
  id: number
  employeeId: number
  employeeName: string
  startDate: string
  endDate: string
  reason: string
  status: "PENDING" | "APPROVED" | "REJECTED"
  createdAt: string
  updatedAt: string
}

// Training interface - matches backend exactly
export interface Training {
  id: number
  title: string
  description: string
  duration: number
  startDate: string
  endDate: string
  maxParticipants: number
  status: "SCHEDULED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED"
  createdAt: string
  updatedAt: string
}

// Employee Training interface - matches backend exactly
export interface EmployeeTraining {
  id: number
  employeeId: number
  employeeName: string
  trainingId: number
  trainingTitle: string
  status: "ASSIGNED" | "COMPLETED"
  completionDate?: string
  createdAt: string
  updatedAt: string
}

// Employee Task interface - matches backend TaskResponseDto exactly
export interface EmployeeTask {
  id: number
  employeeId: number
  employeeName: string
  employeeEmail: string
  title: string
  description: string
  status: "TODO" | "IN_PROGRESS" | "DONE"
  note?: string
  photoUrl?: string
  createdAt: string
  updatedAt: string
}

// Employee Shift interface - matches backend ShiftResponseDto exactly
export interface EmployeeShift {
  id: number
  employeeId: number
  employeeName: string
  employeeEmail: string
  startAt: string
  endAt: string
}

// Admin Shift Management interfaces
export interface AdminShift {
  id: number
  employeeId: number
  employeeName: string
  employeeEmail: string
  startAt: string
  endAt: string
}

export interface CreateShiftRequest {
  employeeId: number
  startAt: string
  endAt: string
}

export interface UpdateShiftRequest {
  startAt: string
  endAt: string
}

// Employee Leave Request interface - matches backend exactly
export interface EmployeeLeaveRequest {
  id: number
  employeeId: number
  employeeName: string
  fromDate: string
  toDate: string
  reason: string
  status: "PENDING" | "APPROVED" | "REJECTED"
  createdAt: string
  updatedAt: string
}

// Employee Training Assignment interface - matches backend exactly
export interface EmployeeTrainingAssignment {
  id: number
  employeeId: number
  employeeName: string
  employeeEmail: string
  trainingId: number
  trainingTitle: string
  trainingStartDate: string
  trainingEndDate: string
  status: "ASSIGNED" | "IN_PROGRESS" | "COMPLETED"
  assignedAt: string
  completedAt?: string
  createdAt: string
  updatedAt: string
}

// Announcement interface - matches backend exactly
export interface Announcement {
  id: number
  title: string
  content: string
  priority: "LOW" | "MEDIUM" | "HIGH"
  isActive: boolean
  createdAt: string
  updatedAt: string
}

// Admin Task Management interfaces
export interface AdminTask {
  id: number
  employeeId: number
  employeeName: string
  employeeEmail: string
  title: string
  description: string
  status: "TODO" | "IN_PROGRESS" | "DONE"
  priority: "LOW" | "MEDIUM" | "HIGH"
  note?: string
  photoUrl?: string
  createdAt: string
  updatedAt: string
}

export interface CreateTaskRequest {
  employeeId: number
  title: string
  description: string
  priority?: "LOW" | "MEDIUM" | "HIGH"
}

export interface UpdateTaskRequest {
  status: "TODO" | "IN_PROGRESS" | "DONE"
  note?: string
  photoUrl?: string
}

// ==================== AUTHENTICATION TYPES ====================

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  firstName: string
  lastName: string
  email: string
  password: string
  phone: string
  address: string
  birthDate: string
  role?: "CLIENT" | "EMPLOYEE"
}

// LoginResponse - CORRECTED to match backend exactly
export interface LoginResponse {
  token: string
  tokenType: string
  userId: number
  email: string
  firstName: string
  lastName: string
  role: string
  expiresAt: string
  client: boolean
  employee: boolean
  admin: boolean
  roleDisplayName: string
  tokenValid: boolean
  fullName: string
}

// ==================== REQUEST/RESPONSE DTOS ====================

export interface CreateReservationRequest {
  roomId: number
  checkIn: string
  checkOut: string
  numberOfGuests: number
}

export interface CreateEventBookingRequest {
  eventId: number
  bookingDate: string
  numberOfAttendees: number
}

export interface CreateFeedbackRequest {
  reservationId: number
  rating: number
  comment?: string
}

export interface CreateFeedbackReplyRequest {
  message: string
}

export interface RedeemPointsRequest {
  points: number
  description: string
}

export interface CreateLeaveRequestRequest {
  startDate: string
  endDate: string
  reason: string
}

export interface UpdateTaskRequest {
  status: "TODO" | "IN_PROGRESS" | "DONE"
  note?: string
}

// Employee Dashboard Request/Response DTOs
export interface CreateLeaveRequestRequest {
  fromDate: string
  toDate: string
  reason: string
}

export interface UpdateTrainingStatusRequest {
  status: "ASSIGNED" | "IN_PROGRESS" | "COMPLETED"
}

// Employee Training Assignment Request
export interface EmployeeTrainingCreateRequest {
  employeeId: number
  trainingId: number
}

// Employee Dashboard Data interfaces
export interface EmployeeStats {
  shiftsThisWeek: number
  pendingTasks: number
  trainingStatus: "NONE" | "ASSIGNED" | "IN_PROGRESS" | "COMPLETED"
  leaveBalance: number
  totalShifts: number
  completedTasks: number
  pendingLeaveRequests: number
}

export interface TodayTask {
  id: number
  title: string
  description: string
  priority: "LOW" | "MEDIUM" | "HIGH"
  status: "TODO" | "IN_PROGRESS" | "DONE"
  dueDate: string
  canReply: boolean
}

export interface CalendarEvent {
  id: string
  title: string
  type: "shift" | "task" | "training"
  startTime: string
  endTime: string
  description?: string
  location?: string
  assignedBy?: string
  status?: "pending" | "completed" | "in-progress"
  canReply?: boolean
  replies?: TaskReply[]
  date?: string
  startDate?: string // For training date ranges
  endDate?: string   // For training date ranges
}

export interface TaskReply {
  id: number
  message: string
  createdAt: string
  employeeName: string
}

// ==================== ANALYTICS DTOS ====================

export interface DashboardData {
  revenueSummary: Record<string, any>
  occupancySummary: Record<string, any>
  customerInsights: Record<string, any>
  employeePerformance: Record<string, any>
  operationalKPIs: Record<string, any>
  systemPerformance: Record<string, any>
}

export interface RevenueAnalytics {
  roomRevenue: number
  eventRevenue: number
  totalRevenue: number
  revenueByService: Record<string, number>
  avgRevenuePerCustomer: number
}

export interface OccupancyMetrics {
  roomOccupancyRate: number
  occupiedRoomNights: number
  totalRoomNights: number
  installationUtilizationRate: number
  usedInstallations: number
  totalInstallations: number
}

export interface CustomerInsights {
  totalCustomers: number
  activeLoyaltyUsers: number
  loyaltyEngagementRate: number
  customerFeedback: Record<string, any>
}

export interface EmployeePerformance {
  taskMetrics: Record<string, any>
  trainingMetrics: Record<string, any>
  leaveMetrics: Record<string, any>
}

export interface EmployeeProductivity {
  totalEmployees: number
  activeEmployees: number
  activeEmployeeRate: number
  taskProductivity: Record<string, any>
}

export interface OperationalKPIs {
  totalRooms: number
  totalInstallations: number
  totalEmployees: number
  totalCustomers: number
  serviceMetrics: Record<string, any>
  resourceUtilization: Record<string, any>
}

export interface SystemPerformance {
  bookingSuccessRate: number
  responseMetrics: Record<string, any>
  healthIndicators: Record<string, any>
}

// ==================== AUTHENTICATION API ====================

export const authApi = {
  // Login user
  login: async (credentials: { email: string; password: string }): Promise<{ token: string; user: User }> => {
    return apiClient.post<{ token: string; user: User }>(API_ENDPOINTS.AUTH.LOGIN, credentials)
  },

  // Register user
  register: async (userData: { firstName: string; lastName: string; email: string; password: string }): Promise<User> => {
    return apiClient.post<User>(API_ENDPOINTS.AUTH.REGISTER, userData)
  },

  // Get user profile
  getProfile: async (): Promise<User> => {
    return apiClient.get<User>(API_ENDPOINTS.AUTH.PROFILE)
  },

  // Change password
  changePassword: async (passwordData: { currentPassword: string; newPassword: string }): Promise<void> => {
    return apiClient.put<void>(API_ENDPOINTS.AUTH.CHANGE_PASSWORD, passwordData)
  },

  // Update user profile
  updateProfile: async (profileData: Partial<User>): Promise<User> => {
    return apiClient.put<User>(API_ENDPOINTS.AUTH.PROFILE, profileData)
  },

  // Update notification preferences
  updateNotificationPreferences: async (preferences: any): Promise<void> => {
    return apiClient.put<void>(API_ENDPOINTS.AUTH.NOTIFICATION_PREFERENCES, preferences)
  },

  // Get all users (Admin only)
  getAllUsers: async (): Promise<User[]> => {
    return apiClient.get<User[]>(API_ENDPOINTS.ADMIN_USERS.ALL)
  },

  // Get users by role (Admin only)
  getUsersByRole: async (role: string): Promise<User[]> => {
    return apiClient.get<User[]>(`${API_ENDPOINTS.ADMIN_USERS.ALL}/role/${role}`)
  }
}

// ==================== EMPLOYEE API SERVICE FUNCTIONS ====================

// Employee Profile API
export const employeeApi = {
  // Get employee profile
  getProfile: async (): Promise<Employee> => {
    return apiClient.get<Employee>(API_ENDPOINTS.EMPLOYEE.PROFILE)
  },

  // Get employee tasks
  getTasks: async (status?: "TODO" | "IN_PROGRESS" | "DONE"): Promise<PaginatedResponse<EmployeeTask>> => {
    const params = status ? `?status=${status}` : ""
    return apiClient.get<PaginatedResponse<EmployeeTask>>(`${API_ENDPOINTS.EMPLOYEE.TASKS}${params}`)
  },

  // Update task status
  updateTask: async (taskId: number, request: UpdateTaskRequest): Promise<EmployeeTask> => {
    return apiClient.put<EmployeeTask>(API_ENDPOINTS.EMPLOYEE.TASK_UPDATE(taskId.toString()), request)
  },

  // Get employee shifts
  getShifts: async (): Promise<PaginatedResponse<EmployeeShift>> => {
    return apiClient.get<PaginatedResponse<EmployeeShift>>(API_ENDPOINTS.EMPLOYEE.SHIFTS)
  },

  // Get employee trainings
  getTrainings: async (status?: "ASSIGNED" | "IN_PROGRESS" | "COMPLETED"): Promise<PaginatedResponse<EmployeeTrainingAssignment>> => {
    const params = status ? `?status=${status}` : ""
    return apiClient.get<PaginatedResponse<EmployeeTrainingAssignment>>(`${API_ENDPOINTS.EMPLOYEE.TRAININGS}${params}`)
  },

  // Update training status
  updateTrainingStatus: async (trainingId: number, request: UpdateTrainingStatusRequest): Promise<EmployeeTrainingAssignment> => {
    return apiClient.put<EmployeeTrainingAssignment>(API_ENDPOINTS.EMPLOYEE.TRAINING_UPDATE(trainingId.toString()), request)
  },

  // Get leave requests
  getLeaveRequests: async (status?: "PENDING" | "APPROVED" | "REJECTED"): Promise<PaginatedResponse<EmployeeLeaveRequest>> => {
    const params = status ? `?status=${status}` : ""
    return apiClient.get<PaginatedResponse<EmployeeLeaveRequest>>(`${API_ENDPOINTS.EMPLOYEE.LEAVE_REQUESTS}${params}`)
  },

  // Create leave request
  createLeaveRequest: async (request: CreateLeaveRequestRequest): Promise<EmployeeLeaveRequest> => {
    return apiClient.post<EmployeeLeaveRequest>(API_ENDPOINTS.EMPLOYEE.LEAVE_REQUEST_CREATE, request)
  },

  // Get announcements
  getAnnouncements: async (): Promise<PaginatedResponse<Announcement>> => {
    return apiClient.get<PaginatedResponse<Announcement>>(API_ENDPOINTS.ANNOUNCEMENTS.LIST)
  }
}

// Employee Dashboard Data Aggregation
export const employeeDashboardApi = {
  // Get employee stats (aggregated data)
  getStats: async (): Promise<EmployeeStats> => {
    try {
      // Fetch all data in parallel
      const [tasksResponse, shiftsResponse, trainingsResponse, leaveRequestsResponse] = await Promise.all([
        employeeApi.getTasks(),
        employeeApi.getShifts(),
        employeeApi.getTrainings(),
        employeeApi.getLeaveRequests()
      ])

      // Calculate stats
      const tasks = tasksResponse.content
      const shifts = shiftsResponse.content
      const trainings = trainingsResponse.content
      const leaveRequests = leaveRequestsResponse.content

      // Calculate shifts this week
      const now = new Date()
      const startOfWeek = new Date(now)
      startOfWeek.setDate(now.getDate() - now.getDay())
      const endOfWeek = new Date(startOfWeek)
      endOfWeek.setDate(startOfWeek.getDate() + 6)

      const shiftsThisWeek = shifts.filter(shift => {
        const shiftDate = new Date(shift.startAt)
        return shiftDate >= startOfWeek && shiftDate <= endOfWeek
      }).length

      // Calculate pending tasks
      const pendingTasks = tasks.filter(task => task.status === "TODO").length

      // Calculate training status
      const activeTraining = trainings.find(t => t.status === "ASSIGNED" || t.status === "IN_PROGRESS")
      const trainingStatus = activeTraining ? activeTraining.status : "NONE"

      // Calculate completed tasks
      const completedTasks = tasks.filter(task => task.status === "DONE").length

      // Calculate pending leave requests
      const pendingLeaveRequests = leaveRequests.filter(request => request.status === "PENDING").length

      return {
        shiftsThisWeek,
        pendingTasks,
        trainingStatus: trainingStatus as "NONE" | "ASSIGNED" | "IN_PROGRESS" | "COMPLETED",
        leaveBalance: 20, // Default leave balance - could be calculated from backend
        totalShifts: shifts.length,
        completedTasks,
        pendingLeaveRequests
      }
    } catch (error) {
      console.error("Error fetching employee stats:", error)
      // Return default stats on error
      return {
        shiftsThisWeek: 0,
        pendingTasks: 0,
        trainingStatus: "NONE",
        leaveBalance: 20,
        totalShifts: 0,
        completedTasks: 0,
        pendingLeaveRequests: 0
      }
    }
  },

  // Get today's tasks
  getTodayTasks: async (): Promise<TodayTask[]> => {
    try {
      const response = await employeeApi.getTasks()
      const today = new Date().toISOString().split('T')[0]
      
      return response.content
        .filter(task => task.createdAt.startsWith(today))
        .map(task => ({
          id: task.id,
          title: task.title,
          description: task.description,
          priority: "MEDIUM", // Default priority since backend doesn't have it yet
          status: task.status,
          dueDate: task.createdAt, // Use createdAt as dueDate for now
          canReply: true
        }))
    } catch (error) {
      console.error("Error fetching today's tasks:", error)
      return []
    }
  },

  // Get announcements
  getAnnouncements: async (): Promise<PaginatedResponse<Announcement>> => {
    return employeeApi.getAnnouncements()
  },

  // Get calendar events (shifts, tasks, trainings)
  getCalendarEvents: async (startDate: string, endDate: string): Promise<CalendarEvent[]> => {
    try {
      const [tasksResponse, shiftsResponse, trainingsResponse] = await Promise.all([
        employeeApi.getTasks(),
        employeeApi.getShifts(),
        employeeApi.getTrainings()
      ])

      const events: CalendarEvent[] = []

      // Add tasks
      if (tasksResponse.content) {
        tasksResponse.content.forEach(task => {
          // Use createdAt as the task date since there's no dueDate in the backend
          const taskDate = new Date(task.createdAt).toISOString().split('T')[0]
          
          events.push({
            id: `task-${task.id}`,
            title: task.title,
            type: "task",
            startTime: task.createdAt,
            endTime: task.createdAt,
            description: task.description,
            status: task.status === "DONE" ? "completed" : "pending",
            canReply: true,
            date: taskDate
          })
        })
      }

      // Add shifts
      if (shiftsResponse.content) {
        shiftsResponse.content.forEach(shift => {
          const startTime = new Date(shift.startAt).toLocaleTimeString('en-US', { 
            hour: '2-digit', 
            minute: '2-digit',
            hour12: false 
          })
          const endTime = new Date(shift.endAt).toLocaleTimeString('en-US', { 
            hour: '2-digit', 
            minute: '2-digit',
            hour12: false 
          })
          
          // Ensure we have a proper date string
          const shiftDate = new Date(shift.startAt).toISOString().split('T')[0]
          
          events.push({
            id: `shift-${shift.id}`,
            title: "Work Shift",
            type: "shift",
            startTime,
            endTime,
            location: "Hotel",
            status: "in-progress",
            date: shiftDate
          })
        })
      }

      // Add trainings
      if (trainingsResponse.content) {
        trainingsResponse.content.forEach(training => {
          // Use training start date for the calendar event
          const trainingStartDate = training.trainingStartDate ? 
            new Date(training.trainingStartDate).toISOString().split('T')[0] : 
            (training.assignedAt ? new Date(training.assignedAt).toISOString().split('T')[0] : new Date().toISOString().split('T')[0])
          
          events.push({
            id: `training-${training.id}`,
            title: training.trainingTitle,
            type: "training",
            startTime: "All Day",
            endTime: "All Day",
            description: `Training: ${training.trainingStartDate ? new Date(training.trainingStartDate).toLocaleDateString() : 'TBD'} - ${training.trainingEndDate ? new Date(training.trainingEndDate).toLocaleDateString() : 'TBD'}`,
            status: training.status === "COMPLETED" ? "completed" : "in-progress",
            date: trainingStartDate,
            startDate: training.trainingStartDate ? new Date(training.trainingStartDate).toISOString().split('T')[0] : undefined,
            endDate: training.trainingEndDate ? new Date(training.trainingEndDate).toISOString().split('T')[0] : undefined
          })
        })
      }

      return events
    } catch (error) {
      console.error("Error fetching calendar events:", error)
      return []
    }
  }
}

// ==================== LOYALTY API ====================

export const loyaltyApi = {
  // Get loyalty account for current user
  getAccount: async (): Promise<LoyaltyAccount> => {
    return apiClient.get<LoyaltyAccount>(API_ENDPOINTS.LOYALTY.ACCOUNT)
  },

  // Get loyalty account by user ID
  getAccountByUser: async (userId: string): Promise<LoyaltyAccount> => {
    return apiClient.get<LoyaltyAccount>(API_ENDPOINTS.ADMIN_LOYALTY.ACCOUNT_BY_USER(userId))
  },

  // Get loyalty transactions for current user
  getTransactions: async (): Promise<PaginatedResponse<LoyaltyTransaction>> => {
    return apiClient.get<PaginatedResponse<LoyaltyTransaction>>(API_ENDPOINTS.LOYALTY.TRANSACTIONS)
  },

  // Get loyalty statistics
  getStatistics: async (): Promise<any> => {
    return apiClient.get<any>(API_ENDPOINTS.LOYALTY.STATISTICS)
  },

  // Admin: Get all loyalty accounts
  getAllAccounts: async (): Promise<PaginatedResponse<LoyaltyAccount>> => {
    return apiClient.get<PaginatedResponse<LoyaltyAccount>>(API_ENDPOINTS.ADMIN_LOYALTY.ACCOUNTS)
  },

  // Admin: Get all loyalty transactions
  getAllTransactions: async (): Promise<PaginatedResponse<LoyaltyTransaction>> => {
    return apiClient.get<PaginatedResponse<LoyaltyTransaction>>(API_ENDPOINTS.ADMIN_LOYALTY.TRANSACTIONS)
  }
}

// ==================== ANALYTICS API ====================

// ==================== ADMIN API ====================

export const adminApi = {
  // Get all reservations (admin)
  getAllReservations: async (page: number = 0, size: number = 1000): Promise<PaginatedResponse<any>> => {
    return apiClient.get<PaginatedResponse<any>>(`/admin/reservations?page=${page}&size=${size}`)
  },

  // Get all rooms (admin)
  getAllRooms: async (page: number = 0, size: number = 1000): Promise<PaginatedResponse<any>> => {
    return apiClient.get<PaginatedResponse<any>>(`/rooms?page=${page}&size=${size}`)
  },

  // Get all events (admin)
  getAllEvents: async (page: number = 0, size: number = 1000): Promise<PaginatedResponse<any>> => {
    return apiClient.get<PaginatedResponse<any>>(`/admin/events?page=${page}&size=${size}`)
  },

  // Get event statistics (admin)
  getEventStatistics: async (): Promise<any> => {
    return apiClient.get<any>('/admin/events/statistics')
  }
}

export const analyticsApi = {
  // Get dashboard data
  getDashboardData: async (): Promise<any> => {
    return apiClient.get<any>('/admin/analytics/dashboard')
  },

  // Get revenue analytics
  getRevenueAnalytics: async (startDate: string, endDate: string): Promise<any> => {
    return apiClient.get<any>(`/admin/analytics/revenue?startDate=${startDate}&endDate=${endDate}`)
  },

  // Get revenue trends
  getRevenueTrends: async (months: number = 12): Promise<any> => {
    return apiClient.get<any>(`/admin/analytics/revenue/trends?months=${months}`)
  },

  // Get occupancy metrics
  getOccupancyMetrics: async (startDate: string, endDate: string): Promise<any> => {
    return apiClient.get<any>(`/admin/analytics/occupancy?startDate=${startDate}&endDate=${endDate}`)
  },

  // Get customer insights
  getCustomerInsights: async (): Promise<any> => {
    return apiClient.get<any>('/admin/analytics/customers')
  },

  // Get employee performance
  getEmployeePerformance: async (): Promise<any> => {
    return apiClient.get<any>('/admin/analytics/employees/performance')
  },

  // Get employee productivity
  getEmployeeProductivity: async (): Promise<any> => {
    return apiClient.get<any>('/admin/analytics/employees/productivity')
  },

  // Get operational KPIs
  getOperationalKPIs: async (): Promise<any> => {
    return apiClient.get<any>('/admin/analytics/kpis')
  },

  // Get system performance
  getSystemPerformance: async (): Promise<any> => {
    return apiClient.get<any>('/admin/analytics/system-performance')
  }
}

// ==================== FEEDBACK API ====================

export const feedbackApi = {
  // Get user's feedback
  getUserFeedbacks: async (): Promise<PaginatedResponse<Feedback>> => {
    return apiClient.get<PaginatedResponse<Feedback>>(API_ENDPOINTS.FEEDBACK.MY)
  },

  // Create feedback
  createFeedback: async (request: CreateFeedbackRequest): Promise<Feedback> => {
    return apiClient.post<Feedback>(API_ENDPOINTS.FEEDBACK.CREATE, request)
  },

  // Admin: Get all feedbacks
  getAllFeedbacks: async (): Promise<PaginatedResponse<Feedback>> => {
    return apiClient.get<PaginatedResponse<Feedback>>(API_ENDPOINTS.ADMIN_FEEDBACK.ALL)
  },

  // Admin: Reply to feedback
  replyToFeedback: async (feedbackId: number, reply: string): Promise<Feedback> => {
    return apiClient.post<Feedback>(`/feedback-replies/feedback/${feedbackId}`, { message: reply })
  }
}

// ==================== EVENT API ====================

export const eventApi = {
  // Get all available events (for browsing)
  getAllEvents: async (page: number = 0, size: number = 20): Promise<PaginatedResponse<Event>> => {
    return apiClient.get<PaginatedResponse<Event>>(`${API_ENDPOINTS.EVENTS.LIST}?page=${page}&size=${size}`)
  },

  // Get event by ID
  getEventById: async (eventId: number): Promise<Event> => {
    return apiClient.get<Event>(API_ENDPOINTS.EVENTS.GET(eventId.toString()))
  },

  // Search events with criteria
  searchEvents: async (criteria: EventSearchCriteria): Promise<PaginatedResponse<Event>> => {
    const params = new URLSearchParams()
    if (criteria.name) params.append('name', criteria.name)
    if (criteria.eventType) params.append('eventType', criteria.eventType)
    if (criteria.installationId) params.append('installationId', criteria.installationId.toString())
    if (criteria.minPrice) params.append('minPrice', criteria.minPrice.toString())
    if (criteria.maxPrice) params.append('maxPrice', criteria.maxPrice.toString())
    if (criteria.minCapacity) params.append('minCapacity', criteria.minCapacity.toString())
    if (criteria.page) params.append('page', criteria.page.toString())
    if (criteria.size) params.append('size', criteria.size.toString())
    
    return apiClient.get<PaginatedResponse<Event>>(`${API_ENDPOINTS.EVENTS.SEARCH}?${params.toString()}`)
  },

  // Get upcoming events
  getUpcomingEvents: async (page: number = 0, size: number = 20): Promise<PaginatedResponse<Event>> => {
    return apiClient.get<PaginatedResponse<Event>>(`${API_ENDPOINTS.EVENTS.LIST}/upcoming?page=${page}&size=${size}`)
  },

  // Get events by type
  getEventsByType: async (eventType: string, page: number = 0, size: number = 20): Promise<PaginatedResponse<Event>> => {
    return apiClient.get<PaginatedResponse<Event>>(`${API_ENDPOINTS.EVENTS.LIST}/by-type/${eventType}?page=${page}&size=${size}`)
  },

  // Get events by installation
  getEventsByInstallation: async (installationId: number, page: number = 0, size: number = 20): Promise<PaginatedResponse<Event>> => {
    return apiClient.get<PaginatedResponse<Event>>(`${API_ENDPOINTS.EVENTS.LIST}/by-installation/${installationId}?page=${page}&size=${size}`)
  },

  // Get user's event bookings
  getUserEventBookings: async (): Promise<EventBooking[]> => {
    return apiClient.get<EventBooking[]>(API_ENDPOINTS.EVENT_BOOKINGS.MY)
  }
}

// ==================== ADMIN EMPLOYEES API ====================

export const adminEmployeesApi = {
  // Get all employees
  getAllEmployees: async (): Promise<Employee[]> => {
    return apiClient.get<Employee[]>(`${API_ENDPOINTS.ADMIN_EMPLOYEES.ALL}/active`)
  },

  // Get employee by ID
  getEmployeeById: async (employeeId: number): Promise<Employee> => {
    return apiClient.get<Employee>(API_ENDPOINTS.ADMIN_EMPLOYEES.GET(employeeId.toString()))
  }
}

// ==================== ADMIN TASK MANAGEMENT API ====================

export const adminTaskApi = {
  // Get all tasks with filtering
  getAllTasks: async (status?: "TODO" | "IN_PROGRESS" | "DONE"): Promise<PaginatedResponse<AdminTask>> => {
    const params = status ? `?status=${status}` : ""
    return apiClient.get<PaginatedResponse<AdminTask>>(`${API_ENDPOINTS.ADMIN_EMPLOYEES.TASKS}${params}`)
  },

  // Get tasks for specific employee
  getEmployeeTasks: async (employeeId: number, status?: "TODO" | "IN_PROGRESS" | "DONE"): Promise<PaginatedResponse<AdminTask>> => {
    const params = status ? `?status=${status}` : ""
    return apiClient.get<PaginatedResponse<AdminTask>>(`${API_ENDPOINTS.ADMIN_EMPLOYEES.EMPLOYEE_TASKS(employeeId.toString())}${params}`)
  },

  // Create new task
  createTask: async (request: CreateTaskRequest): Promise<AdminTask> => {
    return apiClient.post<AdminTask>(API_ENDPOINTS.ADMIN_EMPLOYEES.TASKS, request)
  },

  // Update task
  updateTask: async (taskId: number, request: UpdateTaskRequest): Promise<AdminTask> => {
    return apiClient.put<AdminTask>(API_ENDPOINTS.ADMIN_EMPLOYEES.TASK_BY_ID(taskId.toString()), request)
  },

  // Get task by ID
  getTaskById: async (taskId: number): Promise<AdminTask> => {
    return apiClient.get<AdminTask>(API_ENDPOINTS.ADMIN_EMPLOYEES.TASK_BY_ID(taskId.toString()))
  }
}

// ==================== ADMIN SHIFT MANAGEMENT API ====================

export const adminShiftApi = {
  // Get all shifts
  getAllShifts: async (): Promise<PaginatedResponse<AdminShift>> => {
    return apiClient.get<PaginatedResponse<AdminShift>>(API_ENDPOINTS.ADMIN_EMPLOYEES.SHIFTS)
  },

  // Get shifts for specific employee
  getEmployeeShifts: async (employeeId: number): Promise<PaginatedResponse<AdminShift>> => {
    return apiClient.get<PaginatedResponse<AdminShift>>(`${API_ENDPOINTS.ADMIN_EMPLOYEES.ALL}/${employeeId}/shifts`)
  },

  // Create new shift
  createShift: async (request: CreateShiftRequest): Promise<AdminShift> => {
    return apiClient.post<AdminShift>(API_ENDPOINTS.ADMIN_EMPLOYEES.SHIFTS, request)
  },

  // Update shift
  updateShift: async (shiftId: number, request: UpdateShiftRequest): Promise<AdminShift> => {
    return apiClient.put<AdminShift>(`${API_ENDPOINTS.ADMIN_EMPLOYEES.SHIFTS}/${shiftId}`, request)
  },

  // Delete shift
  deleteShift: async (shiftId: number): Promise<void> => {
    return apiClient.delete(`${API_ENDPOINTS.ADMIN_EMPLOYEES.SHIFTS}/${shiftId}`)
  },

  // Get shift by ID
  getShiftById: async (shiftId: number): Promise<AdminShift> => {
    return apiClient.get<AdminShift>(`${API_ENDPOINTS.ADMIN_EMPLOYEES.SHIFTS}/${shiftId}`)
  }
}

// Training Creation Request
export interface TrainingCreateRequest {
  title: string
  startDate: string
  endDate: string
}

// Admin Training Management API
export const adminTrainingApi = {
  // Get all trainings
  getAllTrainings: async (): Promise<PaginatedResponse<Training>> => {
    return apiClient.get<PaginatedResponse<Training>>(API_ENDPOINTS.ADMIN_EMPLOYEES.TRAININGS)
  },
  
  // Create new training
  createTraining: async (request: TrainingCreateRequest): Promise<Training> => {
    return apiClient.post<Training>(API_ENDPOINTS.ADMIN_EMPLOYEES.TRAININGS, request)
  },
  
  // Get training by ID
  getTrainingById: async (trainingId: number): Promise<Training> => {
    return apiClient.get<Training>(`${API_ENDPOINTS.ADMIN_EMPLOYEES.TRAININGS}/${trainingId}`)
  },
  
  // Assign training to employee
  assignTraining: async (request: EmployeeTrainingCreateRequest): Promise<EmployeeTraining> => {
    return apiClient.post<EmployeeTraining>(API_ENDPOINTS.ADMIN_EMPLOYEES.TRAINING_ASSIGN, request)
  },
  
  // Update training status
  updateTrainingStatus: async (employeeId: number, trainingId: number, request: UpdateTrainingStatusRequest): Promise<EmployeeTraining> => {
    return apiClient.put<EmployeeTraining>(`${API_ENDPOINTS.ADMIN_EMPLOYEES.TRAININGS}/${employeeId}/${trainingId}/status`, request)
  },
  
  // Get employee trainings
  getEmployeeTrainings: async (employeeId: number): Promise<PaginatedResponse<EmployeeTraining>> => {
    return apiClient.get<PaginatedResponse<EmployeeTraining>>(`${API_ENDPOINTS.ADMIN_EMPLOYEES.ALL}/${employeeId}/trainings`)
  },
  
  // Get employee trainings by training ID (for statistics)
  getEmployeeTrainingsByTrainingId: async (trainingId: number): Promise<PaginatedResponse<EmployeeTraining>> => {
    // This endpoint doesn't exist in backend, we need to get all employee trainings and filter
    // For now, let's return empty array and handle this differently
    return { content: [], totalElements: 0, totalPages: 0, size: 0, number: 0 }
  }
}

// Admin Leave Management API
export const adminLeaveApi = {
  // Get all pending leave requests
  getPendingLeaveRequests: async (): Promise<PaginatedResponse<LeaveRequest>> => {
    return apiClient.get<PaginatedResponse<LeaveRequest>>(`${API_ENDPOINTS.ADMIN_EMPLOYEES.ALL}/leave-requests/pending`)
  },
  
  // Get all leave requests (with pagination and filtering)
  getAllLeaveRequests: async (): Promise<PaginatedResponse<LeaveRequest>> => {
    return apiClient.get<PaginatedResponse<LeaveRequest>>(`${API_ENDPOINTS.ADMIN_EMPLOYEES.ALL}/leave-requests`)
  },
  
  // Approve a leave request
  approveLeaveRequest: async (requestId: number): Promise<LeaveRequest> => {
    return apiClient.put<LeaveRequest>(`${API_ENDPOINTS.ADMIN_EMPLOYEES.ALL}/leave-requests/${requestId}/approve`)
  },
  
  // Reject a leave request
  rejectLeaveRequest: async (requestId: number): Promise<LeaveRequest> => {
    return apiClient.put<LeaveRequest>(`${API_ENDPOINTS.ADMIN_EMPLOYEES.ALL}/leave-requests/${requestId}/reject`)
  },
  
  // Get leave requests for a specific employee
  getEmployeeLeaveRequests: async (employeeId: number): Promise<PaginatedResponse<LeaveRequest>> => {
    return apiClient.get<PaginatedResponse<LeaveRequest>>(`${API_ENDPOINTS.ADMIN_EMPLOYEES.ALL}/${employeeId}/leave-requests`)
  }
}

// Admin Event Management API
export const adminEventApi = {
  // Get all events
  getAllEvents: async (): Promise<PaginatedResponse<Event>> => {
    return apiClient.get<PaginatedResponse<Event>>(API_ENDPOINTS.ADMIN_EVENTS.ALL)
  },
  
  // Get event by ID
  getEventById: async (eventId: number): Promise<Event> => {
    return apiClient.get<Event>(API_ENDPOINTS.ADMIN_EVENTS.GET(eventId.toString()))
  },
  
  // Create new event
  createEvent: async (eventData: CreateEventRequest): Promise<Event> => {
    return apiClient.post<Event>(API_ENDPOINTS.ADMIN_EVENTS.CREATE, eventData)
  },
  
  // Update event
  updateEvent: async (eventId: number, eventData: UpdateEventRequest): Promise<Event> => {
    return apiClient.put<Event>(API_ENDPOINTS.ADMIN_EVENTS.UPDATE(eventId.toString()), eventData)
  },
  
  // Delete event
  deleteEvent: async (eventId: number): Promise<void> => {
    return apiClient.delete<void>(API_ENDPOINTS.ADMIN_EVENTS.DELETE(eventId.toString()))
  },
  
  // Get event statistics
  getEventStatistics: async (): Promise<any> => {
    return apiClient.get<any>(API_ENDPOINTS.ADMIN_EVENTS.STATISTICS)
  }
}

// Installation API
export const installationApi = {
  // Get all installations
  getAllInstallations: async (): Promise<Installation[]> => {
    return apiClient.get<Installation[]>(API_ENDPOINTS.INSTALLATIONS.ALL)
  },
  
  // Get installation by ID
  getInstallationById: async (installationId: number): Promise<Installation> => {
    return apiClient.get<Installation>(API_ENDPOINTS.INSTALLATIONS.GET(installationId.toString()))
  }
}

// ==================== EVENT BOOKING API ====================

// Event Booking Interfaces
export interface EventBookingCreateRequest {
  eventId: number
  reservationId?: number | null
  numberOfParticipants?: number
  specialRequests?: string
}

export interface EventBookingResponse {
  id: number
  eventId: number
  eventName: string
  eventType: string
  eventStartAt: string
  eventEndAt: string
  eventPrice: number
  eventCurrency: string
  userId: number
  userFirstName: string
  userLastName: string
  userEmail: string
  reservationId: number
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED'
  createdAt: string
  updatedAt: string
}

export interface EventBookingSearchCriteria {
  status?: string
  eventType?: string
  fromDate?: string
  toDate?: string
  page?: number
  size?: number
}

// Event Booking API
export const eventBookingApi = {
  // Create event booking
  createBooking: async (request: EventBookingCreateRequest): Promise<EventBookingResponse> => {
    return apiClient.post<EventBookingResponse>(API_ENDPOINTS.EVENT_BOOKINGS.CREATE, request)
  },

  // Get my event bookings
  getMyBookings: async (page: number = 0, size: number = 10): Promise<PaginatedResponse<EventBookingResponse>> => {
    return apiClient.get<PaginatedResponse<EventBookingResponse>>(`${API_ENDPOINTS.EVENT_BOOKINGS.MY}?page=${page}&size=${size}`)
  },

  // Get event booking by ID
  getBookingById: async (bookingId: number): Promise<EventBookingResponse> => {
    return apiClient.get<EventBookingResponse>(API_ENDPOINTS.EVENT_BOOKINGS.GET(bookingId.toString()))
  },

  // Cancel event booking
  cancelBooking: async (bookingId: number, reason?: string): Promise<EventBookingResponse> => {
    const url = reason 
      ? `${API_ENDPOINTS.EVENT_BOOKINGS.CANCEL(bookingId.toString())}?reason=${encodeURIComponent(reason)}`
      : API_ENDPOINTS.EVENT_BOOKINGS.CANCEL(bookingId.toString())
    return apiClient.put<EventBookingResponse>(url, {})
  },

  // Search event bookings
  searchBookings: async (criteria: EventBookingSearchCriteria): Promise<PaginatedResponse<EventBookingResponse>> => {
    const params = new URLSearchParams()
    if (criteria.status) params.append('status', criteria.status)
    if (criteria.eventType) params.append('eventType', criteria.eventType)
    if (criteria.fromDate) params.append('fromDate', criteria.fromDate)
    if (criteria.toDate) params.append('toDate', criteria.toDate)
    if (criteria.page) params.append('page', criteria.page.toString())
    if (criteria.size) params.append('size', criteria.size.toString())
    
    const queryString = params.toString()
    const url = queryString 
      ? `${API_ENDPOINTS.EVENT_BOOKINGS.MY}?${queryString}`
      : API_ENDPOINTS.EVENT_BOOKINGS.MY
    
    return apiClient.get<PaginatedResponse<EventBookingResponse>>(url)
  }
}

// ==================== RESERVATION API ====================

// Reservation Interfaces
export interface CreateReservationRequest {
  roomId: number
  checkIn: string
  checkOut: string
  numberOfGuests: number
  pointsUsed?: number
  currency?: string
}

export interface ReservationResponse {
  id: number
  roomId: number
  roomNumber: string
  roomType: string
  checkIn: string
  checkOut: string
  numberOfGuests: number
  totalPrice: number
  currency: string
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED'
  pointsUsed?: number
  pointsDiscount?: number
  clientId: number
  createdAt: string
  updatedAt: string
}

// Reservation API
export const reservationApi = {
  // Create reservation
  createReservation: async (request: CreateReservationRequest): Promise<ReservationResponse> => {
    return apiClient.post<ReservationResponse>(API_ENDPOINTS.CLIENT_RESERVATIONS.CREATE, request)
  },

  // Get my reservations
  getMyReservations: async (page: number = 0, size: number = 10): Promise<PaginatedResponse<ReservationResponse>> => {
    return apiClient.get<PaginatedResponse<ReservationResponse>>(`${API_ENDPOINTS.CLIENT_RESERVATIONS.MY}?page=${page}&size=${size}`)
  },

  // Get reservation by ID
  getReservationById: async (reservationId: number): Promise<ReservationResponse> => {
    return apiClient.get<ReservationResponse>(API_ENDPOINTS.CLIENT_RESERVATIONS.GET(reservationId.toString()))
  },

  // Cancel reservation
  cancelReservation: async (reservationId: number, reason?: string): Promise<ReservationResponse> => {
    const url = reason 
      ? `${API_ENDPOINTS.CLIENT_RESERVATIONS.CANCEL(reservationId.toString())}?reason=${encodeURIComponent(reason)}`
      : API_ENDPOINTS.CLIENT_RESERVATIONS.CANCEL(reservationId.toString())
    return apiClient.put<ReservationResponse>(url, {})
  },

  // Search reservations
  searchReservations: async (criteria: any): Promise<PaginatedResponse<ReservationResponse>> => {
    const params = new URLSearchParams()
    if (criteria.status) params.append('status', criteria.status)
    if (criteria.fromDate) params.append('fromDate', criteria.fromDate)
    if (criteria.toDate) params.append('toDate', criteria.toDate)
    if (criteria.page) params.append('page', criteria.page.toString())
    if (criteria.size) params.append('size', criteria.size.toString())
    
    const queryString = params.toString()
    const url = queryString 
      ? `${API_ENDPOINTS.CLIENT_RESERVATIONS.SEARCH}?${queryString}`
      : API_ENDPOINTS.CLIENT_RESERVATIONS.MY
    
    return apiClient.get<PaginatedResponse<ReservationResponse>>(url)
  }
}

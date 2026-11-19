"use client"

// React hooks for API integration with SWR for caching and state management

import useSWR from "swr"
import { useState } from "react"
import {
  apiClient,
  API_ENDPOINTS,
  ApiError,
  type User,
  type Room,
  type Reservation,
  type DashboardData,
  type CreateReservationRequest,
  type CreateRoomRequest,
  type LoginRequest,
  type LoginResponse,
  type RegisterRequest,
} from "./api"

type Booking = Reservation
type DashboardStats = DashboardData
type CreateBookingRequest = CreateReservationRequest
type CreateUserRequest = RegisterRequest
type AuthResponse = LoginResponse & { refreshToken?: string; user?: User }
const ENDPOINTS = API_ENDPOINTS as Record<string, any>

// Generic fetcher function for SWR
const fetcher = <T = any>(url: string): Promise<T> => apiClient.get<T>(url)

// Authentication hooks
export function useAuth() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const login = async (credentials: LoginRequest): Promise<AuthResponse | null> => {
    setIsLoading(true)
    setError(null)

    try {
      const response = await apiClient.post<AuthResponse>(API_ENDPOINTS.AUTH.LOGIN, credentials)

      // Store tokens and user data
      if (typeof window !== "undefined") {
        localStorage.setItem("token", response.token)
        if (response.refreshToken) localStorage.setItem("refreshToken", response.refreshToken)
        if (response.user) localStorage.setItem("user", JSON.stringify(response.user))
      }

      return response
    } catch (err) {
      const errorMessage = err instanceof ApiError ? err.message : "Login failed"
      setError(errorMessage)
      return null
    } finally {
      setIsLoading(false)
    }
  }

  const register = async (userData: RegisterRequest): Promise<boolean> => {
    setIsLoading(true)
    setError(null)

    try {
      await apiClient.post(API_ENDPOINTS.AUTH.REGISTER, userData)
      return true
    } catch (err) {
      const errorMessage = err instanceof ApiError ? err.message : "Registration failed"
      setError(errorMessage)
      return false
    } finally {
      setIsLoading(false)
    }
  }

  const logout = async () => {
    try {
      await apiClient.post(ENDPOINTS.AUTH.LOGOUT)
    } catch (err) {
      // Continue with logout even if API call fails
      console.error("Logout API call failed:", err)
    } finally {
      // Clear local storage
      if (typeof window !== "undefined") {
        localStorage.removeItem("token")
        localStorage.removeItem("refreshToken")
        localStorage.removeItem("user")
      }
    }
  }

  return { login, register, logout, isLoading, error }
}

// Room hooks
export function useRooms() {
  const { data, error, mutate } = useSWR<Room[]>(API_ENDPOINTS.ROOMS.LIST, fetcher)

  return {
    rooms: data,
    isLoading: !error && !data,
    error,
    mutate,
  }
}

export function useRoom(roomId: string) {
  const { data, error, mutate } = useSWR<Room>(roomId ? API_ENDPOINTS.ROOMS.GET(roomId) : null, fetcher)

  return {
    room: data,
    isLoading: !error && !data,
    error,
    mutate,
  }
}

export function useCreateRoom() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const createRoom = async (roomData: CreateRoomRequest): Promise<Room | null> => {
    setIsLoading(true)
    setError(null)

    try {
      const response = await apiClient.post<Room>(ENDPOINTS.ROOMS.CREATE, roomData)
      return response
    } catch (err) {
      const errorMessage = err instanceof ApiError ? err.message : "Failed to create room"
      setError(errorMessage)
      return null
    } finally {
      setIsLoading(false)
    }
  }

  return { createRoom, isLoading, error }
}

// Booking hooks
export function useBookings(userId?: string, roomId?: string) {
  let endpoint = ENDPOINTS.BOOKINGS.LIST

  if (userId) {
    endpoint = ENDPOINTS.BOOKINGS.USER_BOOKINGS(userId)
  } else if (roomId) {
    endpoint = ENDPOINTS.BOOKINGS.ROOM_BOOKINGS(roomId)
  }

  const { data, error, mutate } = useSWR<Booking[]>(endpoint, fetcher)

  return {
    bookings: data,
    isLoading: !error && !data,
    error,
    mutate,
  }
}

export function useCreateBooking() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const createBooking = async (bookingData: CreateBookingRequest): Promise<Booking | null> => {
    setIsLoading(true)
    setError(null)

    try {
      const response = await apiClient.post<Booking>(ENDPOINTS.BOOKINGS.CREATE, bookingData)
      return response
    } catch (err) {
      const errorMessage = err instanceof ApiError ? err.message : "Failed to create booking"
      setError(errorMessage)
      return null
    } finally {
      setIsLoading(false)
    }
  }

  return { createBooking, isLoading, error }
}

// Admin hooks
export function useDashboardStats() {
  const { data, error, mutate } = useSWR<DashboardStats>(ENDPOINTS.ADMIN.STATS, fetcher)

  return {
    stats: data,
    isLoading: !error && !data,
    error,
    mutate,
  }
}

export function useUsers() {
  const { data, error, mutate } = useSWR<User[]>(ENDPOINTS.USERS.LIST, fetcher)

  return {
    users: data,
    isLoading: !error && !data,
    error,
    mutate,
  }
}

export function useCreateUser() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const createUser = async (userData: CreateUserRequest): Promise<User | null> => {
    setIsLoading(true)
    setError(null)

    try {
      const response = await apiClient.post<User>(ENDPOINTS.USERS.CREATE, userData)
      return response
    } catch (err) {
      const errorMessage = err instanceof ApiError ? err.message : "Failed to create user"
      setError(errorMessage)
      return null
    } finally {
      setIsLoading(false)
    }
  }

  return { createUser, isLoading, error }
}

// Generic mutation hooks
export function useUpdateResource<T>(endpoint: string) {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const updateResource = async (data: Partial<T>): Promise<T | null> => {
    setIsLoading(true)
    setError(null)

    try {
      const response = await apiClient.put<T>(endpoint, data)
      return response
    } catch (err) {
      const errorMessage = err instanceof ApiError ? err.message : "Update failed"
      setError(errorMessage)
      return null
    } finally {
      setIsLoading(false)
    }
  }

  return { updateResource, isLoading, error }
}

export function useDeleteResource() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const deleteResource = async (endpoint: string): Promise<boolean> => {
    setIsLoading(true)
    setError(null)

    try {
      await apiClient.delete(endpoint)
      return true
    } catch (err) {
      const errorMessage = err instanceof ApiError ? err.message : "Delete failed"
      setError(errorMessage)
      return false
    } finally {
      setIsLoading(false)
    }
  }

  return { deleteResource, isLoading, error }
}

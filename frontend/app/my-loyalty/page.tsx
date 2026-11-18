"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { ArrowLeft, Star, TrendingUp, Award, Download, Plus, Minus, Gift } from "lucide-react"
import Link from "next/link"
import { loyaltyApi } from "@/lib/api"
import type { LoyaltyAccount, LoyaltyTransaction } from "@/lib/api"
import { toast } from "sonner"

export default function MyLoyaltyPage() {
  const [loyaltyAccount, setLoyaltyAccount] = useState<LoyaltyAccount | null>(null)
  const [transactions, setTransactions] = useState<LoyaltyTransaction[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedPeriod, setSelectedPeriod] = useState("all")
  const [selectedType, setSelectedType] = useState("all")

  useEffect(() => {
    const fetchLoyaltyData = async () => {
      try {
        setLoading(true)
        // Fetch loyalty account
        const account = await loyaltyApi.getAccount()
        setLoyaltyAccount(account)
        
        // Fetch transactions - handle paginated response
        const transactionResponse = await loyaltyApi.getTransactions()
        setTransactions(transactionResponse.content || [])
      } catch (error) {
        console.error('Failed to fetch loyalty data:', error)
        toast.error("Failed to load loyalty data")
        // Set default values to prevent undefined errors
        setLoyaltyAccount(null)
        setTransactions([])
      } finally {
        setLoading(false)
      }
    }

    fetchLoyaltyData()
  }, [])

  const filteredTransactions = transactions.filter(transaction => {
    if (selectedType !== "all" && transaction.type !== selectedType) {
      return false
    }
    // Add date filtering logic here if needed
    return true
  })

  // Calculate totals from transactions
  const totalPointsEarned = transactions
    .filter(t => t.type === "EARN")
    .reduce((sum, t) => sum + t.points, 0)

  const totalPointsRedeemed = transactions
    .filter(t => t.type === "REDEEM")
    .reduce((sum, t) => sum + t.points, 0)

  const getTransactionIcon = (type: string) => {
    switch (type) {
      case "EARN":
        return <Plus className="h-4 w-4 text-green-600" />
      case "REDEEM":
        return <Minus className="h-4 w-4 text-red-600" />
      default:
        return <Star className="h-4 w-4" />
    }
  }

  const getTransactionBadge = (type: string) => {
    switch (type) {
      case "EARN":
        return <Badge variant="default" className="bg-green-100 text-green-800">Earned</Badge>
      case "REDEEM":
        return <Badge variant="destructive">Redeemed</Badge>
      default:
        return <Badge variant="secondary">{type}</Badge>
    }
  }

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-6xl">
        <div className="text-center py-8">Loading loyalty data...</div>
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
            <h1 className="text-3xl font-bold text-foreground">My Loyalty Points</h1>
            <p className="text-muted-foreground mt-2">Track your points earnings, redemptions, and rewards</p>
          </div>
        </div>
        <Button variant="outline">
          <Download className="h-4 w-4 mr-2" />
          Export History
        </Button>
      </div>

      {/* Points Overview */}
      {loyaltyAccount ? (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <Card>
            <CardHeader className="text-center">
              <div className="mx-auto w-12 h-12 bg-primary/10 rounded-full flex items-center justify-center mb-2">
                <Star className="h-6 w-6 text-primary" />
              </div>
              <CardTitle className="text-2xl">{loyaltyAccount.balance?.toLocaleString() || '0'}</CardTitle>
              <CardDescription>Current Balance</CardDescription>
            </CardHeader>
          </Card>

          <Card>
            <CardHeader className="text-center">
              <div className="mx-auto w-12 h-12 bg-green-100 rounded-full flex items-center justify-center mb-2">
                <TrendingUp className="h-6 w-6 text-green-600" />
              </div>
              <CardTitle className="text-2xl">{totalPointsEarned.toLocaleString()}</CardTitle>
              <CardDescription>Total Earned</CardDescription>
            </CardHeader>
          </Card>

          <Card>
            <CardHeader className="text-center">
              <div className="mx-auto w-12 h-12 bg-red-100 rounded-full flex items-center justify-center mb-2">
                <Award className="h-6 w-6 text-red-600" />
              </div>
              <CardTitle className="text-2xl">{totalPointsRedeemed.toLocaleString()}</CardTitle>
              <CardDescription>Total Redeemed</CardDescription>
            </CardHeader>
          </Card>
        </div>
      ) : (
        <div className="text-center py-8 mb-8">
          <Star className="h-16 w-16 mx-auto text-muted-foreground mb-4" />
          <h3 className="text-xl font-semibold">No Loyalty Account Found</h3>
          <p className="text-muted-foreground mt-2">You don&apos;t have a loyalty account yet. Make a reservation to start earning points!</p>
        </div>
      )}

      <Tabs defaultValue="transactions" className="space-y-6">
        <TabsList>
          <TabsTrigger value="transactions">Transaction History</TabsTrigger>
          <TabsTrigger value="rewards">Available Rewards</TabsTrigger>
        </TabsList>

        <TabsContent value="transactions" className="space-y-6">
          {/* Filters */}
          <div className="flex gap-4">
            <Select value={selectedPeriod} onValueChange={setSelectedPeriod}>
              <SelectTrigger className="w-48">
                <SelectValue placeholder="Select period" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Time</SelectItem>
                <SelectItem value="month">This Month</SelectItem>
                <SelectItem value="quarter">This Quarter</SelectItem>
                <SelectItem value="year">This Year</SelectItem>
              </SelectContent>
            </Select>

            <Select value={selectedType} onValueChange={setSelectedType}>
              <SelectTrigger className="w-48">
                <SelectValue placeholder="Select type" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Transactions</SelectItem>
                <SelectItem value="EARN">Earned Points</SelectItem>
                <SelectItem value="REDEEM">Redeemed Points</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* Transactions List */}
          <Card>
            <CardHeader>
              <CardTitle>Transaction History</CardTitle>
              <CardDescription>
                {filteredTransactions.length} transactions found
              </CardDescription>
            </CardHeader>
            <CardContent>
              {filteredTransactions.length === 0 ? (
                <div className="text-center py-8">
                  <Star className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
                  <h3 className="text-lg font-semibold">No transactions found</h3>
                  <p className="text-muted-foreground">Your transaction history will appear here.</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {filteredTransactions.map((transaction) => (
                    <div key={transaction.id} className="flex items-center justify-between p-4 border rounded-lg">
                      <div className="flex items-center gap-3">
                        {getTransactionIcon(transaction.type)}
                        <div>
                          <p className="font-medium">{transaction.type === "EARN" ? "Points Earned" : "Points Redeemed"}</p>
                          <p className="text-sm text-muted-foreground">
                            {new Date(transaction.createdAt).toLocaleDateString()} at{" "}
                            {new Date(transaction.createdAt).toLocaleTimeString()}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-3">
                        {getTransactionBadge(transaction.type)}
                        <div className="text-right">
                          <p className={`font-semibold ${
                            transaction.type === "EARN" ? "text-green-600" : "text-red-600"
                          }`}>
                            {transaction.type === "EARN" ? "+" : "-"}{Math.abs(transaction.points)}
                          </p>
                          <p className="text-sm text-muted-foreground">points</p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="rewards" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Available Rewards</CardTitle>
              <CardDescription>
                Redeem your loyalty points for exclusive rewards
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-center py-8">
                <Gift className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
                <h3 className="text-lg font-semibold">Rewards Coming Soon</h3>
                <p className="text-muted-foreground">We&apos;re working on exciting rewards for our loyal customers.</p>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}
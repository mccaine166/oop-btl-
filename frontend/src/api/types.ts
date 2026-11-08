export interface RevenueOverviewDto {
  totalRevenue: number
  ticketRevenue: number
  foodRevenue: number
  averageTransactionValue: number
  ticketRevenueShare: number
  foodRevenueShare: number
}

export interface DailyRevenueDto {
  date: string
  revenue: number
}

export interface MovieRevenueDto {
  movieId: string
  movieName: string
  revenue: number
}

export interface FnbSummaryDto {
  productId: string
  productName: string
  quantity: number
  revenue: number
  averageSalesPerDay: number
  consumptionRate: number
  daysOfStockRemaining: number | null
  productType?: 'SINGLE' | 'COMBO'
}

export interface InventorySummaryDto {
  productId: string
  productName: string
  currentStock: number
  consumptionRate: number
  daysOfStockRemaining: number | null
}

export type RecommendationType =
  | 'INCREASE_SHOWTIME'
  | 'DECREASE_SHOWTIME'
  | 'RESTOCK'

export interface RecommendationDto {
  id: string
  type: RecommendationType
  targetId: string
  targetName: string
  action: string
  reason: string
  analysisWeekStart: string
  analysisWeekEnd: string
  applyWeekStart: string
  applyWeekEnd: string
  confidence: number
  priority: number
}

export interface ApiError {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}

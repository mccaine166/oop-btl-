import type {
  ApiError,
  DailyRevenueDto,
  FnbSummaryDto,
  InventorySummaryDto,
  MovieRevenueDto,
  RecommendationDto,
  RevenueOverviewDto,
} from './types'

export class ApiRequestError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = status
  }
}

function getBaseUrl(): string {
  return (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/+$/, '')
}

function buildApiUrl(path: string): string {
  const baseUrl = getBaseUrl()
  return baseUrl ? `${baseUrl}${path}` : path
}

function buildWeekUrl(path: string, weekStart: string, weekEnd: string): string {
  const params = new URLSearchParams({
    weekStart,
    weekEnd,
  })

  return `${buildApiUrl(path)}?${params.toString()}`
}

async function parseErrorMessage(response: Response): Promise<string> {
  const rawText = await response.text()
  if (!rawText) {
    return 'Không kết nối được máy chủ'
  }

  try {
    const payload = JSON.parse(rawText) as Partial<ApiError>
    if (typeof payload.message === 'string' && payload.message.trim().length > 0) {
      return payload.message
    }
  } catch {
    // Ignore invalid JSON and fall back to the default server message.
  }

  return 'Không kết nối được máy chủ'
}

async function requestJson<T>(url: string, signal?: AbortSignal): Promise<T> {
  try {
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        Accept: 'application/json',
      },
      signal,
    })

    if (!response.ok) {
      const message = await parseErrorMessage(response)
      throw new ApiRequestError(response.status, message)
    }

    const contentType = response.headers.get('content-type') ?? ''
    if (contentType.includes('application/json')) {
      return (await response.json()) as T
    }

    return undefined as T
  } catch (error) {
    if (error instanceof ApiRequestError || (error instanceof DOMException && error.name === 'AbortError')) {
      throw error
    }

    throw new Error('Không kết nối được máy chủ')
  }
}

export async function getRevenue(
  weekStart: string,
  weekEnd: string,
  signal?: AbortSignal,
): Promise<RevenueOverviewDto> {
  return requestJson<RevenueOverviewDto>(buildWeekUrl('/api/analytics/revenue', weekStart, weekEnd), signal)
}

export async function getDailyRevenue(
  weekStart: string,
  weekEnd: string,
  signal?: AbortSignal,
): Promise<DailyRevenueDto[]> {
  return requestJson<DailyRevenueDto[]>(buildWeekUrl('/api/analytics/revenue/daily', weekStart, weekEnd), signal)
}

export async function getMovieRevenue(
  weekStart: string,
  weekEnd: string,
  signal?: AbortSignal,
): Promise<MovieRevenueDto[]> {
  return requestJson<MovieRevenueDto[]>(buildWeekUrl('/api/analytics/revenue/movie', weekStart, weekEnd), signal)
}

export async function getFnb(
  weekStart: string,
  weekEnd: string,
  signal?: AbortSignal,
): Promise<FnbSummaryDto[]> {
  return requestJson<FnbSummaryDto[]>(buildWeekUrl('/api/analytics/fnb', weekStart, weekEnd), signal)
}

export async function getInventory(
  weekStart: string,
  weekEnd: string,
  signal?: AbortSignal,
): Promise<InventorySummaryDto[]> {
  return requestJson<InventorySummaryDto[]>(buildWeekUrl('/api/analytics/inventory', weekStart, weekEnd), signal)
}

export async function getShowtimeRecommendations(
  weekStart: string,
  weekEnd: string,
  signal?: AbortSignal,
): Promise<RecommendationDto[]> {
  return requestJson<RecommendationDto[]>(buildWeekUrl('/api/recommendations/showtimes', weekStart, weekEnd), signal)
}

export async function getInventoryRecommendations(
  weekStart: string,
  weekEnd: string,
  signal?: AbortSignal,
): Promise<RecommendationDto[]> {
  return requestJson<RecommendationDto[]>(buildWeekUrl('/api/recommendations/inventory', weekStart, weekEnd), signal)
}

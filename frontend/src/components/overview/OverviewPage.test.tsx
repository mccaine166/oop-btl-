import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError, getDailyRevenue, getMovieRevenue, getRevenue } from '../../api/client'
import { OverviewPage } from './OverviewPage'

vi.mock('../../api/client', () => ({
  ApiRequestError: class ApiRequestError extends Error {
    status: number

    constructor(status: number, message: string) {
      super(message)
      this.name = 'ApiRequestError'
      this.status = status
    }
  },
  getRevenue: vi.fn(),
  getDailyRevenue: vi.fn(),
  getMovieRevenue: vi.fn(),
}))

describe('OverviewPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders KPI values in VND format from API data', async () => {
    vi.mocked(getRevenue).mockResolvedValue({
      totalRevenue: 1234567,
      ticketRevenue: 500000,
      foodRevenue: 734567,
      averageTransactionValue: 245000,
      ticketRevenueShare: 40.5,
      foodRevenueShare: 59.5,
    })

    vi.mocked(getDailyRevenue).mockResolvedValue([
      { date: '2026-09-14', revenue: 200000 },
      { date: '2026-09-15', revenue: 300000 },
      { date: '2026-09-16', revenue: 0 },
      { date: '2026-09-17', revenue: 0 },
      { date: '2026-09-18', revenue: 500000 },
      { date: '2026-09-19', revenue: 0 },
      { date: '2026-09-20', revenue: 0 },
    ])

    vi.mocked(getMovieRevenue).mockResolvedValue([
      { movieId: 'movie-1', movieName: 'Movie 1', revenue: 700000 },
      { movieId: 'movie-2', movieName: 'Movie 2', revenue: 300000 },
    ])

    render(<OverviewPage weekStart="2026-09-14" />)

    await waitFor(() => {
      expect(screen.getByText(/1\.234\.567/)).toBeInTheDocument()
    })

    const ticketCard = screen.getByLabelText('Doanh thu vé')
    expect(within(ticketCard).getByText(/40\.50%/)).toBeInTheDocument()

    const foodCard = screen.getByLabelText('Doanh thu F&B')
    expect(within(foodCard).getByText(/59\.50%/)).toBeInTheDocument()
  })

  it('shows an empty state when all values are zero or empty', async () => {
    vi.mocked(getRevenue).mockResolvedValue({
      totalRevenue: 0,
      ticketRevenue: 0,
      foodRevenue: 0,
      averageTransactionValue: 0,
      ticketRevenueShare: 0,
      foodRevenueShare: 0,
    })
    vi.mocked(getDailyRevenue).mockResolvedValue([])
    vi.mocked(getMovieRevenue).mockResolvedValue([])

    render(<OverviewPage weekStart="2026-09-14" />)

    await waitFor(() => {
      expect(screen.getByText(/Không có dữ liệu trong khoảng thời gian này/i)).toBeInTheDocument()
    })
  })

  it('shows API error and retry triggers a new request', async () => {
    const error = new ApiRequestError(400, 'Invalid date format. Expected yyyy-MM-dd')
    vi.mocked(getRevenue).mockRejectedValue(error)
    vi.mocked(getDailyRevenue).mockResolvedValue([])
    vi.mocked(getMovieRevenue).mockResolvedValue([])

    render(<OverviewPage weekStart="2026-09-14" />)

    await waitFor(() => {
      expect(screen.getByText('Invalid date format. Expected yyyy-MM-dd')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }))

    await waitFor(() => {
      expect(getRevenue).toHaveBeenCalledTimes(2)
    })
  })
})

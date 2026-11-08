import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { RecommendationPage } from './RecommendationPage'

vi.mock('../../api/client', () => ({
  getShowtimeRecommendations: vi.fn(),
  getInventoryRecommendations: vi.fn(),
}))

import { getInventoryRecommendations, getShowtimeRecommendations } from '../../api/client'

describe('RecommendationPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders two cards for same movie across different weeks and expands the reason text', async () => {
    vi.mocked(getShowtimeRecommendations).mockResolvedValue([
      {
        id: 'showtime-increase-movie-2-1900-2200-2026-09-14',
        type: 'INCREASE_SHOWTIME',
        targetId: 'movie-2',
        targetName: 'Movie 2',
        action: 'Tăng 2 suất phim Movie 2 khung 19:00–22:00',
        reason: 'Phim có occupancy cao trong khung giờ này và cần thêm suất chiếu.',
        analysisWeekStart: '2026-09-14',
        analysisWeekEnd: '2026-09-20',
        applyWeekStart: '2026-09-21',
        applyWeekEnd: '2026-09-27',
        confidence: 0.63,
        priority: 5,
      },
      {
        id: 'showtime-increase-movie-2-1900-2200-2026-09-21',
        type: 'INCREASE_SHOWTIME',
        targetId: 'movie-2',
        targetName: 'Movie 2',
        action: 'Tăng 1 suất phim Movie 2 khung 19:00–22:00',
        reason: 'Tuần sau vẫn cần tăng thêm suất trong khung cao điểm.',
        analysisWeekStart: '2026-09-21',
        analysisWeekEnd: '2026-09-27',
        applyWeekStart: '2026-09-28',
        applyWeekEnd: '2026-10-04',
        confidence: 0.58,
        priority: 4,
      },
    ])

    render(<RecommendationPage weekStart="2026-09-14" />)

    await waitFor(() => {
      expect(screen.getAllByText('Tăng 2 suất phim Movie 2 khung 19:00–22:00').length).toBeGreaterThan(0)
    })

    expect(screen.getByText('63%')).toBeInTheDocument()
    expect(screen.getAllByText('Độ tin cậy').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Movie 2').length).toBeGreaterThan(0)

    fireEvent.click(screen.getAllByRole('button', { name: 'Xem thêm' })[0])
    expect(screen.getByText('Phim có occupancy cao trong khung giờ này và cần thêm suất chiếu.')).toBeInTheDocument()
  })

  it('only calls the currently active tab API, and inventory tab loads only when selected', async () => {
    vi.mocked(getShowtimeRecommendations).mockResolvedValue([])
    vi.mocked(getInventoryRecommendations).mockResolvedValue([])

    render(<RecommendationPage weekStart="2026-09-14" />)

    await waitFor(() => {
      expect(getShowtimeRecommendations).toHaveBeenCalledTimes(1)
    })

    expect(getInventoryRecommendations).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('tab', { name: 'Nhập hàng' }))

    await waitFor(() => {
      expect(getInventoryRecommendations).toHaveBeenCalledTimes(1)
    })

    expect(getShowtimeRecommendations).toHaveBeenCalledTimes(1)
  })

  it('shows the correct showtime filter counts and empty state text', async () => {
    vi.mocked(getShowtimeRecommendations).mockResolvedValue([
      {
        id: '1',
        type: 'INCREASE_SHOWTIME',
        targetId: 'movie-1',
        targetName: 'Movie 1',
        action: 'Tăng 1 suất phim Movie 1',
        reason: 'Có nhu cầu cao.',
        analysisWeekStart: '2026-09-14',
        analysisWeekEnd: '2026-09-20',
        applyWeekStart: '2026-09-21',
        applyWeekEnd: '2026-09-27',
        confidence: 0.7,
        priority: 3,
      },
      {
        id: '2',
        type: 'DECREASE_SHOWTIME',
        targetId: 'movie-2',
        targetName: 'Movie 2',
        action: 'Giảm 1 suất phim Movie 2',
        reason: 'Khung giờ ít khách.',
        analysisWeekStart: '2026-09-14',
        analysisWeekEnd: '2026-09-20',
        applyWeekStart: '2026-09-21',
        applyWeekEnd: '2026-09-27',
        confidence: 0.5,
        priority: 2,
      },
    ])

    render(<RecommendationPage weekStart="2026-09-14" />)

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Tất cả \(2\)/i })).toBeInTheDocument()
    })

    expect(screen.getByRole('button', { name: /Tăng suất \(1\)/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Giảm suất \(1\)/i })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /Giảm suất \(1\)/i }))
    expect(screen.getByText('Giảm 1 suất phim Movie 2')).toBeInTheDocument()
    expect(screen.queryByText('Tăng 1 suất phim Movie 1')).not.toBeInTheDocument()
  })
})

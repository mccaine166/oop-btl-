import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

describe('App', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('keeps the correct weekStart when moving to the next week', async () => {
    const fetchMock = vi.mocked(globalThis.fetch)
    fetchMock.mockImplementation(async (input: RequestInfo | URL) => {
      const url = String(input)
      const payload = {
        totalRevenue: 0,
        ticketRevenue: 0,
        foodRevenue: 0,
        averageTransactionValue: 0,
        ticketRevenueShare: 0,
        foodRevenueShare: 0,
      }

      if (url.includes('/api/analytics/revenue')) {
        return {
          ok: true,
          status: 200,
          headers: new Headers({ 'content-type': 'application/json' }),
          json: async () => payload,
          text: async () => JSON.stringify(payload),
        } as Response
      }

      if (url.includes('/api/analytics/revenue/daily')) {
        return {
          ok: true,
          status: 200,
          headers: new Headers({ 'content-type': 'application/json' }),
          json: async () => [],
          text: async () => '[]',
        } as Response
      }

      if (url.includes('/api/analytics/revenue/movie')) {
        return {
          ok: true,
          status: 200,
          headers: new Headers({ 'content-type': 'application/json' }),
          json: async () => [],
          text: async () => '[]',
        } as Response
      }

      return {
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => [],
        text: async () => '[]',
      } as Response
    })

    render(<App />)

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalled()
    })

    const nextButton = screen.getByLabelText('Tuần sau')
    fireEvent.click(nextButton)

    await waitFor(() => {
      const usesNextWeek = fetchMock.mock.calls.some(([url]) => String(url).includes('weekStart=2026-09-21'))
      expect(usesNextWeek).toBe(true)
    })
  })
})

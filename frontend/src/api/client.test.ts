import { renderHook, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { useApi } from '../hooks/useApi'
import { ApiRequestError, getRevenue } from './client'

describe('API client', () => {
  it('sends correct URL and query parameters', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: {
        get: () => 'application/json',
      },
      json: async () => ({ totalRevenue: 123456 }),
    })

    vi.stubGlobal('fetch', fetchMock)

    const response = await getRevenue('2026-09-14', '2026-09-20')

    expect(response).toEqual({ totalRevenue: 123456 })
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/analytics/revenue?weekStart=2026-09-14&weekEnd=2026-09-20',
      expect.objectContaining({ method: 'GET' }),
    )
  })

  it('throws ApiRequestError with backend message on 400', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 400,
        text: async () => JSON.stringify({ message: 'Missing required parameter: weekEnd' }),
        headers: {
          get: () => 'application/json',
        },
      }),
    )

    await expect(getRevenue('2026-09-14', '2026-09-20')).rejects.toMatchObject({
      name: 'ApiRequestError',
      status: 400,
      message: 'Missing required parameter: weekEnd',
    })
  })

  it('uses default message for network failures', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('failed to fetch')))

    await expect(getRevenue('2026-09-14', '2026-09-20')).rejects.toThrow(
      'Không kết nối được máy chủ',
    )
  })
})

describe('useApi hook', () => {
  it('ignores stale results from canceled requests', async () => {
    const fetcherMock = vi.fn(
      (week: string, signal?: AbortSignal) =>
        new Promise<{ week: string }>((resolve, reject) => {
          const timeoutId = window.setTimeout(() => {
            if (signal?.aborted) {
              reject(new DOMException('Aborted', 'AbortError'))
              return
            }
            resolve({ week })
          }, 10)

          signal?.addEventListener(
            'abort',
            () => {
              window.clearTimeout(timeoutId)
              reject(new DOMException('Aborted', 'AbortError'))
            },
            { once: true },
          )
        }),
    )

    const { result, rerender } = renderHook(
      ({ week }) => useApi((signal) => fetcherMock(week, signal), [week]),
      {
        initialProps: { week: '2026-09-14' },
      },
    )

    rerender({ week: '2026-09-21' })

    await waitFor(() => {
      expect(fetcherMock).toHaveBeenCalledTimes(2)
    })

    await waitFor(() => {
      expect(result.current.data).toEqual({ week: '2026-09-21' })
    })
    expect(result.current.error).toBeNull()
  })
})

export { ApiRequestError }

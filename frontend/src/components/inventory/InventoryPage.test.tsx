import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { InventoryPage } from './InventoryPage'

describe('InventoryPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  function mockInventory(payload: unknown, recommendations: unknown = []) {
    const fetchMock = vi.mocked(globalThis.fetch)
    fetchMock.mockImplementation(async (input: RequestInfo | URL) => {
      const url = String(input)

      if (url.includes('/api/analytics/inventory')) {
        return {
          ok: true,
          status: 200,
          headers: new Headers({ 'content-type': 'application/json' }),
          json: async () => payload,
          text: async () => JSON.stringify(payload),
        } as Response
      }

      if (url.includes('/api/recommendations/inventory')) {
        return {
          ok: true,
          status: 200,
          headers: new Headers({ 'content-type': 'application/json' }),
          json: async () => recommendations,
          text: async () => JSON.stringify(recommendations),
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
  }

  it('renders alert badges at the correct thresholds and null values', async () => {
    mockInventory([
      { productId: 'p1', productName: 'Combo A', currentStock: 12, consumptionRate: 5, daysOfStockRemaining: 2 },
      { productId: 'p2', productName: 'Combo B', currentStock: 30, consumptionRate: 4, daysOfStockRemaining: 3 },
      { productId: 'p3', productName: 'Popcorn', currentStock: 40, consumptionRate: 4, daysOfStockRemaining: 7 },
      { productId: 'p4', productName: 'Hotdog', currentStock: 50, consumptionRate: 2, daysOfStockRemaining: 9 },
      { productId: 'p5', productName: 'Unknown', currentStock: 20, consumptionRate: 0, daysOfStockRemaining: null },
    ])

    render(<InventoryPage weekStart="2026-09-14" />)

    await waitFor(() => expect(screen.getByText('Combo A')).toBeInTheDocument())

    expect(screen.getAllByText('Sắp hết').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Cần theo dõi').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Ổn định').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Chưa có dữ liệu').length).toBeGreaterThan(0)
  })

  it('matches recommendations to the correct product row and expands the reason', async () => {
    mockInventory([
      { productId: 'p1', productName: 'Combo A', currentStock: 10, consumptionRate: 4, daysOfStockRemaining: 2 },
      { productId: 'p2', productName: 'Combo B', currentStock: 20, consumptionRate: 3, daysOfStockRemaining: 6 },
    ], [
      {
        id: 'restock-1',
        type: 'RESTOCK',
        targetId: 'p2',
        targetName: 'Combo B',
        action: 'Nhập thêm 12 Combo B',
        reason: 'Cần nhập thêm 12 Combo B vì tồn kho thấp.',
        analysisWeekStart: '2026-09-14',
        analysisWeekEnd: '2026-09-20',
        applyWeekStart: '2026-09-21',
        applyWeekEnd: '2026-09-27',
        confidence: 0.8,
        priority: 1,
      },
    ])

    render(<InventoryPage weekStart="2026-09-14" />)

    await waitFor(() => expect(screen.getByText('Nhập thêm 12 Combo B')).toBeInTheDocument())

    expect(screen.queryByText('Cần nhập thêm 12 Combo B vì tồn kho thấp.')).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Nhập thêm 12 Combo B' }))

    expect(screen.getByText('Cần nhập thêm 12 Combo B vì tồn kho thấp.')).toBeInTheDocument()
    expect(screen.queryByText('Nhập thêm 12 Combo B')).toBeInTheDocument()
  })

  it('ignores recommendation rows whose targetId is not present in the inventory list', async () => {
    mockInventory([
      { productId: 'p1', productName: 'Combo A', currentStock: 10, consumptionRate: 4, daysOfStockRemaining: 2 },
    ], [
      {
        id: 'restock-1',
        type: 'RESTOCK',
        targetId: 'missing',
        targetName: 'Missing Product',
        action: 'Nhập thêm 99 Missing Product',
        reason: 'Không nên hiển thị vì không có sản phẩm tương ứng.',
        analysisWeekStart: '2026-09-14',
        analysisWeekEnd: '2026-09-20',
        applyWeekStart: '2026-09-21',
        applyWeekEnd: '2026-09-27',
        confidence: 0.7,
        priority: 2,
      },
    ])

    render(<InventoryPage weekStart="2026-09-14" />)

    await waitFor(() => expect(screen.getByText('Combo A')).toBeInTheDocument())

    expect(screen.queryByText('Nhập thêm 99 Missing Product')).not.toBeInTheDocument()
    expect(screen.queryByText('Không nên hiển thị vì không có sản phẩm tương ứng.')).not.toBeInTheDocument()
  })
})

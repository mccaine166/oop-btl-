import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { FnbPage } from './FnbPage'

describe('FnbPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  function mockFetchResponse(payload: unknown) {
    const fetchMock = vi.mocked(globalThis.fetch)
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => payload,
      text: async () => JSON.stringify(payload),
    } as Response)
  }

  it('sorts rows by quantity then by revenue when clicked', async () => {
    mockFetchResponse([
      {
        productId: 'prod-1',
        productName: 'Combo A',
        quantity: 120,
        revenue: 600000,
        averageSalesPerDay: 17,
        consumptionRate: 8,
        daysOfStockRemaining: 4,
        productType: 'COMBO',
      },
      {
        productId: 'prod-2',
        productName: 'Popcorn',
        quantity: 60,
        revenue: 900000,
        averageSalesPerDay: 8,
        consumptionRate: 3,
        daysOfStockRemaining: null,
        productType: 'SINGLE',
      },
      {
        productId: 'prod-3',
        productName: 'Soft Drink',
        quantity: 30,
        revenue: 200000,
        averageSalesPerDay: 4,
        consumptionRate: 2,
        daysOfStockRemaining: 7,
        productType: 'SINGLE',
      },
    ])

    render(<FnbPage weekStart="2026-09-14" />)

    await waitFor(() => expect(screen.getByText('Combo A')).toBeInTheDocument())

    const table = screen.getByRole('table')
    const rows = within(table).getAllByRole('row')
    expect(rows[1]).toHaveTextContent('Combo A')
    expect(rows[2]).toHaveTextContent('Popcorn')

    fireEvent.click(screen.getByRole('button', { name: 'Doanh thu' }))

    await waitFor(() => {
      const sortedRows = within(screen.getByRole('table')).getAllByRole('row')
      expect(sortedRows[1]).toHaveTextContent('Popcorn')
      expect(sortedRows[2]).toHaveTextContent('Combo A')
    })
  })

  it('renders a dash and tooltip text when days of stock is null', async () => {
    mockFetchResponse([
      {
        productId: 'prod-1',
        productName: 'Combo A',
        quantity: 100,
        revenue: 500000,
        averageSalesPerDay: 14,
        consumptionRate: 7,
        daysOfStockRemaining: null,
      },
    ])

    render(<FnbPage weekStart="2026-09-14" />)

    await waitFor(() => expect(screen.getByText('—')).toBeInTheDocument())

    const value = screen.getByLabelText('Chưa có dữ liệu tiêu thụ')
    expect(value).toHaveAttribute('title', 'Chưa có dữ liệu tiêu thụ')
  })

  it('hides the product type filter when productType is missing', async () => {
    mockFetchResponse([
      {
        productId: 'prod-1',
        productName: 'Combo A',
        quantity: 50,
        revenue: 300000,
        averageSalesPerDay: 7,
        consumptionRate: 5,
        daysOfStockRemaining: 3,
      },
    ])

    render(<FnbPage weekStart="2026-09-14" />)

    await waitFor(() => expect(screen.getByText('Combo A')).toBeInTheDocument())

    expect(screen.queryByLabelText('Bộ lọc loại sản phẩm')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Combo' })).not.toBeInTheDocument()
  })

  it('shows and applies the product type filter when productType is available', async () => {
    mockFetchResponse([
      {
        productId: 'prod-1',
        productName: 'Combo A',
        quantity: 50,
        revenue: 300000,
        averageSalesPerDay: 7,
        consumptionRate: 5,
        daysOfStockRemaining: 3,
        productType: 'COMBO',
      },
      {
        productId: 'prod-2',
        productName: 'Popcorn',
        quantity: 30,
        revenue: 200000,
        averageSalesPerDay: 4,
        consumptionRate: 2,
        daysOfStockRemaining: 6,
        productType: 'SINGLE',
      },
    ])

    render(<FnbPage weekStart="2026-09-14" />)

    await waitFor(() => expect(screen.getByRole('button', { name: 'Combo' })).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: 'Combo' }))

    expect(screen.getByText('Combo A')).toBeInTheDocument()
    expect(screen.queryByText('Popcorn')).not.toBeInTheDocument()
  })
})

import { useMemo, useState } from 'react'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { getFnb } from '../../api/client'
import type { FnbSummaryDto } from '../../api/types'
import { useApi } from '../../hooks/useApi'
import { weekRange } from '../../utils/week'

const currencyFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
})

type SortKey = 'quantity' | 'revenue' | 'averageSalesPerDay' | 'consumptionRate' | 'daysOfStockRemaining' | 'productName'
type ProductFilter = 'all' | 'COMBO' | 'SINGLE'

function formatCurrency(value: number): string {
  return currencyFormatter.format(value)
}

function compareValues(a: number | null, b: number | null): number {
  if (a === null && b === null) {
    return 0
  }

  if (a === null) {
    return 1
  }

  if (b === null) {
    return -1
  }

  return a - b
}

function FnbTable({
  rows,
  sortKey,
  sortDirection,
  onSort,
}: {
  rows: FnbSummaryDto[]
  sortKey: SortKey
  sortDirection: 'asc' | 'desc'
  onSort: (key: SortKey) => void
}) {
  const getSortIndicator = (key: SortKey) =>
    sortKey === key ? (sortDirection === 'asc' ? ' ↑' : ' ↓') : ''

  return (
    <div className="table-wrapper">
      <table className="fnb-table">
        <thead>
          <tr>
            <th>
              <button type="button" onClick={() => onSort('productName')}>
                Sản phẩm{getSortIndicator('productName')}
              </button>
            </th>
            <th>
              <button type="button" onClick={() => onSort('quantity')}>
                Số lượng bán{getSortIndicator('quantity')}
              </button>
            </th>
            <th>
              <button type="button" onClick={() => onSort('revenue')}>
                Doanh thu{getSortIndicator('revenue')}
              </button>
            </th>
            <th>
              <button type="button" onClick={() => onSort('averageSalesPerDay')}>
                TB/ngày{getSortIndicator('averageSalesPerDay')}
              </button>
            </th>
            <th>
              <button type="button" onClick={() => onSort('consumptionRate')}>
                Tốc độ tiêu thụ{getSortIndicator('consumptionRate')}
              </button>
            </th>
            <th>
              <button type="button" onClick={() => onSort('daysOfStockRemaining')}>
                Số ngày tồn còn lại{getSortIndicator('daysOfStockRemaining')}
              </button>
            </th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.productId}>
              <td>{row.productName}</td>
              <td>{row.quantity}</td>
              <td>{formatCurrency(row.revenue)}</td>
              <td>{row.averageSalesPerDay}</td>
              <td>{row.consumptionRate}</td>
              <td>
                {row.daysOfStockRemaining === null ? (
                  <span title="Chưa có dữ liệu tiêu thụ" aria-label="Chưa có dữ liệu tiêu thụ">
                    —
                  </span>
                ) : (
                  row.daysOfStockRemaining
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

interface FnbPageProps {
  weekStart: string
}

export function FnbPage({ weekStart }: FnbPageProps) {
  const { weekEnd } = weekRange(weekStart)
  const { data, loading, error, reload } = useApi<FnbSummaryDto[]>(async (signal) => {
    return getFnb(weekStart, weekEnd, signal)
  }, [weekStart, weekEnd])

  const [sortKey, setSortKey] = useState<SortKey>('quantity')
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc')
  const [filter, setFilter] = useState<ProductFilter>('all')
  const [chartMetric, setChartMetric] = useState<'quantity' | 'revenue'>('quantity')

  const hasProductType = Array.isArray(data) && data.some((item) => item.productType !== undefined)

  const sortedRows = useMemo(() => {
    const base = Array.isArray(data) ? [...data] : []
    const filtered = hasProductType
      ? base.filter((item) => filter === 'all' || item.productType === filter)
      : base

    return filtered.sort((left, right) => {
      const sortMultiplier = sortDirection === 'asc' ? 1 : -1

      if (sortKey === 'productName') {
        return left.productName.localeCompare(right.productName) * sortMultiplier
      }

      const leftValue = sortKey === 'daysOfStockRemaining'
        ? left.daysOfStockRemaining
        : left[sortKey]
      const rightValue = sortKey === 'daysOfStockRemaining'
        ? right.daysOfStockRemaining
        : right[sortKey]

      if (typeof leftValue === 'number' && typeof rightValue === 'number') {
        return (leftValue - rightValue) * sortMultiplier
      }

      return compareValues(leftValue as number | null, rightValue as number | null) * sortMultiplier
    })
  }, [data, filter, hasProductType, sortDirection, sortKey])

  const chartData = [...sortedRows]
    .slice(0, 6)
    .map((item) => ({
      name: item.productName,
      value: chartMetric === 'quantity' ? item.quantity : item.revenue,
    }))

  const handleSort = (nextKey: SortKey) => {
    if (sortKey === nextKey) {
      setSortDirection((current) => (current === 'asc' ? 'desc' : 'asc'))
      return
    }

    setSortKey(nextKey)
    setSortDirection(nextKey === 'productName' ? 'asc' : 'desc')
  }

  if (loading && !data) {
    return (
      <section className="panel" aria-live="polite" aria-busy="true">
        <div className="panel-header">
          <h2>F&B</h2>
          <span>Đang tải...</span>
        </div>
        <p className="placeholder-text">Đang tải dữ liệu F&B...</p>
      </section>
    )
  }

  if (error && !data) {
    return (
      <section className="panel" aria-live="polite">
        <div className="panel-header">
          <h2>F&B</h2>
        </div>
        <div className="overview-state error-state" role="alert">
          <p>{error}</p>
          <button type="button" onClick={() => void reload()}>
            Thử lại
          </button>
        </div>
      </section>
    )
  }

  if (!data || data.length === 0) {
    return (
      <section className="panel" aria-live="polite">
        <div className="panel-header">
          <h2>F&B</h2>
        </div>
        <div className="overview-state empty-state">
          <p>Không có dữ liệu F&B trong khoảng thời gian này.</p>
        </div>
      </section>
    )
  }

  return (
    <section className={loading ? 'panel fnb-page stale-loading' : 'panel fnb-page'} aria-live="polite" aria-busy={loading}>
      <div className="panel-header">
        <h2>F&B</h2>
        <span>{weekStart} → {weekEnd}</span>
      </div>

      {hasProductType ? (
        <div className="fnb-filters" aria-label="Bộ lọc loại sản phẩm">
          {(['all', 'COMBO', 'SINGLE'] as const).map((option) => (
            <button
              key={option}
              type="button"
              className={filter === option ? 'filter-chip active' : 'filter-chip'}
              onClick={() => setFilter(option)}
            >
              {option === 'all' ? 'Tất cả' : option === 'COMBO' ? 'Combo' : 'Món lẻ'}
            </button>
          ))}
        </div>
      ) : null}

      <div className="chart-card fnb-chart-card" role="img" aria-label="Biểu đồ top sản phẩm F&B">
        <h3>Top sản phẩm</h3>
        <div className="chart-toggle" aria-label="Chọn chỉ số top sản phẩm">
          <button
            type="button"
            className={chartMetric === 'quantity' ? 'toggle active' : 'toggle'}
            onClick={() => setChartMetric('quantity')}
          >
            Theo số lượng
          </button>
          <button
            type="button"
            className={chartMetric === 'revenue' ? 'toggle active' : 'toggle'}
            onClick={() => setChartMetric('revenue')}
          >
            Theo doanh thu
          </button>
        </div>

        <ResponsiveContainer width="100%" height={260}>
          <BarChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" tick={{ fontSize: 12 }} />
            <YAxis tickFormatter={(value) => (
              chartMetric === 'quantity' ? String(value) : `${Math.round(Number(value) / 1000)}k`
            )} />
            <Tooltip
              formatter={(value: number | string) => [
                chartMetric === 'quantity' ? Number(value).toString() : formatCurrency(Number(value)),
                chartMetric === 'quantity' ? 'Số lượng' : 'Doanh thu',
              ]}
            />
            <Bar dataKey="value" fill="#10b981" radius={[6, 6, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>

      <FnbTable rows={sortedRows} sortKey={sortKey} sortDirection={sortDirection} onSort={handleSort} />
    </section>
  )
}

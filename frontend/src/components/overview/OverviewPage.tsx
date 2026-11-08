import { Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import {
  getDailyRevenue,
  getMovieRevenue,
  getRevenue,
} from '../../api/client'
import type { DailyRevenueDto, MovieRevenueDto, RevenueOverviewDto } from '../../api/types'
import { useApi } from '../../hooks/useApi'
import { weekRange } from '../../utils/week'

const moneyFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
})

const weekdayMap = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']

function formatCurrency(value: number): string {
  return moneyFormatter.format(value)
}

function formatDayLabel(date: string): string {
  const current = new Date(`${date}T00:00:00`)
  return `${weekdayMap[current.getDay()]} ${String(current.getDate()).padStart(2, '0')}/${String(current.getMonth() + 1).padStart(2, '0')}`
}

function renderKpiCard(label: string, value: string, note: string) {
  return (
    <div className="kpi-card" aria-label={label}>
      <p>{label}</p>
      <strong>{value}</strong>
      <span>{note}</span>
    </div>
  )
}

interface OverviewPageProps {
  weekStart: string
}

type OverviewData = {
  revenue: RevenueOverviewDto
  daily: DailyRevenueDto[]
  movie: MovieRevenueDto[]
}

export function OverviewPage({ weekStart }: OverviewPageProps) {
  const { weekEnd } = weekRange(weekStart)

  const { data, loading, error, reload } = useApi<OverviewData>(async (signal) => {
    const [revenue, daily, movie] = await Promise.all([
      getRevenue(weekStart, weekEnd, signal),
      getDailyRevenue(weekStart, weekEnd, signal),
      getMovieRevenue(weekStart, weekEnd, signal),
    ])

    return { revenue, daily, movie }
  }, [weekStart, weekEnd])

  if (loading && !data) {
    return <div className="overview-state" aria-busy="true">Đang tải dữ liệu…</div>
  }

  if (error && !data) {
    return (
      <div className="overview-state error-state" role="alert">
        <p>{error}</p>
        <button type="button" onClick={() => void reload()}>
          Thử lại
        </button>
      </div>
    )
  }

  if (!data) {
    return <div className="overview-state">Không có dữ liệu.</div>
  }

  const { revenue, daily, movie } = data
  const dailyData = Array.isArray(daily) ? daily : []
  const movieData = Array.isArray(movie) ? movie : []

  const hasAnyData =
    revenue.totalRevenue > 0 ||
    revenue.ticketRevenue > 0 ||
    revenue.foodRevenue > 0 ||
    dailyData.some((item) => item.revenue > 0) ||
    movieData.some((item) => item.revenue > 0)

  if (!hasAnyData) {
    return (
      <div className="overview-state empty-state">
        <p>Không có dữ liệu trong khoảng thời gian này.</p>
      </div>
    )
  }

  const dailySeries = Array.from({ length: 7 }, (_, index) => {
    const start = new Date(`${weekStart}T00:00:00`)
    const current = new Date(start)
    current.setDate(start.getDate() + index)
    const dateKey = current.toISOString().slice(0, 10)
    const match = dailyData.find((item) => item.date === dateKey)

    return {
      date: dateKey,
      label: formatDayLabel(dateKey),
      revenue: match?.revenue ?? 0,
    }
  })

  const movieSeries = [...movieData].sort((a, b) => b.revenue - a.revenue)
  const pieData = [
    { name: 'Vé', value: revenue.ticketRevenue, color: '#2563eb' },
    { name: 'F&B', value: revenue.foodRevenue, color: '#10b981' },
  ]
  const pieTotal = pieData.reduce((sum, item) => sum + item.value, 0) || 1

  return (
    <section className={loading ? 'overview-page stale-loading' : 'overview-page'} aria-label="Tổng quan doanh thu" aria-busy={loading}>
      <div className="kpi-grid">
        {renderKpiCard('Tổng doanh thu', formatCurrency(revenue.totalRevenue), 'Tổng quan tuần')}
        {renderKpiCard('Doanh thu vé', formatCurrency(revenue.ticketRevenue), `${revenue.ticketRevenueShare.toFixed(2)}%`)}
        {renderKpiCard('Doanh thu F&B', formatCurrency(revenue.foodRevenue), `${revenue.foodRevenueShare.toFixed(2)}%`)}
        {renderKpiCard('Giá trị giao dịch trung bình', formatCurrency(revenue.averageTransactionValue), 'Mỗi giao dịch')}
      </div>

      <div className="chart-grid">
        <div className="chart-card" role="img" aria-label="Biểu đồ doanh thu theo ngày">
          <h3>Doanh thu theo ngày</h3>
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={dailySeries}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="label" tick={{ fontSize: 12 }} />
              <YAxis tickFormatter={(value) => `${Math.round(Number(value) / 1000000)}M`} />
              <Tooltip formatter={(value: number | string) => [formatCurrency(Number(value)), 'Doanh thu']} />
              <Bar dataKey="revenue" fill="#2563eb" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="chart-card" role="img" aria-label="Biểu đồ doanh thu theo phim">
          <h3>Doanh thu theo phim</h3>
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={movieSeries} layout="vertical" margin={{ left: 20 }}>
              <XAxis type="number" tickFormatter={(value) => `${Math.round(Number(value) / 1000000)}M`} />
              <YAxis type="category" dataKey="movieName" width={90} />
              <Tooltip formatter={(value: number | string) => [formatCurrency(Number(value)), 'Doanh thu']} />
              <Bar dataKey="revenue" fill="#10b981" radius={[0, 6, 6, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="chart-card donut-card" role="img" aria-label="Biểu đồ tỉ trọng doanh thu vé và F&B">
        <h3>Tỉ trọng vé vs F&B</h3>
        <div className="donut-content">
          <ResponsiveContainer width="100%" height={260}>
            <PieChart>
              <Pie
                data={pieData}
                dataKey="value"
                nameKey="name"
                innerRadius={55}
                outerRadius={82}
                paddingAngle={3}
                label={({ name, percent }) => `${name} ${((percent ?? 0) * 100).toFixed(0)}%`}
              >
                {pieData.map((entry) => (
                  <Cell key={entry.name} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip formatter={(value: number | string) => [formatCurrency(Number(value)), 'Doanh thu']} />
            </PieChart>
          </ResponsiveContainer>

          <div className="legend">
            {pieData.map((entry) => (
              <div key={entry.name} className="legend-item">
                <span className="dot" style={{ background: entry.color }} />
                <span>{entry.name}</span>
                <strong>{((entry.value / pieTotal) * 100).toFixed(2)}%</strong>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}

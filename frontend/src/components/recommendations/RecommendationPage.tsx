import { useMemo, useState } from 'react'
import { getInventoryRecommendations, getShowtimeRecommendations } from '../../api/client'
import type { RecommendationDto, RecommendationType } from '../../api/types'
import { useApi } from '../../hooks/useApi'
import { weekRange } from '../../utils/week'

type RecommendationTab = 'showtimes' | 'inventory'
type RecommendationFilter = 'all' | 'INCREASE_SHOWTIME' | 'DECREASE_SHOWTIME'

const typeConfig: Record<RecommendationType, { label: string; className: string; icon: string }> = {
  INCREASE_SHOWTIME: { label: 'Tăng suất', className: 'badge increase', icon: '↑' },
  DECREASE_SHOWTIME: { label: 'Giảm suất', className: 'badge decrease', icon: '↓' },
  RESTOCK: { label: 'Nhập hàng', className: 'badge restock', icon: '＋' },
}

function formatShortDate(value: string): string {
  const date = new Date(`${value}T00:00:00`)
  return `${String(date.getDate()).padStart(2, '0')}/${String(date.getMonth() + 1).padStart(2, '0')}`
}

function formatDateRange(start: string, end: string): string {
  return `${formatShortDate(start)} – ${formatShortDate(end)}`
}

function formatPriority(priority: number): string {
  if (priority >= 3) {
    return 'Ưu tiên cao'
  }

  if (priority >= 2) {
    return 'Ưu tiên trung bình'
  }

  return 'Ưu tiên thấp'
}

interface RecommendationPageProps {
  weekStart: string
}

export function RecommendationPage({ weekStart }: RecommendationPageProps) {
  const { weekEnd } = weekRange(weekStart)
  const [activeTab, setActiveTab] = useState<RecommendationTab>('showtimes')
  const [filter, setFilter] = useState<RecommendationFilter>('all')
  const [expanded, setExpanded] = useState<Record<string, boolean>>({})

  const showtimeRequest = useApi<RecommendationDto[]>(async (signal) => {
    if (activeTab !== 'showtimes') {
      return []
    }

    return getShowtimeRecommendations(weekStart, weekEnd, signal)
  }, [activeTab, weekStart, weekEnd])

  const inventoryRequest = useApi<RecommendationDto[]>(async (signal) => {
    if (activeTab !== 'inventory') {
      return []
    }

    return getInventoryRecommendations(weekStart, weekEnd, signal)
  }, [activeTab, weekStart, weekEnd])

  const currentData = activeTab === 'showtimes' ? showtimeRequest.data : inventoryRequest.data
  const currentLoading = activeTab === 'showtimes' ? showtimeRequest.loading : inventoryRequest.loading
  const currentError = activeTab === 'showtimes' ? showtimeRequest.error : inventoryRequest.error
  const currentReload = activeTab === 'showtimes' ? showtimeRequest.reload : inventoryRequest.reload

  const visibleData = useMemo(() => {
    if (!Array.isArray(currentData)) {
      return []
    }

    const sorted = [...currentData].sort((left, right) => right.priority - left.priority)
    if (activeTab !== 'showtimes') {
      return sorted
    }

    if (filter === 'all') {
      return sorted
    }

    return sorted.filter((item) => item.type === filter)
  }, [activeTab, currentData, filter])

  const typeCounts = useMemo(() => {
    if (!Array.isArray(currentData)) {
      return { all: 0, INCREASE_SHOWTIME: 0, DECREASE_SHOWTIME: 0 }
    }

    return currentData.reduce(
      (accumulator, item) => {
        accumulator.all += 1
        if (item.type === 'INCREASE_SHOWTIME' || item.type === 'DECREASE_SHOWTIME') {
          accumulator[item.type] += 1
        }
        return accumulator
      },
      { all: 0, INCREASE_SHOWTIME: 0, DECREASE_SHOWTIME: 0 },
    )
  }, [currentData])

  if (currentLoading && !currentData) {
    return (
      <section className="panel" aria-live="polite" aria-busy="true">
        <div className="panel-header">
          <h2>Gợi ý</h2>
          <span>Đang tải...</span>
        </div>
        <p className="placeholder-text">Đang tải gợi ý...</p>
      </section>
    )
  }

  if (currentError && !currentData) {
    return (
      <section className="panel" aria-live="polite">
        <div className="panel-header">
          <h2>Gợi ý</h2>
        </div>
        <div className="overview-state error-state" role="alert">
          <p>{currentError}</p>
          <button type="button" onClick={() => void currentReload()}>
            Thử lại
          </button>
        </div>
      </section>
    )
  }

  return (
    <section className={currentLoading ? 'panel recommendation-page stale-loading' : 'panel recommendation-page'} aria-live="polite" aria-busy={currentLoading}>
      <div className="panel-header">
        <h2>Gợi ý</h2>
        <span>{weekStart} → {weekEnd}</span>
      </div>

      <div className="recommendation-tabs" role="tablist" aria-label="Chọn loại gợi ý">
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'showtimes'}
          className={activeTab === 'showtimes' ? 'recommendation-tab active' : 'recommendation-tab'}
          onClick={() => setActiveTab('showtimes')}
        >
          Suất chiếu
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'inventory'}
          className={activeTab === 'inventory' ? 'recommendation-tab active' : 'recommendation-tab'}
          onClick={() => setActiveTab('inventory')}
        >
          Nhập hàng
        </button>
      </div>

      {activeTab === 'showtimes' ? (
        <div className="filter-row" aria-label="Bộ lọc gợi ý suất chiếu">
          {([
            { key: 'all', label: 'Tất cả' },
            { key: 'INCREASE_SHOWTIME', label: 'Tăng suất' },
            { key: 'DECREASE_SHOWTIME', label: 'Giảm suất' },
          ] as const).map((option) => {
            const count = option.key === 'all' ? typeCounts.all : typeCounts[option.key]
            return (
              <button
                key={option.key}
                type="button"
                className={filter === option.key ? 'filter-chip active' : 'filter-chip'}
                onClick={() => setFilter(option.key)}
              >
                {option.label} ({count})
              </button>
            )
          })}
        </div>
      ) : null}

      {!visibleData.length ? (
        <div className="overview-state empty-state">
          <p>Chưa có gợi ý cho tuần này</p>
        </div>
      ) : (
        <div className="recommendation-list">
          {visibleData.map((recommendation) => {
            const config = typeConfig[recommendation.type as RecommendationType]
            const isExpanded = !!expanded[recommendation.id]
            const confidencePercent = Math.round(recommendation.confidence * 100)

            return (
              <article key={recommendation.id} className="recommendation-card">
                <div className="recommendation-header">
                  <span className={config.className}>
                    {config.icon} {config.label}
                  </span>
                  <span className="priority-label" aria-label="Mức ưu tiên">
                    {formatPriority(recommendation.priority)}
                  </span>
                </div>

                <h3>{recommendation.action}</h3>
                <p className="recommendation-target">{recommendation.targetName}</p>

                <div className="confidence-block" aria-label={`Độ tin cậy ${confidencePercent}%`}>
                  <div className="confidence-meta">
                    <span>Độ tin cậy</span>
                    <strong>{confidencePercent}%</strong>
                  </div>
                  <div className="progress-track" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={confidencePercent}>
                    <span style={{ width: `${confidencePercent}%` }} />
                  </div>
                </div>

                <div className="recommendation-text-wrap">
                  <p className={isExpanded ? 'recommendation-text expanded' : 'recommendation-text collapsed'}>
                    {recommendation.reason}
                  </p>
                  <button
                    type="button"
                    className="text-toggle"
                    onClick={() => setExpanded((current) => ({
                      ...current,
                      [recommendation.id]: !current[recommendation.id],
                    }))}
                  >
                    {isExpanded ? 'Thu gọn' : 'Xem thêm'}
                  </button>
                </div>

                <div className="recommendation-meta">
                  <span>
                    Phân tích: {formatDateRange(recommendation.analysisWeekStart, recommendation.analysisWeekEnd)}
                  </span>
                  <span>
                    → Áp dụng: {formatDateRange(recommendation.applyWeekStart, recommendation.applyWeekEnd)}
                  </span>
                </div>
              </article>
            )
          })}
        </div>
      )}
    </section>
  )
}

import { useMemo, useState } from 'react'
import { getInventory, getInventoryRecommendations } from '../../api/client'
import type { InventorySummaryDto, RecommendationDto } from '../../api/types'
import { useApi } from '../../hooks/useApi'
import { STOCK_ALERT_DAYS_CRITICAL, STOCK_ALERT_DAYS_WARNING } from '../../config'
import { weekRange } from '../../utils/week'

type InventoryAlert = 'Sắp hết' | 'Cần theo dõi' | 'Ổn định' | 'Chưa có dữ liệu'

function getAlertStatus(daysOfStockRemaining: number | null): InventoryAlert {
  if (daysOfStockRemaining === null) {
    return 'Chưa có dữ liệu'
  }

  if (daysOfStockRemaining < STOCK_ALERT_DAYS_CRITICAL) {
    return 'Sắp hết'
  }

  if (daysOfStockRemaining <= STOCK_ALERT_DAYS_WARNING) {
    return 'Cần theo dõi'
  }

  return 'Ổn định'
}

function getAlertClass(status: InventoryAlert): string {
  switch (status) {
    case 'Sắp hết':
      return 'alert-badge danger'
    case 'Cần theo dõi':
      return 'alert-badge warning'
    case 'Ổn định':
      return 'alert-badge success'
    case 'Chưa có dữ liệu':
    default:
      return 'alert-badge neutral'
  }
}

interface InventoryPageProps {
  weekStart: string
}

export function InventoryPage({ weekStart }: InventoryPageProps) {
  const { weekEnd } = weekRange(weekStart)

  const { data: inventoryData, loading: inventoryLoading, error: inventoryError, reload: reloadInventory } = useApi<InventorySummaryDto[]>(async (signal) => {
    return getInventory(weekStart, weekEnd, signal)
  }, [weekStart, weekEnd])

  const { data: recommendationData, loading: recommendationLoading, error: recommendationError, reload: reloadRecommendations } = useApi<RecommendationDto[]>(async (signal) => {
    return getInventoryRecommendations(weekStart, weekEnd, signal)
  }, [weekStart, weekEnd])

  const recommendationMap = useMemo(() => {
    const map = new Map<string, RecommendationDto>()
    if (Array.isArray(recommendationData)) {
      for (const item of recommendationData) {
        if (item.targetId) {
          map.set(item.targetId, item)
        }
      }
    }
    return map
  }, [recommendationData])

  const rows = useMemo(() => {
    if (!Array.isArray(inventoryData)) {
      return []
    }

    return inventoryData.map((item) => ({
      ...item,
      recommendation: recommendationMap.get(item.productId) ?? null,
    }))
  }, [inventoryData, recommendationMap])

  const [expanded, setExpanded] = useState<Record<string, boolean>>({})

  if ((inventoryLoading || recommendationLoading) && (!inventoryData && !recommendationData)) {
    return (
      <section className="panel" aria-live="polite" aria-busy="true">
        <div className="panel-header">
          <h2>Tồn kho</h2>
          <span>Đang tải...</span>
        </div>
        <p className="placeholder-text">Đang tải dữ liệu tồn kho...</p>
      </section>
    )
  }

  const hasError = inventoryError || recommendationError
  if (hasError && !inventoryData && !recommendationData) {
    return (
      <section className="panel" aria-live="polite">
        <div className="panel-header">
          <h2>Tồn kho</h2>
        </div>
        <div className="overview-state error-state" role="alert">
          <p>{inventoryError ?? recommendationError}</p>
          <button type="button" onClick={() => {
            void reloadInventory()
            void reloadRecommendations()
          }}>
            Thử lại
          </button>
        </div>
      </section>
    )
  }

  if (!inventoryData || inventoryData.length === 0) {
    return (
      <section className="panel" aria-live="polite">
        <div className="panel-header">
          <h2>Tồn kho</h2>
        </div>
        <div className="overview-state empty-state">
          <p>Không có dữ liệu tồn kho trong khoảng thời gian này.</p>
        </div>
      </section>
    )
  }

  return (
    <section className={inventoryLoading || recommendationLoading ? 'panel inventory-page stale-loading' : 'panel inventory-page'} aria-live="polite" aria-busy={inventoryLoading || recommendationLoading}>
      <div className="panel-header">
        <h2>Tồn kho</h2>
        <span>{weekStart} → {weekEnd}</span>
      </div>

      <div className="table-wrapper">
        <table className="inventory-table">
          <thead>
            <tr>
              <th>Sản phẩm</th>
              <th>Tồn hiện tại</th>
              <th>Tốc độ tiêu thụ</th>
              <th>Số ngày còn lại</th>
              <th>Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((item) => {
              const status = getAlertStatus(item.daysOfStockRemaining)
              const recommendation = item.recommendation
              const isExpanded = !!expanded[item.productId]

              return (
                <tr key={item.productId}>
                  <td>
                    <div className="inventory-product-cell">
                      <span>{item.productName}</span>
                      {recommendation ? (
                        <div className="stock-recommendation">
                          <button
                            type="button"
                            className="recommendation-toggle"
                            onClick={() => setExpanded((current) => ({
                              ...current,
                              [item.productId]: !current[item.productId],
                            }))}
                          >
                            {recommendation.action}
                          </button>
                          {isExpanded ? (
                            <div className="recommendation-detail">{recommendation.reason}</div>
                          ) : null}
                        </div>
                      ) : null}
                    </div>
                  </td>
                  <td>{item.currentStock}</td>
                  <td>{item.consumptionRate}</td>
                  <td>
                    {item.daysOfStockRemaining === null ? '—' : item.daysOfStockRemaining}
                  </td>
                  <td>
                    <span className={getAlertClass(status)}>{status}</span>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </section>
  )
}

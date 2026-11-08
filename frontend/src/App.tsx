import { type KeyboardEvent as ReactKeyboardEvent, useEffect, useState } from 'react'
import './App.css'
import { FnbPage } from './components/fnb/FnbPage'
import { InventoryPage } from './components/inventory/InventoryPage'
import { OverviewPage } from './components/overview/OverviewPage'
import { RecommendationPage } from './components/recommendations/RecommendationPage'
import { WeekPicker } from './components/WeekPicker'
import { DEFAULT_WEEK_START } from './config'
import { toMonday, weekRange } from './utils/week'

type TabKey = 'overview' | 'fnb' | 'inventory' | 'recommendations'

const tabs: Array<{ key: TabKey; label: string }> = [
  { key: 'overview', label: 'Tổng quan' },
  { key: 'fnb', label: 'F&B' },
  { key: 'inventory', label: 'Tồn kho' },
  { key: 'recommendations', label: 'Gợi ý' },
]

function App() {
  const [tab, setTab] = useState<TabKey>('overview')
  const [weekStart, setWeekStart] = useState<string>(() => {
    const params = new URLSearchParams(window.location.search)
    const stored = params.get('weekStart')
    return stored ? toMonday(stored) : DEFAULT_WEEK_START
  })

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    params.set('weekStart', weekStart)
    const nextUrl = `${window.location.pathname}?${params.toString()}`
    window.history.replaceState({}, '', nextUrl)
  }, [weekStart])

  const handleTabKeyDown = (event: ReactKeyboardEvent<HTMLButtonElement>, index: number) => {
    if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft') {
      return
    }

    event.preventDefault()
    const nextIndex = event.key === 'ArrowRight' ? (index + 1) % tabs.length : (index - 1 + tabs.length) % tabs.length
    setTab(tabs[nextIndex].key)
    const nextButton = document.querySelectorAll<HTMLButtonElement>('[role="tab"]')[nextIndex]
    nextButton?.focus()
  }

  const range = weekRange(weekStart)

  return (
    <main className="app-shell" aria-label="Dashboard phân tích rạp">
      <header className="topbar">
        <div>
          <p className="eyebrow">Cinema Revenue Analytics</p>
          <h1>Dashboard</h1>
        </div>
        <WeekPicker value={weekStart} onChange={setWeekStart} />
      </header>

      <nav className="tabs" aria-label="Chuyển tab dashboard" role="tablist">
        {tabs.map((item, index) => (
          <button
            key={item.key}
            type="button"
            role="tab"
            aria-selected={item.key === tab}
            tabIndex={item.key === tab ? 0 : -1}
            className={item.key === tab ? 'tab active' : 'tab'}
            onClick={() => setTab(item.key)}
            onKeyDown={(event) => handleTabKeyDown(event, index)}
          >
            {item.label}
          </button>
        ))}
      </nav>

      {tab === 'overview' ? (
        <OverviewPage weekStart={weekStart} />
      ) : tab === 'fnb' ? (
        <FnbPage weekStart={weekStart} />
      ) : tab === 'inventory' ? (
        <InventoryPage weekStart={weekStart} />
      ) : tab === 'recommendations' ? (
        <RecommendationPage weekStart={weekStart} />
      ) : (
        <section className="panel" aria-live="polite">
          <div className="panel-header">
            <h2>{tabs.find((item) => item.key === tab)?.label}</h2>
            <span>{range.weekStart} → {range.weekEnd}</span>
          </div>
          <p className="placeholder-text">
            Dữ liệu cho tuần đang chọn sẽ được render tại đây khi các module analytics được nối API.
          </p>
        </section>
      )}
    </main>
  )
}

export default App

import { type ChangeEvent } from 'react'
import { formatRange, weekRange, toMonday } from '../utils/week'

function parseLocalDate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}

interface WeekPickerProps {
  value: string
  onChange: (weekStart: string) => void
}

export function WeekPicker({ value, onChange }: WeekPickerProps) {
  const { weekStart, weekEnd } = weekRange(value)

  const handlePrevious = () => {
    const next = parseLocalDate(value)
    next.setDate(next.getDate() - 7)
    onChange(toMonday(next))
  }

  const handleNext = () => {
    const next = parseLocalDate(value)
    next.setDate(next.getDate() + 7)
    onChange(toMonday(next))
  }

  const handleInputChange = (event: ChangeEvent<HTMLInputElement>) => {
    const nextValue = event.target.value
    if (!nextValue) {
      return
    }
    onChange(toMonday(nextValue))
  }

  return (
    <div className="week-picker">
      <button type="button" onClick={handlePrevious} aria-label="Tuần trước">
        ◀
      </button>
      <span className="week-label">{formatRange(weekStart, weekEnd)}</span>
      <button type="button" onClick={handleNext} aria-label="Tuần sau">
        ▶
      </button>
      <input
        type="date"
        aria-label="Chọn ngày trong tuần"
        value={value}
        onChange={handleInputChange}
      />
    </div>
  )
}

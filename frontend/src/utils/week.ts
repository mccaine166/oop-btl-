export function formatDateLocal(value: Date): string {
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function parseLocalDate(value: string | Date): Date {
  if (value instanceof Date) {
    return new Date(value.getFullYear(), value.getMonth(), value.getDate())
  }

  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}

function addDays(date: Date, days: number): Date {
  const next = new Date(date)
  next.setDate(next.getDate() + days)
  return next
}

export function toMonday(date: string | Date): string {
  const current = parseLocalDate(date)
  const day = current.getDay()
  const diffToMonday = day === 0 ? -6 : 1 - day
  const monday = addDays(current, diffToMonday)
  return formatDateLocal(monday)
}

export function weekRange(monday: string): { weekStart: string; weekEnd: string } {
  const mondayDate = parseLocalDate(monday)
  const sundayDate = addDays(mondayDate, 6)
  return {
    weekStart: formatDateLocal(mondayDate),
    weekEnd: formatDateLocal(sundayDate),
  }
}

export function shiftWeek(monday: string, n: number): string {
  const mondayDate = parseLocalDate(monday)
  return formatDateLocal(addDays(mondayDate, n * 7))
}

export function formatRange(weekStart: string, weekEnd: string): string {
  const start = parseLocalDate(weekStart)
  const end = parseLocalDate(weekEnd)

  const startText = `${String(start.getDate()).padStart(2, '0')}/${String(start.getMonth() + 1).padStart(2, '0')}`
  const endText = `${String(end.getDate()).padStart(2, '0')}/${String(end.getMonth() + 1).padStart(2, '0')}`
  return `${startText} – ${endText}`
}

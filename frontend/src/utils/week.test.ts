import { describe, expect, it } from 'vitest'
import { shiftWeek, toMonday } from './week'

describe('week utilities', () => {
  it('toMonday returns the Monday of the given week', () => {
    expect(toMonday('2026-09-14')).toBe('2026-09-14')
    expect(toMonday('2026-09-20')).toBe('2026-09-14')
    expect(toMonday('2026-09-16')).toBe('2026-09-14')
  })

  it('shiftWeek crosses month and year boundaries correctly', () => {
    expect(shiftWeek('2026-09-14', 1)).toBe('2026-09-21')
    expect(shiftWeek('2026-09-28', 1)).toBe('2026-10-05')
    expect(shiftWeek('2026-12-28', 1)).toBe('2027-01-04')
  })
})

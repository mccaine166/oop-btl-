import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { WeekPicker } from './WeekPicker'

describe('WeekPicker', () => {
  it('calls onChange with the correct Monday when user navigates or picks a date', () => {
    const onChange = vi.fn()
    render(<WeekPicker value="2026-09-14" onChange={onChange} />)

    fireEvent.click(screen.getByLabelText('Tuần trước'))
    expect(onChange).toHaveBeenLastCalledWith('2026-09-07')

    fireEvent.click(screen.getByLabelText('Tuần sau'))
    expect(onChange).toHaveBeenLastCalledWith('2026-09-21')

    fireEvent.change(screen.getByLabelText('Chọn ngày trong tuần'), {
      target: { value: '2026-09-16' },
    })
    expect(onChange).toHaveBeenLastCalledWith('2026-09-14')
  })
})

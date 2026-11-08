import { useCallback, useEffect, useRef, useState, type DependencyList } from 'react'

interface UseApiResult<T> {
  data: T | null
  loading: boolean
  error: string | null
  reload: () => Promise<void>
}

export function useApi<T>(
  fetcher: (signal?: AbortSignal) => Promise<T>,
  deps: DependencyList = [],
): UseApiResult<T> {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)
  const fetcherRef = useRef(fetcher)

  useEffect(() => {
    fetcherRef.current = fetcher
  }, [fetcher])

  const runRequest = useCallback(async (signal?: AbortSignal) => {
    setLoading(true)
    setError(null)

    try {
      const result = await fetcherRef.current(signal)
      if (signal?.aborted) {
        return
      }
      setData(result)
    } catch (caughtError) {
      if (signal?.aborted) {
        return
      }

      const nextMessage =
        caughtError instanceof Error ? caughtError.message : 'Không kết nối được máy chủ'
      setError(nextMessage)
    } finally {
      if (!signal || !signal.aborted) {
        setLoading(false)
      }
    }
  }, [])

  const reload = useCallback(async () => {
    const controller = new AbortController()
    await runRequest(controller.signal)
  }, [runRequest])

  useEffect(() => {
    const controller = new AbortController()

    void runRequest(controller.signal)

    return () => {
      controller.abort()
    }
  }, [runRequest, ...deps])

  return {
    data,
    loading,
    error,
    reload,
  }
}

import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import {
  type LiveFeed,
  LiveFeedClient,
  type LiveFeedResult,
} from "@/api/live-feed";
import { createDemoFeed } from "@/pages/stream-wall/demo-feed";

export type LiveFeedStatus = "loading" | "ready" | "error";

export interface UseLiveFeed {
  feed: LiveFeed | null;
  status: LiveFeedStatus;
  /** Where the current feed came from — useful for a "demo" indicator. */
  source: LiveFeedResult["source"] | null;
  error: string | null;
  refresh: () => void;
}

const POLL_INTERVAL_MS = 30_000;

export interface UseLiveFeedOptions {
  endpoint?: string;
  pollIntervalMs?: number;
}

/**
 * React binding over {@link LiveFeedClient}. Polls the feed on an interval and
 * exposes a manual refresh. In dev it falls back to the local demo feed so the
 * wall always has something to render.
 */
export function useLiveFeed(options: UseLiveFeedOptions = {}): UseLiveFeed {
  const { endpoint, pollIntervalMs = POLL_INTERVAL_MS } = options;

  const client = useMemo(
    () =>
      new LiveFeedClient({
        endpoint,
        fallbackFeed: createDemoFeed,
        useFallbackOnError: import.meta.env.DEV,
      }),
    [endpoint],
  );

  const [feed, setFeed] = useState<LiveFeed | null>(null);
  const [status, setStatus] = useState<LiveFeedStatus>("loading");
  const [source, setSource] = useState<LiveFeedResult["source"] | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Bumping this triggers a fresh load via the effect below.
  const [refreshKey, setRefreshKey] = useState(0);
  const refresh = useCallback(() => setRefreshKey((key) => key + 1), []);

  const isMounted = useRef(true);
  useEffect(() => {
    isMounted.current = true;
    return () => {
      isMounted.current = false;
    };
  }, []);

  useEffect(() => {
    const currentRefreshKey = refreshKey;
    const controller = new AbortController();
    let timer: ReturnType<typeof setTimeout> | undefined;
    let cancelled = false;

    const load = async () => {
      try {
        const result = await client.fetchLive(controller.signal);
        if (cancelled || !isMounted.current) {
          return;
        }
        setFeed(result.feed);
        setSource(result.source);
        setStatus("ready");
        setError(null);
      } catch (caught) {
        if (cancelled || controller.signal.aborted || !isMounted.current) {
          return;
        }
        setFeed(null);
        setSource(null);
        setStatus("error");
        setError(
          caught instanceof Error ? caught.message : "Unknown feed error",
        );
      } finally {
        if (!cancelled && isMounted.current) {
          timer = setTimeout(load, pollIntervalMs);
        }
      }
    };

    void load();
    void currentRefreshKey;

    return () => {
      cancelled = true;
      controller.abort();
      if (timer) {
        clearTimeout(timer);
      }
    };
  }, [client, pollIntervalMs, refreshKey]);

  return { feed, status, source, error, refresh };
}

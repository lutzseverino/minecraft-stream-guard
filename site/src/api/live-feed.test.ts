import { describe, expect, it } from "vitest";

import { LiveFeedClient, LiveFeedError, normalizeFeed } from "@/api/live-feed";

describe("normalizeFeed", () => {
  it("normalizes Twitch records when only a URL is provided", () => {
    const feed = normalizeFeed({
      updatedAt: "2026-06-26T12:00:00Z",
      streamers: [
        {
          playerName: "Lutz",
          provider: "twitch",
          url: "https://www.twitch.tv/lutzseverino",
        },
      ],
    });

    expect(feed.streamers).toHaveLength(1);
    expect(feed.streamers[0]).toMatchObject({
      playerName: "Lutz",
      provider: "twitch",
      channel: "",
      url: "https://www.twitch.tv/lutzseverino",
      embed: { kind: "twitch", channel: "lutzseverino" },
    });
  });

  it("rejects malformed payloads instead of silently showing an empty wall", () => {
    expect(() => normalizeFeed({ updatedAt: "now" })).toThrow(LiveFeedError);
  });
});

describe("LiveFeedClient", () => {
  it("uses the development fallback only when enabled", async () => {
    const client = new LiveFeedClient({
      fetchImpl: () => Promise.reject(new Error("offline")),
      fallbackFeed: () => ({
        updatedAt: "2026-06-26T12:00:00Z",
        streamers: [],
      }),
      useFallbackOnError: true,
    });

    await expect(client.fetchLive()).resolves.toMatchObject({
      source: "fallback",
      feed: { streamers: [] },
    });
  });
});

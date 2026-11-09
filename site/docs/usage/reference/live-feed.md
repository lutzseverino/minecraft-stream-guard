# Live Feed

This reference describes the feed shape that StreamGuard Site accepts from
`GET /api/live`.

## Structure

The response is a JSON object with an optional ISO-8601 `updatedAt` string and a
required `streamers` array. Each streamer requires:

- `playerName`: Minecraft player name
- `provider`: `twitch` or `youtube`
- at least one of `channel` or `url`

Optional fields are `title`, `thumbnailUrl`, `viewerCount`, `liveSince`, and an
`embed` object. Twitch embeds may declare `embed.channel`. YouTube embeds may
declare `embed.videoId` or `embed.channelId`.

## Rules

- The client rejects a response that is not an object or lacks `streamers`.
- The client rejects the complete feed when any streamer is invalid.
- Missing provider URLs are derived from the provider and channel.
- Twitch channel names are normalized to lowercase for embeds.
- YouTube video IDs may be inferred from watch, short, and live URLs.
- Missing `updatedAt` values are replaced with the client's current timestamp.
- The site polls every 30 seconds by default.
- Production failures surface as errors; only development may use the demo feed.

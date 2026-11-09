import { describe, expect, it } from "vitest";

import { buildEmbedTarget } from "@/api/embeds";

describe("buildEmbedTarget", () => {
  it("adds all Twitch parent hosts", () => {
    const target = buildEmbedTarget(
      { kind: "twitch", channel: "lutzseverino" },
      { twitchParents: ["streams.example.com", "localhost"] },
    );

    expect(target.iframeSrc).toContain("channel=lutzseverino");
    expect(target.iframeSrc).toContain("parent=streams.example.com");
    expect(target.iframeSrc).toContain("parent=localhost");
    expect(target.linkOnly).toBe(false);
  });
});

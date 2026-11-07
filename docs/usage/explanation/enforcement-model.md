# Enforcement Model

StreamGuard separates stream verification from game policy. A provider answers
whether a link is live; it does not decide what a player may do in Minecraft.

## Player State

A player may be unlinked, linked but not live, live, manually verified, or
bypassed. Unlinked and not-live players have distinct movement, chat, command,
and join-kick settings. Live, manually verified, and bypassed players pass the
gate unless a newer state supersedes them.

## Guarded Actions

World-affecting events are classified separately from the player state. This is
why a server can allow movement and chat while blocking building, containers,
item transfer, crafting, trading, combat, buckets, and fire. The distinction
also keeps authentication and stream setup usable on strict servers.

## Freshness and Failure

Verification is a time-sensitive observation, not permanent identity. Scheduled
rechecks refresh it, while caching and batching limit provider traffic. A saved
provider observation eventually becomes too old to grant access; manual
administrator verification remains durable. A provider failure is distinct from
a confirmed offline result and should not silently become an offline denial.
Slow results are discarded when a player has since changed their link or an
administrator has changed their state.

Provider verification is not an identity proof. Twitch and YouTube expose public
live status, so a player can submit a channel they do not own. Servers that
cannot rely on an honor system need a separate administrator-controlled
ownership process.

Bypasses are explicit operational exceptions. Temporary bypasses have a bounded
duration; persistent bypasses remain until removed. Reasons make administrator
decisions easier to audit.

# Commands and Permissions

## Player Commands

Player commands can only be run by a player.

| Command | Purpose |
| --- | --- |
| `/stream` or `/stream status` | Show the caller's link and verification state. |
| `/stream setup` | Open the configured provider picker. |
| `/stream link twitch <channel-login>` | Save a Twitch channel and immediately verify it. |
| `/stream link youtube <reference>` | Save a YouTube channel, handle, URL, or video and immediately verify it. |
| `/stream verify` | Check the saved link now. |
| `/stream cancel` | Cancel the active onboarding flow. |

## Administrator Commands

| Command | Permission | Purpose |
| --- | --- | --- |
| `/streamguard status <player>` | `streamguard.status.others` | Show another player's verification state. |
| `/streamguard bypass grant <player> [duration] [reason]` | `streamguard.bypass` | Grant a persistent or temporary bypass. |
| `/streamguard bypass remove <player>` | `streamguard.bypass` | Remove a bypass. |
| `/streamguard verify <player> [reason]` | `streamguard.admin` | Mark a player manually verified until an administrator removes it. |
| `/streamguard unverify <player> [reason]` | `streamguard.admin` | Remove the current verification. |
| `/streamguard reload` | `streamguard.reload` | Reload plugin configuration. |

Durations accept seconds, minutes, hours, or days, using forms such as `30s`,
`20m`, `2h`, and `1d`. Omitting the duration creates a persistent bypass. The
configured maximum duration still applies.

## Permission Nodes

| Permission | Default | Purpose |
| --- | --- | --- |
| `streamguard.admin` | Operator | Administrative commands and all child permissions. |
| `streamguard.bypass` | Operator | Grant and remove bypasses. |
| `streamguard.reload` | Operator | Reload configuration. |
| `streamguard.status.others` | Operator | View another player's status. |
| `streamguard.bypass.always` | Disabled | Always bypass enforcement. |

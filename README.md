# NexusSpawnShield v1.0.2

An override zone around world spawn. Inside a huge fixed-coordinate radius, this plugin forces
the outcome of the events that matter, at the LAST possible priority (`MONITOR` -- see "Why
MONITOR, not HIGHEST" below) -- so it doesn't matter which other plugin (NexusRealms' land claims
were the leading suspect) was denying something first, or what priority it used. Two rules, both
forced both ways:

- **Functional blocks always work, for everyone.** Doors, trapdoors, fence gates, buttons,
  levers, pressure plates, chests, trapped chests, barrels, ender chests, shulker boxes,
  furnaces, crafting tables, anvils -- every block you right-click to use. This is the actual
  fix for "you can't use that here" on the chest at spawn.
- **Killing, breaking, and placing are force-blocked for everyone except an exempt list, and
  force-ALLOWED for that list.** By default the list is just `RealSociety5107`. Killing means
  damaging any entity (mob or player), by melee or by a fired projectile (arrow, trident, etc).
  Placing includes bucket-emptying (dumping lava/water); breaking includes bucket-filling. Being
  on the exempt list doesn't just mean this plugin leaves you alone -- it means this plugin
  actively guarantees the action succeeds, even if something else (NexusRealms' team-ownership
  check, for instance) would otherwise say no.

## Why MONITOR, not HIGHEST

v1.0.0/v1.0.1 used `EventPriority.HIGHEST`, which fixed the chest issue in testing but not for a
real player -- because Bukkit does NOT guarantee execution order between two listeners at the
*same* priority. If NexusRealms' own protection is also registered at HIGHEST (plausible for a
plugin whose author also wanted the final say), it was a coin flip which of the two ran last, and
that coin came up wrong. `MONITOR` is the one priority tier guaranteed to run after every other
tier on the server, full stop, regardless of plugin load order -- so this is now genuinely the
last word. This deliberately breaks the polite Bukkit convention that MONITOR shouldn't mutate
events; that convention is for passive tools like stats/logging plugins, not for something
explicitly built to be an unconditional override.

As defense in depth, `InventoryOpenEvent` is also force-un-cancelled inside the zone (MONITOR), in
case something blocks the container GUI itself rather than (or in addition to) the initial
interact.

## The zone

Centered on **block (-16649, 72, 9646)**, which is chunk (-1041, 602), in the world named
`world` -- taken from your F3 debug screen (`minecraft:overworld` is the client-side label;
`world` is virtually always the actual Bukkit/Paper world name for the main overworld unless
it's been renamed). **Confirm this with `/nexusspawnshield status` once it's installed** -- if
the zone doesn't seem to be doing anything, this is the first thing to check.

Radius is **1000 chunks**, Chebyshev ("chessboard") distance from that center chunk -- a
2001x2001 chunk square, roughly 32,000 x 32,000 blocks on a side. That is a genuinely enormous
area (for reference, that's larger than most servers' entire explored map), exactly as you asked
for. If that turns out to be more than you actually wanted once you see it in practice, lower
`zone.radius-chunks` in the config and `/nexusspawnshield reload`.

## Commands (all require `nexusspawnshield.admin`, default: op)

- `/nexusspawnshield status` -- zone bounds, on/off, exempt list, your own bypass state, total
  blocked since startup. **Run by a player, it also reports your current world/coordinates, your
  chunk distance from the zone center, and an explicit IN ZONE / OUTSIDE ZONE verdict** -- this
  is the fastest way to check whether `zone.world` actually matches this server's real world name
  (the most likely reason the zone would silently do nothing).
- `/nexusspawnshield toggle` -- turn the whole zone on/off.
- `/nexusspawnshield bypass` -- toggle **your own** personal bypass (lets you kill/break/place
  in the zone regardless of the exempt list). Resets to your permission default on restart.
  Requires `nexusspawnshield.bypass` (default: op) to have any effect.
- `/nexusspawnshield exempt add <name>` -- add a player to the always-allowed list. Persists to
  config.yml immediately.
- `/nexusspawnshield exempt remove <name>` -- remove one.
- `/nexusspawnshield exempt list` -- show the current list.
- `/nexusspawnshield log [count]` -- recent blocked kill/break/place attempts (who, where, when),
  default 15, kept in memory only, capped at the last 200.
- `/nexusspawnshield reload` -- reload config.yml (zone bounds, toggles, exempt list).

## Config (`plugins/NexusSpawnShield/config.yml`)

Fully commented -- `zone.*` for the world/center/radius, `settings.*` to turn any one of the
four restrictions (killing/breaking/placing/buckets) off independently or turn off the
force-allow-interactions override, `exempt-players` for the always-allowed list (also editable
live via the `exempt` command), and `heartbeat.*` for how often blocked attempts get summarized.

## What this does NOT do

This doesn't touch NexusRealms (or whatever else) directly, and doesn't diagnose or fix whatever
its actual land-claim/permission setting is -- it wins the argument after the fact instead,
inside this zone only. Outside the zone, NexusRealms and everything else behaves exactly as
before. It also doesn't affect the vanilla `/kill` command specifically (command-block or
console-run mass kills) -- that's NexusMobShield's job, already installed separately; the two
plugins don't conflict, since NexusMobShield only ever looks at `DamageCause.KILL` and this
plugin's kill-blocking only looks at player-caused combat damage.

## Build

```
mvn clean package
```

Produces `target/nexusspawnshield-1.0.2.jar`. Requires Java 21 and network access to
`repo.papermc.io` / Maven Central. See CHANGES.md for how this was verified in the sandbox
(full compile against a hand-written stub library, plus standalone checks of the zone-distance
math, the blocked-attempt ring buffer, and the bypass-toggle logic) -- run your own
`mvn clean package` against the real Paper API.

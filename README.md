# NexusSpawnShield v1.0.0

An override zone around world spawn. Inside a huge fixed-coordinate radius, this plugin forces
the outcome of the events that matter, at the highest possible priority -- so it doesn't matter
which other plugin (NexusRealms' land claims were the leading suspect) was denying something
first. Two rules, both forced:

- **Functional blocks always work, for everyone.** Doors, trapdoors, fence gates, buttons,
  levers, pressure plates, chests, trapped chests, barrels, ender chests, shulker boxes,
  furnaces, crafting tables, anvils -- every block you right-click to use. This is the actual
  fix for "you can't use that here" on the chest at spawn.
- **Killing, breaking, and placing are blocked for everyone except an exempt list.** By default
  that list is just `RealSociety5107`. Killing means damaging any entity (mob or player), by
  melee or by a fired projectile (arrow, trident, etc). Placing includes bucket-emptying
  (dumping lava/water); breaking includes bucket-filling.

## Why this design

You described the actual bug (chest blocked at spawn) and, separately, asked for an override
covering a 1000-chunk radius around the coordinates you gave, where functional items always work
but nobody but you can kill or build -- except the people you explicitly allow. This plugin does
exactly that, and does it in a way that wins regardless of *why* something else was blocking
things, because every handler runs at `EventPriority.HIGHEST` without `ignoreCancelled` -- the
same pattern that fixed an identical "chest blocked despite being allow-listed" bug once already
in this ecosystem (NexusSpawn v1.0.1). If NexusRealms (or anything else) tries to deny an
interaction inside this zone, this plugin's ALLOW is applied afterward and wins.

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
  blocked since startup.
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

Produces `target/nexusspawnshield-1.0.0.jar`. Requires Java 21 and network access to
`repo.papermc.io` / Maven Central. See CHANGES.md for how this was verified in the sandbox
(full compile against a hand-written stub library, plus standalone checks of the zone-distance
math, the blocked-attempt ring buffer, and the bypass-toggle logic) -- run your own
`mvn clean package` against the real Paper API.

# NexusSpawnShield changelog

## v1.0.0 -- first release

Built from two connected requests: a report that a player got "you can't use that here" trying
to open the chest at world spawn (narrowed down, by elimination, to NexusRealms' land-claim
protection as the leading suspect -- NexusDenyBlocks and NexusHeartbeat/NexusWarbeasts were ruled
out by design, an unidentified "server wiki" plugin flagged as a remaining unknown), followed by
an explicit request to build an override instead of chasing the exact root cause: "make an
override... a thousand chunks at this specific coordinate in a radius is protected by the
server... you're allowed to use levers, buttons, chests, ender chest, all the functional items
you're just not allowed to kill or place or break anything. And the only person that can is this
gamer tag... RealSociety5107... make it to where I can add others as well."

**Design decision:** rather than trying to find and fix the exact NexusRealms setting (never
directly confirmed -- no live server access in this session), this plugin wins regardless of the
cause. Every handler runs at `EventPriority.HIGHEST` without `ignoreCancelled`, so whatever any
other plugin decided first gets overridden here, inside the zone, every time. This is the same
"final override" pattern already proven once in this ecosystem (NexusSpawn v1.0.1) for the exact
same symptom class (interaction blocked despite being allow-listed).

**Two forced rules, deliberately opposite:**
- Right-click block interactions (the actual "functional items" -- doors, buttons, levers,
  chests, ender chests, everything else you use rather than break) are force-ALLOWed for
  everyone in the zone, via `PlayerInteractEvent.setUseInteractedBlock/setUseItemInHand(Event
  .Result.ALLOW)`.
- Breaking (`BlockBreakEvent`), placing (`BlockPlaceEvent`), bucket-emptying/filling
  (`PlayerBucketEmptyEvent`/`PlayerBucketFillEvent`), and killing (`EntityDamageByEntityEvent`,
  covering both melee and projectiles fired by a player) are cancelled for everyone in the zone
  **except** players on the exempt list or currently bypassing.

**Zone:** Chebyshev-distance chunk radius (same convention as NexusSpawn/NexusSeasons),
1000 chunks, centered on block (-16649, 72, 9646) -- read off the two F3 debug screenshots
provided (Block: -16649 72 9646, Chunk: -1041 4 602, world `minecraft:overworld`). Config uses
Bukkit world name `world` as the default, since that's the standard Paper name for the main
overworld and the client-side `minecraft:overworld` label doesn't tell us if it was renamed --
flagged prominently in the README and in `/nexusspawnshield status` output as the first thing to
verify once installed.

**Exempt list:** starts with `RealSociety5107` (the gamertag given), stored in config.yml as
`exempt-players`, editable live with `/nexusspawnshield exempt add|remove|list` -- satisfies "I
can add others as well" without needing a config edit + reload each time. A separate personal
`/nexusspawnshield bypass` toggle (permission-gated, resets on restart) is also available for
admins, independent of the exempt list, matching this ecosystem's existing AdminBypassManager
convention (NexusSpawn, NexusRealms, NexusDenyBlocks all have the same shape).

**Deliberately does not touch:** the vanilla `/kill` command specifically (NexusMobShield's job,
already installed and unaffected -- it only watches `DamageCause.KILL`, this plugin's kill
protection only watches player-caused combat damage, so there's no overlap or conflict) or
NexusRealms itself (this plugin doesn't inspect or change any of its settings -- it just wins the
argument afterward, inside this zone).

**Verification:** built a stub library covering this plugin's Bukkit/Paper surface (extended from
this session's NexusMobShield stub set -- added `PlayerInteractEvent` + `Event.Result` +
`Action`, `PlayerBucketEmptyEvent`/`PlayerBucketFillEvent`, `Projectile`/`ProjectileSource` for
resolving a projectile's shooter, and cancel support on `BlockBreakEvent`/`BlockPlaceEvent`) and
compiled the whole tree with `javac -Xlint:all -Werror`: 0 errors, 0 warnings. Separately verified
against the compiled classes directly: the zone's Chebyshev distance math (a block exactly at the
1000-chunk boundary is in-zone, one further out is not), the blocked-attempt ring buffer (200-cap
rolls off oldest first, periodic counter drains to zero without double-counting), and the personal
bypass toggle (an op-permission player defaults to bypassing, a non-permission player defaults to
not, and toggling flips both correctly and reversibly) -- all checks passed.

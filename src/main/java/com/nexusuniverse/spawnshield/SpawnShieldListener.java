package com.nexusuniverse.spawnshield;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ProjectileSource;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * The override itself. Every handler here runs at HIGHEST priority WITHOUT ignoreCancelled, so
 * this plugin's decision is the one that sticks no matter what any other plugin (NexusRealms'
 * land-claim protection included) already decided on the same event -- the same pattern
 * NexusSpawn v1.0.1 used to fix an identical "chest blocked despite being allow-listed" bug.
 *
 * Two opposite rules, both forced:
 *   - Right-click block interactions inside the zone: ALWAYS allowed, for everyone. This is the
 *     actual fix for "you can't use that here" on doors/chests/buttons/levers/etc.
 *   - Killing (damaging any entity), breaking blocks, and placing blocks (buckets included)
 *     inside the zone: blocked for everyone EXCEPT players on the exempt list or currently
 *     bypassing.
 */
public final class SpawnShieldListener implements Listener {

    private final ZoneSettings settings;
    private final ExemptManager exemptManager;
    private final AdminBypassManager bypassManager;
    private final BlockedAttemptLog log;

    public SpawnShieldListener(ZoneSettings settings, ExemptManager exemptManager,
                                AdminBypassManager bypassManager, BlockedAttemptLog log) {
        this.settings = settings;
        this.exemptManager = exemptManager;
        this.bypassManager = bypassManager;
        this.log = log;
    }

    private boolean isPrivileged(Player player) {
        return player != null && (exemptManager.isExempt(player.getName()) || bypassManager.isBypassing(player));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (!settings.forceAllowInteractions()) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !event.hasBlock()) {
            return;
        }
        if (!settings.isInZone(event.getClickedBlock().getLocation())) {
            return;
        }
        // Force-allow, for everyone -- doors, trapdoors, fence gates, buttons, levers, pressure
        // plates, chests, trapped chests, barrels, ender chests, shulker boxes, furnaces,
        // crafting tables, anvils, every other right-click-to-use block. This overrides any
        // earlier DENY from another plugin on this same event.
        event.setUseInteractedBlock(Event.Result.ALLOW);
        event.setUseItemInHand(Event.Result.ALLOW);
        event.setCancelled(false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent event) {
        if (!settings.blockBreaking() || event.getBlock() == null) {
            return;
        }
        if (!settings.isInZone(event.getBlock().getLocation())) {
            return;
        }
        Player player = event.getPlayer();
        if (isPrivileged(player)) {
            return;
        }
        event.setCancelled(true);
        recordAndNotify("break", player, event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent event) {
        if (!settings.blockPlacing() || event.getBlock() == null) {
            return;
        }
        if (!settings.isInZone(event.getBlock().getLocation())) {
            return;
        }
        Player player = event.getPlayer();
        if (isPrivileged(player)) {
            return;
        }
        event.setCancelled(true);
        recordAndNotify("place", player, event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!settings.blockBuckets() || event.getBlockClicked() == null) {
            return;
        }
        if (!settings.isInZone(event.getBlockClicked().getLocation())) {
            return;
        }
        Player player = event.getPlayer();
        if (isPrivileged(player)) {
            return;
        }
        event.setCancelled(true);
        recordAndNotify("place", player, event.getBlockClicked().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!settings.blockBuckets() || event.getBlockClicked() == null) {
            return;
        }
        if (!settings.isInZone(event.getBlockClicked().getLocation())) {
            return;
        }
        Player player = event.getPlayer();
        if (isPrivileged(player)) {
            return;
        }
        event.setCancelled(true);
        recordAndNotify("break", player, event.getBlockClicked().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!settings.blockKilling()) {
            return;
        }
        Entity damaged = event.getEntity();
        if (damaged == null) {
            return;
        }
        Location location = damaged.getLocation();
        if (!settings.isInZone(location)) {
            return;
        }
        Player attacker = resolveAttackingPlayer(event.getDamager());
        if (attacker == null || isPrivileged(attacker)) {
            return;
        }
        event.setCancelled(true);
        recordAndNotify("kill", attacker, location);
    }

    /** A player attacking directly, or a projectile (arrow, trident, snowball, ...) they fired. */
    private Player resolveAttackingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    private void recordAndNotify(String kind, Player player, Location location) {
        String name = player != null ? player.getName() : "unknown";
        String world = location.getWorld() != null ? location.getWorld().getName() : "?";
        log.record(kind, name, world, location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (player != null) {
            player.sendMessage("You can't " + kind + " here -- this area is protected.");
        }
    }
}

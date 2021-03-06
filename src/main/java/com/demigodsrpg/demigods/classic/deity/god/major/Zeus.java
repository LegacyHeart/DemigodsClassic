package com.demigodsrpg.demigods.classic.deity.god.major;

import com.demigodsrpg.demigods.classic.DGClassic;
import com.demigodsrpg.demigods.classic.ability.Ability;
import com.demigodsrpg.demigods.classic.ability.AbilityResult;
import com.demigodsrpg.demigods.classic.deity.Deity;
import com.demigodsrpg.demigods.classic.deity.IDeity;
import com.demigodsrpg.demigods.classic.model.PlayerModel;
import com.demigodsrpg.demigods.classic.util.TargetingUtil;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Zeus implements IDeity {
    @Override
    public String getDeityName() {
        return "Zeus";
    }

    @Override
    public String getNomen() {
        return "child of " + getDeityName();
    }

    @Override
    public String getInfo() {
        return "God of the sky.";
    }

    @Override
    public ChatColor getColor() {
        return ChatColor.YELLOW;
    }

    @Override
    public Sound getSound() {
        return Sound.AMBIENCE_THUNDER;
    }

    @Override
    public MaterialData getClaimMaterial() {
        return new MaterialData(Material.FEATHER);
    }

    @Override
    public Importance getImportance() {
        return Importance.MAJOR;
    }

    @Override
    public Alliance getDefaultAlliance() {
        return Alliance.OLYMPIAN;
    }

    @Override
    public Pantheon getPantheon() {
        return Pantheon.OLYMPIAN;
    }

    // -- ABILITIES -- //

    @Ability(name = "Lightning", command = "lightning", info = "Strike lightning at a target location.", cost = 140, delay = 1000)
    public AbilityResult lightningAbility(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Define variables
        Location target;
        LivingEntity entity = TargetingUtil.autoTarget(player);
        boolean notify;

        if (entity != null) {
            target = entity.getLocation();
            notify = true;
        } else {
            target = TargetingUtil.directTarget(player);
            notify = false;
        }

        return strikeLightning(player, target, notify) ? AbilityResult.SUCCESS : AbilityResult.OTHER_FAILURE;
    }

    private static boolean strikeLightning(Player player, Location target, boolean notify) {
        // Set variables
        PlayerModel model = DGClassic.PLAYER_R.fromPlayer(player);

        if (!player.getWorld().equals(target.getWorld())) return false;
        Location toHit = TargetingUtil.adjustedAimLocation(model, target);

        player.getWorld().strikeLightningEffect(toHit);

        for (Entity entity : toHit.getBlock().getChunk().getEntities()) {
            if (entity instanceof LivingEntity) {
                if (!DGClassic.BATTLE_R.canTarget(entity)) continue;
                LivingEntity livingEntity = (LivingEntity) entity;
                if (livingEntity.equals(player)) continue;
                double damage = 10 * model.getAscensions();
                if ((toHit.getBlock().getType().equals(Material.WATER) || toHit.getBlock().getType().equals(Material.STATIONARY_WATER)) && livingEntity.getLocation().distance(toHit) < 8) {
                    damage += 4;
                    livingEntity.damage(damage);
                    entity.setLastDamageCause(new EntityDamageEvent(livingEntity, EntityDamageEvent.DamageCause.LIGHTNING, damage));
                } else if (livingEntity.getLocation().distance(toHit) < 2) {
                    damage += 3;
                    livingEntity.damage(damage);
                    entity.setLastDamageCause(new EntityDamageEvent(livingEntity, EntityDamageEvent.DamageCause.LIGHTNING, damage));
                }
            }
        }

        if (!TargetingUtil.isHit(target, toHit) && notify) player.sendMessage(ChatColor.RED + "Missed...");

        return true;
    }

    @Ability(name = "Shove", command = "shove", info = "Use the force of wind to shove your enemies.", cost = 170, delay = 1500)
    public AbilityResult pullAbility(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PlayerModel model = DGClassic.PLAYER_R.fromPlayer(player);

        double devotion = model.getDevotion(Deity.ZEUS);
        double multiply = 0.1753 * Math.pow(devotion, 0.322917);

        LivingEntity hit = TargetingUtil.autoTarget(player);

        if (hit != null) {
            player.sendMessage(ChatColor.YELLOW + "*whoosh*");

            Vector v = player.getLocation().toVector();
            Vector victor = hit.getLocation().toVector().subtract(v);
            victor.multiply(multiply);
            hit.setVelocity(victor);

            return AbilityResult.SUCCESS;
        }

        return AbilityResult.NO_TARGET_FOUND;
    }

    @Ability(name = "Storm", command = "storm", info = "Strike fear into the hearts of your enemies.", cost = 3700, cooldown = 600000, type = Ability.Type.ULTIMATE)
    public AbilityResult stormAbility(PlayerInteractEvent event) {
        final Player player = event.getPlayer();

        // Define variables
        PlayerModel model = DGClassic.PLAYER_R.fromPlayer(event.getPlayer());

        // Define variables
        final int ultimateSkillLevel = model.getAscensions();
        final int damage = 10 * model.getAscensions();
        final int radius = (int) Math.log10(10 * ultimateSkillLevel) * 25;

        // Make it stormy for the caster
        setWeather(player, 100);

        // Strike targets
        for (final Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            // Validate them first
            if (!(entity instanceof LivingEntity)) continue;
            if (entity instanceof Player) {
                PlayerModel opponent = DGClassic.PLAYER_R.fromPlayer((Player) entity);
                if (opponent != null && model.getAlliance().equals(opponent.getAlliance())) continue;
            }
            if (DGClassic.BATTLE_R.canParticipate(entity) && !DGClassic.BATTLE_R.canTarget(entity)) continue;

            // Make it stormy for players
            if (entity instanceof Player) setWeather((Player) entity, 100);

            // Strike them with a small delay
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(DGClassic.getInst(), new BukkitRunnable() {
                @Override
                public void run() {
                    for (int i = 0; i <= 3; i++) {
                        player.getWorld().strikeLightningEffect(entity.getLocation());
                        if (entity.getLocation().getBlock().getType().equals(Material.WATER)) {
                            ((LivingEntity) entity).damage(damage + 4);
                        } else {
                            ((LivingEntity) entity).damage(damage);
                        }
                        entity.setLastDamageCause(new EntityDamageByEntityEvent(player, entity, EntityDamageEvent.DamageCause.LIGHTNING, damage));
                    }
                }
            }, 15);
        }

        return AbilityResult.SUCCESS;
    }

    private static void setWeather(final Player player, long ticks) {
        // Set the weather
        player.setPlayerWeather(WeatherType.DOWNFALL);

        // Create the runnable to switch back
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(DGClassic.getInst(), new BukkitRunnable() {
            @Override
            public void run() {
                player.resetPlayerWeather();
            }
        }, ticks);
    }

    @Ability(name = "No Fall Damage", info = "Take no fall damage.", type = Ability.Type.PASSIVE, placeholder = true)
    public void noFallDamageAbility() {
        // Do nothing, handled directly in the ability listener to save time
    }
}

package so.max1soft.skills;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.earth2me.essentials.Essentials;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static so.max1soft.skills.ColorUtils.formatColors;

public class SkillsListener implements Listener {

    private final Main plugin;
    private final FileConfiguration config;

    private static final Map<UUID, String> activeSkills = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> actionBarTasks = new HashMap<>();
    private static final Map<UUID, Long> skillCooldowns = new HashMap<>();
    private static final Map<UUID, Long> frozenPlayers = new HashMap<>();
    private static final Map<Snowball, BukkitRunnable> snowballParticleTasks = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> playerParticleTasks = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> glowTasks = new HashMap<>();

    public SkillsListener(Main plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void activateSkill(Player player, String skillName) {
        // Проверка на режим бога и полета

        if (activeSkills.containsKey(player.getUniqueId())) {
            sendMessage(player, "skill_already_active");
            return;
        }

        activeSkills.put(player.getUniqueId(), skillName);

        String actionBarName = config.getString("skills." + skillName.toLowerCase() + ".actionbarname", "Активный скил: " + skillName);

        actionBarName = formatColors(actionBarName);

        startActionBarUpdater(player, actionBarName);
        sendMessage(player, "skill_activated", "{skillName}", skillName);
    }

    public void disableSkills(Player player) {
        activeSkills.remove(player.getUniqueId());
        skillCooldowns.remove(player.getUniqueId());
        removeActionBarUpdater(player);
        sendMessage(player, "skills_disabled");
    }

    private void startActionBarUpdater(Player player, String message) {
        removeActionBarUpdater(player);

        BukkitRunnable actionBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeSkills.containsKey(player.getUniqueId())) {
                    updateActionBar(player, message);
                } else {
                    cancel();
                }
            }
        };

        actionBarTask.runTaskTimer(plugin, 0L, 20L); // Обновляем ActionBar каждую секунду
        actionBarTasks.put(player.getUniqueId(), actionBarTask);
    }

    public void updateActionBar(Player player, String message) {
        String formattedMessage = ColorUtils.formatColors(message);
        TextComponent textComponent = new TextComponent(formattedMessage);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, textComponent);
    }

    private void removeActionBarUpdater(Player player) {
        BukkitRunnable task = actionBarTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        updateActionBar(player, "");
    }

    private void sendMessage(Player player, String key, String... placeholders) {
        String message = config.getString("messages." + key, "");
        for (int i = 0; i < placeholders.length; i += 2) {
            message = message.replace(placeholders[i], placeholders[i + 1]);
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        CMIUser user = Main.inst.getCMI().getPlayerManager().getUser(event.getPlayer());

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (player.isSneaking()) {


                String skillName = activeSkills.get(player.getUniqueId());
                if (skillName != null) {
                    List<String> disabledWorlds = config.getStringList("disabled_worlds");
                    String currentWorldName = player.getWorld().getName();
                    if (disabledWorlds.contains(currentWorldName)) {
                        player.sendMessage(config.getString("skills_disabled_in_world").replace("&", "§"));
                        return;
                    }

                    if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE){
                        event.getPlayer().sendMessage(Main.inst.getConfig().getString("skill_not_allowed_in_mode").replace("&", "§"));
                        return;
                    }
                    if (isGodMode(user)) {
                        event.getPlayer().sendMessage(Main.inst.getConfig().getString("skill_not_allowed_in_mode").replace("&", "§"));
                        return;
                    }
                    if (player.isFlying()){
                        event.getPlayer().sendMessage(Main.inst.getConfig().getString("skill_not_allowed_in_mode").replace("&", "§"));
                        return;
                    }
                    long currentTime = System.currentTimeMillis();
                    long lastUsedTime = skillCooldowns.getOrDefault(player.getUniqueId(), 0L);
                    long cooldown = config.getLong("cooldowns." + skillName.toLowerCase(), 10) * 1000;
                    if (currentTime - lastUsedTime < cooldown) {
                        sendMessage(player, "skill_on_cooldown");
                        return;
                    }

                    String colorHex = config.getString("skills." + skillName.toLowerCase() + ".color", "#FFFFFF");
                    Color color = Color.fromRGB(
                            Integer.parseInt(colorHex.substring(1, 3), 16),
                            Integer.parseInt(colorHex.substring(3, 5), 16),
                            Integer.parseInt(colorHex.substring(5, 7), 16)
                    );

                    applyGlow(player, color);

                    switch (skillName.toUpperCase()) {
                        case "THUNDER":
                            activateThunderSkill(player);
                            break;
                        case "ICE":
                            activateIceSkill(player);
                            break;
                        case "HEAL":
                            activateHealSkill(player);
                            break;
                        case "KNOCKBACK":
                            activateKnockbackSkill(player);
                            break;
                        case "BERSERK":
                            activateBerserkSkill(player);
                            break;
                        default:
                            sendMessage(player, "unknown_skill");
                            break;
                    }
                    skillCooldowns.put(player.getUniqueId(), currentTime);
                }
            }
        }
    }
    private boolean isGodMode(CMIUser user) {
        try {
            Method isGodMethod = user.getClass().getMethod("isGod");
            return (boolean) isGodMethod.invoke(user);
        } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }
    }
    private void applyGlow(Player player, Color color) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0));
        sendMessage(player, "glow_activated");

        BukkitRunnable glowTask = new BukkitRunnable() {
            @Override
            public void run() {
                removeGlow(player);
            }
        };
        glowTask.runTaskLater(plugin, 100L);

        glowTasks.put(player.getUniqueId(), glowTask);

        BukkitRunnable particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    double radius = 1.2;
                    int numParticles = 100;
                    for (int i = 0; i < numParticles; i++) {
                        double theta = Math.random() * 2 * Math.PI;
                        double phi = Math.acos(2 * Math.random() - 1);

                        double x = radius * Math.sin(phi) * Math.cos(theta);
                        double y = radius * Math.sin(phi) * Math.sin(theta);
                        double z = radius * Math.cos(phi);

                        Location particleLocation = player.getLocation().add(x, y + 1, z);

                        player.getWorld().spawnParticle(
                                Particle.REDSTONE,
                                particleLocation,
                                0,
                                new Particle.DustOptions(color, 1)
                        );
                    }
                } else {
                    cancel();
                }
            }
        };
        particleTask.runTaskTimer(plugin, 0L, 10L);
        glowTasks.put(player.getUniqueId(), particleTask);
    }

    private void removeGlow(Player player) {
        player.removePotionEffect(PotionEffectType.GLOWING);
        sendMessage(player, "glow_removed");

        BukkitRunnable particleTask = glowTasks.remove(player.getUniqueId());
        if (particleTask != null) {
            particleTask.cancel();
        }
    }

    private void activateThunderSkill(Player player) {
        Player target = null;
        double closestDistance = 10;
        for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
            if (entity instanceof Player) {
                double distance = entity.getLocation().distance(player.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    target = (Player) entity;
                }
            }
        }

        if (target != null) {
            double damage = config.getDouble("skills.thunder.damage", 2.5);
            target.getWorld().strikeLightning(target.getLocation());
            target.damage(damage * 2);
            sendMessage(player, "thunder_hit", "{targetName}", target.getName());
        } else {
            sendMessage(player, "no_players_nearby", "{radius}", "10");
        }
    }

    private void activateIceSkill(Player player) {
        Snowball snowball = player.launchProjectile(Snowball.class);
        snowball.setMetadata("ICE_SKILL", new FixedMetadataValue(plugin, true));
        sendMessage(player, "ice_skill_activated");

        BukkitRunnable particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!snowball.isValid() || !snowball.getWorld().equals(player.getWorld())) {
                    cancel();
                    snowballParticleTasks.remove(snowball);
                    return;
                }
                Vector velocity = snowball.getVelocity();
                Vector direction = velocity.clone().normalize();
                snowball.getWorld().spawnParticle(Particle.CLOUD, snowball.getLocation().subtract(direction.multiply(0.5)), 5, 0.1, 0.1, 0.1);
            }
        };
        particleTask.runTaskTimer(plugin, 0L, 1L);
        snowballParticleTasks.put(snowball, particleTask);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Snowball && event.getEntity().hasMetadata("ICE_SKILL")) {
            Snowball snowball = (Snowball) event.getEntity();
            BukkitRunnable particleTask = snowballParticleTasks.remove(snowball);
            if (particleTask != null) {
                particleTask.cancel();
            }

            if (event.getHitEntity() instanceof Player) {
                Player target = (Player) event.getHitEntity();
                long freezeTime = config.getLong("skills.ice.freeze_time", 5) * 20;
                frozenPlayers.put(target.getUniqueId(), System.currentTimeMillis() + freezeTime * 50);

                BukkitRunnable iceParticleTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (frozenPlayers.containsKey(target.getUniqueId())) {
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 0));
                            target.getWorld().spawnParticle(Particle.SNOWBALL, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5);
                        } else {
                            cancel();
                        }
                    }
                };
                iceParticleTask.runTaskTimer(plugin, 0L, 10L);
                playerParticleTasks.put(target.getUniqueId(), iceParticleTask);
                movePlayerToLowestBlock(target);

                sendMessage(target, "frozen");
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (frozenPlayers.containsKey(player.getUniqueId())) {
            long freezeEndTime = frozenPlayers.get(player.getUniqueId());
            if (System.currentTimeMillis() < freezeEndTime) {
                event.setCancelled(true);
            } else {
                frozenPlayers.remove(player.getUniqueId());
                sendMessage(player, "thawed");

                BukkitRunnable particleTask = playerParticleTasks.remove(player.getUniqueId());
                if (particleTask != null) {
                    particleTask.cancel();
                }
            }
        }
    }

    private void activateHealSkill(Player player) {
        double healAmount = config.getDouble("skills.heal.heal_amount", 3);
        double newHealth = Math.min(player.getHealth() + healAmount * 2, player.getMaxHealth());
        player.setHealth(newHealth);
        sendMessage(player, "healed", "{healAmount}", String.valueOf(healAmount));
    }

    private void activateKnockbackSkill(Player player) {
        Player target = null;
        double closestDistance = 5;
        for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
            if (entity instanceof Player) {
                double distance = entity.getLocation().distance(player.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    target = (Player) entity;
                }
            }
        }

        if (target != null) {
            double knockbackDistance = config.getDouble("skills.knockback.distance", 20);
            Vector direction = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(knockbackDistance);
            target.setVelocity(direction);
            sendMessage(player, "knocked_back", "{targetName}", target.getName());
        } else {
            sendMessage(player, "no_players_nearby", "{radius}", "5");
        }
    }

    private void movePlayerToLowestBlock(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world != null) {
            int x = location.getBlockX();
            int z = location.getBlockZ();
            int y = location.getBlockY();

            for (int i = y; i >= 0; i--) {
                Location checkLocation = new Location(world, x, i, z);
                if (checkLocation.getBlock().getType().isSolid()) {
                    player.teleport(new Location(world, x, i + 1, z));
                    break;
                }
            }
        }
    }

    private void activateBerserkSkill(Player player) {
        int strengthLevel = config.getInt("skills.berserk.strength_level", 7);
        int strengthDuration = config.getInt("skills.berserk.strength_duration", 5) * 20;
        int slownessLevel = config.getInt("skills.berserk.slowness_level", 3);
        int slownessDuration = config.getInt("skills.berserk.slowness_duration", 10) * 20;

        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, strengthDuration, strengthLevel - 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slownessDuration, slownessLevel - 1));
        sendMessage(player, "berserk_activated");
    }
}

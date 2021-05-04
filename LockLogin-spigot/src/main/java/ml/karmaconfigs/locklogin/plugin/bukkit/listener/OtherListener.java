package ml.karmaconfigs.locklogin.plugin.bukkit.listener;

import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class OtherListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onDamage(EntityDamageEvent e) {
        if (!e.isCancelled()) {
            if (!e.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
                Entity entity = e.getEntity();
                if (entity instanceof Player) {
                    Player player = (Player) entity;
                    User user = new User(player);
                    if (user.isLockLoginUser()) {
                        ClientSession session = user.getSession();
                        if (session.isValid()) {
                            if (!session.isLogged() || !session.isTempLogged() || !session.isCaptchaLogged()) {
                                e.setCancelled(true);

                                if (e.getCause().equals(EntityDamageEvent.DamageCause.VOID)) {
                                    Block highest = player.getWorld().getHighestBlockAt(player.getLocation());
                                    player.teleport(highest.getLocation().add(0D, 1D, 0D));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onDamageByEntity(EntityDamageByEntityEvent e) {
        if (!e.isCancelled()) {
            Entity target = e.getEntity();
            Entity damager = e.getDamager();

            Message messages = new Message();

            if (target instanceof Player) {
                Player victim = (Player) target;
                User tar_user = new User(victim);

                if (tar_user.isLockLoginUser()) {
                    ClientSession tar_session = tar_user.getSession();
                    if (tar_session.isValid()) {
                        if (!tar_session.isLogged() || !tar_session.isTempLogged() || !tar_session.isCaptchaLogged()) {
                            if (damager instanceof Player) {
                                Player attacker = (Player) damager;
                                User at_user = new User(attacker);

                                at_user.send(messages.prefix() + messages.notVerified(victim));
                                e.setCancelled(true);
                            } else {
                                e.setCancelled(true);
                                damager.remove();
                            }
                        }
                    }
                }
            } else {
                if (damager instanceof Player) {
                    Player attacker = (Player) damager;
                    User at_user = new User(attacker);

                    if (at_user.isLockLoginUser()) {
                        ClientSession session = at_user.getSession();
                        e.setCancelled(!session.isLogged() || !session.isTempLogged() || !session.isCaptchaLogged());
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onMove(PlayerMoveEvent e) {
        if (!e.isCancelled()) {
            Player player = e.getPlayer();
            User user = new User(player);
            if (user.isLockLoginUser()) {
                ClientSession session = user.getSession();

                if (session.isValid()) {
                    if (!session.isLogged() || !session.isTempLogged()) {
                        Block from = e.getFrom().getBlock();

                        if (e.getTo() != null) {
                            Block to = e.getTo().getBlock();

                            if (from.getLocation().getX() != to.getLocation().getX() || from.getLocation().getZ() != to.getLocation().getZ()) {
                                e.setCancelled(true);
                            } else {
                                e.setCancelled(to.getLocation().getY() > from.getLocation().getY());
                            }
                        }
                    }
                } else {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onBlockPace(BlockPlaceEvent e) {
        if (!e.isCancelled()) {
            Player player = e.getPlayer();
            User user = new User(player);
            ClientSession session = user.getSession();

            if (user.isLockLoginUser()) {
                if (session.isValid()) {
                    e.setCancelled(!session.isCaptchaLogged() || !session.isLogged() || !session.isTempLogged());
                } else {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onBlockBreak(BlockBreakEvent e) {
        if (!e.isCancelled()) {
            Player player = e.getPlayer();
            User user = new User(player);
            ClientSession session = user.getSession();

            if (user.isLockLoginUser()) {
                if (session.isValid()) {
                    e.setCancelled(!session.isCaptchaLogged() || !session.isLogged() || !session.isTempLogged());
                } else {
                    e.setCancelled(true);
                }
            }
        }
    }

    //Prevent player hunger level going down if not logged
    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onHunger(FoodLevelChangeEvent e) {
        Entity entity = e.getEntity();
        if (entity instanceof Player) {
            Player player = (Player) entity;
            User user = new User(player);
            ClientSession session = user.getSession();

            if (user.isLockLoginUser()) {
                if (session.isValid()) {
                    e.setCancelled(!session.isCaptchaLogged() || !session.isLogged() || !session.isTempLogged());
                } else {
                    e.setCancelled(true);
                }
            }
        }
    }
}

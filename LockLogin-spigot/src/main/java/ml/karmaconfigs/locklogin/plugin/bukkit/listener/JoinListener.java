package ml.karmaconfigs.locklogin.plugin.bukkit.listener;

import ml.karmaconfigs.api.bukkit.Console;
import ml.karmaconfigs.api.bukkit.reflections.BarMessage;
import ml.karmaconfigs.api.bukkit.timer.AdvancedPluginTimer;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountID;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.modules.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.modules.event.user.UserJoinEvent;
import ml.karmaconfigs.locklogin.api.modules.event.user.UserPostJoinEvent;
import ml.karmaconfigs.locklogin.api.modules.event.user.UserPreJoinEvent;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.client.OfflineClient;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.Spawn;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.lock.LockedAccount;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.lock.LockedData;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.ClientVisor;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.SessionCheck;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.security.BruteForce;
import ml.karmaconfigs.locklogin.plugin.common.security.client.AccountData;
import ml.karmaconfigs.locklogin.plugin.common.security.client.IpData;
import ml.karmaconfigs.locklogin.plugin.common.security.client.Name;
import ml.karmaconfigs.locklogin.plugin.common.security.client.Proxy;
import ml.karmaconfigs.locklogin.plugin.common.utils.InstantParser;
import ml.karmaconfigs.locklogin.plugin.common.utils.UUIDGen;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.bukkit.event.server.ServerListPingEvent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;
import static ml.karmaconfigs.locklogin.plugin.bukkit.plugin.PluginPermission.altInfo;

public final class JoinListener implements Listener {

    private final static Map<InetAddress, String> verified = new HashMap<>();

    private static final String IPV4_REGEX =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static final Pattern IPv4_PATTERN = Pattern.compile(IPV4_REGEX);

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onPreLogin_APICall(AsyncPlayerPreLoginEvent e) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        if (!config.isBungeeCord()) {
            UserPreJoinEvent event = new UserPreJoinEvent(e.getAddress(), e.getUniqueId(), e.getName(), e);
            JavaModuleManager.callEvent(event);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onLogin_APICall(PlayerLoginEvent e) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        if (!config.isBungeeCord()) {
            OfflinePlayer offline = plugin.getServer().getOfflinePlayer(e.getPlayer().getUniqueId());

            UserJoinEvent event = new UserJoinEvent(e.getAddress(), e.getPlayer().getUniqueId(), offline.getName(), e);
            JavaModuleManager.callEvent(event);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onPostLogin_APICall(PlayerJoinEvent e) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        if (!config.isBungeeCord()) {
            UserPostJoinEvent event = new UserPostJoinEvent(fromPlayer(e.getPlayer()), e);
            JavaModuleManager.callEvent(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onServerPing(ServerListPingEvent e) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        if (!config.isBungeeCord())
            verified.put(e.getAddress(), "");
    }

    @SuppressWarnings("all")
    @EventHandler(priority = EventPriority.LOWEST)
    public final void onPreLogin(AsyncPlayerPreLoginEvent e) {
        Message messages = new Message();
        InetAddress ip = e.getAddress();

        String address = "null";
        try {
            address = ip.getHostAddress();
        } catch (Throwable ignored) {
        }
        if (e.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.ALLOWED)) {
            try {
                if (validateIP(ip)) {
                    PluginConfiguration config = CurrentPlatform.getConfiguration();

                    if (!config.isBungeeCord()) {
                        UUID gen_uuid = UUIDGen.getUUID(e.getName());
                        UUID tar_uuid = e.getUniqueId();

                        if (config.registerOptions().maxAccounts() > 0) {
                            AccountData data = new AccountData(ip, AccountID.fromUUID(e.getUniqueId()));

                            if (data.allow(config.registerOptions().maxAccounts())) {
                                data.save();

                                int amount = data.getAlts().size();
                                if (amount > 2) {
                                    for (Player online : plugin.getServer().getOnlinePlayers()) {
                                        if (online.hasPermission(altInfo())) {
                                            User user = new User(online);
                                            user.send(messages.prefix() + messages.altFound(e.getName(), amount - 1));
                                        }
                                    }

                                    if (!messages.altFound(e.getName(), amount).replaceAll("\\s", "").isEmpty())
                                        Console.send(messages.prefix() + messages.altFound(e.getName(), amount - 1));
                                }
                            } else {
                                e.disallow(PlayerPreLoginEvent.Result.KICK_FULL, StringUtils.toColor(messages.maxRegisters()));
                                return;
                            }
                        }

                        if (config.bruteForceOptions().getMaxTries() > 0) {
                            BruteForce protection = new BruteForce(ip);
                            if (protection.isBlocked()) {
                                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, StringUtils.toColor(messages.ipBlocked(protection.getBlockLeft())));
                                return;
                            }
                        }

                        if (config.antiBot()) {
                            if (verified.containsKey(ip)) {
                                String name = verified.getOrDefault(ip, "");

                                if (!name.replaceAll("\\s", "").isEmpty() && !name.equals(e.getName())) {
                                    //The anti bot is like a whitelist, only players in a certain list can join, the difference with LockLogin is that players are
                                    //assigned to an IP, so the anti bot security is reinforced
                                    e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, StringUtils.toColor(messages.antiBot()));
                                    return;
                                } else {
                                    if (name.replaceAll("\\s", "").isEmpty())
                                        verified.put(ip, name);
                                }
                            } else {
                                //The anti bot is like a whitelist, only players in a certain list can join, the difference with LockLogin is that players are
                                //assigned to an IP, so the anti bot security is reinforced
                                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, StringUtils.toColor(messages.antiBot()));
                                return;
                            }
                        }

                        if (!gen_uuid.equals(tar_uuid)) {
                            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, StringUtils.toColor(messages.uuidFetchError()));
                            return;
                        }

                        Name name = new Name(e.getName());
                        name.check();

                        if (name.notValid()) {
                            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, StringUtils.toColor(messages.illegalName(name.getInvalidChars())));
                            return;
                        }

                        Player player = plugin.getServer().getPlayer(tar_uuid);
                        if (player != null) {
                            if (config.allowSameIP()) {
                                InetSocketAddress playerIP = player.getAddress();
                                if (playerIP != null) {
                                    if (!playerIP.getAddress().equals(ip)) {
                                        e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, StringUtils.toColor(messages.alreadyPlaying()));
                                        return;
                                    }
                                }
                            }
                        }

                        OfflineClient offline = new OfflineClient(e.getName());
                        AccountManager manager = offline.getAccount();
                        if (manager != null) {
                            LockedAccount account = new LockedAccount(manager.getUUID());
                            LockedData data = account.getData();

                            if (data.isLocked()) {
                                String administrator = data.getAdministrator();
                                Instant date = data.getLockDate();
                                InstantParser parser = new InstantParser(date);
                                String dateString = parser.getDay() + " " + parser.getMonth() + " " + parser.getYear();

                                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, StringUtils.toColor(messages.forcedAccountRemoval(administrator + " [ " + dateString + " ]")));
                                logger.scheduleLog(Level.WARNING, "Client {0} tried to join, but his account was blocked by {1} on {2}", e.getName(), administrator, dateString);
                                return;
                            }
                        }

                        //Allow the player at the eyes of the plugin
                        e.allow();

                        //This is only required while in bungeecord mode
                        if (config.isBungeeCord()) {
                            AdvancedPluginTimer timer = new AdvancedPluginTimer(plugin, 5, false);
                            timer.addActionOnEnd(() -> {
                                Player online = plugin.getServer().getPlayer(e.getUniqueId());
                                if (online != null && online.isOnline()) {
                                    User user = new User(online);
                                    if (!user.getSession().isValid())
                                        user.kick(messages.bungeeProxy());
                                }
                            }).start();
                        }
                    }
                } else {
                    e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, StringUtils.toColor(messages.ipProxyError()));
                    logger.scheduleLog(Level.GRAVE, "Player {0} tried to join with an invalid IP address ( {1} ), his connection got rejected with ip is proxy message", e.getName(), address);
                }
            } catch (Throwable ex) {
                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, StringUtils.toColor(messages.ipProxyError()));
                logger.scheduleLog(Level.GRAVE, "Player {0} tried to join with an invalid IP address ( {1} ), his connection got rejected with ip is proxy message", e.getName(), address);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onLogin(PlayerLoginEvent e) {
        if (e.getResult().equals(PlayerLoginEvent.Result.ALLOWED)) {
            Player player = e.getPlayer();
            Message messages = new Message();
            PluginConfiguration config = CurrentPlatform.getConfiguration();

            User user = new User(player);
            if (!user.isLockLoginUser())
                user.applyLockLoginUser();

            if (!config.isBungeeCord()) {
                IpData data = new IpData(e.getAddress());
                int amount = data.getClonesAmount();

                if (amount + 1 == config.accountsPerIP()) {
                    e.disallow(PlayerLoginEvent.Result.KICK_OTHER, StringUtils.toColor(messages.maxIP()));
                    return;
                }
                data.addClone();

                ClientSession session = user.getSession();
                session.validate();
                session.setPinLogged(false);
                session.set2FALogged(false);
                session.setLogged(false);

                //Automatically mark players as captcha verified if captcha is disabled
                if (!config.captchaOptions().isEnabled())
                    session.setCaptchaLogged(true);

                if (config.enableSpawn()) {
                    Spawn spawn = new Spawn(player.getWorld());
                    spawn.load();

                    spawn.teleport(player);
                }

                //Allow the player at the eyes of the plugin
                e.allow();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onPostLogin(PlayerJoinEvent e) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        Player player = e.getPlayer();
        InetSocketAddress ip = player.getAddress();
        User user = new User(player);

        if (!config.isBungeeCord()) {
            Message messages = new Message();

            String join_message = e.getJoinMessage();

            Proxy proxy = new Proxy(ip);
            if (proxy.isProxy()) {
                user.kick(messages.ipProxyError());
                return;
            }

            user.savePotionEffects();
            user.applySessionEffects();

            if (config.clearChat()) {
                for (int i = 0; i < 150; i++)
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> player.sendMessage(""));
            }

            ClientSession session = user.getSession();

            BarMessage bar = new BarMessage(player, messages.captcha(session.getCaptcha()));
            if (!session.isCaptchaLogged())
                bar.send(true);

            SessionCheck check = new SessionCheck(player, target -> {
                bar.setMessage("");
                bar.stop();
            }, target -> {
                bar.setMessage("");
                bar.stop();
            });

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, check);

            if (join_message != null)
                plugin.getServer().getOnlinePlayers().forEach(target -> target.sendMessage(StringUtils.toColor(join_message)));
        }

        if (player.getLocation().getBlock().getType().name().contains("PORTAL"))
            user.setTempSpectator(true);

        ClientVisor visor = new ClientVisor(player);
        visor.hide();
    }

    /**
     * Check if the ip is valid
     *
     * @param ip the ip address
     * @return if the ip is valid
     */
    private boolean validateIP(final InetAddress ip) {
        if (StringUtils.isNullOrEmpty(ip.getHostAddress())) {
            return false;
        }

        Matcher matcher = IPv4_PATTERN.matcher(ip.getHostAddress());
        return matcher.matches();
    }
}

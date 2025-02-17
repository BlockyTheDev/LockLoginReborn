package eu.locklogin.plugin.bungee.util.player;

/*
 * Private GSA code
 *
 * The use of this code
 * without GSA team authorization
 * will be a violation of
 * terms of use determined
 * in <a href="http://karmaconfigs.cf/license/"> here </a>
 * or (fallback domain) <a href="https://karmaconfigs.github.io/page/license"> here </a>
 */

import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.security.GoogleAuthFactory;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.other.name.AccountNameDatabase;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.file.options.LoginConfig;
import eu.locklogin.api.file.options.RegisterConfig;
import eu.locklogin.api.module.plugin.api.event.user.SessionInitializationEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.util.enums.Manager;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.permissibles.Permission;
import eu.locklogin.plugin.bungee.plugin.sender.DataSender;
import eu.locklogin.plugin.bungee.util.files.Proxy;
import ml.karmaconfigs.api.bungee.makeiteasy.BossMessage;
import ml.karmaconfigs.api.bungee.makeiteasy.TitleMessage;
import ml.karmaconfigs.api.common.boss.BossColor;
import ml.karmaconfigs.api.common.boss.BossProvider;
import ml.karmaconfigs.api.common.boss.ProgressiveBar;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import net.md_5.bungee.api.ServerConnectRequest;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static eu.locklogin.plugin.bungee.LockLogin.*;

/**
 * Initialize the user
 */
public final class User {

    private final static KarmaSource lockLogin = APISource.loadProvider("LockLogin");
    private final static Map<UUID, AccountManager> managers = new ConcurrentHashMap<>();
    private final static Map<UUID, SessionCheck<ProxiedPlayer>> sessionChecks = new ConcurrentHashMap<>();
    @SuppressWarnings("FieldMayBeFinal") //This could be modified by the cache loader, so it can't be final
    private static Map<UUID, ClientSession> sessions = new ConcurrentHashMap<>();
    private final ProxiedPlayer player;

    /**
     * Initialize the user
     *
     * @param _player the player
     * @throws IllegalStateException as part of session and account managers setup, if some of
     *                               they are null or can't be initialized, this exception will be thrown and the plugin will be
     *                               instantly disabled
     */
    public User(final ProxiedPlayer _player) throws IllegalStateException {
        player = _player;

        User loaded = UserDatabase.loadUser(player);
        if (loaded == null) {
            if (CurrentPlatform.isValidAccountManager()) {
                AccountManager manager = CurrentPlatform.getAccountManager(Manager.CUSTOM, AccountID.fromUUID(player.getUniqueId()));

                if (manager == null) {
                    throw new IllegalStateException("Cannot initialize user with a null player account manager");
                } else {
                    AccountNameDatabase database = new AccountNameDatabase(player.getUniqueId());
                    lockLogin.async().queue(() -> {
                        database.assign(StringUtils.stripColor(player.getName()));
                        database.assign(StringUtils.stripColor(player.getDisplayName()));
                    });

                    //Try to fix the empty manager values that are
                    //required
                    if (manager.exists()) {
                        String name = manager.getName();
                        AccountID id = manager.getUUID();

                        if (StringUtils.isNullOrEmpty(name))
                            manager.setName(StringUtils.stripColor(player.getDisplayName()));

                        if (StringUtils.isNullOrEmpty(id.getId()))
                            manager.saveUUID(AccountID.fromUUID(player.getUniqueId()));
                    }

                    managers.put(player.getUniqueId(), manager);
                }
            } else {
                throw new IllegalStateException("Cannot initialize user with an invalid player account manager");
            }

            if (!sessions.containsKey(player.getUniqueId())) {
                if (CurrentPlatform.isValidSessionManager()) {
                    ClientSession session = CurrentPlatform.getSessionManager(null);

                    if (session == null) {
                        throw new IllegalStateException("Cannot initialize user with a null session manager");
                    } else {
                        session.initialize();

                        ModulePlayer modulePlayer = new ModulePlayer(
                                player.getName(),
                                player.getUniqueId(),
                                session,
                                managers.get(player.getUniqueId()),
                                getIp(player.getSocketAddress()));
                        CurrentPlatform.connectPlayer(modulePlayer, player);

                        Event event = new SessionInitializationEvent(modulePlayer, session, null);
                        ModulePlugin.callEvent(event);

                        sessions.put(player.getUniqueId(), session);
                    }
                } else {
                    throw new IllegalStateException("Cannot initialize user with a null session manager");
                }
            }

            UserDatabase.insert(player, this);
        } else {
            ModulePlayer modulePlayer = CurrentPlatform.getServer().getPlayer(player.getUniqueId());
            if (modulePlayer == null || modulePlayer.getAddress() == null) {
                InetAddress ip = getIp(player.getSocketAddress());

                modulePlayer = new ModulePlayer(
                        player.getName(),
                        player.getUniqueId(),
                        sessions.get(player.getUniqueId()),
                        managers.get(player.getUniqueId()),
                        ip);
                CurrentPlatform.connectPlayer(modulePlayer, player);
            }
        }
    }

    /**
     * Get the map of sessions
     *
     * @return the map of sessions
     */
    public static Map<UUID, ClientSession> getSessionMap() {
        return new HashMap<>(sessions);
    }

    /**
     * Get the player session
     *
     * @param player the player
     * @return the player session
     */
    public static ClientSession getSession(final ProxiedPlayer player) {
        return sessions.get(player.getUniqueId());
    }

    /**
     * Get the player account manager
     *
     * @param player the player
     * @return the player account manager
     */
    public static AccountManager getManager(final ProxiedPlayer player) {
        return managers.get(player.getUniqueId());
    }

    /**
     * Send a message to the player
     *
     * @param message the message to send
     */
    public void send(final String message) {
        String[] parsed = parseMessage(message);

        PluginMessages messages = CurrentPlatform.getMessages();

        if (parsed.length > 1) {
            for (String str : parsed)
                player.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(str)));
        } else {
            //Make sure to avoid null messages
            if (parsed.length == 1) {
                if (!StringUtils.isNullOrEmpty(parsed[0].replace(messages.prefix(), ""))) {
                    player.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(parsed[0])));
                }
            }
        }
    }

    /**
     * Send a message to the player
     *
     * @param message the message to send
     */
    public void send(final TextComponent message) {
        String[] text = parseMessage(message.getText());
        StringBuilder builder = new StringBuilder();
        for (String str : text) builder.append(str);

        message.setText(builder.toString());

        player.sendMessage(message);
    }

    /**
     * Send a title and subtitle to the player
     *
     * @param title    the title to send
     * @param subtitle the subtitle to send
     * @param si the time to show in
     * @param ki the time keep in
     * @param hi the time to hide in
     */
    public void send(final String title, final String subtitle, final int si, final int ki, final int hi) {
        String[] tmpTitle = parseMessage(title);
        String[] tmpSub = parseMessage(subtitle);

        StringBuilder titleBuilder = new StringBuilder();
        StringBuilder subtitleBuilder = new StringBuilder();

        for (String str : tmpTitle) titleBuilder.append(str).append(" ");
        for (String str : tmpSub) subtitleBuilder.append(str).append(" ");

        TitleMessage titleMessage = new TitleMessage(player,
                StringUtils.replaceLast(titleBuilder.toString(), " ", ""),
                StringUtils.replaceLast(subtitleBuilder.toString(), " ", ""));
        titleMessage.send(si, ki, hi);
    }

    /**
     * Check the player server
     *
     * @param index the server try index
     */
    public void checkServer(final int index) {
        ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();
        if (proxy.sendToServers()) {
            ClientSession session = getSession();

            if (session.isValid()) {
                if (session.isLogged() && session.isTempLogged()) {
                    if (Proxy.inAuth(player) && Proxy.lobbiesValid()) {
                        List<ServerInfo> lobbies = proxy.lobbyServers(ServerInfo.class);
                        if (lobbies.size() > index) {
                            ServerInfo lInfo = lobbies.get(index);
                            player.connect(ServerConnectRequest.builder().target(lInfo).connectTimeout(10).reason(ServerConnectEvent.Reason.PLUGIN).callback((result, error) -> {
                                if (error != null || result == ServerConnectRequest.Result.FAIL) {
                                    checkServer(index + 1);

                                    if (error != null) {
                                        logger.scheduleLog(Level.GRAVE, error);
                                    }
                                    logger.scheduleLog(Level.INFO, "Failed to connect client {0} to server {1}", player.getUniqueId(), lInfo.getName());
                                }
                            }).build());
                        }
                    }
                } else {
                    if (!Proxy.inAuth(player) && Proxy.authsValid()) {
                        List<ServerInfo> auths = proxy.authServer(ServerInfo.class);
                        if (auths.size() > index) {
                            ServerInfo aInfo = auths.get(index);
                            player.connect(ServerConnectRequest.builder().target(aInfo).connectTimeout(10).reason(ServerConnectEvent.Reason.PLUGIN).callback((result, error) -> {
                                if (error != null || result == ServerConnectRequest.Result.FAIL) {
                                    checkServer(index + 1);

                                    if (error != null) {
                                        logger.scheduleLog(Level.GRAVE, error);
                                    }
                                    logger.scheduleLog(Level.INFO, "Failed to connect client {0} to server {1}", player.getUniqueId(), aInfo.getName());
                                }
                            }).build());
                        }
                    }
                }
            }
        }
    }

    /**
     * Perform the command as the player
     *
     * @param command the command to perform
     */
    public void performCommand(final String command) {
        if (plugin.getProxy().getDisabledCommands().stream().noneMatch(command::contains))
            plugin.getProxy().getPluginManager().dispatchCommand(player, command);
    }

    /**
     * Kick the player with the specified reason
     *
     * @param reason the reason of the kick
     */
    public synchronized void kick(final String reason) {
        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            String[] parsed = parseMessage(reason);

            if (parsed.length > 1) {
                StringBuilder kickBuilder = new StringBuilder();
                for (String string : parsed)
                    kickBuilder.append(string).append("\n");

                player.disconnect(TextComponent.fromLegacyText(StringUtils.toColor(StringUtils.replaceLast(kickBuilder.toString(), "\n", ""))));
            } else {
                player.disconnect(TextComponent.fromLegacyText(StringUtils.toColor(parsed[0])));
            }
        }, 0, TimeUnit.SECONDS);
    }

    /**
     * Apply the session potion effect
     * types
     */
    public synchronized void applySessionEffects() {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        DataSender.MessageDataBuilder builder = DataSender.getBuilder(DataType.EFFECTS, DataSender.CHANNEL_PLAYER, player);
        if (isRegistered()) {
            LoginConfig login = config.loginOptions();

            builder.addBoolData(login.blindEffect());
        } else {
            RegisterConfig register = config.registerOptions();

            builder.addBoolData(register.blindEffect());
        }

        DataSender.send(player, builder.build());
    }

    /**
     * Restore the player potion effects
     */
    public synchronized void restorePotionEffects() {
        DataSender.MessageData data = DataSender.getBuilder(DataType.EFFECTS, DataSender.CHANNEL_PLAYER, player)
                .addBoolData(false).build();
        DataSender.send(player, data);
    }

    /**
     * Remove the user session check
     */
    public void removeSessionCheck() {
        sessionChecks.remove(player.getUniqueId());
    }

    /**
     * Get the client session checker
     *
     * @return the client session checker
     */
    public SessionCheck<ProxiedPlayer> getChecker() {
        SessionCheck<ProxiedPlayer> checker = sessionChecks.getOrDefault(player.getUniqueId(), null);
        if (checker == null) {
            ModulePlayer sender = getModule();
            if (sender == null) {
                sender = new ModulePlayer(
                        player.getName(),
                        player.getUniqueId(),
                        getSession(),
                        managers.get(player.getUniqueId()),
                        getIp(player.getSocketAddress()));

                CurrentPlatform.connectPlayer(sender, player);
            }

            /*
            So... there was a bug in where sometimes the boss bar would be visible
            even while disabled, that's because I was literally creating the boss
            bar ignoring that option, and then hiding it if disabled.

            The best solution is to just not create it if specified
             */
            BossProvider<ProxiedPlayer> message = null;
            int time = CurrentPlatform.getConfiguration().registerOptions().timeOut();
            if (getManager().isRegistered()) {
                time = CurrentPlatform.getConfiguration().loginOptions().timeOut();

                if (CurrentPlatform.getConfiguration().loginOptions().hasBossBar()) {
                    message = new BossMessage(plugin, CurrentPlatform.getMessages().loginBar("&a", time), time).color(BossColor.GREEN).progress(ProgressiveBar.DOWN);
                }
            } else {
                if (CurrentPlatform.getConfiguration().registerOptions().hasBossBar()) {
                    message = new BossMessage(plugin, CurrentPlatform.getMessages().registerBar("&a", time), time).color(BossColor.GREEN).progress(ProgressiveBar.DOWN);
                }
            }

            checker = new SessionCheck<>(plugin, sender, message);
            sessionChecks.put(player.getUniqueId(), checker);
        }

        return checker;
    }

    /**
     * Get the module player of this player
     *
     * @return this player module player
     */
    public ModulePlayer getModule() {
        return CurrentPlatform.getServer().getPlayer(player.getUniqueId());
    }

    /**
     * Get the current player account manager
     *
     * @return the player account manager
     */
    @NotNull
    public AccountManager getManager() {
        return managers.get(player.getUniqueId());
    }

    /**
     * Get the current player session
     *
     * @return the player session
     */
    @NotNull
    public ClientSession getSession() {
        return sessions.get(player.getUniqueId());
    }

    /**
     * Get the user google auth token factory
     *
     * @return the user google auth token
     * factory
     */
    public GoogleAuthFactory getTokenFactory() {
        return new GoogleAuthFactory(player.getUniqueId(), StringUtils.toColor(player.getDisplayName()));
    }

    /**
     * Check if the user is registered or not
     *
     * @return if the user is registered
     */
    public boolean isRegistered() {
        AccountManager manager = getManager();
        String password = manager.getPassword();

        return !password.replaceAll("\\s", "").isEmpty();
    }

    /**
     * Check if the user has the specified permission
     *
     * @param permission the permission
     * @return if the player has the permission
     */
    public boolean hasPermission(final Permission permission) {
        return permission.isPermissible(player, permission);
    }

    /**
     * Parse the message, replacing "{newline}"
     * with "\n"
     *
     * @return the parsed message
     */
    private String[] parseMessage(final String official) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        if (official.contains("{newline}") || official.contains("\\n") || official.contains("\n")) {
            String messageData = official
                    .replace("{newline}", "\n")
                    .replace("\\n", "\n");
            String[] messages = messageData.split("\\r?\\n");

            for (int i = 0; i < messages.length; i++) {
                String previous = (i - 1 >= 0 ? messages[i - 1] : "");
                String lastColor = StringUtils.getLastColor(previous);
                String message = messages[i];

                ClientSession session = getSession();
                if (session.isCaptchaLogged()) {
                    message = message.replace("{captcha}", "")
                            .replace("<captcha>", "");
                } else {
                    message = message.replace("{captcha}", "<captcha>");
                }
                message = message
                        .replace("{player}", StringUtils.stripColor(player.getDisplayName()))
                        .replace("{ServerName}", config.serverName());

                messages[i] = lastColor + message;
            }

            return messages;
        } else {
            ClientSession session = getSession();

            return new String[]{official
                    .replace("{captcha}", (session.isCaptchaLogged() ? "" : "<captcha>"))
                    .replace("<captcha>", (session.isCaptchaLogged() ? "" : "<captcha>"))
                    .replace("{player}", StringUtils.stripColor(player.getDisplayName()))
                    .replace("{ServerName}", config.serverName())};
        }
    }
}

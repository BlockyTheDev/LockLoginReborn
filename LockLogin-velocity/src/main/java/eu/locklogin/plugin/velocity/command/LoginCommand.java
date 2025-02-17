package eu.locklogin.plugin.velocity.command;

/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 2.1, February 1999
 * <p>
 * Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 * <p>
 * [This is the first released version of the Lesser GPL.  It also counts
 * as the successor of the GNU Library Public License, version 2, hence
 * the version number 2.1.]
 */

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.security.BruteForce;
import eu.locklogin.api.common.security.Password;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.options.BruteForceConfig;
import eu.locklogin.api.file.options.LoginConfig;
import eu.locklogin.api.module.plugin.api.event.user.UserAuthenticateEvent;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.velocity.command.util.BungeeLikeCommand;
import eu.locklogin.plugin.velocity.command.util.SystemCommand;
import eu.locklogin.plugin.velocity.permissibles.PluginPermission;
import eu.locklogin.plugin.velocity.plugin.sender.DataSender;
import eu.locklogin.plugin.velocity.util.player.User;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.string.StringUtils;

import java.net.InetAddress;
import java.util.List;

import static eu.locklogin.plugin.velocity.LockLogin.*;
import static eu.locklogin.plugin.velocity.plugin.sender.DataSender.CHANNEL_PLAYER;
import static eu.locklogin.plugin.velocity.plugin.sender.DataSender.MessageData;

@SystemCommand(command = "login", aliases = {"log"})
public final class LoginCommand extends BungeeLikeCommand {

    /**
     * Initialize the bungee like command
     *
     * @param name    the command name
     * @param aliases the command aliases
     */
    public LoginCommand(final String name, final List<String> aliases) {
        super(name, aliases.toArray(new String[0]));
    }

    /**
     * Execute this command with the specified sender and arguments.
     *
     * @param sender the executor of this command
     * @param args   arguments used to invoke this command
     */
    @Override
    public void execute(CommandSource sender, String[] args) {
        PluginMessages messages = CurrentPlatform.getMessages();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = new User(player);

            PluginConfiguration config = CurrentPlatform.getConfiguration();

            ClientSession session = user.getSession();
            if (session.isValid()) {
                if (!session.isLogged()) {
                    AccountManager manager = user.getManager();
                    if (!manager.exists())
                        if (manager.create()) {
                            logger.scheduleLog(Level.INFO, "Created account of player {0}", StringUtils.stripColor(player.getUsername()));
                        } else {
                            logger.scheduleLog(Level.GRAVE, "Couldn't create account of player {0}", StringUtils.stripColor(player.getUsername()));

                            user.send(messages.prefix() + properties.getProperty("could_not_create_user", "&5&oWe're sorry, but we couldn't create your account"));
                        }

                    if (!user.isRegistered()) {
                        user.send(messages.prefix() + messages.register());
                    } else {
                        String password;

                        switch (args.length) {
                            case 0:
                                user.send(messages.prefix() + messages.login());
                                break;
                            case 1:
                                if (session.isCaptchaLogged()) {
                                    password = args[0];

                                    Password checker = new Password(password);
                                    checker.addInsecure(player.getUsername(), player.getGameProfile().getName(), StringUtils.stripColor(player.getUsername()), StringUtils.stripColor(player.getGameProfile().getName()));

                                    BruteForce protection = null;
                                    InetAddress ip = player.getRemoteAddress().getAddress();
                                    if (ip != null)
                                        protection = new BruteForce(ip);

                                    CryptoFactory utils = CryptoFactory.getBuilder().withPassword(password).withToken(manager.getPassword()).build();
                                    if (utils.validate()) {
                                        if (!checker.isSecure()) {
                                            user.send(messages.prefix() + messages.loginInsecure());

                                            if (config.blockUnsafePasswords()) {
                                                manager.setPassword(null);

                                                SessionCheck<Player> check = user.getChecker().whenComplete(user::restorePotionEffects);
                                                plugin.getServer().getScheduler().buildTask(plugin.getContainer(), check).schedule();
                                                return;
                                            }
                                        }

                                        MessageData login = DataSender.getBuilder(DataType.SESSION, CHANNEL_PLAYER, player).build();

                                        if (utils.needsRehash(config.passwordEncryption())) {
                                            //Set the player password again to update his hash
                                            manager.setPassword(password);
                                            logger.scheduleLog(Level.INFO, "Updated password hash of {0} from {1} to {2}",
                                                    StringUtils.stripColor(player.getUsername()),
                                                    utils.getTokenHash().name(),
                                                    config.passwordEncryption().name());
                                        }

                                        boolean checkServer = false;
                                        if (!manager.has2FA() && !manager.hasPin()) {
                                            UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PASSWORD,
                                                    UserAuthenticateEvent.Result.SUCCESS,
                                                    user.getModule(),
                                                    messages.logged(),
                                                    null);
                                            ModulePlugin.callEvent(event);
                                            MessageData pin = DataSender.getBuilder(DataType.PIN, CHANNEL_PLAYER, player).addTextData("close").build();
                                            MessageData gauth = DataSender.getBuilder(DataType.GAUTH, CHANNEL_PLAYER, player).build();

                                            DataSender.send(player, pin);
                                            DataSender.send(player, gauth);

                                            session.set2FALogged(true);
                                            session.setPinLogged(true);

                                            user.send(messages.prefix() + event.getAuthMessage());
                                            checkServer = true;
                                        } else {
                                            UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PASSWORD,
                                                    UserAuthenticateEvent.Result.SUCCESS_TEMP,
                                                    user.getModule(),
                                                    messages.logged(),
                                                    null);
                                            ModulePlugin.callEvent(event);

                                            if (manager.hasPin()) {
                                                session.setPinLogged(false);

                                                DataSender.send(player, DataSender.getBuilder(DataType.PIN, CHANNEL_PLAYER, player).addTextData("open").build());
                                            } else {
                                                user.send(messages.prefix() + event.getAuthMessage());
                                                user.send(messages.prefix() + messages.gAuthInstructions());
                                            }
                                        }

                                        session.setLogged(true);
                                        if (protection != null)
                                            protection.success();

                                        user.restorePotionEffects();

                                        DataSender.send(player, login);

                                        if (checkServer)
                                            user.checkServer(0);

                                        if (!manager.has2FA() && config.captchaOptions().isEnabled() && user.hasPermission(PluginPermission.forceFA())) {
                                            user.performCommand("2fa setup " + password);
                                        }
                                    } else {
                                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PASSWORD,
                                                UserAuthenticateEvent.Result.ERROR,
                                                user.getModule(),
                                                messages.incorrectPassword(),
                                                null);
                                        ModulePlugin.callEvent(event);

                                        if (protection != null) {
                                            protection.fail();

                                            BruteForceConfig bruteForce = config.bruteForceOptions();
                                            LoginConfig loginConfig = config.loginOptions();

                                            if (bruteForce.getMaxTries() > 0 && protection.tries() >= bruteForce.getMaxTries()) {
                                                protection.block(bruteForce.getBlockTime());
                                                user.kick(messages.ipBlocked(protection.getBlockLeft()));
                                            } else {
                                                if (loginConfig.maxTries() > 0 && protection.tries() >= loginConfig.maxTries()) {
                                                    protection.success();
                                                    user.kick(event.getAuthMessage());
                                                } else {
                                                    user.send(messages.prefix() + event.getAuthMessage());
                                                }
                                            }
                                        } else {
                                            user.send(messages.prefix() + event.getAuthMessage());
                                        }
                                    }
                                } else {
                                    if (config.captchaOptions().isEnabled()) {
                                        user.send(messages.prefix() + messages.invalidCaptcha());
                                    }
                                }
                                break;
                            case 2:
                                if (session.isCaptchaLogged()) {
                                    user.send(messages.prefix() + messages.login());
                                } else {
                                    password = args[0];
                                    String captcha = args[1];

                                    if (session.getCaptcha().equals(captcha)) {
                                        session.setCaptchaLogged(true);

                                        user.performCommand("login " + password);
                                        DataSender.send(player, DataSender.getBuilder(DataType.CAPTCHA, DataSender.CHANNEL_PLAYER, player).build());
                                    } else {
                                        user.send(messages.prefix() + messages.invalidCaptcha());
                                    }
                                }
                                break;
                            default:
                                if (!session.isLogged()) {
                                    user.send(messages.prefix() + messages.login());
                                } else {
                                    if (session.isTempLogged()) {
                                        user.send(messages.prefix() + messages.gAuthenticate());
                                    } else {
                                        user.send(messages.prefix() + messages.alreadyLogged());
                                    }
                                }
                                break;
                        }
                    }
                } else {
                    if (session.isTempLogged()) {
                        user.send(messages.prefix() + messages.alreadyLogged());
                    } else {
                        user.send(messages.prefix() + messages.gAuthenticate());
                    }
                }
            } else {
                user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            }
        } else {
            console.send(messages.prefix() + properties.getProperty("command_not_available", "&cThis command is not available for console"));
        }
    }
}

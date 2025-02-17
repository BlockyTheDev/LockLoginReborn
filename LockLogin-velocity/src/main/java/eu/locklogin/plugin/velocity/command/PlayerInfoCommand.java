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
import com.velocitypowered.api.proxy.server.RegisteredServer;
import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.common.session.PersistentSessionData;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.InstantParser;
import eu.locklogin.api.common.utils.other.LockedAccount;
import eu.locklogin.api.common.utils.other.name.AccountNameDatabase;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.plugin.Alias;
import eu.locklogin.api.util.enums.Manager;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.velocity.command.util.BungeeLikeCommand;
import eu.locklogin.plugin.velocity.command.util.SystemCommand;
import eu.locklogin.plugin.velocity.permissibles.PluginPermission;
import eu.locklogin.plugin.velocity.plugin.sender.AccountParser;
import eu.locklogin.plugin.velocity.plugin.sender.DataSender;
import eu.locklogin.plugin.velocity.util.files.client.OfflineClient;
import eu.locklogin.plugin.velocity.util.player.User;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import net.kyori.adventure.text.Component;

import java.time.Instant;
import java.util.*;

import static eu.locklogin.plugin.velocity.LockLogin.*;

@SystemCommand(command = "playerinfo")
public final class PlayerInfoCommand extends BungeeLikeCommand {

    /**
     * Initialize the bungee like command
     *
     * @param name    the command name
     * @param aliases the command aliases
     */
    public PlayerInfoCommand(final String name, final List<String> aliases) {
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

            Set<AccountManager> accounts = new HashSet<>();
            int sent = 0;
            int max = 0;

            if (user.getSession().isValid()) {
                if (user.hasPermission(PluginPermission.infoRequest())) {
                    switch (args.length) {
                        case 0:
                            user.send(messages.prefix() + messages.infoUsage());
                            break;
                        case 1:
                            String target = args[0];
                            if (target.startsWith("@") || target.startsWith("#")) {
                                String name;
                                switch (target.substring(0, 1)) {
                                    case "@":
                                        name = target.replaceFirst("@", "");

                                        if (!name.equalsIgnoreCase("everyone") && !name.equalsIgnoreCase("persistent") && !(name.toLowerCase().startsWith("permission[") && name.endsWith("]"))) {
                                            Alias alias = new Alias(name);
                                            if (alias.exists()) {
                                                Set<AccountID> ids = alias.getUsers();
                                                for (AccountID id : ids) {
                                                    OfflineClient offline = new OfflineClient(id);
                                                    AccountManager manager = offline.getAccount();

                                                    if (manager != null)
                                                        accounts.add(manager);
                                                }

                                                for (AccountManager account : accounts) {
                                                    player.sendActionBar(Component.text().content(StringUtils.toColor("&aSending player accounts ( " + sent + " of " + max + " )")).build());

                                                    DataSender.send(player, DataSender.getBuilder(DataType.PLAYER, DataSender.PLUGIN_CHANNEL, player).addTextData(StringUtils.serialize(account)).build());
                                                    sent++;
                                                }

                                                AccountParser parser = new AccountParser(accounts);
                                                DataSender.send(player, DataSender.getBuilder(DataType.INFOGUI, DataSender.PLUGIN_CHANNEL, player).addTextData(parser.toString()).build());
                                            } else {
                                                user.send(messages.prefix() + messages.aliasNotFound(name));
                                            }
                                        } else {
                                            AccountParser parser;

                                            switch (name) {
                                                case "everyone":
                                                    for (Player online : plugin.getServer().getAllPlayers()) {
                                                        AccountManager manager = CurrentPlatform.getAccountManager(Manager.CUSTOM, AccountID.fromUUID(online.getUniqueId()));
                                                        if (manager != null)
                                                            accounts.add(manager);
                                                    }

                                                    for (AccountManager account : accounts) {
                                                        player.sendActionBar(Component.text().content(StringUtils.toColor("&aSending player accounts ( " + sent + " of " + max + " )")).build());

                                                        DataSender.send(player, DataSender.getBuilder(DataType.PLAYER, DataSender.PLUGIN_CHANNEL, player).addTextData(StringUtils.serialize(account)).build());
                                                        sent++;
                                                    }

                                                    parser = new AccountParser(accounts);
                                                    DataSender.send(player, DataSender.getBuilder(DataType.INFOGUI, DataSender.PLUGIN_CHANNEL, player).addTextData(parser.toString()).build());
                                                case "persistent":
                                                    accounts = PersistentSessionData.getPersistentAccounts();

                                                    for (AccountManager account : accounts) {
                                                        player.sendActionBar(Component.text().content(StringUtils.toColor("&aSending player accounts ( " + sent + " of " + max + " )")).build());

                                                        DataSender.send(player, DataSender.getBuilder(DataType.PLAYER, DataSender.PLUGIN_CHANNEL, player).addTextData(StringUtils.serialize(account)).build());
                                                        sent++;
                                                    }

                                                    parser = new AccountParser(accounts);
                                                    DataSender.send(player, DataSender.getBuilder(DataType.INFOGUI, DataSender.PLUGIN_CHANNEL, player).addTextData(parser.toString()).build());
                                                    break;
                                                default:
                                                    String permission = StringUtils.replaceLast(name.replaceFirst("permission\\[", ""), "]", "");

                                                    for (Player online : plugin.getServer().getAllPlayers()) {
                                                        if (online.hasPermission(permission)) {

                                                            AccountManager manager = CurrentPlatform.getAccountManager(Manager.CUSTOM, AccountID.fromUUID(online.getUniqueId()));
                                                            if (manager != null)
                                                                accounts.add(manager);
                                                        }
                                                    }

                                                    for (AccountManager account : accounts) {
                                                        player.sendActionBar(Component.text().content(StringUtils.toColor("&aSending player accounts ( " + sent + " of " + max + " )")).build());

                                                        DataSender.send(player, DataSender.getBuilder(DataType.PLAYER, DataSender.PLUGIN_CHANNEL, player).addTextData(StringUtils.serialize(account)).build());
                                                        sent++;
                                                    }

                                                    parser = new AccountParser(accounts);
                                                    DataSender.send(player, DataSender.getBuilder(DataType.INFOGUI, DataSender.PLUGIN_CHANNEL, player).addTextData(parser.toString()).build());
                                                    break;
                                            }
                                        }
                                        break;
                                    case "#":
                                        name = target.replaceFirst("#", "");
                                        Optional<RegisteredServer> info = plugin.getServer().getServer(name);

                                        if (info.isPresent()) {
                                            for (Player connection : info.get().getPlayersConnected()) {
                                                User conUser = new User(connection);
                                                accounts.add(conUser.getManager());
                                            }

                                            for (AccountManager account : accounts) {
                                                player.sendActionBar(Component.text().content(StringUtils.toColor("&aSending player accounts ( " + sent + " of " + max + " )")).build());

                                                DataSender.send(player, DataSender.getBuilder(DataType.PLAYER, DataSender.PLUGIN_CHANNEL, player).addTextData(StringUtils.serialize(account)).build());
                                                sent++;
                                            }

                                            AccountParser parser = new AccountParser(accounts);
                                            DataSender.send(player, DataSender.getBuilder(DataType.INFOGUI, DataSender.PLUGIN_CHANNEL, player).addTextData(parser.toString()).build());
                                        } else {
                                            user.send(messages.prefix() + messages.aliasNotFound(name));
                                        }
                                        break;
                                }
                            } else {
                                AccountNameDatabase.find(target).whenComplete((nsr) -> {
                                    if (nsr.singleResult()) {
                                        OfflineClient offline = new OfflineClient(target);
                                        AccountManager manager = offline.getAccount();

                                        if (manager != null) {
                                            AccountID id = manager.getUUID();
                                            LockedAccount account = new LockedAccount(id);
                                            User tarUser = null;

                                            Optional<Player> tar = plugin.getServer().getPlayer(UUID.fromString(id.getId()));

                                            if (tar.isPresent())
                                                tarUser = new User(tar.get());

                                            user.send(properties.getProperty("player_information_header", "&5&m--------------&r&e LockLogin player info &5&m--------------"));
                                            user.send("&r");

                                            List<String> parsed = parseMessages(account.isLocked());

                                            InstantParser creation_parser = new InstantParser(manager.getCreationTime());
                                            String creation_date = StringUtils.formatString("{0}/{1}/{2}", creation_parser.getDay(), creation_parser.getMonth(), creation_parser.getYear());

                                            int msg = -1;
                                            user.send(StringUtils.formatString(parsed.get(++msg), account.isLocked()));
                                            if (account.isLocked()) {
                                                Instant date = account.getLockDate();
                                                InstantParser parser = new InstantParser(date);
                                                String dateString = parser.getYear() + " " + parser.getMonth() + " " + parser.getDay();

                                                user.send(StringUtils.formatString(parsed.get(++msg), account.getIssuer()));
                                                user.send(StringUtils.formatString(parsed.get(++msg), dateString));
                                            }
                                            user.send(StringUtils.formatString(parsed.get(++msg), manager.getName()));
                                            user.send(StringUtils.formatString(parsed.get(++msg), manager.getUUID().getId()));
                                            user.send(StringUtils.formatString(parsed.get(++msg), (!manager.getPassword().replaceAll("\\s", "").isEmpty())));
                                            user.send(StringUtils.formatString(parsed.get(++msg), (tarUser != null ? tarUser.getSession().isLogged() : "false")));
                                            user.send(StringUtils.formatString(parsed.get(++msg), (tarUser != null ? tarUser.getSession().isTempLogged() : "false")));
                                            user.send(StringUtils.formatString(parsed.get(++msg), (manager.has2FA() && !manager.getGAuth().replaceAll("\\s", "").isEmpty())));
                                            user.send(StringUtils.formatString(parsed.get(++msg), (manager.hasPin())));
                                            user.send(StringUtils.formatString(parsed.get(++msg), creation_date, creation_parser.getDifference()));
                                        } else {
                                            user.send(messages.prefix() + messages.neverPlayer(target));
                                        }
                                    } else {
                                        AccountNameDatabase.otherPossible(target).whenComplete((possible) -> user.send(messages.multipleNames(target, possible)));
                                    }
                                });
                            }
                            break;
                        default:
                            for (String name : args) {
                                OfflineClient offline = new OfflineClient(name);
                                AccountManager manager = offline.getAccount();

                                if (manager != null) {
                                    accounts.add(manager);
                                }
                            }

                            for (AccountManager account : accounts) {
                                player.sendActionBar(Component.text().content(StringUtils.toColor("&aSending player accounts ( " + sent + " of " + max + " )")).build());

                                DataSender.send(player, DataSender.getBuilder(DataType.PLAYER, DataSender.PLUGIN_CHANNEL, player).addTextData(StringUtils.serialize(account)).build());
                                sent++;
                            }

                            AccountParser parser = new AccountParser(accounts);
                            DataSender.send(player, DataSender.getBuilder(DataType.INFOGUI, DataSender.PLUGIN_CHANNEL, player).addTextData(parser.toString()).build());
                            break;
                    }
                } else {
                    user.send(messages.prefix() + messages.permissionError(PluginPermission.playerInfo()));
                }
            } else {
                user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            }
        } else {
            console.send(messages.prefix() + properties.getProperty("command_not_available", "&cThis command is not available for console"));
        }
    }

    /**
     * Parse the player info message read
     * from plugin_messages.properties
     *
     * @param condition if the player account is locked
     * @return the parsed player info message
     */
    private List<String> parseMessages(final boolean condition) {
        String[] propData = properties.getProperty("player_information_message", "&7Locked: &d{0},condition=&7Locked by: &d{0}%&7Locked since: &d{0};,&7Name: &d{0},&7Account ID: &d{0},&7Registered: &d{0},&7Logged: &d{0},&7Temp logged: &d{0},&72FA: &d{0},&7Pin: &d{0},&7Created on: &d{0} ( {1} ago )").split(",");
        List<String> cmdMessages = new ArrayList<>();

        for (String propMSG : propData) {
            if (propMSG.startsWith("condition=")) {
                String[] conditionData = propMSG.split(";");

                String condition_message;
                if (condition) {
                    condition_message = conditionData[0].replaceFirst("condition=", "");
                } else {
                    try {
                        condition_message = conditionData[1];
                    } catch (Throwable ex) {
                        condition_message = "";
                    }
                }

                if (!condition_message.isEmpty()) {
                    if (condition_message.contains("%")) {
                        String[] conditionMsgData = condition_message.split("%");

                        cmdMessages.addAll(Arrays.asList(conditionMsgData));
                    } else {
                        cmdMessages.add(condition_message);
                    }
                }
            } else {
                cmdMessages.add(propMSG);
            }
        }

        return cmdMessages;
    }
}
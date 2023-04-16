package io.github.turpcoding.easyreport;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;

public class ReportCommand implements CommandExecutor {
    private final EasyReport mainClass = EasyReport.getInstance();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        int argsNumber = args.length;
        // Check if commandSender is a player.
        if (!(commandSender instanceof Player)) {
            try {
                Bukkit.getLogger().log(Level.INFO, "Someone not considered a player entity just tried to use the report command.");
                if (mainClass.getConfig().getBoolean("discord.enabled"))
                    DiscordWebhookAPI.executeWebhook(
                        "WARNING",
                        "Someone not considered a player entity just tried to use the report command.\\nCommand arguments used: " + Arrays.toString(args), Color.YELLOW);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return true;
        }

        Player player = (Player) commandSender;
        try {
            // /REPORT HELP -> display help.
            if (argsNumber == 0 || (args[0].equalsIgnoreCase("help") && argsNumber == 1)) {
                if (!permissionCheck(player, "report.help")) {return true;}

                String helpWithoutPrefix = "&e/report help &f- displays all the available commands.\n" +
                        "&e/report &6<player> <reason> &f- reports a player.\n" +
                        "&e/report records &6<player> &f- shows a player's report records.\n" +
                        "&e/report addstaff &6<player> &f- adds a player to the staff list so they can get notified about reports.\n" +
                        "&e/report removestaff &6<player> &f- removes a player from the staff list so they stop getting notified about reports.\n" +
                        "&e/report reload &f- reloads the plugin's 'config.yml' and 'staff.yml' files.\n";

                String helpWithPrefix = "&c[Hypster-Report]&r - Hypster-Report is a simple plugin forked from EasyReport by Turp's\n" + helpWithoutPrefix;

                String help = ChatColor.translateAlternateColorCodes('&',
                        mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                ? helpWithoutPrefix
                                : helpWithPrefix);

                player.sendMessage(help);
                return true;
            }

            // /REPORT RELOAD

            if (args[0].equalsIgnoreCase("reload") && argsNumber == 1) {
                if (!permissionCheck(player, "report.reload")) {return true;}

                mainClass.reloadConfig();
                mainClass.reloadCustomConfig();

                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                ?  "&aHypster-Report is a simple plugin forked from EasyReport by Turp's"
                                : "&c[Hypster-Report]&r &aHypster-Report is a simple plugin forked from EasyReport by Turp's"));
                return true;
            }

            // /REPORT ADDSTAFF <PLAYER>
            if (args[0].equalsIgnoreCase("addstaff") && argsNumber == 2) {
                if (!permissionCheck(player, "report.addstaff")) {return true;}

                ArrayList<String> staffMembersList = (ArrayList<String>) Objects.requireNonNull(
                        mainClass.getCustomConfig().getStringList("staffMembers"), "'staffMembers' cannot be null.");

                for (String staffMember : staffMembersList) {
                    if (staffMember.equalsIgnoreCase(args[1])) {

                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                        ?  "&cPlayer already on the staff list."
                                        : "&c[Hypster-Report]&r &cPlayer already on the staff list."));
                        return true;
                    }
                }

                // Here the username was not on the list so the file is updated.
                ArrayList<String> currentList = (ArrayList<String>) mainClass.getCustomConfig().getStringList("staffMembers");
                currentList.add(args[1]);
                mainClass.getCustomConfig().set("staffMembers", currentList);
                mainClass.saveCustomConfig();

                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                ? "&aPlayer added to the staff list."
                                : "&c[Hypster-Report]&r &aPlayer added to the staff list."));
                return true;
            }

            // /REPORT REMOVESTAFF <PLAYER>
            if (args[0].equalsIgnoreCase("removestaff") && argsNumber == 2) {
                if (!permissionCheck(player, "report.removestaff")) {return true;}
                ArrayList<String> staffMembersList = (ArrayList<String>) Objects.requireNonNull(
                        mainClass.getCustomConfig().getStringList("staffMembers"), "'staffMembers' cannot be null.");

                // Converting the list to a set to remove possible duplicates.
                Set<String> staffMembersListSet = new HashSet<>(staffMembersList);

                for (String staffMember : staffMembersListSet) {
                    // Here the username was on the list so the file is now being updated.
                    if (staffMember.equalsIgnoreCase(args[1])) {

                        // Remove the player from the set.
                        staffMembersListSet.removeIf(x->x.equalsIgnoreCase(args[1]));

                        // Convert the set back to a list.
                        staffMembersList = new ArrayList<>(staffMembersListSet);

                        mainClass.getCustomConfig().set("staffMembers", staffMembersList);
                        mainClass.saveCustomConfig();

                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                        ? "&aRemoved player from the staff list."
                                        : "&c[Hypster-Report]&r &aRemoved player from the staff list."));
                        return true;
                    }
                }
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                ? "&cPlayer not on the staff list."
                                : "&c[Hypster-Report]&r &cPlayer not on the staff list."));
                return true;
            }

            // /REPORT RECORDS <PLAYER>
            try {
                if (args[0].equalsIgnoreCase("records") && argsNumber == 2 && mainClass.getConfig().getBoolean("database.enabled")) {
                    if (!permissionCheck(player, "report.records")) {return true;}

                    DatabaseBridge db = new DatabaseBridge();
                    ArrayList<ArrayList<String>> reportRecords = db.getReports(args[1]);

                    // If the ResultSet is empty.
                    if (reportRecords.get(0).get(0).equals("Empty")) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                        ? "&f" + args[1] + " &cdoes not have any records."
                                        : "&c[Hypster-Report]&r " + args[1] + " &cdoes not have any records."));
                        return true;
                    }
                    // Gets the player name from the database instead of arg[1] for guaranteed correct capitalization.
                    String playerBeingQueried = reportRecords.get(0).get(0);

                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                    ? "&cReports against &f" + playerBeingQueried + ":"
                                    : "&c[Hypster-Report]&r &cReports against &f" + playerBeingQueried + ":"));

                    for (ArrayList<String> currentRecord : reportRecords) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&fReported on &7" + currentRecord.get(3)  + " &f by &a" + currentRecord.get(1) + "&f for &c" + currentRecord.get(2)));
                    }

                    player.sendMessage(ChatColor.translateAlternateColorCodes(
                            '&', "&cTotal number of reports: &f" + reportRecords.size()));
                    return true;

                } else if (args[0].equalsIgnoreCase("records") && argsNumber == 2 && !(mainClass.getConfig().getBoolean("database.enabled"))) {
                    if (!permissionCheck(player, "report.records")) {return true;}
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                            ? "&cYou have to first set up a database and configure 'config.yml' accordingly in order to use this feature."
                            : "&c[Hypster-Report]&r &cYou have to first set up a database and configure 'config.yml' accordingly in order to use this feature."));
                    return true;

                } else if (args[0].equalsIgnoreCase("records") && argsNumber == 1) {
                    if (!permissionCheck(player, "report.records")) {return true;}
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                ? "&cUsage: /report records <player>"
                                : "&c[Hypster-Report]&r &cUsage: /report records <player>"));
                    return true;
                }
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "A general exception while running /report records just got caught.");
                e.printStackTrace();
                return true;
            }

            // /REPORT <PLAYER> <REASON>
            Player reportedPlayer;
            if ((reportedPlayer = Bukkit.getPlayer(args[0])) != null && argsNumber == 2 && !reportedPlayer.equals(player)) {
                if (!permissionCheck(player, "report.report")) {return true;}

                String reportReason = String.join(" ", Arrays.copyOfRange(args, 1, argsNumber)).replace("-", " ");

                String staffNotificationMsg = MessageFormat.format(
                        mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                        ? "&c&lNEW REPORT! \n \n&cReporter: &a{0} \n&cReported Username: &a{1} \n&cReason: &a{2} \n&cServer: &a{3} \n&r \n&c&lPlease handle this. Check Discord for more information!\n&r\n&a"
                        : "&r\n&c[Hypster-Report]&r &c&lNEW REPORT! \n \n&cReporter: &a{0} \n&cReported Username: &a{1} \n&cReason: &a{2} \n&cServer: &a{3} \n&r \n&c&lPlease handle this. Check Discord for more information!\n&r\n&a"
                        , player.getName(), reportedPlayer.getName(), reportReason);

                boolean staffNotified = false;
                Player staff;
                for (String staffMember : mainClass.getCustomConfig().getStringList("staffMembers")) {
                    if ((staff = Bukkit.getPlayer(staffMember)) != null) {

                        staff.sendMessage(ChatColor.translateAlternateColorCodes('&', staffNotificationMsg));
                        staffNotified = true;

                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                        ? "&aThanks for reporting cheating and helping make the Hypster community safer. We will review your report further!"
                                        : "&c[Hypster-Report]&r &aThanks for reporting cheating and helping make the Hypster community safer. We will review your report further!"));

                        if (mainClass.getConfig().getBoolean("staffNotificationSound"))
                            staff.playSound(staff.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
                    }
                }

                if (!staffNotified) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                    ? "&aThanks for reporting cheating and helping make the Hypster community safer. We will review your report further! but due to high demand, it is possible that the process will be a little longer than usual."
                                    : "&c[Hypster-Report]&r &aThanks for reporting cheating and helping make the Hypster community safer. We will review your report further! but due to high demand, it is possible that the process will be a little longer than usual."));
                }

                try {
                    // Save report to a database.
                    if (mainClass.getConfig().getBoolean("database.enabled")) {
                        DatabaseBridge db = new DatabaseBridge();
                        boolean status = db.insertInto(reportedPlayer.getName(), player.getName(), args[1]);

                        int i = 0;
                        // If the insertInto fails, try again until it either works or the counter reaches 5.
                        while (i < 5 && !(status)) {
                            status = db.insertInto(reportedPlayer.getName(), player.getName(), args[1]);
                            i++;
                        }
                    }
                    // Discord webhook stuff if enabled.
                    if (mainClass.getConfig().getBoolean("discord.enabled")) {
                        DiscordWebhookAPI.executeWebhook("NEW REPORT",
                                MessageFormat.format(
                                        "**{0}** was just reported by **{1}** for `{2}`. Reported in server NULL", reportedPlayer.getName(), player.getName(), reportReason)
                                , Color.RED);
                    }
                    return true;

                } catch (IOException e) {
                    Bukkit.getLogger().log(Level.WARNING, "An IOException just got caught.");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4Internal error."));
                    e.printStackTrace();
                    return true;
                }
            } else if (reportedPlayer != null && argsNumber == 2 && reportedPlayer.equals(player)) {
                if (!permissionCheck(player, "report.report")) {return true;}
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                ? "&cHey! You cannot report yourself."
                                : "&c[Hypster-Report]&r &cHey! You cannot report yourself."));
                return true;

            } else if (reportedPlayer == null && argsNumber == 2) {
                if (!permissionCheck(player, "report.report")) {return true;}
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                ? "&cThe player you tried to report is not online or not on the same server as you."
                                : "&c[Hypster-Report]&r &cThe player you tried to report is not online or not on the same server as you."));
            } else {
                if (!permissionCheck(player, "report.report")) {return true;}
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                                ? "&cUsage: /report <player> <reason> \n&cExample: &f/report steve hacking-xray"
                                : "&c[Hypster-Report]&r &cUsage: /report <player> <reason> \n&cExample: &f/report steve hacking-xray"));
                return true;
            }
        } catch (Exception e) {
            try {
                Bukkit.getLogger().log(Level.WARNING, "A general exception just got caught.");
                if (mainClass.getConfig().getBoolean("discord.enabled"))
                    DiscordWebhookAPI.executeWebhook("CRITICAL ERROR", "Internal error.\\n" +
                        " Player who issued the command: " + player.getName(), Color.RED);
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }
        return true;
    }

    private boolean permissionCheck(Player player, String perm) {
        if (!(player.hasPermission(perm) || player.hasPermission("report.*"))) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    mainClass.getConfig().getBoolean("removePluginPrefixFromChatMessages")
                            ? "&cYou do not have permission to use this command."
                            : "&c[Hypster-Report]&r &cYou do not have permission to use this command."));
            return false;
        }
        return true;
    }
}

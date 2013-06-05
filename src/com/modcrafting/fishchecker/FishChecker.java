/*
 * Copyright (C) 2012 Deathmarine
 * 
 * FishChecker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FishChecker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with FishChecker.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.modcrafting.fishchecker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class FishChecker extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        if (!this.getServer().getOnlineMode()) {
            this.getLogger()
                    .info(": We're sorry but fishchecker will not function correctly in offline mode.");
            this.setEnabled(false);
            return;
        }
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPermission("fishcheck.override"))
            return;
        final Player player = event.getPlayer();
        final String name = player.getName();
        this.getServer().getScheduler()
                .runTaskAsynchronously(this, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            URL url = new URL("http://api.fishbans.com/stats/"
                                    + name + "/");
                            String line;
                            StringBuilder builder = new StringBuilder();
                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(url.openStream()));
                            while ((line = reader.readLine()) != null)
                                builder.append(line);
                            JSONParser parser = new JSONParser();
                            Object obj = parser.parse(builder.toString());
                            reader.close();

                            JSONObject jsonObject = (JSONObject) obj;
                            JSONObject bans = (JSONObject) jsonObject
                                    .get("stats");
                            if (bans == null)
                                return;
                            JSONObject service = (JSONObject) bans
                                    .get("service");
                            long mcbansAmt = 0L;
                            if (service.get("mcbans") != null)
                                mcbansAmt = getValue(service.get("mcbans"));
                            long mcbouncerAmt = 0L;
                            if (service.get("mcbouncer") != null)
                                mcbouncerAmt = getValue(service
                                        .get("mcbouncer"));
                            long mcblockitAmt = 0L;
                            if (service.get("mcblockit") != null)
                                mcblockitAmt = getValue(service
                                        .get("mcblockit"));
                            long minebansAmt = 0L;
                            if (service.get("minebans") != null)
                                minebansAmt = getValue(service.get("minebans"));
                            long glizerAmt = 0L;
                            if (service.get("glizer") != null)
                                glizerAmt = getValue(service.get("glizer"));

                            long sum = mcbansAmt + mcbouncerAmt + mcblockitAmt
                                    + minebansAmt + glizerAmt;
                            if (sum > 0L)
                                printToAdmins(ChatColor.GRAY + "Player: "
                                        + name + " has " + ChatColor.RED
                                        + String.valueOf(sum) + ChatColor.GRAY
                                        + " Ban(s).");
                            if (mcbansAmt > 0L)
                                printToAdmins(ChatColor.GRAY + "McBans: "
                                        + ChatColor.RED
                                        + String.valueOf(mcbansAmt));
                            if (mcbouncerAmt > 0L)
                                printToAdmins(ChatColor.GRAY + "McBouncer: "
                                        + ChatColor.RED
                                        + String.valueOf(mcbouncerAmt));
                            if (mcblockitAmt > 0L)
                                printToAdmins(ChatColor.GRAY + "McBlockit: "
                                        + ChatColor.RED
                                        + String.valueOf(mcblockitAmt));
                            if (minebansAmt > 0L)
                                printToAdmins(ChatColor.GRAY + "MineBans: "
                                        + ChatColor.RED
                                        + String.valueOf(minebansAmt));
                            if (glizerAmt > 0L)
                                printToAdmins(ChatColor.GRAY + "Glizer: "
                                        + ChatColor.RED
                                        + String.valueOf(glizerAmt));
                            if (sum > 0L)
                                printToAdmins(ChatColor.GRAY + "Use "
                                        + ChatColor.GREEN + "/fishcheck "
                                        + name + ChatColor.GRAY
                                        + " for more info.");
                            if (player.hasPermission("fishcheck.alertself")) {
                                player.sendMessage(ChatColor.GRAY + "Player: "
                                        + name + " has " + ChatColor.RED
                                        + String.valueOf(sum) + ChatColor.GRAY
                                        + " Ban(s).");
                                if (sum > 0L)
                                    player.sendMessage(ChatColor.GREEN + "See "
                                            + ChatColor.GRAY
                                            + "http://fishbans.com/u/"
                                            + name.toLowerCase() + "/"
                                            + ChatColor.GREEN
                                            + " for more info.");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command,
            String label, final String[] args) {
        if (!sender.hasPermission(command.getPermission())) {
            sender.sendMessage(ChatColor.RED
                    + "You do not have the required permissions.");
            return true;
        }
        if (args.length < 1)
            return false;
        sender.sendMessage(ChatColor.GRAY
                + "Checking Fishbans for information on " + ChatColor.DARK_RED
                + args[0] + ChatColor.GRAY + " !");
        this.getServer().getScheduler()
                .runTaskAsynchronously(this, new Runnable() {

                    @Override
                    public void run() {
                        try {
                            HashMap<String, Object> map = new HashMap<String, Object>();
                            URL url = new URL("http://api.fishbans.com/bans/"
                                    + args[0].toLowerCase() + "/");
                            String line;
                            StringBuilder builder = new StringBuilder();
                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(url.openStream()));
                            while ((line = reader.readLine()) != null) {
                                builder.append(line);
                            }
                            JSONParser parser = new JSONParser();
                            Object obj = parser.parse(builder.toString());
                            reader.close();
                            JSONObject jsonObject = (JSONObject) obj;
                            JSONObject bans = (JSONObject) jsonObject
                                    .get("bans");
                            if (bans == null) {
                                sender.sendMessage("Player Does Not Exist.");
                                return;
                            }
                            JSONObject service = (JSONObject) bans
                                    .get("service");
                            if (service == null) {
                                sender.sendMessage("Player Does Not Exist.");
                                return;
                            }
                            JSONObject mcbans = (JSONObject) service
                                    .get("mcbans");
                            if (mcbans == null) {
                                sender.sendMessage("Mcbans: Player Not Found.");
                                return;
                            }
                            long mcbansAmt = 0L;
                            if (mcbans.get("bans") != null)
                                mcbansAmt = getValue(mcbans.get("bans"));
                            if (mcbansAmt > 0L) {
                                JSONObject mcbansInfo = castToJSON(mcbans
                                        .get("ban_info"));
                                sender.sendMessage(ChatColor.RED + "Mcbans: "
                                        + String.valueOf(mcbansAmt));
                                toJavaMap(mcbansInfo, map);
                                if (mcbansInfo != null)
                                    outputHashMap(map, sender);
                                map.clear();
                            } else {
                                sender.sendMessage(ChatColor.GREEN + "Mcbans: "
                                        + String.valueOf(mcbansAmt));
                            }
                            JSONObject mcbouncer = (JSONObject) service
                                    .get("mcbouncer");
                            if (mcbouncer == null) {
                                sender.sendMessage("McBouncer: Player Not Found.");
                                return;
                            }
                            long mcbouncerAmt = 0L;
                            if (mcbouncer.get("bans") != null)
                                mcbouncerAmt = getValue(mcbouncer.get("bans"));
                            if (mcbouncerAmt > 0L) {
                                JSONObject mcbouncerInfo = castToJSON(mcbouncer
                                        .get("ban_info"));
                                sender.sendMessage(ChatColor.RED
                                        + "Mcbouncer: "
                                        + String.valueOf(mcbouncerAmt));
                                toJavaMap(mcbouncerInfo, map);
                                if (mcbouncerInfo != null)
                                    outputHashMap(map, sender);
                                map.clear();
                            } else {
                                sender.sendMessage(ChatColor.GREEN
                                        + "Mcbouncer: "
                                        + String.valueOf(mcbouncerAmt));
                            }
                            JSONObject mcblockit = (JSONObject) service
                                    .get("mcblockit");
                            if (mcblockit == null) {
                                sender.sendMessage("McBlockit: Player Not Found.");
                                return;
                            }
                            long mcblockitAmt = 0L;
                            if (mcblockit.get("bans") != null)
                                mcblockitAmt = getValue(mcblockit.get("bans"));
                            if (mcblockitAmt > 0L) {
                                JSONObject mcblockitInfo = castToJSON(mcblockit
                                        .get("ban_info"));
                                sender.sendMessage(ChatColor.RED
                                        + "McBlockit: "
                                        + String.valueOf(mcblockitAmt));
                                toJavaMap(mcblockitInfo, map);
                                if (mcblockitInfo != null)
                                    outputHashMap(map, sender);
                                map.clear();
                            } else {
                                sender.sendMessage(ChatColor.GREEN
                                        + "McBlockit: "
                                        + String.valueOf(mcblockitAmt));
                            }
                            JSONObject minebans = (JSONObject) service
                                    .get("minebans");
                            if (minebans == null) {
                                sender.sendMessage("Minebans: Player Not Found.");
                                return;
                            }
                            long minebansAmt = 0L;
                            if (minebans.get("bans") != null)
                                minebansAmt = getValue(minebans.get("bans"));
                            if (minebansAmt > 0L) {
                                JSONObject minebansInfo = castToJSON(minebans
                                        .get("ban_info"));
                                sender.sendMessage(ChatColor.RED + "Minebans: "
                                        + String.valueOf(minebansAmt));
                                toJavaMap(minebansInfo, map);
                                if (minebansInfo != null)
                                    outputHashMap(map, sender);
                                map.clear();
                            } else {
                                sender.sendMessage(ChatColor.GREEN
                                        + "Minebans: "
                                        + String.valueOf(minebansAmt));
                            }
                            JSONObject glizer = (JSONObject) service
                                    .get("glizer");
                            if (glizer == null) {
                                sender.sendMessage("Glizer: Player Not Found.");
                                return;
                            }
                            long glizerAmt = 0L;
                            if (glizer.get("bans") != null)
                                glizerAmt = getValue(glizer.get("bans"));
                            if (glizerAmt > 0L) {
                                JSONObject glizerInfo = castToJSON(glizer
                                        .get("ban_info"));
                                sender.sendMessage(ChatColor.RED + "Glizer: "
                                        + String.valueOf(glizerAmt));
                                toJavaMap(glizerInfo, map);
                                if (glizerInfo != null)
                                    outputHashMap(map, sender);
                                map.clear();
                            } else {
                                sender.sendMessage(ChatColor.GREEN + "Glizer: "
                                        + String.valueOf(glizerAmt));
                            }
                            long sum = mcbansAmt + mcbouncerAmt + mcblockitAmt
                                    + minebansAmt + glizerAmt;
                            String risk = ChatColor.GREEN + "Low";
                            if (sum > 0L) {
                                risk = ChatColor.YELLOW + "Medium";
                                if (sum >= 5 && sum < 15)
                                    risk = ChatColor.RED + "High";
                                if (sum > 15)
                                    risk = ChatColor.DARK_RED + "Extreme";
                            }
                            sender.sendMessage(ChatColor.GRAY
                                    + "Player Risk is status: " + risk);
                            sender.sendMessage(ChatColor.GREEN + "See "
                                    + ChatColor.GRAY + "http://fishbans.com/u/"
                                    + args[0].toLowerCase() + "/");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

        return true;
    }

    private void printToAdmins(String string) {
        for (Player player : this.getServer().getOnlinePlayers()) {
            if (player.hasPermission("fishcheck.messages") || player.isOp()) {
                player.sendMessage(string);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public void toJavaMap(JSONObject o, Map<String, Object> b) {
        if (o == null)
            return;
        Iterator ji = o.keySet().iterator();
        while (ji.hasNext()) {
            String key = (String) ji.next();
            Object val = o.get(key);
            if (val.getClass() == JSONObject.class) {
                Map<String, Object> sub = new HashMap<String, Object>();
                toJavaMap((JSONObject) val, sub);
                b.put(key, sub);
            } else if (val.getClass() == JSONArray.class) {
                List<Object> l = new ArrayList<Object>();
                JSONArray arr = (JSONArray) val;
                for (int a = 0; a < arr.size(); a++) {
                    Map<String, Object> sub = new HashMap<String, Object>();
                    Object element = arr.get(a);
                    if (element instanceof JSONObject) {
                        toJavaMap((JSONObject) element, sub);
                        l.add(sub);
                    } else {
                        l.add(element);
                    }
                }
                b.put(key, l);
            } else {
                b.put(key, val);
            }
        }
    }

    private JSONObject castToJSON(Object object) {
        if (object instanceof JSONObject) {
            return (JSONObject) object;
        }
        return null;
    }

    private void outputHashMap(HashMap<String, Object> map, CommandSender sender) {
        if (map == null) {
            sender.sendMessage(ChatColor.GREEN + "Nothing Found");
            return;
        }
        for (String str : map.keySet()) {
            sender.sendMessage(ChatColor.GRAY + str + ": " + ChatColor.DARK_RED
                    + map.get(str).toString());
        }
    }

    private long getValue(Object obj) {
        long v = 0L;
        if (obj instanceof Long) {
            v = (Long) obj;
        } else if (obj instanceof String) {
            try {
                v = Long.parseLong((String) obj);
            } catch (NumberFormatException nfe) {
                return 0L;
            }
        }
        return v;
    }
}

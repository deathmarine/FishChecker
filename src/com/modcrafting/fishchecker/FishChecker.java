package com.modcrafting.fishchecker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class FishChecker extends JavaPlugin implements Listener, CommandExecutor{
	public void onEnable(){				
		this.getServer().getPluginManager().registerEvents(this, this);
		this.getCommand("fishcheck").setExecutor(this);
	}
	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent event){
		final Player player = event.getPlayer();
		if(player.isOp() || player.hasPermission("fishcheck.override")) return;
		this.getServer().getScheduler().scheduleAsyncDelayedTask(this,new Runnable(){
			@Override
			public void run() {
				URL url;
				URLConnection connection;
				try {
					url = new URL("http://www.fishbans.com/api/bans/"+player.getName()+"/force/");
					connection = url.openConnection();
					connection.addRequestProperty("Referer","http://" + Bukkit.getServer().getIp());
					String line;
					StringBuilder builder = new StringBuilder();
					BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
					while((line = reader.readLine()) != null) {
					 builder.append(line);
					}
					JSONParser parser = new JSONParser();
					Object obj = parser.parse(builder.toString());
					
					JSONObject jsonObject = (JSONObject) obj;
					JSONObject bans = (JSONObject) jsonObject.get("bans");
					if(bans == null){
						return;
					}
					JSONObject service = (JSONObject) bans.get("service");
					
					JSONObject mcbans = (JSONObject) service.get("mcbans");
					long mcbansAmt = 0;
					if(mcbans.get("bans") != null) mcbansAmt = (Long) mcbans.get("bans");
					JSONObject mcbouncer = (JSONObject) service.get("mcbouncer");
					long mcbouncerAmt = 0;
					if(mcbouncer.get("bans") != null) mcbouncerAmt = (Long) mcbouncer.get("bans");
					JSONObject mcblockit = (JSONObject) service.get("mcblockit");
					long mcblockitAmt =  0;
					if(mcblockit.get("bans") != null) mcblockitAmt = (Long) mcblockit.get("bans");
					
					printToAdmins(ChatColor.GRAY+"Player: "+player.getName()+" has "+ChatColor.RED+String.valueOf(mcbansAmt+mcbouncerAmt+mcblockitAmt)+ChatColor.GRAY+" Ban(s).");
					if(mcbansAmt > 0) printToAdmins(ChatColor.GRAY+"McBans: "+ChatColor.RED+String.valueOf(mcbansAmt));
					if(mcbouncerAmt > 0) printToAdmins(ChatColor.GRAY+"McBouncer: "+ChatColor.RED+String.valueOf(mcbouncerAmt));
					if(mcblockitAmt > 0) printToAdmins(ChatColor.GRAY+"McBlockit: "+ChatColor.RED+String.valueOf(mcblockitAmt));
					if(mcbansAmt > 0 || mcbouncerAmt > 0 || mcblockitAmt > 0)printToAdmins(ChatColor.GRAY+"Use "+ChatColor.GREEN+"/fishcheck "+event.getPlayer().getName()+ChatColor.GRAY+" for more info.");
					
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public boolean onCommand(final CommandSender sender, Command command, String label, final String[] args) {
		if(sender instanceof Player){
			if(!((Player) sender).hasPermission("fishcheck.command") || !((Player) sender).isOp()){
				sender.sendMessage(ChatColor.RED + "You do not have the required permissions.");
				return true;
			}
		}
		if(args.length < 1) return false;
		this.getServer().getScheduler().scheduleAsyncDelayedTask(this,new Runnable(){

			HashMap<String, Object> map = new HashMap<String, Object>();
			@Override
			public void run() {
				URL url;
				URLConnection connection;
				try {
					url = new URL("http://www.fishbans.com/api/bans/"+args[0].toLowerCase()+"/force/");
					connection = url.openConnection();
					connection.addRequestProperty("Referer","http://" + Bukkit.getServer().getIp());
					String line;
					StringBuilder builder = new StringBuilder();
					BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
					while((line = reader.readLine()) != null) {
					 builder.append(line);
					}
					JSONParser parser = new JSONParser();
					Object obj = parser.parse(builder.toString());
					
					JSONObject jsonObject = (JSONObject) obj;
					JSONObject bans = (JSONObject) jsonObject.get("bans");
					if(bans == null){
						sender.sendMessage("Player Does Not Exist.");
						return;
					}
					JSONObject service = (JSONObject) bans.get("service");
					if(service == null){
						sender.sendMessage("Player Does Not Exist.");
						return;
					}
					JSONObject mcbans = (JSONObject) service.get("mcbans");
					if(mcbans == null){
						sender.sendMessage("Mcbans: Player Not Found.");
						return;
					}
					long mcbansAmt = 0;
					if(mcbans.get("bans") != null){
						mcbansAmt = (Long) mcbans.get("bans");
					}
					if(mcbansAmt > 0){
						JSONObject mcbansInfo = castToJSON(mcbans.get("ban_info"));
						sender.sendMessage(ChatColor.RED+"Mcbans: " + String.valueOf(mcbansAmt));
						toJavaMap(mcbansInfo, map);
						if(mcbansInfo != null) outputHashMap(map, sender);
						map.clear();
					}else{
						sender.sendMessage(ChatColor.GREEN+"Mcbans: "+String.valueOf(mcbansAmt));
					}
					
					//McBouncer
					JSONObject mcbouncer = (JSONObject) service.get("mcbouncer");
					if(mcbouncer == null){
						sender.sendMessage("McBouncer: Player Not Found.");
						return;
					}
					long mcbouncerAmt = 0;
					if(mcbouncer.get("bans") != null){
						mcbouncerAmt = (Long) mcbouncer.get("bans");
					}
					if(mcbouncerAmt > 0){
						JSONObject mcbouncerInfo = castToJSON(mcbouncer.get("ban_info"));
						sender.sendMessage("Mcbouncer: " + String.valueOf(mcbouncerAmt));
						toJavaMap(mcbouncerInfo, map);
						if(mcbouncerInfo != null) outputHashMap(map, sender);
						map.clear();				
					}else{
						sender.sendMessage(ChatColor.GREEN+"Mcbans: "+String.valueOf(mcbansAmt));
					}
					
					//McBlockIt
					JSONObject mcblockit = (JSONObject) service.get("mcblockit");
					if(mcblockit == null){
						sender.sendMessage("McBlockit: Player Not Found.");
						return;
					}
					long mcblockitAmt = 0;
					if(mcblockit.get("bans") != null){
						mcblockitAmt = (Long) mcblockit.get("bans");
					}
					if(mcblockitAmt > 0){
						JSONObject mcblockitInfo = castToJSON(mcblockit.get("ban_info"));
						sender.sendMessage("McBlockit: " + String.valueOf(mcblockitAmt));
						toJavaMap(mcblockitInfo, map);
						if(mcblockitInfo != null) outputHashMap(map, sender);
						map.clear();				
					}else{
						sender.sendMessage(ChatColor.GREEN+"McBlockit: "+String.valueOf(mcbansAmt));
					}
					
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		});
		
		return true;
	}
	public void printToAdmins(String string){
		for(Player player: this.getServer().getOnlinePlayers()){
			if(player.hasPermission("fishcheck.messages") || player.isOp()){
				player.sendMessage(string);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public void toJavaMap(JSONObject o, Map<String, Object> b) {
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
	public JSONObject castToJSON(Object object){
		if(object instanceof JSONObject){
			return (JSONObject) object;
		}
		return null;
	}
	public void outputHashMap(HashMap<String, Object> map, CommandSender sender){
		if (map == null){
			sender.sendMessage(ChatColor.GREEN+ "Nothing Found");
			return;
		}
		Iterator<String> cKeys = map.keySet().iterator();
	    Iterator<Object> cValue = map.values().iterator();
		while (cValue.hasNext() && cKeys.hasNext()){
			sender.sendMessage(ChatColor.RED + cKeys.next()+": "+cValue.next().toString());
		}
	}
}

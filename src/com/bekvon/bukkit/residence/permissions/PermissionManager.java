package com.bekvon.bukkit.residence.permissions;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.PlayerGroup;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.vaultinterface.ResidenceVaultAdapter;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.platymuus.bukkit.permissions.PermissionsPlugin;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PermissionManager {
    protected static PermissionsInterface perms;
    protected LinkedHashMap<String, PermissionGroup> groups;
    protected Map<String, String> playersGroup;
    protected FlagPermissions globalFlagPerms;

    protected HashMap<String, PlayerGroup> groupsMap = new HashMap<String, PlayerGroup>();

    public PermissionManager(FileConfiguration config, FileConfiguration flags) {
	try {
	    groups = new LinkedHashMap<String, PermissionGroup>();
	    playersGroup = Collections.synchronizedMap(new HashMap<String, String>());
	    globalFlagPerms = new FlagPermissions();
	    this.readConfig(config, flags);
	    boolean enable = config.getBoolean("Global.EnablePermissions", true);
	    if (enable) {
		this.checkPermissions();
	    }
	} catch (Exception ex) {
	    Logger.getLogger(PermissionManager.class.getName()).log(Level.SEVERE, null, ex);
	}
    }

    public FlagPermissions getAllFlags() {
	return this.globalFlagPerms;
    }

    public Map<String, String> getPlayersGroups() {
	return playersGroup;
    }

    public Map<String, PermissionGroup> getGroups() {
	return groups;
    }

    public PermissionGroup getGroup(Player player) {
	PermissionGroup group = Residence.getPlayerManager().getGroup(player.getName());
	if (group != null) {
	    return group;
	}
	return groups.get(this.getGroupNameByPlayer(player));
    }

    public PermissionGroup getGroup(String player, String world) {
	PermissionGroup group = Residence.getPlayerManager().getGroup(player);
	if (group != null) {
	    return group;
	}
	return groups.get(this.getGroupNameByPlayer(player, world));
    }

    public PermissionGroup getGroupByName(String group) {
	group = group.toLowerCase();
	if (!groups.containsKey(group)) {
	    return groups.get(Residence.getConfigManager().getDefaultGroup());
	}
	return groups.get(group);
    }

    public String getGroupNameByPlayer(Player player) {
	return this.getGroupNameByPlayer(player.getName(), player.getWorld().getName());
    }

    public String getGroupNameByPlayer(String player, String world) {
	if (!this.groupsMap.containsKey(player)) {
	    updateGroupNameForPlayer(Bukkit.getPlayer(player));
	}
	PlayerGroup PGroup = this.groupsMap.get(player);
	if (PGroup != null) {
	    String group = PGroup.getGroup(world);
	    if (group != null)
		return group;
	}
	return Residence.getConfigManager().getDefaultGroup().toLowerCase();
    }

    public String getPermissionsGroup(Player player) {
	return this.getPermissionsGroup(player.getName(), player.getWorld().getName()).toLowerCase();
    }

    public String getPermissionsGroup(String player, String world) {
	if (perms == null)
	    return Residence.getConfigManager().getDefaultGroup().toLowerCase();
	return perms.getPlayerGroup(player, world).toLowerCase();
    }

    public void updateGroupNameForPlayer(Player player) {
	updateGroupNameForPlayer(player, false);
    }

    public void updateGroupNameForPlayer(Player player, boolean force) {
	if (player == null)
	    return;
	updateGroupNameForPlayer(player.getName(), player.getWorld().getName(), force);
    }

    public void updateGroupNameForPlayer(String playerName, String world, boolean force) {
	PlayerGroup GPlayer;
	if (!groupsMap.containsKey(playerName)) {
	    GPlayer = new PlayerGroup(playerName);
	    groupsMap.put(playerName, GPlayer);
	} else
	    GPlayer = groupsMap.get(playerName);
	GPlayer.updateGroup(world, force);
    }

    public boolean isResidenceAdmin(CommandSender sender) {
	return (sender.hasPermission("residence.admin") || (sender.isOp() && Residence.getConfigManager().getOpsAreAdmins()));
    }

    private void checkPermissions() {
	Server server = Residence.getServ();
	Plugin p = server.getPluginManager().getPlugin("Vault");
	if (p != null) {
	    ResidenceVaultAdapter vault = new ResidenceVaultAdapter(server);
	    if (vault.permissionsOK()) {
		perms = vault;
		Logger.getLogger("Minecraft").log(Level.INFO, "[Residence] Found Vault using permissions plugin:" + vault.getPermissionsName());
		return;
	    } else {
		Logger.getLogger("Minecraft").log(Level.INFO, "[Residence] Found Vault, but Vault reported no usable permissions system...");
	    }
	}
	p = server.getPluginManager().getPlugin("PermissionsBukkit");
	if (p != null) {
	    perms = new PermissionsBukkitAdapter((PermissionsPlugin) p);
	    Logger.getLogger("Minecraft").log(Level.INFO, "[Residence] Found PermissionsBukkit Plugin!");
	    return;
	}
	p = server.getPluginManager().getPlugin("bPermissions");
	if (p != null) {
	    perms = new BPermissionsAdapter();
	    Logger.getLogger("Minecraft").log(Level.INFO, "[Residence] Found bPermissions Plugin!");
	    return;
	}
	p = server.getPluginManager().getPlugin("Permissions");
	if (p != null) {
	    if (Residence.getConfigManager().useLegacyPermissions()) {
		perms = new LegacyPermissions(((Permissions) p).getHandler());
		Logger.getLogger("Minecraft").log(Level.INFO, "[Residence] Found Permissions Plugin!");
		Logger.getLogger("Minecraft").log(Level.INFO, "[Residence] Permissions running in Legacy mode!");
	    } else {
		perms = new OriginalPermissions(((Permissions) p).getHandler());
		Logger.getLogger("Minecraft").log(Level.INFO, "[Residence] Found Permissions Plugin!");
	    }
	    return;
	}
	Logger.getLogger("Minecraft").log(Level.INFO, "[Residence] Permissions plugin NOT FOUND!");
    }

    private void readConfig(FileConfiguration config, FileConfiguration flags) {
	String defaultGroup = Residence.getConfigManager().getDefaultGroup().toLowerCase();
	globalFlagPerms = FlagPermissions.parseFromConfigNode("FlagPermission", flags.getConfigurationSection("Global"));
	ConfigurationSection nodes = config.getConfigurationSection("Groups");
	if (nodes != null) {
	    Set<String> entrys = nodes.getKeys(false);
	    int i = 0;
	    for (String key : entrys) {
		try {
		    i++;
		    groups.put(key.toLowerCase(), new PermissionGroup(key.toLowerCase(), nodes.getConfigurationSection(key), globalFlagPerms, i));
		    List<String> mirrors = nodes.getConfigurationSection(key).getStringList("Mirror");
		    for (String group : mirrors) {
			groups.put(group.toLowerCase(), new PermissionGroup(key.toLowerCase(), nodes.getConfigurationSection(key), globalFlagPerms));
		    }
		} catch (Exception ex) {
		    System.out.println("[Residence] Error parsing group from config:" + key + " Exception:" + ex);
		}
	    }
	}
	if (!groups.containsKey(defaultGroup)) {
	    groups.put(defaultGroup, new PermissionGroup(defaultGroup));
	}
	if (config.isConfigurationSection("GroupAssignments")) {
	    Set<String> keys = config.getConfigurationSection("GroupAssignments").getKeys(false);
	    if (keys != null) {
		for (String key : keys) {
		    playersGroup.put(key.toLowerCase(), config.getString("GroupAssignments." + key, defaultGroup).toLowerCase());
		}
	    }
	}
    }

    public boolean hasGroup(String group) {
	group = group.toLowerCase();
	return groups.containsKey(group);
    }

    public PermissionsInterface getPermissionsPlugin() {
	return perms;
    }
}

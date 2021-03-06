package com.bekvon.bukkit.residence.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.cmd;
import com.bekvon.bukkit.residence.permissions.PermissionGroup;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;

public class unstuck implements cmd {

    @Override
    public boolean perform(String[] args, boolean resadmin, Command command, CommandSender sender) {
	if (!(sender instanceof Player))
	    return false;

	Player player = (Player) sender;

	if (args.length != 1)
	    return false;

	PermissionGroup group = Residence.getPermissionManager().getGroup(player);
	if (!group.hasUnstuckAccess()) {
	    player.sendMessage(Residence.getLM().getMessage("General.NoPermission"));
	    return true;
	}
	ClaimedResidence res = Residence.getResidenceManager().getByLoc(player.getLocation());
	if (res == null) {
	    player.sendMessage(Residence.getLM().getMessage("Residence.NotIn"));
	} else {
	    player.sendMessage(Residence.getLM().getMessage("General.Moved"));
	    player.teleport(res.getOutsideFreeLoc(player.getLocation(), player));
	}
	return true;
    }
}

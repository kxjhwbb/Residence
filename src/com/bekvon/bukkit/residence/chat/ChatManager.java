package com.bekvon.bukkit.residence.chat;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.api.ChatInterface;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Server;

public class ChatManager implements ChatInterface {

    protected Map<String, ChatChannel> channelmap;
    protected Server server;

    public ChatManager() {
	server = Residence.getServ();
	channelmap = new HashMap<String, ChatChannel>();
    }

    public boolean setChannel(String player, String resName) {
	ClaimedResidence res = Residence.getResidenceManager().getByName(resName);
	if (res == null)
	    return false;
	return setChannel(player, res);
    }

    public boolean setChannel(String player, ClaimedResidence res) {
	this.removeFromChannel(player);
	if (!channelmap.containsKey(res.getName()))
	    channelmap.put(res.getName(), new ChatChannel(res.getName(), res.getChatPrefix(), res.getChannelColor()));
	channelmap.get(res.getName()).join(player);
	return true;
    }

    public boolean removeFromChannel(String player) {
	for (ChatChannel chan : channelmap.values()) {
	    if (chan.hasMember(player)) {
		chan.leave(player);
		return true;
	    }
	}
	return false;
    }

    public ChatChannel getChannel(String channel) {
	return channelmap.get(channel);
    }

    public ChatChannel getPlayerChannel(String player) {
	for (ChatChannel chan : channelmap.values()) {
	    if (chan.hasMember(player))
		return chan;
	}
	return null;
    }
}

package com.bekvon.bukkit.residence.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.WeatherType;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.chat.ChatChannel;
import com.bekvon.bukkit.residence.economy.rent.RentableLand;
import com.bekvon.bukkit.residence.economy.rent.RentedLand;
import com.bekvon.bukkit.residence.event.*;
import com.bekvon.bukkit.residence.gui.SetFlag;
import com.bekvon.bukkit.residence.permissions.PermissionGroup;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.signsStuff.Signs;
import com.bekvon.bukkit.residence.utils.GetTime;

public class ResidencePlayerListener implements Listener {

    protected Map<String, String> currentRes;
    protected Map<String, Long> lastUpdate;
    protected Map<String, Location> lastOutsideLoc;
    protected int minUpdateTime;
    protected boolean chatenabled;
    protected List<String> playerToggleChat = new ArrayList<String>();

    public static Map<String, SetFlag> GUI = new HashMap<String, SetFlag>();

    private Residence plugin;

    public ResidencePlayerListener(Residence plugin) {
	currentRes = new HashMap<String, String>();
	lastUpdate = new HashMap<String, Long>();
	lastOutsideLoc = new HashMap<String, Location>();
	playerToggleChat.clear();
	minUpdateTime = Residence.getConfigManager().getMinMoveUpdateInterval();
	chatenabled = Residence.getConfigManager().chatEnabled();
	for (Player player : Bukkit.getOnlinePlayers()) {
	    lastUpdate.put(player.getName(), System.currentTimeMillis());
	}
	this.plugin = plugin;
    }

    public Map<String, SetFlag> getGUImap() {
	return GUI;
    }

    public void reload() {
	currentRes = new HashMap<String, String>();
	lastUpdate = new HashMap<String, Long>();
	lastOutsideLoc = new HashMap<String, Location>();
	playerToggleChat.clear();
	minUpdateTime = Residence.getConfigManager().getMinMoveUpdateInterval();
	chatenabled = Residence.getConfigManager().chatEnabled();
	for (Player player : Bukkit.getOnlinePlayers()) {
	    lastUpdate.put(player.getName(), System.currentTimeMillis());
	}
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResidenceBackup(ResidenceFlagChangeEvent event) {
	if (!event.getFlag().equalsIgnoreCase("backup"))
	    return;
	Player player = event.getPlayer();
	if (!Residence.getConfigManager().RestoreAfterRentEnds)
	    return;
	if (!Residence.getConfigManager().SchematicsSaveOnFlagChange)
	    return;
	if (Residence.getSchematicManager() == null)
	    return;
	if (player != null && !player.hasPermission("residence.backup"))
	    event.setCancelled(true);
	else
	    Residence.getSchematicManager().save(event.getResidence());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResidenceBackupRename(ResidenceRenameEvent event) {
	Residence.getSchematicManager().rename(event.getResidence(), event.getNewResidenceName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResidenceDelete(ResidenceDeleteEvent event) {
	Residence.getSchematicManager().delete(event.getResidence());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLogin(PlayerLoginEvent event) {
	if (!Residence.getConfigManager().isRentInformOnEnding())
	    return;
	final Player player = event.getPlayer();
	Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
	    public void run() {
		if (!player.isOnline())
		    return;
		List<String> list = Residence.getRentManager().getRentedLandsList(player.getName());
		if (list.isEmpty())
		    return;
		for (String one : list) {
		    RentedLand rentedland = Residence.getRentManager().getRentedLand(one);
		    if (rentedland == null)
			continue;
		    if (rentedland.AutoPay)
			continue;
		    if (rentedland.endTime - System.currentTimeMillis() < Residence.getConfigManager().getRentInformBefore() * 60 * 24 * 7) {
			player.sendMessage(Residence.getLM().getMessage("Residence.EndingRent", one, GetTime.getTime(rentedland.endTime)));
		    }
		}
	    }
	}, Residence.getConfigManager().getRentInformDelay() * 20L);
    }

    @EventHandler
    public void onFishingRodUse(PlayerFishEvent event) {
	Player player = event.getPlayer();
	if (event.getCaught() instanceof ArmorStand || event.getCaught() instanceof Boat || event.getCaught() instanceof LivingEntity) {
	    FlagPermissions perm = Residence.getPermsByLoc(event.getCaught().getLocation());
	    ClaimedResidence res = Residence.getResidenceManager().getByLoc(event.getCaught().getLocation());
	    if (!perm.has("hook", false) && res != null && !res.getPermissions().playerHas(player, "hook", true) ||
		!perm.has("hook", false) && res != null && !res.getPermissions().playerHas(player, "hook", false)) {
		event.setCancelled(true);
		if (res != null)
		    player.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "hook", res.getName()));
		return;
	    }
	}
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFlagChangeDayNight(ResidenceFlagChangeEvent event) {
	if (event.isCancelled())
	    return;

	if (!event.getFlag().equalsIgnoreCase("day") && !event.getFlag().equalsIgnoreCase("night"))
	    return;

	switch (event.getNewState()) {
	case NEITHER:
	case FALSE:
	    for (Player one : event.getResidence().getPlayersInResidence())
		one.resetPlayerTime();
	    break;
	case INVALID:
	    break;
	case TRUE:
	    if (event.getFlag().equalsIgnoreCase("day"))
		for (Player one : event.getResidence().getPlayersInResidence())
		    one.setPlayerTime(6000L, false);
	    if (event.getFlag().equalsIgnoreCase("night"))
		for (Player one : event.getResidence().getPlayersInResidence())
		    one.setPlayerTime(14000L, false);
	    break;
	default:
	    break;
	}
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFlagChangeSunRain(ResidenceFlagChangeEvent event) {
	if (event.isCancelled())
	    return;

	if (!event.getFlag().equalsIgnoreCase("sun") && !event.getFlag().equalsIgnoreCase("rain"))
	    return;

	switch (event.getNewState()) {
	case NEITHER:
	case FALSE:
	    for (Player one : event.getResidence().getPlayersInResidence())
		one.resetPlayerWeather();
	    break;
	case INVALID:
	    break;
	case TRUE:
	    if (event.getFlag().equalsIgnoreCase("sun"))
		for (Player one : event.getResidence().getPlayersInResidence())
		    one.setPlayerWeather(WeatherType.CLEAR);
	    if (event.getFlag().equalsIgnoreCase("rain"))
		for (Player one : event.getResidence().getPlayersInResidence())
		    one.setPlayerWeather(WeatherType.DOWNFALL);
	    break;
	default:
	    break;
	}
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;
	Player player = event.getPlayer();
	String resname = Residence.getPlayerListener().getCurrentResidenceName(player.getName());
	if (resname == null)
	    return;
	ClaimedResidence res = Residence.getResidenceManager().getByName(resname);
	if (res == null)
	    return;
	if (res.getPermissions().playerHas(player.getName(), "command", true))
	    return;

	if (Residence.getPermissionManager().isResidenceAdmin(player))
	    return;

	if (player.hasPermission("residence.flag.command.bypass"))
	    return;

	String msg = event.getMessage().replace(" ", "_");

	int white = 0;
	int black = 0;

	for (String oneWhite : res.getCmdWhiteList()) {
	    if (msg.startsWith("/" + oneWhite)) {
		if (oneWhite.contains("_") && oneWhite.split("_").length > white)
		    white = oneWhite.split("_").length;
		else if (white == 0)
		    white = 1;
	    }
	}

	for (String oneBlack : res.getCmdBlackList()) {
	    if (msg.startsWith("/" + oneBlack)) {
		if (msg.contains("_"))
		    black = oneBlack.split("_").length;
		else
		    black = 1;
		break;
	    }
	}

	if (black == 0)
	    for (String oneBlack : res.getCmdBlackList()) {
		if (oneBlack.equalsIgnoreCase("*")) {
		    if (msg.contains("_"))
			black = msg.split("_").length;
		    break;
		}
	    }

	if (white != 0 && white >= black || black == 0)
	    return;

	event.setCancelled(true);
	player.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "command", res.getName()));

    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFlagGuiClick(InventoryClickEvent event) {
	if (this.getGUImap().size() == 0)
	    return;

	Player player = (Player) event.getWhoClicked();

	if (!this.getGUImap().containsKey(player.getName()))
	    return;

	event.setCancelled(true);
	int slot = event.getRawSlot();

	if (slot > 53 || slot < 0)
	    return;

	SetFlag setFlag = this.getGUImap().get(player.getName());
	ClickType click = event.getClick();
	InventoryAction action = event.getAction();
	setFlag.toggleFlag(slot, click, action);
	setFlag.recalculateInv();
	player.getOpenInventory().getTopInventory().setContents(setFlag.getInventory().getContents());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFlagGuiClose(InventoryCloseEvent event) {
	if (this.getGUImap().isEmpty())
	    return;
	HumanEntity player = event.getPlayer();
	if (!this.getGUImap().containsKey(player.getName()))
	    return;
	this.getGUImap().remove(player.getName());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSignInteract(PlayerInteractEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;

	Block block = event.getClickedBlock();

	if (block == null)
	    return;

	if (!(block.getState() instanceof Sign))
	    return;

	Player player = (Player) event.getPlayer();

	Location loc = block.getLocation();

	for (Signs one : Residence.getSignUtil().getSigns().GetAllSigns()) {
	    if (!one.GetLocation().getWorld().getName().equalsIgnoreCase(loc.getWorld().getName()))
		continue;
	    if (one.GetLocation().getBlockX() != loc.getBlockX())
		continue;
	    if (one.GetLocation().getBlockY() != loc.getBlockY())
		continue;
	    if (one.GetLocation().getBlockZ() != loc.getBlockZ())
		continue;

	    String landName = one.GetResidence();

	    boolean ForSale = Residence.getTransactionManager().isForSale(landName);
	    boolean ForRent = Residence.getRentManager().isForRent(landName);

	    if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
		if (ForSale) {
		    Bukkit.dispatchCommand(player, "res market buy " + landName);
		    break;
		}

		if (ForRent) {
		    if (Residence.getRentManager().isRented(landName) && player.isSneaking())
			Bukkit.dispatchCommand(player, "res market release " + landName);
		    else {
			boolean stage = true;
			if (player.isSneaking())
			    stage = false;
			Bukkit.dispatchCommand(player, "res market rent " + landName + " " + stage);
		    }
		    break;
		}
	    } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
		if (ForRent && Residence.getRentManager().isRented(landName) && Residence.getRentManager().getRentingPlayer(landName).equals(player.getName())) {
		    Residence.getRentManager().payRent(player, landName, false);
		}
	    }
	}
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignCreate(SignChangeEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;
	Block block = event.getBlock();

	if (!(block.getState() instanceof Sign))
	    return;

	Sign sign = (Sign) block.getState();

	if (!ChatColor.stripColor(event.getLine(0)).equalsIgnoreCase(Residence.getLM().getMessage("Sign.TopLine")))
	    return;

	Signs signInfo = new Signs();

	Location loc = sign.getLocation();

	String landName = null;

	ClaimedResidence res = null;
	if (!event.getLine(1).equalsIgnoreCase("")) {

	    String resname = event.getLine(1);
	    if (!event.getLine(2).equalsIgnoreCase(""))
		resname += "." + event.getLine(2);
	    if (!event.getLine(3).equalsIgnoreCase(""))
		resname += "." + event.getLine(3);

	    res = Residence.getResidenceManager().getByName(resname);

	    if (res == null) {
		event.getPlayer().sendMessage(Residence.getLM().getMessage("Invalid.Residence"));
		return;
	    }

	    landName = res.getName();

	} else {
	    res = Residence.getResidenceManager().getByLoc(loc);
	    landName = Residence.getResidenceManager().getNameByLoc(loc);
	}

	final ClaimedResidence residence = res;

	boolean ForSale = Residence.getTransactionManager().isForSale(landName);
	boolean ForRent = Residence.getRentManager().isForRent(landName);

	int category = 1;
	if (Residence.getSignUtil().getSigns().GetAllSigns().size() > 0)
	    category = Residence.getSignUtil().getSigns().GetAllSigns().get(Residence.getSignUtil().getSigns().GetAllSigns().size() - 1).GetCategory() + 1;

	if (ForSale || ForRent) {
	    signInfo.setCategory(category);
	    signInfo.setResidence(landName);
	    signInfo.setLocation(loc);
//	    signInfo.updateLocation();
	    Residence.getSignUtil().getSigns().addSign(signInfo);
	    Residence.getSignUtil().saveSigns();
	}
	Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
	    public void run() {
		Residence.getSignUtil().CheckSign(residence);
	    }
	}, 5L);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSignDestroy(BlockBreakEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;
	if (event.isCancelled())
	    return;

	Block block = event.getBlock();

	if (block == null)
	    return;

	if (!(block.getState() instanceof Sign))
	    return;

	Location loc = block.getLocation();

	for (Signs one : Residence.getSignUtil().getSigns().GetAllSigns()) {

	    if (!one.GetLocation().getWorld().getName().equalsIgnoreCase(loc.getWorld().getName()))
		continue;
	    if (one.GetLocation().getBlockX() != loc.getBlockX())
		continue;
	    if (one.GetLocation().getBlockY() != loc.getBlockY())
		continue;
	    if (one.GetLocation().getBlockZ() != loc.getBlockZ())
		continue;

	    Residence.getSignUtil().getSigns().removeSign(one);
	    Residence.getSignUtil().saveSigns();
	    break;
	}
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
	String pname = event.getPlayer().getName();
	currentRes.remove(pname);
	lastUpdate.remove(pname);
	lastOutsideLoc.remove(pname);
	Residence.getChatManager().removeFromChannel(pname);
	Residence.getPlayerListener().removePlayerResidenceChat(pname);
	Residence.getOfflinePlayerMap().put(pname, (OfflinePlayer) event.getPlayer());
	if (Residence.getAutoSelectionManager().getList().containsKey(pname.toLowerCase()))
	    Residence.getAutoSelectionManager().getList().remove(pname);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerChangedWorldEvent event) {
	Player player = event.getPlayer();
	if (Residence.getPermissionManager().isResidenceAdmin(player)) {
	    Residence.turnResAdminOn(player);
	}
	Residence.getPermissionManager().updateGroupNameForPlayer(player, true);

	FlagPermissions perms = Residence.getPermsByLocForPlayer(player.getLocation(), player);

	if ((player.getAllowFlight() || player.isFlying()) && perms.has("nofly", false) && !Residence.isResAdminOn(player) && !player.hasPermission(
	    "residence.nofly.bypass")) {
	    Location lc = player.getLocation();
	    Location location = new Location(lc.getWorld(), lc.getX(), lc.getBlockY(), lc.getZ());
	    location.setPitch(lc.getPitch());
	    location.setYaw(lc.getYaw());
	    int from = location.getBlockY();
	    int maxH = location.getWorld().getMaxHeight() - 1;
	    for (int i = 0; i < maxH; i++) {
		location.setY(from - i);
		Block block = location.getBlock();
		if (!Residence.getNms().isEmptyBlock(block)) {
		    location.setY(from - i + 1);
		    break;
		}
		if (location.getBlockY() <= 0) {
		    player.setFlying(false);
		    player.setAllowFlight(false);
		    player.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "Fly", location.getWorld().getName()));
		    return;
		}
	    }
	    player.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "Fly", location.getWorld().getName()));
	    player.teleport(location);
	    player.setFlying(false);
	    player.setAllowFlight(false);
	}
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
	Player player = event.getPlayer();
	lastUpdate.put(player.getName(), 0L);
	if (Residence.getPermissionManager().isResidenceAdmin(player)) {
	    Residence.turnResAdminOn(player);
	}
	handleNewLocation(player, player.getLocation(), true);

	final Player p = player;
	Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
	    @Override
	    public void run() {
		Residence.getPlayerManager().playerJoin(p);
		if (p != null)
		    Residence.getPermissionManager().updateGroupNameForPlayer(p, true);
		return;
	    }
	});

	if (player.hasPermission("residence.versioncheck")) {
	    Residence.getVersionChecker().VersionCheck(player);
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerSpawn(PlayerRespawnEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getRespawnLocation().getWorld()))
	    return;
	Location loc = event.getRespawnLocation();
	Boolean bed = event.isBedSpawn();
	Player player = event.getPlayer();
	ClaimedResidence res = Residence.getResidenceManager().getByLoc(loc);
	if (res == null) {
	    return;
	}
	if (res.getPermissions().playerHas(player.getName(), "move", true)) {
	    return;
	}
	if (bed) {
	    loc = player.getWorld().getSpawnLocation();
	}
	res = Residence.getResidenceManager().getByLoc(loc);
	if (res != null && !res.getPermissions().playerHas(player.getName(), "move", true)) {
	    loc = res.getOutsideFreeLoc(loc, player);
	}

	player.sendMessage(Residence.getLM().getMessage("General.NoSpawn"));
	event.setRespawnLocation(loc);
    }

    @SuppressWarnings("deprecation")
    private boolean isContainer(Material mat, Block block) {
	return FlagPermissions.getMaterialUseFlagList().containsKey(mat) && FlagPermissions.getMaterialUseFlagList().get(mat).equals("container") || Residence
	    .getConfigManager().getCustomContainers().contains(block.getTypeId());
    }

    @SuppressWarnings("deprecation")
    private boolean isCanUseEntity_RClickOnly(Material mat, Block block) {

	switch (mat) {
	case ITEM_FRAME:
	case BEACON:
	case FLOWER_POT:
	case COMMAND:
	case ANVIL:
	case CAKE_BLOCK:
	case NOTE_BLOCK:
	case DIODE:
	case DIODE_BLOCK_OFF:
	case DIODE_BLOCK_ON:
	case REDSTONE_COMPARATOR:
	case REDSTONE_COMPARATOR_OFF:
	case REDSTONE_COMPARATOR_ON:
	case BED_BLOCK:
	case WORKBENCH:
	case BREWING_STAND:
	case ENCHANTMENT_TABLE:
	case DAYLIGHT_DETECTOR:
	case DAYLIGHT_DETECTOR_INVERTED:
	    return true;
	default:
	    return Residence.getConfigManager().getCustomRightClick().contains(Integer.valueOf(block.getTypeId()));
	}
    }

    private boolean isCanUseEntity(Material mat, Block block) {
	return Residence.getNms().isCanUseEntity_BothClick(mat, block) || isCanUseEntity_RClickOnly(mat, block);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerFireInteract(PlayerInteractEvent event) {

	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;
	if (event.getAction() != Action.LEFT_CLICK_BLOCK)
	    return;

	Block block = event.getClickedBlock();

	if (block == null)
	    return;

	Block relativeBlock = block.getRelative(event.getBlockFace());

	if (relativeBlock == null)
	    return;

	Player player = event.getPlayer();
	FlagPermissions perms = Residence.getPermsByLocForPlayer(block.getLocation(), player);
	if (relativeBlock.getType() == Material.FIRE) {
	    boolean hasplace = perms.playerHas(player.getName(), player.getWorld().getName(), "place", perms.playerHas(player.getName(), player.getWorld().getName(),
		"build", true));
	    if (!hasplace) {
		event.setCancelled(true);
		player.sendMessage(Residence.getLM().getMessage("Flag.Deny", "build"));
		return;
	    }
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlatePress(PlayerInteractEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;
	if (event.getAction() != Action.PHYSICAL)
	    return;
	Block block = event.getClickedBlock();
	if (block == null)
	    return;
	Material mat = block.getType();
	Player player = event.getPlayer();
	FlagPermissions perms = Residence.getPermsByLocForPlayer(block.getLocation(), player);
	String world = player.getWorld().getName();
	boolean resadmin = Residence.isResAdminOn(player);
	if (!resadmin) {
	    boolean hasuse = perms.playerHas(player.getName(), world, "use", true);
	    boolean haspressure = perms.playerHas(player.getName(), world, "pressure", hasuse);
	    if ((!hasuse && !haspressure || !haspressure) && (mat == Material.STONE_PLATE || mat == Material.WOOD_PLATE || Residence.getNms().isPlate(mat))) {
		event.setCancelled(true);
		return;
	    }
	}
	if (!perms.playerHas(player.getName(), world, "trample", perms.playerHas(player.getName(), world, "build", true)) && (mat == Material.SOIL
	    || mat == Material.SOUL_SAND)) {
	    event.setCancelled(true);
	    return;
	}
	return;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSelection(PlayerInteractEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;
	if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK)
	    return;

	Player player = event.getPlayer();
	@SuppressWarnings("deprecation")
	int heldItemId = player.getItemInHand().getTypeId();

	if (heldItemId != Residence.getConfigManager().getSelectionTooldID())
	    return;

	if (Residence.wepid == Residence.getConfigManager().getSelectionTooldID())
	    return;

	if (player.getGameMode() == GameMode.CREATIVE)
	    event.setCancelled(true);

	boolean resadmin = Residence.isResAdminOn(player);

	PermissionGroup group = Residence.getPermissionManager().getGroup(player);
	if (player.hasPermission("residence.select") || player.hasPermission("residence.create") && !player.isPermissionSet("residence.select") || group
	    .canCreateResidences() && !player.isPermissionSet("residence.create") && !player.isPermissionSet("residence.select") || resadmin) {

	    Block block = event.getClickedBlock();

	    if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
		Location loc = block.getLocation();
		Residence.getSelectionManager().placeLoc1(player, loc, true);
		player.sendMessage(Residence.getLM().getMessage("Select.PrimaryPoint", Residence.getLM().getMessage("General.CoordsTop", loc.getBlockX(), loc.getBlockY(),
		    loc.getBlockZ())));
		event.setCancelled(true);
	    } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && Residence.getNms().isMainHand(event)) {
		Location loc = block.getLocation();
		Residence.getSelectionManager().placeLoc2(player, loc, true);
		player.sendMessage(Residence.getLM().getMessage("Select.SecondaryPoint", Residence.getLM().getMessage("General.CoordsBottom", loc.getBlockX(), loc
		    .getBlockY(), loc.getBlockZ())));
		event.setCancelled(true);
	    }

	    if (Residence.getSelectionManager().hasPlacedBoth(player.getName()))
		Residence.getSelectionManager().showSelectionInfoInActionBar(player);
	}
	return;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInfoCheck(PlayerInteractEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;
	if (event.getAction() != Action.LEFT_CLICK_BLOCK)
	    return;
	Block block = event.getClickedBlock();
	if (block == null)
	    return;
	Player player = event.getPlayer();

	ItemStack item = event.getItem();
	if (item == null)
	    return;

	int heldItemId = item.getTypeId();

	if (heldItemId != Residence.getConfigManager().getInfoToolID())
	    return;

	Location loc = block.getLocation();
	String res = Residence.getResidenceManager().getNameByLoc(loc);
	if (res != null)
	    Residence.getResidenceManager().printAreaInfo(res, player, false);
	else
	    player.sendMessage(Residence.getLM().getMessage("Residence.NoResHere"));
	event.setCancelled(true);
	return;

    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;
	Player player = event.getPlayer();
	ItemStack iih = Residence.getNms().itemInMainHand(player);
	Material heldItem = iih.getType();
	int heldItemId = iih.getTypeId();
	Block block = event.getClickedBlock();
	if (block == null)
	    return;

	Material mat = block.getType();

	if (!(event.getAction() == Action.PHYSICAL || (isContainer(mat, block) || isCanUseEntity_RClickOnly(mat, block)) && event.getAction() == Action.RIGHT_CLICK_BLOCK
	    || Residence.getNms().isCanUseEntity_BothClick(mat, block))) {
	    if (heldItemId != Residence.getConfigManager().getSelectionTooldID() && heldItemId != Residence.getConfigManager().getInfoToolID()
		&& heldItem != Material.INK_SACK && !Residence.getNms().isArmorStandMaterial(heldItem) && !Residence.getNms().isBoat(heldItem)) {
		return;
	    }
	}

	if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK)
	    return;

	String world = player.getWorld().getName();
	String permgroup = Residence.getPermissionManager().getGroupNameByPlayer(player);
	boolean resadmin = Residence.isResAdminOn(player);
	if (!resadmin && !Residence.getItemManager().isAllowed(heldItem, permgroup, world)) {
	    player.sendMessage(Residence.getLM().getMessage("General.ItemBlacklisted"));
	    event.setCancelled(true);
	    return;
	}

	if (resadmin)
	    return;

	int blockId = block.getTypeId();
	FlagPermissions perms = Residence.getPermsByLocForPlayer(block.getLocation(), player);
	if (heldItem != null && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
	    if (heldItem == Material.INK_SACK) {
		if (Residence.getNms().itemInMainHand(player).getData().getData() == 15 && block.getType() == Material.GRASS || iih.getData().getData() == 3
		    && blockId == 17 && (block.getData() == 3 || block.getData() == 7 || block.getData() == 11 || block.getData() == 15)) {
		    perms = Residence.getPermsByLocForPlayer(block.getRelative(event.getBlockFace()).getLocation(), player);
		    if (!perms.playerHas(player.getName(), world, "build", true)) {
			player.sendMessage(Residence.getLM().getMessage("Flag.Deny", "build"));
			event.setCancelled(true);
			return;
		    }
		}
	    }
	    if (Residence.getNms().isArmorStandMaterial(heldItem) || Residence.getNms().isBoat(heldItem)) {
		perms = Residence.getPermsByLocForPlayer(block.getRelative(event.getBlockFace()).getLocation(), player);
		if (!perms.playerHas(player.getName(), world, "build", true)) {
		    player.sendMessage(Residence.getLM().getMessage("Flag.Deny", "build"));
		    event.setCancelled(true);
		    return;
		}
	    }
	}

	if (isContainer(mat, block) || isCanUseEntity(mat, block)) {
	    boolean hasuse = perms.playerHas(player.getName(), world, "use", true);
	    for (Entry<Material, String> checkMat : FlagPermissions.getMaterialUseFlagList().entrySet()) {
		if (mat != checkMat.getKey())
		    continue;

		if (perms.playerHas(player.getName(), world, checkMat.getValue(), hasuse))
		    continue;

		if (hasuse || checkMat.getValue().equals("container")) {
		    event.setCancelled(true);
		    player.sendMessage(Residence.getLM().getMessage("Flag.Deny", checkMat.getValue()));
		    return;
		} else {
		    event.setCancelled(true);
		    player.sendMessage(Residence.getLM().getMessage("Flag.Deny", "use"));
		    return;
		}

	    }
	    if (Residence.getConfigManager().getCustomContainers().contains(blockId)) {
		if (!perms.playerHas(player.getName(), world, "container", hasuse)) {
		    event.setCancelled(true);
		    player.sendMessage(Residence.getLM().getMessage("Flag.Deny", "container"));
		    return;
		}
	    }
	    if (Residence.getConfigManager().getCustomBothClick().contains(blockId)) {
		if (!hasuse) {
		    event.setCancelled(true);
		    player.sendMessage(Residence.getLM().getMessage("Flag.Deny", "use"));
		    return;
		}
	    }
	    if (Residence.getConfigManager().getCustomRightClick().contains(blockId) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
		if (!hasuse) {
		    event.setCancelled(true);
		    player.sendMessage(Residence.getLM().getMessage("Flag.Deny", "use"));
		    return;
		}
	    }
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerTradeEntity(PlayerInteractEntityEvent event) {

	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;
	Player player = event.getPlayer();
	if (Residence.isResAdminOn(player))
	    return;

	Entity ent = event.getRightClicked();
	/* Trade */
	if (ent.getType() != EntityType.VILLAGER)
	    return;

	ClaimedResidence res = Residence.getResidenceManager().getByLoc(ent.getLocation());

	if (res != null && !res.getPermissions().playerHas(player.getName(), "trade", true)) {
	    player.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "trade", res.getName()));
	    event.setCancelled(true);
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteractWithHorse(PlayerInteractEntityEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;
	Player player = event.getPlayer();
	if (Residence.isResAdminOn(player))
	    return;

	Entity ent = event.getRightClicked();

	if (ent.getType() != EntityType.HORSE)
	    return;

	ClaimedResidence res = Residence.getResidenceManager().getByLoc(ent.getLocation());
	if (res == null)
	    return;
	if (!res.isOwner(player) && !res.getPermissions().playerHas(player.getName(), "container", true) && player.isSneaking()) {
	    player.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "container", res.getName()));
	    event.setCancelled(true);
	} else if (!res.isOwner(player) && !res.getPermissions().playerHas(player.getName(), "riding", false)) {
	    player.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "riding", res.getName()));
	    event.setCancelled(true);
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteractWithMinecartStorage(PlayerInteractEntityEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;
	Player player = event.getPlayer();
	if (Residence.isResAdminOn(player))
	    return;

	Entity ent = event.getRightClicked();

	if (ent.getType() != EntityType.MINECART_CHEST && ent.getType() != EntityType.MINECART_HOPPER)
	    return;

	ClaimedResidence res = Residence.getResidenceManager().getByLoc(ent.getLocation());
	if (res == null)
	    return;
	if (!res.isOwner(player) && !res.getPermissions().playerHas(player.getName(), "container", true)) {
	    player.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "container", res.getName()));
	    event.setCancelled(true);
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteractWithMinecart(PlayerInteractEntityEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;
	Player player = event.getPlayer();
	if (Residence.isResAdminOn(player))
	    return;

	Entity ent = event.getRightClicked();

	if (ent.getType() != EntityType.MINECART && ent.getType() != EntityType.BOAT)
	    return;

	ClaimedResidence res = Residence.getResidenceManager().getByLoc(ent.getLocation());
	if (res == null)
	    return;
	if (!res.isOwner(player) && !res.getPermissions().playerHas(player.getName(), "riding", false)) {
	    player.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "riding", res.getName()));
	    event.setCancelled(true);
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDyeSheep(PlayerInteractEntityEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;
	Player player = event.getPlayer();
	if (Residence.isResAdminOn(player))
	    return;

	Entity ent = event.getRightClicked();
	/* Dye */
	if (ent.getType() != EntityType.SHEEP)
	    return;

	ClaimedResidence res = Residence.getResidenceManager().getByLoc(ent.getLocation());
	if (res == null)
	    return;
	if (!res.isOwner(player) && !res.getPermissions().playerHas(player.getName(), "dye", true)) {
	    player.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "dye", res.getName()));
	    event.setCancelled(true);
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerShearEntity(PlayerShearEntityEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;
	if (event.isCancelled())
	    return;

	Player player = event.getPlayer();
	if (Residence.isResAdminOn(player))
	    return;

	Entity ent = event.getEntity();

	ClaimedResidence res = Residence.getResidenceManager().getByLoc(ent.getLocation());
	if (res == null)
	    return;

	if (!res.isOwner(player) && !res.getPermissions().playerHas(player.getName(), "shear", true)) {
	    player.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "Shear", res.getName()));
	    event.setCancelled(true);
	}

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerItemFrameInteract(PlayerInteractEntityEvent event) {

	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;
	Player player = event.getPlayer();
	if (Residence.isResAdminOn(player))
	    return;

	Entity ent = event.getRightClicked();

	/* Container - ItemFrame protection */
	if (!(ent instanceof Hanging))
	    return;

	Hanging hanging = (Hanging) ent;

	if (hanging.getType() != EntityType.ITEM_FRAME) {
	    return;
	}

	Material heldItem = Residence.getNms().itemInMainHand(player).getType();

	FlagPermissions perms = Residence.getPermsByLocForPlayer(ent.getLocation(), player);
	String world = player.getWorld().getName();
	String permgroup = Residence.getPermissionManager().getGroupNameByPlayer(player);
	if (!Residence.getItemManager().isAllowed(heldItem, permgroup, world)) {
	    player.sendMessage(Residence.getLM().getMessage("General.ItemBlacklisted"));
	    event.setCancelled(true);
	    return;
	}
	if (!perms.playerHas(player.getName(), world, "container", perms.playerHas(player.getName(), world, "use", true))) {
	    event.setCancelled(true);
	    player.sendMessage(Residence.getLM().getMessage("Flag.Deny", "container"));
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;
	Player player = event.getPlayer();
	if (Residence.isResAdminOn(player))
	    return;

	Location loc = event.getBlockClicked().getLocation();

	ClaimedResidence res = Residence.getResidenceManager().getByLoc(loc);
	if (res != null) {
	    if (Residence.getConfigManager().preventRentModify() && Residence.getConfigManager().enabledRentSystem()) {
		if (Residence.getRentManager().isRented(res.getName())) {
		    player.sendMessage(Residence.getLM().getMessage("Rent.ModifyDeny"));
		    event.setCancelled(true);
		    return;
		}
	    }

	    Material mat = event.getBucket();
	    if ((!res.getPermissions().playerHas(player.getName(), "bucket", true) && !res.getPermissions().playerHas(player.getName(), "bucketempty", true))
		&& Residence.getConfigManager().getNoPlaceWorlds().contains(loc.getWorld().getName())) {
		if (mat == Material.LAVA_BUCKET) {
		    event.setCancelled(true);
		    return;
		}
		if (mat == Material.WATER_BUCKET) {
		    event.setCancelled(true);
		    return;
		}
	    }
	}

	String pname = player.getName();
	FlagPermissions perms = Residence.getPermsByLocForPlayer(loc, player);
	if (!perms.playerHas(pname, player.getWorld().getName(), "bucket", perms.playerHas(pname, player.getWorld().getName(), "build", true)) &&
	    !perms.playerHas(pname, player.getWorld().getName(), "bucketempty", perms.playerHas(pname, player.getWorld().getName(), "build", true))) {
	    player.sendMessage(Residence.getLM().getMessage("Flag.Deny", "bucket"));
	    event.setCancelled(true);
	    return;
	}

	Material mat = event.getBucket();
	int level = Residence.getConfigManager().getPlaceLevel();
	if (res == null && Residence.getConfigManager().isNoLavaPlace() && loc.getBlockY() >= level - 1 && Residence.getConfigManager()
	    .getNoPlaceWorlds().contains(loc.getWorld().getName())) {
	    if (mat == Material.LAVA_BUCKET) {
		if (!Residence.getLM().getMessage("General.CantPlaceLava").equalsIgnoreCase(""))
		    player.sendMessage(Residence.getLM().getMessage("General.CantPlaceLava", level));
		event.setCancelled(true);
		return;
	    }
	}

	if (res == null && Residence.getConfigManager().isNoWaterPlace() && loc.getBlockY() >= level - 1 && Residence.getConfigManager()
	    .getNoPlaceWorlds().contains(loc.getWorld().getName()))
	    if (mat == Material.WATER_BUCKET) {
		if (!Residence.getLM().getMessage("General.CantPlaceWater").equalsIgnoreCase(""))
		    player.sendMessage(Residence.getLM().getMessage("General.CantPlaceWater", level));
		event.setCancelled(true);
		return;
	    }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;
	Player player = event.getPlayer();
	if (Residence.isResAdminOn(player))
	    return;

	ClaimedResidence res = Residence.getResidenceManager().getByLoc(event.getBlockClicked().getLocation());
	if (res != null) {
	    if (Residence.getConfigManager().preventRentModify() && Residence.getConfigManager().enabledRentSystem()) {
		if (Residence.getRentManager().isRented(res.getName())) {
		    player.sendMessage(Residence.getLM().getMessage("Rent.ModifyDeny"));
		    event.setCancelled(true);
		    return;
		}
	    }
	}

	String pname = player.getName();
	FlagPermissions perms = Residence.getPermsByLocForPlayer(event.getBlockClicked().getLocation(), player);
	boolean hasbucket = perms.playerHas(pname, player.getWorld().getName(), "bucket", perms.playerHas(pname, player.getWorld().getName(), "build", true));
	boolean hasbucketfill = perms.playerHas(pname, player.getWorld().getName(), "bucketfill", perms.playerHas(pname, player.getWorld().getName(), "build", true));
	if (!hasbucket && !hasbucketfill) {
	    player.sendMessage(Residence.getLM().getMessage("Flag.Deny", "bucket"));
	    event.setCancelled(true);
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;
	Player player = event.getPlayer();

	if (player.hasMetadata("NPC"))
	    return;

	Location loc = event.getTo();

	if (Residence.isResAdminOn(player)) {
	    handleNewLocation(player, loc, false);
	    return;
	}

	ClaimedResidence res = Residence.getResidenceManager().getByLoc(loc);
	if (res != null) {
	    if (event.getCause() == TeleportCause.COMMAND || event.getCause() == TeleportCause.NETHER_PORTAL || event
		.getCause() == TeleportCause.PLUGIN) {
		String areaname = res.getName();
		if (!res.getPermissions().playerHas(player.getName(), "move", true) && !res.isOwner(player)) {
		    event.setCancelled(true);
		    player.sendMessage(Residence.getLM().getMessage("Residence.MoveDeny", areaname));
		    return;
		}
	    } else if (event.getCause() == TeleportCause.ENDER_PEARL) {
		String areaname = res.getName();
		if (!res.getPermissions().playerHas(player.getName(), "enderpearl", true)) {
		    event.setCancelled(true);
		    player.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "enderpearl", areaname));
		    return;
		}
	    }
	    if (event.getCause() == TeleportCause.PLUGIN || event.getCause() == TeleportCause.COMMAND && Residence.getConfigManager().isBlockAnyTeleportation()
		|| Residence.getNms().isChorusTeleport(event.getCause())) {
		if (!res.isOwner(player) && !res.getPermissions().playerHas(player.getName(), "tp", true) && !player.hasPermission("residence.admin.tp")) {
		    String areaname = res.getName();
		    event.setCancelled(true);
		    player.sendMessage(Residence.getLM().getMessage("General.TeleportDeny", areaname));
		    return;
		}
	    }
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDeath(final PlayerDeathEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getEntity().getWorld()))
	    return;
	Player player = event.getEntity();
	if (player == null)
	    return;
	if (player.hasMetadata("NPC"))
	    return;
	Location loc = player.getLocation();
	ClaimedResidence res = Residence.getResidenceManager().getByLoc(loc);
	if (res == null)
	    return;

	if (res.getPermissions().has("keepinv", false))
	    event.setKeepInventory(true);

	if (res.getPermissions().has("keepexp", false))
	    event.setKeepLevel(true);

	if (res.getPermissions().has("respawn", false) && Bukkit.getVersion().toString().contains("Spigot"))
	    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
		public void run() {
		    try {
			event.getEntity().spigot().respawn();
		    } catch (Exception e) {
		    }
		    return;
		}
	    }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;
	Player player = event.getPlayer();
	if (player == null)
	    return;

	if (player.hasMetadata("NPC"))
	    return;

	Location locfrom = event.getFrom();
	Location locto = event.getTo();
	if (locfrom.getX() == locto.getX() && locfrom.getY() == locto.getY() && locfrom.getZ() == locto.getZ())
	    return;

	String name = player.getName();

	if (name == null)
	    return;

	long last = lastUpdate.get(name);
	long now = System.currentTimeMillis();
	if (now - last < Residence.getConfigManager().getMinMoveUpdateInterval())
	    return;

	this.lastUpdate.put(name, now);

	handleNewLocation(player, locto, true);
	if (!Residence.getTeleportDelayMap().isEmpty() && Residence.getConfigManager().getTeleportDelay() > 0 && Residence.getTeleportDelayMap().contains(player
	    .getName())) {
	    Residence.getTeleportDelayMap().remove(player.getName());
	    player.sendMessage(Residence.getLM().getMessage("General.TeleportCanceled"));
	}
    }

    public void handleNewLocation(final Player player, Location loc, boolean move) {

	String pname = player.getName();
	ClaimedResidence res = Residence.getResidenceManager().getByLoc(loc);

	ClaimedResidence orres = res;
	String areaname = null;
	String subzone = null;
	if (res != null) {
	    areaname = res.getName();
	    while (res.getSubzoneByLoc(loc) != null) {
		res = res.getSubzoneByLoc(player.getLocation());
		subzone = res.getName();
		areaname = areaname + "." + subzone;
	    }
	}

	ClaimedResidence ResOld = null;
	if (currentRes.containsKey(pname)) {
	    ResOld = Residence.getResidenceManager().getByName(currentRes.get(pname));
	    if (ResOld == null) {
		currentRes.remove(pname);
	    } else {
		if (res != null && ResOld.getName().equals(res.getName())) {

		    if (player.isFlying() && res.getPermissions().playerHas(pname, "nofly", false) && !Residence.isResAdminOn(player) && !player.hasPermission(
			"residence.nofly.bypass")) {
			Location lc = player.getLocation();
			Location location = new Location(lc.getWorld(), lc.getX(), lc.getBlockY(), lc.getZ());
			location.setPitch(lc.getPitch());
			location.setYaw(lc.getYaw());
			int from = location.getBlockY();
			int maxH = location.getWorld().getMaxHeight() - 1;
			for (int i = 0; i < maxH; i++) {
			    location.setY(from - i);
			    Block block = location.getBlock();
			    if (!Residence.getNms().isEmptyBlock(block)) {
				location.setY(from - i + 1);
				break;
			    }
			    if (location.getBlockY() <= 0) {
				Location lastLoc = lastOutsideLoc.get(pname);
				if (lastLoc != null)
				    player.teleport(lastLoc);
				else
				    player.teleport(res.getOutsideFreeLoc(loc, player));
				player.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "Fly", orres.getName()));
				return;
			    }
			}
			player.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "Fly", orres.getName()));
			player.teleport(location);
			player.setFlying(false);
			player.setAllowFlight(false);
		    }

		    lastOutsideLoc.put(pname, loc);
		    return;
		}
	    }
	}

	if (!Residence.getAutoSelectionManager().getList().isEmpty()) {
	    Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
		@Override
		public void run() {
		    Residence.getAutoSelectionManager().UpdateSelection(player);
		    return;
		}
	    });
	}

	if (res == null) {
	    lastOutsideLoc.put(pname, loc);
	    if (ResOld != null) {
		String leave = ResOld.getLeaveMessage();

		// New ResidenceChangeEvent
		ResidenceChangedEvent chgEvent = new ResidenceChangedEvent(ResOld, null, player);
		Residence.getServ().getPluginManager().callEvent(chgEvent);

		if (ResOld.getPermissions().has("night", true) || ResOld.getPermissions().has("day", true))
		    player.resetPlayerTime();

		if (ResOld.getPermissions().has("sun", true) || ResOld.getPermissions().has("rain", true))
		    player.resetPlayerWeather();

		if (leave != null && !leave.equals("")) {
		    if (Residence.getConfigManager().useActionBar()) {
			Residence.getAB().send(player, (new StringBuilder()).append(ChatColor.YELLOW).append(insertMessages(player, ResOld.getName(), ResOld, leave))
			    .toString());
		    } else {
			player.sendMessage(ChatColor.YELLOW + this.insertMessages(player, ResOld.getName(), ResOld, leave));
		    }
		}
		currentRes.remove(pname);
	    }
	    return;
	}

	if (move) {
	    if (!res.getPermissions().playerHas(pname, "move", true) && !Residence.isResAdminOn(player) && !res.isOwner(player)) {
		Location lastLoc = lastOutsideLoc.get(pname);

		if (Residence.getConfigManager().BounceAnimation()) {
		    Residence.getSelectionManager().MakeBorders(player, res.getAreaArray()[0].getLowLoc(), res.getAreaArray()[0].getHighLoc(), true);
		}

		ClaimedResidence preRes = Residence.getResidenceManager().getByLoc(lastLoc);

		if (preRes != null && !preRes.getPermissions().playerHas(pname, "tp", true) && !player.hasPermission("residence.admin.tp")) {
		    Location newLoc = res.getOutsideFreeLoc(loc, player);
		    player.teleport(newLoc);
		} else if (lastLoc != null) {
		    player.teleport(lastLoc);
		}

		if (Residence.getConfigManager().useActionBar()) {
		    Residence.getAB().send(player, Residence.getLM().getMessage("Residence.MoveDeny", orres.getName()));
		} else {
		    player.sendMessage(Residence.getLM().getMessage("Residence.MoveDeny", orres.getName()));
		}
		return;
		// Preventing fly in residence only when player has move permission
	    }

	    if (player.isFlying() && res.getPermissions().playerHas(pname, "nofly", false) && !Residence.isResAdminOn(player) && !player.hasPermission(
		"residence.nofly.bypass")) {
		Location lc = player.getLocation();
		Location location = new Location(lc.getWorld(), lc.getX(), lc.getBlockY(), lc.getZ());
		location.setPitch(lc.getPitch());
		location.setYaw(lc.getYaw());
		int from = location.getBlockY();
		int maxH = location.getWorld().getMaxHeight() - 1;
		for (int i = 0; i < maxH; i++) {
		    location.setY(from - i);
		    Block block = location.getBlock();
		    if (!Residence.getNms().isEmptyBlock(block)) {
			location.setY(from - i + 1);
			break;
		    }
		    if (location.getBlockY() <= 0) {
			Location lastLoc = lastOutsideLoc.get(pname);
			if (lastLoc != null)
			    player.teleport(lastLoc);
			else
			    player.teleport(res.getOutsideFreeLoc(loc, player));

			player.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "Fly", orres.getName()));
			return;
		    }
		}
		player.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "Fly", orres.getName()));
		player.teleport(location);
		player.setFlying(false);
		player.setAllowFlight(false);
	    }

	    if (res.getPermissions().has("day", false))
		player.setPlayerTime(6000L, false);
	    else if (res.getPermissions().has("night", false))
		player.setPlayerTime(14000L, false);

	    if (res.getPermissions().has("sun", false))
		player.setPlayerWeather(WeatherType.CLEAR);
	    else if (res.getPermissions().has("rain", false))
		player.setPlayerWeather(WeatherType.DOWNFALL);
	}

	lastOutsideLoc.put(pname, loc);

	if (!currentRes.containsKey(pname) || ResOld != res) {
	    currentRes.put(pname, areaname);

	    // "from" residence for ResidenceChangedEvent
	    ClaimedResidence chgFrom = null;
	    if (ResOld != res && ResOld != null) {
		String leave = ResOld.getLeaveMessage();
		chgFrom = ResOld;

		if (ResOld.getPermissions().has("night", false) || ResOld.getPermissions().has("day", false))
		    player.resetPlayerTime();

		if (ResOld.getPermissions().has("sun", false) || ResOld.getPermissions().has("rain", false))
		    player.resetPlayerWeather();

		if (leave != null && !leave.equals("") && ResOld != res.getParent()) {
		    if (Residence.getConfigManager().useActionBar()) {
			Residence.getAB().send(player, (new StringBuilder()).append(ChatColor.YELLOW).append(insertMessages(player, ResOld.getName(), ResOld, leave))
			    .toString());
		    } else {
			player.sendMessage(ChatColor.YELLOW + this.insertMessages(player, ResOld.getName(), ResOld, leave));
		    }
		}
	    }

	    String enterMessage = res.getEnterMessage();

	    // New ResidenceChangedEvent
	    ResidenceChangedEvent chgEvent = new ResidenceChangedEvent(chgFrom, res, player);
	    Residence.getServ().getPluginManager().callEvent(chgEvent);

	    if (!(ResOld != null && res == ResOld.getParent())) {
		if (Residence.getConfigManager().isExtraEnterMessage() && !res.isOwner(player) && (Residence.getRentManager().isForRent(areaname) || Residence
		    .getTransactionManager().isForSale(areaname))) {
		    if (Residence.getRentManager().isForRent(areaname) && !Residence.getRentManager().isRented(areaname)) {
			RentableLand rentable = Residence.getRentManager().getRentableLand(areaname);
			if (rentable != null)
			    Residence.getAB().send(player, Residence.getLM().getMessage("Residence.CanBeRented", areaname, rentable.cost, rentable.days));
		    } else if (Residence.getTransactionManager().isForSale(areaname) && !res.isOwner(player)) {
			int sale = Residence.getTransactionManager().getSaleAmount(areaname);
			Residence.getAB().send(player, Residence.getLM().getMessage("Residence.CanBeBought", areaname, sale));
		    }
		} else if (enterMessage != null && !enterMessage.equals("")) {
		    if (Residence.getConfigManager().useActionBar()) {
			Residence.getAB().send(player, (new StringBuilder()).append(ChatColor.YELLOW).append(insertMessages(player, areaname, res, enterMessage))
			    .toString());
		    } else {
			player.sendMessage(ChatColor.YELLOW + this.insertMessages(player, areaname, res, enterMessage));
		    }
		}
	    }
	}
    }

    public String insertMessages(Player player, String areaname, ClaimedResidence res, String message) {
	try {
	    message = message.replaceAll("%player", player.getName());
	    message = message.replaceAll("%owner", res.getPermissions().getOwner());
	    message = message.replaceAll("%residence", areaname);
	} catch (Exception ex) {
	    return "";
	}
	return message;
    }

    public void doHeals() {
	try {
	    for (Player player : Bukkit.getServer().getOnlinePlayers()) {
		String resname = Residence.getPlayerListener().getCurrentResidenceName(player.getName());
		ClaimedResidence res = null;

		if (resname == null)
		    continue;

		res = Residence.getResidenceManager().getByName(resname);

		if (!res.getPermissions().has("healing", false))
		    continue;

		Damageable damage = player;
		double health = damage.getHealth();
		if (health < damage.getMaxHealth() && !player.isDead()) {
		    player.setHealth(health + 1);
		}
	    }
	} catch (Exception ex) {
	}
    }

    public void feed() {
	try {
	    for (Player player : Bukkit.getServer().getOnlinePlayers()) {
		String resname = Residence.getPlayerListener().getCurrentResidenceName(player.getName());

		if (resname == null)
		    continue;

		ClaimedResidence res = Residence.getResidenceManager().getByName(resname);

		if (!res.getPermissions().has("feed", false))
		    continue;

		int food = player.getFoodLevel();
		if (food < 20 && !player.isDead()) {
		    player.setFoodLevel(food + 1);
		}
	    }
	} catch (Exception ex) {
	}
    }

    public void DespawnMobs() {
	try {
	    for (Player player : Bukkit.getServer().getOnlinePlayers()) {
		String resname = Residence.getPlayerListener().getCurrentResidenceName(player.getName());

		if (resname == null)
		    continue;

		ClaimedResidence res = null;
		res = Residence.getResidenceManager().getByName(resname);

		if (!res.getPermissions().has("nomobs", false))
		    continue;

		List<Entity> entities = Bukkit.getServer().getWorld(res.getWorld()).getEntities();
		for (Entity ent : entities) {
		    if (!ResidenceEntityListener.isMonster(ent))
			continue;

		    if (res.containsLoc(ent.getLocation())) {
			ent.remove();
		    }
		}
	    }
	} catch (Exception ex) {
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getPlayer().getWorld()))
	    return;
	String pname = event.getPlayer().getName();
	if (!chatenabled || !playerToggleChat.contains(pname))
	    return;

	ChatChannel channel = Residence.getChatManager().getPlayerChannel(pname);
	if (channel != null) {
	    channel.chat(pname, event.getMessage());
	}
	event.setCancelled(true);
    }

    public void tooglePlayerResidenceChat(Player player, String residence) {
	String pname = player.getName();
	playerToggleChat.add(pname);
	player.sendMessage(Residence.getLM().getMessage("Chat.ChatChannelChange", residence));
    }

    public void removePlayerResidenceChat(String pname) {
	playerToggleChat.remove(pname);
	Player player = Bukkit.getPlayer(pname);
	if (player != null)
	    player.sendMessage(Residence.getLM().getMessage("Chat.ChatChannelLeave"));
    }

    public void removePlayerResidenceChat(Player player) {
	String pname = player.getName();
	playerToggleChat.remove(pname);
	player.sendMessage(Residence.getLM().getMessage("Chat.ChatChannelLeave"));
    }

    public String getCurrentResidenceName(String player) {
	return currentRes.get(player);
    }
}

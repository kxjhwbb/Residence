package com.bekvon.bukkit.residence.listeners;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bekvon.bukkit.residence.protection.FlagPermissions;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Hanging;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.potion.PotionEffect;

public class ResidenceEntityListener implements Listener {

    Residence plugin;

    public ResidenceEntityListener(Residence plugin) {
	this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEndermanChangeBlock(EntityChangeBlockEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getBlock().getWorld()))
	    return;
	if (event.getEntityType() != EntityType.ENDERMAN)
	    return;
	FlagPermissions perms = Residence.getPermsByLoc(event.getBlock().getLocation());
	if (!perms.has("destroy", true)) {
	    event.setCancelled(true);
	}
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityInteract(EntityInteractEvent event) {
	// disabling event on world
	Block block = event.getBlock();
	if (block == null)
	    return;
	if (Residence.isDisabledWorldListener(block.getWorld()))
	    return;
	Material mat = block.getType();
	Entity entity = event.getEntity();
	FlagPermissions perms = Residence.getPermsByLoc(block.getLocation());
	boolean hastrample = perms.has("trample", perms.has("hasbuild", true));
	if (!hastrample && !(entity.getType() == EntityType.FALLING_BLOCK) && (mat == Material.SOIL || mat == Material.SOUL_SAND)) {
	    event.setCancelled(true);
	}
    }

    public static boolean isMonster(Entity ent) {
	return (ent instanceof Monster || ent instanceof Slime || ent instanceof Ghast);
    }

    private boolean isTamed(Entity ent) {
	return (ent instanceof Tameable ? ((Tameable) ent).isTamed() : false);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void AnimalKilling(EntityDamageByEntityEvent event) {
	// disabling event on world
	Entity entity = event.getEntity();
	if (entity == null)
	    return;
	if (Residence.isDisabledWorldListener(entity.getWorld()))
	    return;
	if (!Residence.getNms().isAnimal(entity))
	    return;

	Entity damager = event.getDamager();

	if (!(damager instanceof Arrow) && !(damager instanceof Player))
	    return;

	if (damager instanceof Arrow && !(((Arrow) damager).getShooter() instanceof Player))
	    return;

	Player cause = null;

	if (damager instanceof Player) {
	    cause = (Player) damager;
	} else {
	    cause = (Player) ((Arrow) damager).getShooter();
	}

	if (cause == null)
	    return;

	if (Residence.isResAdminOn(cause))
	    return;

	ClaimedResidence res = Residence.getResidenceManager().getByLoc(entity.getLocation());

	if (res == null)
	    return;

	if (!res.getPermissions().playerHas(cause.getName(), "animalkilling", true)) {
	    cause.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "AnimalKilling", res.getName()));
	    event.setCancelled(true);
	}
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void AnimalDamageByMobs(EntityDamageByEntityEvent event) {

	// disabling event on world
	Entity entity = event.getEntity();
	if (entity == null)
	    return;
	if (Residence.isDisabledWorldListener(entity.getWorld()))
	    return;
	if (!Residence.getNms().isAnimal(entity))
	    return;

	Entity damager = event.getDamager();

	if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player || damager instanceof Player)
	    return;

	FlagPermissions perms = Residence.getPermsByLoc(entity.getLocation());
	FlagPermissions world = Residence.getWorldFlags().getPerms(entity.getWorld().getName());
	if (!perms.has("animalkilling", world.has("animalkilling", true))) {
	    event.setCancelled(true);
	    return;
	}
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void OnEntityDeath(EntityDeathEvent event) {
	// disabling event on world
	LivingEntity ent = event.getEntity();
	if (ent == null)
	    return;
	if (Residence.isDisabledWorldListener(ent.getWorld()))
	    return;
	if (ent instanceof Player)
	    return;
	Location loc = ent.getLocation();
	FlagPermissions perms = Residence.getPermsByLoc(loc);
	if (!perms.has("mobitemdrop", true)) {
	    event.getDrops().clear();
	}
	if (!perms.has("mobexpdrop", true)) {
	    event.setDroppedExp(0);
	}
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void VehicleDestroy(VehicleDestroyEvent event) {
	// disabling event on world
	Entity damager = event.getAttacker();
	if (damager == null)
	    return;

	if (Residence.isDisabledWorldListener(damager.getWorld()))
	    return;

	Vehicle vehicle = event.getVehicle();

	if (vehicle == null)
	    return;

	if (damager instanceof Projectile && !(((Projectile) damager).getShooter() instanceof Player) || !(damager instanceof Player)) {
	    FlagPermissions perms = Residence.getPermsByLoc(vehicle.getLocation());
	    if (!perms.has("vehicledestroy", true)) {
		event.setCancelled(true);
		return;
	    }
	}

	Player cause = null;

	if (damager instanceof Player) {
	    cause = (Player) damager;
	} else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player) {
	    cause = (Player) ((Projectile) damager).getShooter();
	}

	if (cause == null)
	    return;

	if (Residence.isResAdminOn(cause))
	    return;

	ClaimedResidence res = Residence.getResidenceManager().getByLoc(vehicle.getLocation());

	if (res == null)
	    return;

	if (!res.getPermissions().playerHas(cause.getName(), "vehicledestroy", true)) {
	    cause.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "vehicledestroy", res.getName()));
	    event.setCancelled(true);
	}
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void MonsterKilling(EntityDamageByEntityEvent event) {
	// disabling event on world
	Entity entity = event.getEntity();
	if (entity == null)
	    return;
	if (Residence.isDisabledWorldListener(entity.getWorld()))
	    return;
	if (!isMonster(entity))
	    return;

	Entity damager = event.getDamager();

	if (!(damager instanceof Arrow) && !(damager instanceof Player))
	    return;

	if (damager instanceof Arrow && !(((Arrow) damager).getShooter() instanceof Player))
	    return;

	Player cause = null;

	if (damager instanceof Player) {
	    cause = (Player) damager;
	} else {
	    cause = (Player) ((Arrow) damager).getShooter();
	}

	if (cause == null)
	    return;

	if (Residence.isResAdminOn(cause))
	    return;

	ClaimedResidence res = Residence.getResidenceManager().getByLoc(entity.getLocation());

	if (res == null)
	    return;

	if (!res.getPermissions().playerHas(cause.getName(), "mobkilling", true)) {
	    cause.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "MobKilling", res.getName()));
	    event.setCancelled(true);
	}
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void AnimalLeash(PlayerLeashEntityEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getEntity().getWorld()))
	    return;
	Player player = event.getPlayer();

	Entity entity = event.getEntity();

	if (!Residence.getNms().isAnimal(entity) && !(player instanceof Player))
	    return;

	if (Residence.isResAdminOn(player))
	    return;

	ClaimedResidence res = Residence.getResidenceManager().getByLoc(entity.getLocation());

	if (res == null)
	    return;

	if (!res.getPermissions().playerHas(player.getName(), "leash", true)) {
	    player.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "Leash", res.getName()));
	    event.setCancelled(true);
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
	// disabling event on world
	Entity ent = event.getEntity();
	if (ent == null)
	    return;
	if (Residence.isDisabledWorldListener(ent.getWorld()))
	    return;
	FlagPermissions perms = Residence.getPermsByLoc(event.getLocation());

	if (Residence.getNms().isAnimal(ent)) {
	    if (!perms.has("animals", true)) {
		event.setCancelled(true);
		return;
	    } else
		switch (event.getSpawnReason()) {
		case BUILD_WITHER:
		    break;
		case BUILD_IRONGOLEM:
		case BUILD_SNOWMAN:
		case CUSTOM:
		case DEFAULT:
		    if (!perms.has("canimals", true)) {
			event.setCancelled(true);
			return;
		    }
		    break;
		case BREEDING:
		case CHUNK_GEN:
		case CURED:
		case DISPENSE_EGG:
		case EGG:
		case JOCKEY:
		case MOUNT:
		case VILLAGE_INVASION:
		case VILLAGE_DEFENSE:
		case NETHER_PORTAL:
		case OCELOT_BABY:
		case NATURAL:
		    if (!perms.has("nanimals", true)) {
			event.setCancelled(true);
			return;
		    }
		    break;
		case SPAWNER_EGG:
		case SPAWNER:
		    if (!perms.has("sanimals", true)) {
			event.setCancelled(true);
			return;
		    }
		    break;
		default:
		    break;
		}
	} else if (isMonster(ent))
	    if (!perms.has("monsters", true)) {
		event.setCancelled(true);
		return;
	    } else
		switch (event.getSpawnReason()) {
		case BUILD_WITHER:
		case CUSTOM:
		case DEFAULT:
		    if (!perms.has("cmonsters", true)) {
			event.setCancelled(true);
			return;
		    }
		    break;
		case CHUNK_GEN:
		case CURED:
		case DISPENSE_EGG:
		case INFECTION:
		case JOCKEY:
		case MOUNT:
		case NETHER_PORTAL:
		case SILVERFISH_BLOCK:
		case SLIME_SPLIT:
		case LIGHTNING:
		case NATURAL:
		    if (!perms.has("nmonsters", true)) {
			event.setCancelled(true);
			return;
		    }
		    break;
		case SPAWNER_EGG:
		case SPAWNER:
		    if (!perms.has("smonsters", true)) {
			event.setCancelled(true);
			return;
		    }
		    break;
		default:
		    break;
		}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {

	// disabling event on world
	Player player = event.getPlayer();
	if (player == null)
	    return;
	if (Residence.isDisabledWorldListener(player.getWorld()))
	    return;
	if (Residence.isResAdminOn(player))
	    return;

	FlagPermissions perms = Residence.getPermsByLocForPlayer(event.getEntity().getLocation(), player);
	String pname = player.getName();
	String world = player.getWorld().getName();
	if (!perms.playerHas(pname, world, "place", perms.playerHas(pname, world, "build", true))) {
	    event.setCancelled(true);
	    player.sendMessage(Residence.getLM().getMessage("Flag.Deny", "place"));
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
	// disabling event on world
	Hanging ent = event.getEntity();
	if (ent == null)
	    return;
	if (Residence.isDisabledWorldListener(ent.getWorld()))
	    return;

	if (!(event.getRemover() instanceof Player))
	    return;

	Player player = (Player) event.getRemover();
	if (Residence.isResAdminOn(player))
	    return;

	if (Residence.getResidenceManager().isOwnerOfLocation(player, ent.getLocation()))
	    return;

	String pname = player.getName();
	FlagPermissions perms = Residence.getPermsByLocForPlayer(ent.getLocation(), player);
	String world = ent.getWorld().getName();
	if (!perms.playerHas(pname, world, "destroy", perms.playerHas(pname, world, "build", true))) {
	    event.setCancelled(true);
	    player.sendMessage(Residence.getLM().getMessage("Flag.Deny", "destroy"));
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {

	// disabling event on world
	Hanging ent = event.getEntity();
	if (ent == null)
	    return;
	if (Residence.isDisabledWorldListener(ent.getWorld()))
	    return;

	if (event.getRemover() instanceof Player)
	    return;

	FlagPermissions perms = Residence.getPermsByLoc(ent.getLocation());
	if (!perms.has("destroy", perms.has("build", true))) {
	    event.setCancelled(true);
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityCombust(EntityCombustEvent event) {
	// disabling event on world
	Entity ent = event.getEntity();
	if (ent == null)
	    return;
	if (Residence.isDisabledWorldListener(ent.getWorld()))
	    return;
	FlagPermissions perms = Residence.getPermsByLoc(ent.getLocation());
	if (!perms.has("burn", true)) {
	    event.setCancelled(true);
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
	// disabling event on world
	Entity ent = event.getEntity();
	if (ent == null)
	    return;
	if (Residence.isDisabledWorldListener(ent.getWorld()))
	    return;
	EntityType entity = event.getEntityType();
	FlagPermissions perms = Residence.getPermsByLoc(ent.getLocation());

	switch (entity) {
	case CREEPER:
	    if (!perms.has("creeper", perms.has("explode", true))) {
		if (Residence.getConfigManager().isCreeperExplodeBelow()) {
		    if (ent.getLocation().getBlockY() >= Residence.getConfigManager().getCreeperExplodeBelowLevel()) {
			event.setCancelled(true);
			ent.remove();
		    } else {
			ClaimedResidence res = Residence.getResidenceManager().getByLoc(ent.getLocation());
			if (res != null) {
			    event.setCancelled(true);
			    ent.remove();
			}
		    }
		} else {
		    event.setCancelled(true);
		    ent.remove();
		}
	    }
	    break;
	case PRIMED_TNT:
	case MINECART_TNT:
	    if (!perms.has("tnt", perms.has("explode", true))) {
		if (Residence.getConfigManager().isTNTExplodeBelow()) {
		    if (ent.getLocation().getBlockY() >= Residence.getConfigManager().getTNTExplodeBelowLevel()) {
			event.setCancelled(true);
			ent.remove();
		    } else {
			ClaimedResidence res = Residence.getResidenceManager().getByLoc(ent.getLocation());
			if (res != null) {
			    event.setCancelled(true);
			    ent.remove();
			}
		    }
		} else {
		    event.setCancelled(true);
		    ent.remove();
		}
	    }
	    break;
	case SMALL_FIREBALL:
	case FIREBALL:
	    if (!perms.has("fireball", perms.has("explode", true))) {
		event.setCancelled(true);
		ent.remove();
	    }
	    break;
	default:
	    if (!perms.has("destroy", true)) {
		event.setCancelled(true);
		ent.remove();
	    }
	    break;
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
	// disabling event on world
	Entity ent = event.getEntity();
	if (ent == null)
	    return;
	if (Residence.isDisabledWorldListener(ent.getWorld()))
	    return;
	if (event.isCancelled())
	    return;
	Boolean cancel = false;
	EntityType entity = event.getEntityType();
	FlagPermissions perms = Residence.getPermsByLoc(ent.getLocation());
	FlagPermissions world = Residence.getWorldFlags().getPerms(ent.getWorld().getName());

	switch (entity) {
	case CREEPER:
	    if (!perms.has("creeper", perms.has("explode", true)))
		if (Residence.getConfigManager().isCreeperExplodeBelow()) {
		    if (ent.getLocation().getBlockY() >= Residence.getConfigManager().getCreeperExplodeBelowLevel())
			cancel = true;
		    else {
			ClaimedResidence res = Residence.getResidenceManager().getByLoc(ent.getLocation());
			if (res != null)
			    cancel = true;
		    }
		} else
		    cancel = true;
	    break;
	case PRIMED_TNT:
	case MINECART_TNT:
	    if (!perms.has("tnt", perms.has("explode", true))) {
		if (Residence.getConfigManager().isTNTExplodeBelow()) {
		    if (ent.getLocation().getBlockY() >= Residence.getConfigManager().getTNTExplodeBelowLevel())
			cancel = true;
		    else {
			ClaimedResidence res = Residence.getResidenceManager().getByLoc(ent.getLocation());
			if (res != null)
			    cancel = true;
		    }
		} else
		    cancel = true;
	    }
	    break;
	case SMALL_FIREBALL:
	case FIREBALL:
	    if (!perms.has("fireball", perms.has("explode", true)))
		cancel = true;
	    break;
	default:
	    if (!perms.has("destroy", world.has("destroy", true)))
		cancel = true;
	    break;
	}

	if (cancel) {
	    event.setCancelled(true);
	    ent.remove();
	    return;
	}

	List<Block> preserve = new ArrayList<Block>();
	for (Block block : event.blockList()) {
	    FlagPermissions blockperms = Residence.getPermsByLoc(block.getLocation());

	    switch (entity) {
	    case CREEPER:
		if (!blockperms.has("creeper", blockperms.has("explode", true)))
		    if (Residence.getConfigManager().isCreeperExplodeBelow()) {
			if (block.getY() >= Residence.getConfigManager().getCreeperExplodeBelowLevel())
			    preserve.add(block);
			else {
			    ClaimedResidence res = Residence.getResidenceManager().getByLoc(block.getLocation());
			    if (res != null)
				preserve.add(block);
			}
		    } else
			preserve.add(block);
		continue;
	    case PRIMED_TNT:
	    case MINECART_TNT:
		if (!blockperms.has("tnt", blockperms.has("explode", true))) {
		    if (Residence.getConfigManager().isTNTExplodeBelow()) {
			if (block.getY() >= Residence.getConfigManager().getTNTExplodeBelowLevel())
			    preserve.add(block);
			else {
			    ClaimedResidence res = Residence.getResidenceManager().getByLoc(block.getLocation());
			    if (res != null)
				preserve.add(block);
			}
		    } else
			preserve.add(block);
		}
		continue;
	    case ENDER_DRAGON:
		if (!blockperms.has("dragongrief", false))
		    preserve.add(block);
		break;
	    case ENDER_CRYSTAL:
		if (!blockperms.has("explode", false))
		    preserve.add(block);
		continue;
	    case SMALL_FIREBALL:
	    case FIREBALL:
		if (!blockperms.has("fireball", blockperms.has("explode", true)))
		    preserve.add(block);
		continue;
	    default:
		if (!blockperms.has("destroy", world.has("destroy", true)))
		    preserve.add(block);
		continue;
	    }
	}

	for (Block block : preserve) {
	    event.blockList().remove(block);
	}

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSplashPotion(PotionSplashEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getEntity().getWorld()))
	    return;
	if (event.isCancelled())
	    return;

	boolean harmfull = false;
	mein: for (PotionEffect one : event.getPotion().getEffects()) {
	    for (String oneHarm : Residence.getConfigManager().getNegativePotionEffects()) {
		if (oneHarm.equalsIgnoreCase(one.getType().getName())) {
		    harmfull = true;
		    break mein;
		}
	    }
	}
	if (!harmfull)
	    return;

	Entity ent = event.getEntity();
	boolean srcpvp = Residence.getPermsByLoc(ent.getLocation()).has("pvp", true);
	Iterator<LivingEntity> it = event.getAffectedEntities().iterator();
	while (it.hasNext()) {
	    LivingEntity target = it.next();
	    if (target.getType() != EntityType.PLAYER)
		continue;
	    Boolean tgtpvp = Residence.getPermsByLoc(target.getLocation()).has("pvp", true);
	    if (!srcpvp || !tgtpvp)
		event.setIntensity(target, 0);
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {

	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getEntity().getWorld()))
	    return;
	if (event.getEntityType() != EntityType.ITEM_FRAME && !Residence.getNms().isArmorStandEntity(event.getEntityType()))
	    return;

	Entity dmgr = event.getDamager();

	Player player = null;
	if (dmgr instanceof Player) {
	    player = (Player) event.getDamager();
	} else if (dmgr instanceof Projectile && ((Projectile) dmgr).getShooter() instanceof Player) {
	    player = (Player) ((Projectile) dmgr).getShooter();
	} else if ((dmgr instanceof Projectile) && (!(((Projectile) dmgr).getShooter() instanceof Player))) {

	    Location loc = event.getEntity().getLocation();
	    FlagPermissions perm = Residence.getPermsByLoc(loc);

	    if (!perm.has("destroy", true)) {
		event.setCancelled(true);
	    }
	    return;
	} else if (dmgr.getType() == EntityType.PRIMED_TNT || dmgr.getType() == EntityType.MINECART_TNT || dmgr.getType() == EntityType.WITHER_SKULL || dmgr
	    .getType() == EntityType.WITHER) {
	    FlagPermissions perms = Residence.getPermsByLoc(event.getEntity().getLocation());
	    boolean destroy = perms.has("explode", false);
	    if (!destroy) {
		event.setCancelled(true);
		return;
	    }
	}

	Location loc = event.getEntity().getLocation();
	ClaimedResidence res = Residence.getResidenceManager().getByLoc(loc);
	if (res == null)
	    return;

	if (isMonster(dmgr) && !res.getPermissions().has("destroy", false)) {
	    event.setCancelled(true);
	    return;
	}

	if (player == null)
	    return;

	if (Residence.isResAdminOn(player))
	    return;

	if (!res.isOwner(player) && !res.getPermissions().playerHas(player.getName(), "destroy", false)) {
	    event.setCancelled(true);
	    player.sendMessage(Residence.getLM().getMessage("Residence.FlagDeny", "destroy", res.getName()));
	}
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
	// disabling event on world
	if (Residence.isDisabledWorldListener(event.getEntity().getWorld()))
	    return;
	Entity ent = event.getEntity();
	if (ent.hasMetadata("NPC"))
	    return;

	boolean tamedAnimal = isTamed(ent);
	ClaimedResidence area = Residence.getResidenceManager().getByLoc(ent.getLocation());

	/* Living Entities */
	if (event instanceof EntityDamageByEntityEvent) {
	    EntityDamageByEntityEvent attackevent = (EntityDamageByEntityEvent) event;
	    Entity damager = attackevent.getDamager();

	    if (area != null && ent instanceof Player && damager instanceof Player) {
		if (area.getPermissions().has("overridepvp", false) || Residence.getConfigManager().isOverridePvp() && area.getPermissions().has("pvp", false)) {
		    Player player = (Player) event.getEntity();
		    Damageable damage = player;
		    damage.damage(event.getDamage());
		    damage.setVelocity(damager.getLocation().getDirection());
		    event.setCancelled(true);
		    return;
		}
	    }

	    ClaimedResidence srcarea = null;
	    if (damager != null) {
		srcarea = Residence.getResidenceManager().getByLoc(damager.getLocation());
	    }
	    boolean srcpvp = true;
	    boolean allowSnowBall = false;
	    boolean isSnowBall = false;
	    if (srcarea != null) {
		srcpvp = srcarea.getPermissions().has("pvp", true);
	    }
	    ent = attackevent.getEntity();
	    if ((ent instanceof Player || tamedAnimal) && (damager instanceof Player || (damager instanceof Projectile && (((Projectile) damager)
		.getShooter() instanceof Player))) && event.getCause() != DamageCause.FALL) {
		Player attacker = null;
		if (damager instanceof Player) {
		    attacker = (Player) damager;
		} else if (damager instanceof Projectile) {
		    Projectile project = (Projectile) damager;
		    if (project.getType() == EntityType.SNOWBALL && srcarea != null) {
			isSnowBall = true;
			allowSnowBall = srcarea.getPermissions().has("snowball", false);
		    }
		    attacker = (Player) ((Projectile) damager).getShooter();
		}
		if (!srcpvp && !isSnowBall || !allowSnowBall && isSnowBall) {
		    attacker.sendMessage(Residence.getLM().getMessage("General.NoPVPZone"));
		    event.setCancelled(true);
		    return;
		}
		/* Check for Player vs Player */
		if (area == null) {
		    /* World PvP */
		    if (!Residence.getWorldFlags().getPerms(damager.getWorld().getName()).has("pvp", true)) {
			attacker.sendMessage(Residence.getLM().getMessage("General.WorldPVPDisabled"));
			event.setCancelled(true);
		    }
		} else {
		    /* Normal PvP */
		    if (!isSnowBall && !area.getPermissions().has("pvp", true) || isSnowBall && !allowSnowBall) {
			attacker.sendMessage(Residence.getLM().getMessage("General.NoPVPZone"));
			event.setCancelled(true);
		    }
		}
		return;
	    } else if ((ent instanceof Player || tamedAnimal) && (damager instanceof Creeper)) {
		if (area == null && !Residence.getWorldFlags().getPerms(damager.getWorld().getName()).has("creeper", true)) {
		    event.setCancelled(true);
		} else if (area != null && !area.getPermissions().has("creeper", true)) {
		    event.setCancelled(true);
		}
	    }
	}
	if (area == null) {
	    if (!Residence.getWorldFlags().getPerms(ent.getWorld().getName()).has("damage", true) && (ent instanceof Player || tamedAnimal)) {
		event.setCancelled(true);
	    }
	} else {
	    if (!area.getPermissions().has("damage", true) && (ent instanceof Player || tamedAnimal)) {
		event.setCancelled(true);
	    }
	}
	if (event.isCancelled()) {
	    /* Put out a fire on a player */
	    if ((ent instanceof Player || tamedAnimal) && (event.getCause() == EntityDamageEvent.DamageCause.FIRE || event
		.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK)) {
		ent.setFireTicks(0);
	    }
	}
    }
}

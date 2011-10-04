package joren.spawn;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Person extends Ent {

	private Player[] people;
	
	/*
	 * Describes the kind of mob you want to spawn.
	 * @param type: the type of mob
	 * @param passenger: What mob will be riding this one; if none, null
	 * @param size: Size for slimes; ignored if not a slime
	 * @param health: Health for mobs
	 * @param healthPercentage: true if health should be taken as a percentage of normal health
	 * @param fireTicks: How many ticks the mob will burn (needs to be really, really, high)
	 * @param velocity: How fast you want the mob to be going when it spawns
	 * @param angry: is the mob angry or not?
	 * @param setAngry: Should we manually set the angry parameter, or leave it at the mob type's default?
	 */

	public Person(Player[] people, String alias, boolean angry, boolean bounce, boolean color, DyeColor colorCode, int fireTicks, boolean health, boolean healthIsPercentage, int healthValue, boolean mount, boolean naked, boolean owned, Player[] owner, Ent passenger, boolean size, int sizeValue, boolean target, Player[] targets, int velocity) {
		super (null, alias, angry, bounce, color, colorCode, fireTicks, health, healthIsPercentage, healthValue, mount, naked, owned, owner, passenger, size, sizeValue, target, targets, velocity);
		this.people = people;
	}

	public void setPassenger(Ent passenger)
	{
		this.passenger=passenger;
	}
	
	/*
	 * Spawns many mobs.
	 * @param player: The player who wants to spawn the mobs
	 * @param plugin: Um... seems to only be needed for printing errors.
	 * @param location: A Location describing where the mobs will spawn
	 * @param count: How many mobs you want
	 */

	public boolean spawn(Player player, Spawn plugin, Location location, int count)
	{
		for (int i=0; i<count; i++)
			if (spawnSingle(player, plugin, location)==null)
			{
				plugin.warning("Exception has been thrown; halting entity spawning");
				return false;
			}
		return true;
	}

	/*
	 * Spawns a single mob, and returns it.  Useful if you want to manipulate the mob after being spawned.
	 * @param player: The player who wants to spawn the mobs
	 * @param plugin: Um... seems to only be needed for printing errors.
	 * @param location: A Location describing where the mobs will spawn
	 */

	public Entity spawnSingle(Player p, Spawn plugin, Location loc) throws IllegalArgumentException
	{
		Player person = pickPlayer(people);
		person.leaveVehicle();
		person.teleport(loc);
		if (passenger!=null)
		{
			Entity rider=passenger.spawnSingle(p, plugin, loc);
			person.setPassenger(rider);
		}
		if (health)//Do not use setExperience, that is only for orbs.
		{
			if ((healthIsPercentage))
			person.setHealth((int)((double)healthValue));
			else
				person.setHealth(healthValue);
		}
		if (fireTicks!=-1)
			person.setFireTicks(fireTicks);
		if (velocity!=0)
		{
			Vector vel = Vector.getRandom();
			vel.setX((vel.getX()*2-1)*velocity);
			vel.setY(vel.getY()*velocity);
			vel.setZ((vel.getZ()*2-1)*velocity);
			person.setVelocity(vel);
		}
		if (naked)
		{
			person.getInventory().setArmorContents(null);
		}
		return person;
	}

	public String description()
	{
		return super.description();
	}

}

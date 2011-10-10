package joren.spawn;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;


/**
 * Person class intended to facilitate the use of Players within a spawning sequences indicated by an Ent
 * @author Joren Combs
 * @version 0.1
 * @see Ent
 */
public class Person extends Ent {

	/** A list of possible Players that can be chosen from according to the alias given */
	private Player[] people;
	
	/**
	 * A Player is basically an Ent meant to allow Players to be a part of your spawning sequence.
	 * Although most of these options are not supported by the Player class, that may change some day;
	 * they are left here in case that ever happens.
	 * 
	 * @param people: The players referred to by the alias
	 * @param alias: The name given to this collection of players
	 * @param angry: If true, entity will be made angry
	 * @param bounce: If true, the (projectile) entity will be made to bounce.  So far, no observable effect
	 * @param color: If true, will set the entity's color to specified colorCode 
	 * @param colorCode: Used to decide the entity's color if entity supports more than one color
	 * @param fireTicks: How many ticks the entity will burn (20 ticks/second)
	 * @param health: If true, will set the entity's health using healthValue
	 * @param healthPercentage: True if healthValue should be taken as a percentage of normal health
	 * @param healthValue: Used to set the entity's health if the entity supports it
	 * @param mount: If true, will make the entity have a mount (e.g. saddle for pig) if it is supported
	 * @param owned: If true, the entity will belong to Owner
	 * @param owner: Used to set the entity's owner if entity supports ownership
	 * @param naked: If true, will strip the entity of clothes/wool
	 * @param passenger: What entity will be riding this one; if none, null
	 * @param size: If true, will set the entity's size to sizeValue
	 * @param sizeValue: If the entity supports it, can be used to set the entity's size
	 * @param target: If true, entity will have a target.  Does not seem to work for ghasts
	 * @param targets: A list of potential targets for the entity to choose from
	 * @param velocity: How fast you want the entity to be going when it spawns
	 */
	public Person(Player[] people, String alias, boolean angry, boolean bounce, boolean color, DyeColor colorCode, int fireTicks, boolean health, boolean healthIsPercentage, int healthValue, boolean mount, boolean naked, boolean owned, Player[] owner, Ent passenger, boolean size, int sizeValue, boolean target, Player[] targets, int velocity) {
		super (null, alias, angry, bounce, color, colorCode, fireTicks, health, healthIsPercentage, healthValue, mount, naked, owned, owner, passenger, size, sizeValue, target, targets, velocity);
		this.people = people;
	}

	public void setPassenger(Ent passenger)
	{
		this.passenger=passenger;
	}
	
	/**
	 * {@inheritDoc}
	 * In the Person class, note that the player will end up within the last sequence spawned that uses this player
	 * @param player: The player who wants to spawn the entities
	 * @param plugin: Needed only for logging
	 * @param location: Where the entities will be spawned (uses all location values, I believe)
	 * @param count: How many entities you want
	 * @return boolean: true if successful, false otherwise.
	 * @see spawn(player, plugin, location)
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

	/**
	 * {@inheritDoc}
	 * In the Person class, this means that the player will be spawned at that location instead of an Entity type.
	 * @param player: The player who wants to spawn the entities
	 * @param plugin: Needed only for logging
	 * @param location: Where the entities will be spawned (uses all location values, I believe)
	 * @return Entity: The entity that was spawned
	 * @see spawn(player, plugin, location, count)
	 * @throws IllegalArgumentException - this usually is thrown when attempting to spawn a superclass not meant to be instantiated; e.g. Animal, LivingEntity, etc.
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
				person.setHealth((int)(((double)healthValue)/5));
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
}

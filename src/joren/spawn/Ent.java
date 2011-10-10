package joren.spawn;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;


/**
 * Ent class intended to serve as a container for spawning Ents
 * 
 * @author Joren Combs
 * @version 0.1
 *
 */
public class Ent {

	protected Class<Entity>[] types;
	String alias;
	protected Ent passenger;
	protected int sizeValue=1, healthValue=100, fireTicks=-1, velocity=0;
	boolean angry=false, bounce=false, color=false, health=false, healthIsPercentage=true, mount=false, naked=false, owned=false, size=false, target=false;
	DyeColor colorCode=DyeColor.WHITE;
	Player[] owner=null, targets=null;
	
	/**
	 * An Ent describes the kind of entity you want to spawn, and gives you functions to spawn it.
	 * @param types: The types of entity referred to by the alias
	 * @param alias: The name given to this collection of entity types
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
	public Ent(Class<Entity>[] types, String alias, boolean angry, boolean bounce, boolean color, DyeColor colorCode, int fireTicks, boolean health, boolean healthIsPercentage, int healthValue, boolean mount, boolean naked, boolean owned, Player[] owner, Ent passenger, boolean size, int sizeValue, boolean target, Player[] targets, int velocity) {
		this.types = types;
		this.alias = alias;
		this.angry=angry;
		this.bounce=bounce;
		this.color=color;
		this.colorCode=colorCode;
		this.fireTicks=fireTicks;
		this.health=health;
		this.healthValue=healthValue;
		this.healthIsPercentage = healthIsPercentage;
		this.mount=mount;
		this.naked=naked;
		this.owned=owned;
		this.owner=owner;
		this.passenger=passenger;
		this.size=size;
		this.sizeValue=sizeValue;
		this.target=target;
		this.targets=targets;
		this.velocity=velocity;
	}

	/**
	 * Utility function; randomly chooses a Player from a list.
	 * @param people: List of potential Players to choose from
	 * @return Player: Randomly chosen from the list
	 */
	protected Player pickPlayer(Player[] people)
	{
		if (people != null)
			if (people.length!=0)
			{
				return people[(int)(Math.random()*people.length)];
			}
		return null;
	}
		
	/**
	 * Utility function; randomly chooses an Entity class from a list.  Used to give a random
	 * quality to the type of entities that may be spawned
	 * @param people: List of potential Entity classes to choose from
	 * @return Entity class randomly chosen from the list
	 */
	public Class<Entity> pick()
	{
		if (types != null)
			if (types.length!=0)
			{
				return types[(int)(Math.random()*types.length)];
			}
		return null;
	}
		
	/**
	 * Utility function; sets this Ent's passenger to another Ent.  Used in the spawn functions
	 * @param people: List of potential Entity classes to choose from
	 * @return Entity class randomly chosen from the list
	 */
	protected void setPassenger(Ent passenger)
	{
		this.passenger=passenger;
	}
	
	/**
	 * Spawns many mobs.
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
	 * Spawns a single entity, and returns it.  Useful if you want to manipulate the entity after being spawned.
	 * @param player: The player who wants to spawn the entities
	 * @param plugin: Needed only for logging
	 * @param location: Where the entities will be spawned (uses all location values, I believe)
	 * @return Entity: The entity that was spawned
	 * @see spawn(player, plugin, location, count)
	 * @throws IllegalArgumentException - this usually is thrown when attempting to spawn a superclass not meant to be instantiated; e.g. Animal, LivingEntity, etc.
	 */
	public Entity spawnSingle(Player p, Spawn plugin, Location loc) throws IllegalArgumentException
	{
		Class<Entity> type = pick();
		try 
		{
			Entity ent;
			if (LivingEntity.class.isAssignableFrom(type))
				ent = p.getWorld().spawnCreature(loc, CreatureType.fromName(type.getSimpleName()));
			else
				ent = p.getWorld().spawn(loc, type);
			if (ent == null)
			{
				plugin.warning("Some things just weren't meant to be spawned - null entity detected.");
				plugin.flag(type);
				return null;
			}
			
			//ANGRY
			
			if (angry)
			{
				Method angryMethod = null;
				try
				{
					angryMethod = type.getMethod("setAngry", boolean.class);
					angryMethod.invoke(ent, true);
				} catch (NoSuchMethodException e)
				{
					try
					{
						angryMethod = type.getMethod("setPowered", boolean.class);
						angryMethod.invoke(ent, true);
					} catch (NoSuchMethodException f){};//yeah, we have to rely on Exceptions to find out if it has a method or not, how sad is that?
				}
				
			}
			
			//BOUNCE
			
			if (bounce)
			{
				Method bounceMethod;
				try
				{
					bounceMethod = type.getMethod("setBounce", boolean.class);
					bounceMethod.invoke(ent, true);		
				} catch (NoSuchMethodException e){}
			}
			
			//COLOR
			
			if (color)
			{
				Method colorMethod;
				try
				{
					colorMethod = type.getMethod("setColor", DyeColor.class);
					colorMethod.invoke(ent, colorCode);
				} catch (NoSuchMethodException e){}
				catch (IllegalArgumentException e){}
			}
			
			//FIRE
			
			if (fireTicks!=-1)
			{
				ent.setFireTicks(fireTicks);
				Method setFuseTicks;
				try
				{
					setFuseTicks = type.getMethod("setFuseTicks", int.class);
					setFuseTicks.invoke(ent, fireTicks);
				} catch (NoSuchMethodException e){}
			}
			
			//HEALTH
			
			if (health)
			{
				try
				{
					Method healthMethod = type.getMethod("setHealth", int.class);
					if ((healthIsPercentage))
					{
						healthMethod.invoke(ent, (int)((((double)healthValue)/100)* ((double)((LivingEntity)ent).getHealth())));
						if (ent instanceof ExperienceOrb)
							((ExperienceOrb)ent).setExperience((int)((((double)healthValue)/100)* ((double)((ExperienceOrb)ent).getExperience())));
					}
					else
					{
						healthMethod.invoke(ent, healthValue);
						if (ent instanceof ExperienceOrb)
							((ExperienceOrb)ent).setExperience(healthValue);
					}
				} catch (NoSuchMethodException e){}
			}

			//MOUNT
			
			if (mount)
			{
				Method mountMethod;
				try
				{
					mountMethod = type.getMethod("setSaddle", boolean.class);
					mountMethod.invoke(ent, true);
				} catch (NoSuchMethodException e){}
			}

			//NAKED
			
			if (naked)
			{
				Method shearMethod;
				try
				{
					shearMethod = type.getMethod("setSheared", boolean.class);
					shearMethod.invoke(ent, true);
				} catch (NoSuchMethodException e){}
			}
			
			//OWNER
			
			if (owned)
			{
				Method tameMethod, ownerMethod;
				try
				{
					tameMethod = type.getMethod("setTamed", boolean.class);
					ownerMethod = type.getMethod("setOwner", AnimalTamer.class);
					if (owner != null)
						ownerMethod.invoke(ent, pickPlayer(owner));
					else
						tameMethod.invoke(ent, owned);
				} catch (NoSuchMethodException e){}
			}

			//PASSENGER
			
			if (passenger!=null)
			{					
				Entity rider=passenger.spawnSingle(p, plugin, loc);
				if (rider instanceof Minecart)
					plugin.warning("Please do not try having a minecart ride a mob, unless you want it to ride the server too.");
				else
					ent.setPassenger(rider);
			}
			
			//SIZE
			
			if (size)
			{
				Method sizeMethod = null;
				try
				{
					sizeMethod = type.getMethod("setSize", int.class);
				} catch (NoSuchMethodException e){};
				sizeMethod.invoke(ent, sizeValue);
			}

			//TARGET
			
			if (target)
			{
				Method targetMethod;
				try
				{
					targetMethod = type.getMethod("setTarget", LivingEntity.class);
					targetMethod.invoke(ent, pickPlayer(targets));
				} catch (NoSuchMethodException e){}
			}
			
			//VELOCITY
			
			if (velocity!=0)
			{
				Vector vel = Vector.getRandom();
				vel.setX((vel.getX()*2-1)*velocity);
				vel.setY(vel.getY()*velocity);
				vel.setZ((vel.getZ()*2-1)*velocity);
				ent.setVelocity(vel);
			}
			
			return ent;
		} catch(InvocationTargetException e)
		{
			plugin.warning("Target " + type.getSimpleName() + " has a method for doing something, but threw an exception when it was invoked:");
			e.printStackTrace();
		} catch(IllegalAccessException e)
		{
			plugin.warning("Target " + type.getSimpleName() + " has a method for doing something, but threw an exception when it was invoked:");
			e.printStackTrace();
		} catch(IllegalArgumentException e)
		{
			plugin.warning("Some things just weren't meant to be spawned:");
			e.printStackTrace();
			plugin.flag(type);
		}
		return null;
	}

	/**
	 * Used to get the description of this Ent.  Recursively gets the description of any Ents riding this one
	 * as well.  Intended to provide somewhat user-friendly output for what just got spawned
	 * @return String: description that can be used in output messages
	 */
	public String description()
	{
		if (passenger != null)
		{
			return passenger.description() + " riding a " + alias;
		}
		return alias;
	}

}

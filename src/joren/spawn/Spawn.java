package joren.spawn;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

/**
 * Spawn
 * @version 0.1
 * @author jorencombs
 * 
 * Originally intended to be a stripped-down version of the SpawnMob Bukkit plugin (most recently adapted
 * by jordanneil23).  However, it has been nuked and rewritten enough that there probably isn't much of
 * the original code left.  Also uses TargetBlock code from toi and Raphfrk
 */
public class Spawn extends JavaPlugin {
	public java.util.logging.Logger log = java.util.logging.Logger.getLogger("Minecraft");
	/** Handle to access the Permissions plugin */
	public static PermissionHandler Permissions;
	/** Name of the plugin, used in output messages */
	protected static String name = "Spawn";
	/** Path where the plugin's saved information is located */
	protected static String path = "plugins" + File.separator + name;
	/** Location of the config YML file */
	protected static String config = path + File.separator + name + ".yml";
	/** Header used for console and player output messages */
	protected static String header = "[Spawn] ";
	/** Represents the plugin's YML configuration */
	protected static Configuration cfg;
	/** True if this plugin is to be used with Permissions, false if not */
	protected boolean permissions = false;
	/** Limitations on how many entities can be spawned and what the maximum size of a spawned entity should be */
	protected int spawnLimit, sizeLimit;

	/**
	 * Initializes plugin description variables (in case they are different from when written)
	 * and initiates a reload of the plugin
	 */
	public void onEnable()
	{
		PluginDescriptionFile pdfFile = this.getDescription();
		name = pdfFile.getName();
		header = "[" + name + "] ";
		path = "plugins" + File.separator + name;
		config = path + File.separator + name + ".yml";
		reload();
		info("Version " + pdfFile.getVersion() + " enabled.");
	}
	
	/**
	 * Saves plugin configuration to disk so that the plugin can be safely disabled.
	 */
	public void onDisable() {
		save();
		log.info("Disabled.");
	}

	/**
	 * Reloads the plugin by re-reading the configuration file and setting associated variables
	 * 
	 * The configuration will be replaced with whatever information is in the file.  Any variables that need to be read from the configuration will be initialized.
	 * 
	 * @return boolean: True if reload was successful.  Currently all reloads are considered successful
	 * since there are fallbacks for cases where the configuration isn't there.
	 */
	public boolean reload()
	{
		info("(re)loading...");
		File file = new File(config);
		cfg = new Configuration(file);
		if(!file.exists())
		{
			if (!saveDefault())
			{
				warning("Running on default values anyway...");
				sizeLimit = 100;
				spawnLimit = 300;
				permissions = true;
			}
		}
		else
		{
			cfg.load();
			sizeLimit = cfg.getInt("Spawn.size-limit", 100);
			spawnLimit = cfg.getInt("Spawn.spawn-limit", 300);
			permissions = cfg.getBoolean("Spawn.use-permissions", true);
			if (permissions)
				setupPermissions();
		}
		info("done.");
		return true;
	}

	/**
	 * Saves a new default configuration file, overwriting old configuration and file in the process
	 * Any existing configuration will be replaced with the default configuration and saved to disk.  Any variables that need to be read from the configuration will be initialized
	 * @return boolean: True if successful, false otherwise
	 */
	public boolean saveDefault()
	{
		info("Resetting configuration file with default values...");
		cfg = new Configuration(new File(config));
		cfg.setProperty("Spawn.use-permissions", true);
		cfg.setProperty("Spawn.spawn-limit", 300);
		cfg.setProperty("Spawn.size-limit", 100);
		if (save())
		{
			reload();
			return true;
		}
		else
			return false;
	}
	
	/**
	 * Saves the configuration file, overwriting old file in the process
	 * 
	 * @return boolean: True if successful, false otherwise.
	 */
	public boolean save()
	{
		info("Saving configuration file...");
		File dir = new File(path);
		if(!dir.exists())
		{
			if (!dir.mkdir())
			{
				severe("Could not create directory " + path + "; if there is a file with this name, please rename it to something else.  Please make sure the server has rights to make this directory.");
				return false;
			}
			info("Created directory " + path + "; this is where your configuration file will be kept.");
		}
		cfg.save();
		File file = new File(config);
		if (!file.exists())
		{
			severe("Configuration could not be saved! Please make sure the server has rights to output to " + config);
			return false;
		}
		info("Saved configuration file: " + config);
		return true;
	}
	
	/**
	 * Sets a flag intended to prevent this entity from ever being spawned by this plugin
	 * 
	 * This is intended for situations where the entity threw an exception indicating that the
	 * game really, really, really was not happy about being told to spawn that entity.  Flagging
	 * this entity is supposed to stop any player (even the admin) from spawning this entity
	 * regardless of permissions, aliases, etc.
	 * 
	 * @param ent - The entity class.  No instance of this class will be spawned using this plugin
	 */
	
	public void flag(Class<Entity> ent)
	{
		if (ent != null)
			cfg.setProperty("Avoid." + ent.getSimpleName(), true);
	}
	
	/**
	 * Utility function; returns a list of players associated with the supplied alias.
	 * 
	 * This can be restricted by permissions in two ways:  permission can be denied for a specific
	 * alias, or permission can be denied for spawning players in general.  User should be aware that
	 * if permission is denied on alias Derp, but there is a player named DerpIsDerp, a list
	 * containing a single player named DerpIsDerp will be returned.
	 * 
	 * @param alias: An alias/name associated with one or more players
	 * @param sender: The person who asked for the alias
	 * @param permsPrefix: The intended purpose of this list, used as a permissions prefix
	 * @return Player[]: A list of players.  If no players were found, returns a size 0 array (not null)
	 */
	
	protected Player[] lookupPlayers(String alias, CommandSender sender, String permsPrefix)
	{
		ArrayList<Player> list = new ArrayList<Player>();
		Player[] derp = new Player[0];//Needed for workaround below
		if (alias == null)
			return list.toArray(derp);
		ConfigurationNode node = cfg.getNode("PlayerAlias." + alias.toLowerCase());
		if (node!=null && allowedTo(sender, permsPrefix + "." + alias))
		{
			for (Iterator<String> i = node.getKeys().iterator(); i.hasNext();)
			{
				String name = i.next();
				Player target = getServer().getPlayerExact(name);
				if (target != null)
					list.add(target);
			}
		}
		else if (allowedTo(sender, permsPrefix + ".player"))
		{
			Player target = getServer().getPlayer(alias);
			if (target == null)
				target = getServer().getPlayerExact(alias);
			if (target != null)
				list.add(target);
		}
		return list.toArray(derp); // what a ridiculous workaround
	}

	/**
	 * Utility function; returns a list of entity classes associated with the supplied alias.
	 * 
	 * This can be restricted by permissions on each entity type (NOT by alias type).  If player
	 * has permissions for only some of the entities indicated by an alias, a partial list will
	 * be returned.
	 * 
	 * @param alias: An alias/name associated with one or more entity types
	 * @param sender: The person who asked for the alias
	 * @param permsPrefix: The intended purpose of this list, used as a permissions prefix
	 * @return Player[]: A list of players.  If no players were found, returns a size 0 array (not null)
	 */
	public Class<Entity>[] lookup(String alias, CommandSender sender, String permsPrefix)
	{
		ArrayList<Class<Entity>> list = new ArrayList<Class<Entity>>();
		Class<Entity>[] derp = new Class[0];//Needed for workaround below
		if (alias == null)
			return list.toArray(derp);
		if (alias.toLowerCase().startsWith("org.bukkit.entity."))//allow user to specify formal name to avoid conflict (e.g. player named Zombie and not able to use lowercase because of lack of alias, which would be generated after using the formal name once)
		{
			if (alias.length() > 18)
				alias = alias.substring(17);
			else
				return (Class<Entity>[]) list.toArray(derp);//an empty list since they didn't finish specifying the class
		}
		ConfigurationNode node = cfg.getNode("Alias." + alias.toLowerCase());
		if (node!=null)	
			for (Iterator<String> i = node.getKeys().iterator(); i.hasNext();)
			{
				String entName = "org.bukkit.entity." + i.next();
				try
				{
					Class<?> c = (Class<?>) Class.forName(entName);
					if (Entity.class.isAssignableFrom(c))
					{
						if (allowedTo(sender, permsPrefix + "." + c.getSimpleName()) && !cfg.getBoolean("Avoid." + c.getSimpleName(), false))
						{
							list.add((Class<Entity>) c);
						}
					}
				}
				catch (ClassNotFoundException e)
				{
					warning("Config file says that " + alias + " is a " + entName + ", but could not find that class.  Skipping...");
				}
			}
		else
		{
			try
			{
				Class<?> c = (Class<?>) Class.forName("org.bukkit.entity." + alias);
				if (Entity.class.isAssignableFrom(c))
				{
					if (allowedTo(sender, permsPrefix + "." + c.getSimpleName()) && !cfg.getBoolean("Avoid." + c.getSimpleName(), false))
					{
						list.add((Class<Entity>) c);
						cfg.setProperty("Alias." + alias.toLowerCase() + "." + c.getSimpleName(), true);
						info("Class " + c.getName() + " has not been invoked before; adding alias to configuration");
					}
				}
			}
			catch (ClassNotFoundException e)
			{
				;//om nom nom
			}
		}
		return (Class<Entity>[]) list.toArray(derp); // what a ridiculous workaround
	}
	
	/**
	 * A ridiculously complicated function for handling player commands
	 * @param sender: The person sending the command
	 * @param command: The command being called
	 * @param commandLabel: Dunno what the difference is between this and command.getName()
	 * @param args: The list of arguments given for the command
	 * 
	 * @return boolean: True if successful (also indicates player used syntax correctly), false otherwise
	 */
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args)
	{
		int[] ignore = {8, 9};
		if (command.getName().equalsIgnoreCase("spawn") || command.getName().equalsIgnoreCase("sp") || command.getName().equalsIgnoreCase("s"))
		{
			if (allowedTo(sender, "spawn"))
			{
				if ((args.length > 0)&&(args.length < 4)) 
				{
					if (args[0].equalsIgnoreCase("kill") || args[0].toLowerCase().startsWith("kill/"))
					{
						if (!allowedTo(sender, "spawn.kill"))
						{
							printHelp(sender);
							return false;
						}
						String type=args[0]; // save parameters for later in case mob is not specified
						int radius = 0;
						if (args.length > 2) //Should be /sm kill <type> <radius>
						{
							type=args[1];
							try
							{
								radius = Integer.parseInt(args[2]);
							}
							catch (NumberFormatException e)
							{
								printHelp(sender);
								return false;
							}
						}
						else if (args.length > 1) //Should be either /sm kill <type> or /sm kill <radius>
							try
							{
								radius = Integer.parseInt(args[1]);
							}
							catch (NumberFormatException e)
							{
								type=args[1];
							}
							
							String mobParam[] = type.split("/"); //Check type for params
							int healthValue=100, sizeValue=1, velocity=0;
							boolean angry = false, bounce = false, color = false, fire = false, health = false, healthIsPercentage = true, mount = false, size = false, target = false, owned = false, naked = false;
							Player owner[]=null, targets[]=null;
							DyeColor colorCode=DyeColor.WHITE;
							if (mobParam.length>1)
							{
								for (int j=1; j<mobParam.length; j++)
								{
									String paramName = mobParam[j].substring(0, 1);
									String param = null;
									if (mobParam[j].length() > 2)
										param = mobParam[j].substring(2);
									if (paramName.equalsIgnoreCase("a"))
									{
										if(allowedTo(sender, "spawn.kill.angry"))
										{
											angry=true;
										}
										else
										{
											sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
											return false;
										}
									}
									else if (paramName.equalsIgnoreCase("c"))
									{
										if(allowedTo(sender, "spawn.kill.color"))
										{
											color=true;
											try
											{
												colorCode = DyeColor.getByData(Byte.parseByte(param));
											}
											catch (NumberFormatException e)
											{
												try
												{
													colorCode = DyeColor.valueOf(DyeColor.class, param.toUpperCase());
												} catch (IllegalArgumentException f)
												{
													sender.sendMessage(ChatColor.RED + "Color parameter must be a valid color or a number from 0 to 15.");
													return false;
												}
											}
										}
										else
										{
											sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
											return false;
										}
									}
									else if (paramName.equalsIgnoreCase("f"))
									{
										if(allowedTo(sender, "spawn.kill.fire"))
											fire=true;
										else
										{
											sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
											return false;
										}
									}
									else if (paramName.equalsIgnoreCase("h"))
									{
										if (allowedTo(sender, "spawn.kill.health"))
										{
											try
											{
												if (param.endsWith("%"))
												{
													sender.sendMessage(ChatColor.RED + "Health parameter must be an integer (Percentage not supported for kill)");
													return false;
												}
												else
												{
													healthIsPercentage=false;
													healthValue = Integer.parseInt(param);
													health=true;
												}
											} catch (NumberFormatException e)
											{
												sender.sendMessage(ChatColor.RED + "Health parameter must be an integer (Percentage not supported for kill)");
												return false;
											}
										}
										else
										{
											sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
											return false;
										}
									}
									else if (paramName.equalsIgnoreCase("m"))
									{
										if(allowedTo(sender, "spawn.kill.mount"))
										{
											mount=true;
										}
										else
										{
											sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
											return false;
										}
									}
									else if (paramName.equalsIgnoreCase("n"))
									{
										if(allowedTo(sender, "spawn.kill.naked"))
											naked=true;
										else
										{
											sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
											return false;
										}
									}
									else if (paramName.equalsIgnoreCase("o"))
									{
										if(allowedTo(sender, "spawn.kill.owner"))
										{
											owned = true;
											owner = lookupPlayers(param, sender, "kill.owner"); // No need to validate; null means that it will kill ALL owned wolves.\
											if ((owner.length == 0)&&(param != null)) // If user typed something, it means they wanted a specific player and would probably be unhappy with killing ALL owners.
												sender.sendMessage(ChatColor.RED + "Could not locate player by that name.");
										}
										else
										{
											sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
											return false;
										}
									}
									else if (paramName.equalsIgnoreCase("s"))
									{
										if(allowedTo(sender, "spawn.kill.size"))
										{
											try
											{
												size = true;
												sizeValue = Integer.parseInt(param); //Size limit only for spawning, not killing.
											} catch (NumberFormatException e)
											{
												sender.sendMessage(ChatColor.RED + "Size parameter must be an integer.");
												return false;
											}
										}
										else
										{
											sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
											return false;
										}
									}
									else if (paramName.equalsIgnoreCase("t"))
									{
										try
										{
											if(allowedTo(sender, "spawn.kill.target"))
											{
												target=true;
												targets = lookupPlayers(param, sender, "kill.target");
												if ((targets.length == 0) && (param != null)) // If user actually bothered to typed something, it means they were trying for a specific player and probably didn't intend for mobs with ANY targets.
												{
													sender.sendMessage(ChatColor.RED + "Could not find a target by that name");
													return false;
												}
											}
											else
											{
												sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
												return false;
											}
										} catch (NumberFormatException e)
										{
											sender.sendMessage(ChatColor.RED + "Size parameter must be an integer.");
											return false;
										}
									}
									else
									{
										sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
										return false;
									}
								}
							}
						int bodyCount=0;
						Class<Entity>[] targetEnts = lookup(mobParam[0], sender, "spawn.kill");
						if (targetEnts.length == 0)
						{
							sender.sendMessage(ChatColor.RED + "Invalid mob type.");
							return false;
						}
						if ((radius != 0)&&(!(sender instanceof Player)))
						{
							sender.sendMessage(ChatColor.RED + "...and where did you think I'd measure that radius from, Mr Console?");
							return false;
						}
							
						bodyCount=Kill(sender, targetEnts, radius, angry, color, colorCode, fire, health, healthValue, mount, naked, owned, owner, size, sizeValue, target, targets);
						sender.sendMessage(ChatColor.BLUE + "Killed " + bodyCount + " " + mobParam[0] + "s.");
						return true;
					}
					// Done with /kill
					else
					{
						if (!(sender instanceof Player))
						{
							printHelp(sender);
							return false;
						}
						Player player = (Player) sender;
						Location loc=player.getLocation();
						Block targetB = new TargetBlock(player, 300, 0.2, ignore).getTargetBlock();
						if (targetB!=null)
						{
							loc.setX(targetB.getLocation().getX());
							loc.setY(targetB.getLocation().getY() + 1);
							loc.setZ(targetB.getLocation().getZ());
						}

						int count=1;
						String[] passengerList = args[0].split(";"); //First, get the passenger list
						Ent index = null, index2 = null;
						for (int i=0; i<passengerList.length; i++)
						{
							if (index != null)
								index.setPassenger(index2);
							String mobParam[] = passengerList[i].split("/"); //Now, look at each passenger to see if there's a parameter
							int healthValue=100, size=1, fireTicks=-1, velocity=0;
							boolean setSize = false, health = false, healthIsPercentage = true, angry = false, bounce = false, color = false, mount = false, target = false, tame = false, naked = false;
							Player targets[]=null;
							Player owner[]=null;
							DyeColor colorCode=DyeColor.WHITE;
							if (mobParam.length>1)
							{
								for (int j=1; j<mobParam.length; j++)
								{
									String paramName = mobParam[j].substring(0, 1);
									String param = null;
									if (mobParam[j].length() > 2)
										param = mobParam[j].substring(2);
									if (paramName.equalsIgnoreCase("a"))
									{
										if(allowedTo(sender, "spawn.angry"))
										{
											angry=true;
										}
										else
										{
											sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
											return false;
										}
									}
									else if (paramName.equalsIgnoreCase("b"))
									{
										if(allowedTo(sender, "spawn.bounce"))
										{
											bounce=true;
										}
										else
										{
											sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
											return false;
										}
									}
									else if (paramName.equalsIgnoreCase("c"))
									{
										if(allowedTo(sender, "spawn.color"))
										{
											color=true;
											try
											{
												colorCode = DyeColor.getByData(Byte.parseByte(param));
											}
											catch (NumberFormatException e)
											{
												try
												{
													colorCode = DyeColor.valueOf(DyeColor.class, param.toUpperCase());
												} catch (IllegalArgumentException f)
												{
													sender.sendMessage(ChatColor.RED + "Color parameter must be a valid color or a number from 0 to 15.");
													return false;
												}
											}
										}
										else
										{
											sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
											return false;
										}
									}
									else if (paramName.equalsIgnoreCase("f"))
									{
										if(allowedTo(sender, "spawn.fire"))
											try
											{
												fireTicks = Integer.parseInt(param)*20;
											} catch (NumberFormatException e)
											{
												sender.sendMessage(ChatColor.RED + "Fire parameter must be an integer.");
												return false;
											}
										else
										{
											sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
											return false;
										}
									}
									else if (paramName.equalsIgnoreCase("h"))
									{
										if (allowedTo(sender, "spawn.health"))
										{
											try
											{
												if (param.endsWith("%"))
												{
													healthIsPercentage=true;
													healthValue = Integer.parseInt(param.substring(0, param.indexOf("%")));
													health=true;
												}
												else
												{
													healthIsPercentage=false;
													healthValue = Integer.parseInt(param);
													health=true;
												}
											} catch (NumberFormatException e)
											{
												sender.sendMessage(ChatColor.RED + "Health parameter must be an integer or a percentage");
												return false;
											}
										}
										else
										{
											sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
											return false;
										}
									}
									else if (paramName.equalsIgnoreCase("m"))
									{
										if(allowedTo(sender, "spawn.mount"))
										{
											mount=true;
										}
										else
										{
											sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
											return false;
										}
									}
									else if (paramName.equalsIgnoreCase("n"))
									{
										if(allowedTo(sender, "spawn.naked"))
											naked=true;
										else
										{
											sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
											return false;
										}
									}
									else if (paramName.equalsIgnoreCase("o"))
									{
										if(allowedTo(sender, "spawn.owner"))
										{
											tame=true;
											owner = lookupPlayers(param, sender, "spawn.owner"); // No need to validate; null means that it will be tame but unownable.  Could be fun.
											if ((owner.length == 0)&&(param != null)) // If user typed something, it means they wanted a specific player and would probably be unhappy with killing ALL owners.
												sender.sendMessage(ChatColor.RED + "Could not locate player by that name.");
										}
										else
										{
											sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
											return false;
										}
									}
									else if (paramName.equalsIgnoreCase("s"))
									{
										if(allowedTo(sender, "spawn.size"))
										{
											try
											{
												setSize = true;
												size = Integer.parseInt(param);
												if (size > sizeLimit)
													size = sizeLimit;
											} catch (NumberFormatException e)
											{
												sender.sendMessage(ChatColor.RED + "Size parameter must be an integer.");
												return false;
											}
										}
										else
										{
											sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
											return false;
										}
									}
									else if (paramName.equalsIgnoreCase("t"))
									{
										try
										{
											if(allowedTo(sender, "spawn.target"))
											{
												target=true;
												targets = lookupPlayers(param, sender, "spawn.target");
												if (targets.length == 0)
												{
													sender.sendMessage(ChatColor.RED + "Could not find a target by that name");
													return false;
												}
											}
											else
											{
												sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
												return false;
											}
										} catch (NumberFormatException e)
										{
											sender.sendMessage(ChatColor.RED + "Size parameter must be an integer.");
											return false;
										}
									}
									else if (paramName.equalsIgnoreCase("v"))
									{
										if(allowedTo(sender, "spawn.velocity"))
										{
											try
											{
												velocity = Integer.parseInt(param);
											} catch (NumberFormatException e)
											{
												sender.sendMessage(ChatColor.RED + "Velocity parameter must be an integer.");
												return false;
											}
										}
										else
										{
											sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
											return false;
										}
									}
									else
									{
										sender.sendMessage(ChatColor.RED + "Invalid parameter " + paramName);
										return false;
									}
								}
							}
							index2=index;
							Player[] people = lookupPlayers(mobParam[0], sender, "spawn.player");
							if (people.length == 0)
							{
								Class<Entity>[] results = lookup(mobParam[0], sender, "spawn.spawn");
								if (results.length == 0)
								{
									sender.sendMessage(ChatColor.RED + "Invalid mob type: " + mobParam[0]);
									return false;
								}
								index = new Ent(results, mobParam[0], angry, bounce, color, colorCode, fireTicks, health, healthIsPercentage, healthValue, mount, naked, tame, owner, index2, setSize, size, target, targets, velocity);
							}
							else
								index = new Person(people, mobParam[0], angry, bounce, color, colorCode, fireTicks, health, healthIsPercentage, healthValue, mount, naked, tame, owner, index2, setSize, size, target, targets, velocity);
						}
						
						if (args.length > 1)
						{
							try
							{
								count=Integer.parseInt(args[1]);
								if (count < 1)
								{
									sender.sendMessage(ChatColor.RED + "Invalid number - must be at least one.");
									return false;
								}
							}
							catch (Exception e)
							{

								return false;
							}
						}
						if (count > spawnLimit)
						{
							info("Player " + sender.getName() + " tried to spawn more than " + spawnLimit + " mobs");
							count = spawnLimit;
						}
						if (index.spawn(player, this, loc, count))
							sender.sendMessage(ChatColor.BLUE + "Spawned " + count + " " + index.description());
						else
							sender.sendMessage(ChatColor.RED + "Some things just weren't meant to be spawned.  Check server log.");
						return true;
					}
				}
				else
				{
					printHelp(sender);
					return false;
				}
			}
		}
		else if (command.getName().equalsIgnoreCase("spawn-admin") || command.getName().equalsIgnoreCase("sp-admin") || command.getName().equalsIgnoreCase("s-admin"))
		{
			if (allowedTo(sender, "spawn-admin"))
			{
				if ((args.length > 0)) 
				{
					if (args[0].equalsIgnoreCase("save"))
					{
						sender.sendMessage(ChatColor.GREEN + "Saving configuration file...");
						if (save())
							sender.sendMessage(ChatColor.GREEN + "Done.");
						else
							sender.sendMessage(ChatColor.RED + "Could not save configuration file - please see server log.");
						return true;
					}
					else if (args[0].equalsIgnoreCase("reset"))
					{
						sender.sendMessage(ChatColor.GREEN + "Resetting configuration file...");
						if (saveDefault())
							sender.sendMessage(ChatColor.GREEN + "Done.");
						else
							sender.sendMessage(ChatColor.RED + "Could not save configuration file - please see server log.");
						return true;
					}
					else if (args[0].equalsIgnoreCase("reload"))
					{
						sender.sendMessage(ChatColor.GREEN + "Reloading Spawn...");
						if (reload())
							sender.sendMessage(ChatColor.GREEN + "Done.");
						else
							sender.sendMessage(ChatColor.RED + "An error occurred while reloading - please see server log.");
						return true;
					}
				}
			}
		}
		else
			sender.sendMessage("Unknown console command. Type \"help\" for help"); // No reason to tell them what they CAN'T do, right?
		return false;
	}
	
	/**
	 * Prints help in accordance with the player's permissions
	 * 
	 * @param sender: The person being "helped"
	 */
	public void printHelp(CommandSender sender)
	{
		if (allowedTo(sender, "spawn.admin"))
		{
			sender.sendMessage(ChatColor.GREEN + "/spawn-admin reload");
			sender.sendMessage(ChatColor.YELLOW + "Reloads Spawn plugin");
			sender.sendMessage(ChatColor.GREEN + "/spawn-admin save");
			sender.sendMessage(ChatColor.YELLOW + "Saves Spawn's configuration file");
			sender.sendMessage(ChatColor.GREEN + "/spawn-admin reset");
			sender.sendMessage(ChatColor.RED + "Overwrites Spawn's configuration file with default settings");
		}
		if (allowedTo(sender, "spawn.spawn") && sender instanceof Player)
		{
			sender.sendMessage(ChatColor.BLUE + "/spawn <entity>/<paramname>:<param>/<paramname>:<param>");
			sender.sendMessage(ChatColor.YELLOW + "Spawns <entity> with parameters");
			if (allowedTo(sender, "spawn.angry"))
			{
				sender.sendMessage(ChatColor.BLUE + "/spawn <entity>/a:");
				sender.sendMessage(ChatColor.YELLOW + "Spawns an angry/powered version of <entity>");
			}
			if (allowedTo(sender, "spawn.bounce"))
			{
				sender.sendMessage(ChatColor.BLUE + "/spawn <entity>/b:");
				sender.sendMessage(ChatColor.YELLOW + "Spawns <entity> projectile that bounces on impact");
			}
			if (allowedTo(sender, "spawn.color"))
			{
				sender.sendMessage(ChatColor.BLUE + "/spawn <entity>/c:<color code 0-15>");
				sender.sendMessage(ChatColor.YELLOW + "Spawns <entity> that has the specified color");
			}
			if (allowedTo(sender, "spawn.fire"))
			{
				sender.sendMessage(ChatColor.BLUE + "/spawn <entity>/f:<number of seconds>");
				sender.sendMessage(ChatColor.YELLOW + "Spawns <entity> that burns for specified number of (unlagged) seconds");
				sender.sendMessage(ChatColor.YELLOW + "Entities that specify a fuse also use this value");
			}
			if (allowedTo(sender, "spawn.health"))
			{
				sender.sendMessage(ChatColor.BLUE + "/spawn <entity>/h:<health>" + ChatColor.YELLOW + " OR " + ChatColor.BLUE + "/spawn <mob>/h:<health%>");
				sender.sendMessage(ChatColor.YELLOW + "Spawns <entity> with specified health (usually only works for 1-10, can also use percentage)");
			}
			if (allowedTo(sender, "spawn.mount"))
			{
				sender.sendMessage(ChatColor.BLUE + "/spawn <entity>/m:");
				sender.sendMessage(ChatColor.YELLOW + "Spawns <entity> with a mount (saddle)");
			}
			if (allowedTo(sender, "spawn.naked"))
			{
				sender.sendMessage(ChatColor.BLUE + "/spawn <entity>/n:");
				sender.sendMessage(ChatColor.YELLOW + "Spawns <entity> with clothing irretrievably destroyed");
			}
			if (allowedTo(sender, "spawn.owner"))
			{
				sender.sendMessage(ChatColor.BLUE + "/spawn <entity>/o:<player name>");
				sender.sendMessage(ChatColor.YELLOW + "Spawns tame <entity> with specified player as owner; if unspecified, will be unownable");
			}
			if (allowedTo(sender, "spawn.passenger"))
			{
				sender.sendMessage(ChatColor.BLUE + "/spawn <entparams>;<entparams2>" + ChatColor.AQUA + "[;<entparams3>...] <number>");
				sender.sendMessage(ChatColor.YELLOW + "Spawns <entity> with parameters riding <ent2>" + ChatColor.DARK_AQUA + " riding <ent3>...");
			}
			if (allowedTo(sender, "spawn.size"))
			{
				sender.sendMessage(ChatColor.BLUE + "/spawn <entity>/s:<size>");
				sender.sendMessage(ChatColor.YELLOW + "Spawns <entity> with specified size (usually only works for slimes)");
			}
			if (allowedTo(sender, "spawn.target"))
			{
				sender.sendMessage(ChatColor.BLUE + "/spawn <entity>/t:<player name>");
				sender.sendMessage(ChatColor.YELLOW + "Spawns <entity> with specified player as target");
			}
			if (allowedTo(sender, "spawn.velocity"))
			{
				sender.sendMessage(ChatColor.BLUE + "/spawn <entity>/v:<velocity>");
				sender.sendMessage(ChatColor.YELLOW + "Spawns <entity> with specified velocity (random direction)");
			}
		}
		if (allowedTo(sender, "spawn.kill"))
		{
			sender.sendMessage(ChatColor.BLUE + "/spawn kill");
			sender.sendMessage(ChatColor.YELLOW + "Kills all entities and gives a body count");
			sender.sendMessage(ChatColor.BLUE + "/spawn kill<params>");
			sender.sendMessage(ChatColor.YELLOW + "Kills all entities with <optional parameters> and gives a body count");
			if (sender instanceof Player)
			{
				sender.sendMessage(ChatColor.BLUE + "/spawn kill <mobtype><params> <radius>");
				sender.sendMessage(ChatColor.YELLOW + "Kills all mobs of <type> with <optional parameters> within <optional radius> of you and gives a body count");
			}
			else
			{
				sender.sendMessage(ChatColor.BLUE + "/spawn kill <mobtype><params>");
				sender.sendMessage(ChatColor.YELLOW + "Kills all mobs of <type> with <optional parameters> and gives a body count");
			}
		}
	}

	/**
	 * Probably the only leftover from SpawnMob.  Should replace with a PluginListener...
	 * 
	 * Tests to see if permissions is working; if so, sets our Permissions handle so we can access it.
	 * Otherwise, sets permissions to false.
	 */
	private void setupPermissions()
	{
		Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");
		if (Spawn.Permissions == null) {
			if (test != null) {
				Spawn.Permissions = ((Permissions)test).getHandler();
				info("Permission system found, plugin enabled");
			} else {
				info("Permission system not detected! Please go into the SpawnMob.properties and set use-permissions to false.");
				info("Please go into the SpawnMob.properties and set use-permissions to false.");
				permissions = false;
			}
		}
	}
	
	/**
	 * Checks to see if sender has permission OR is an op.  If not using permissions, only op is tested.
	 * @param sender: The person whose permission is being checked
 	 * @param permission The permission being checked (e.g. "exampleplugin.examplepermnode")
	 * @returns boolean: True if player has the permission node OR if player is an op
	 */
	boolean allowedTo(CommandSender sender, String permission)
	{
		if (sender.isOp())
			return true;
		else if (permissions && sender instanceof Player)
			return Permissions.has((Player)sender, permission);
		return false;
	}
	
	/**
	 * Test to see whether type (usually a CraftBukkit Entity of some kind) uses any of the (Bukkit)
	 * interfaces in types.  (e.g. CraftCow implements Cow).  Useful for determining if an entity
	 * is in our list of creature types we want to work on.
	 * 
	 * @param types: A list of types that is being used to filter Entities
	 * @param type: The type being filtered
	 * @return boolean: True if type uses any of the types as an Interface
	 */
	private boolean hasClass(Class<Entity>[] types, Class<? extends Entity> type)
	{
		for (int i=0; i<types.length; i++)
			for (int j=0; j<type.getInterfaces().length; j++)
				if (type.getInterfaces()[j]==types[i])
					return true;
		return false;
	}
	
	/**
	 * Tests to see whether subject exists as a member of array.
	 * @param subject: An object being filtered
	 * @param array: A list of objects being used as a filter
	 * @return boolean: True if array has subject in it
	 */
	private boolean existsIn(Object subject, Object[] array)
	{
		for (int i=0; i<array.length; i++)
			if (array[i]==subject)
				return true;
		return false;
	}
	
	/**
	 * Determines if an entity meets the criteria for slaughter.  If so, removes it.
	 * @param types: The types of entity referred to by the alias
	 * @param angry: If true, only kills angry entities
	 * @param color: If true, only kills entities of a specific colorCode 
	 * @param colorCode: Used to decide what color of entity to kill (e.g. only kill blue sheep).
	 * @param fire: If true, only kills burning entities
	 * @param health: If true, only kills entities of a specific health value
	 * @param healthValue: Used to decide exactly how healthy an entity needs to be to die
	 * @param mount: If true, will only kill mounted entities (e.g. saddled pigs)
	 * @param owned: If true, will only kill owned entities (e.g. owned wolves) (default is to IGNORE owned entities, use carefully!)
	 * @param owner: If set and owned is true, only kills entities owned by that player.  If set to null and owned is true, kills ALL owned entities
	 * @param naked: If true, only kills naked entities (e.g. sheared sheep)
	 * @param size: If true, only kills entities of a specific size
	 * @param sizeValue: Used to decide exactly how big an entity needs to be to die
	 * @param target: If true, only kills entities that currently have a target
	 * @param targets: If set and target is true, only killed entities with these targets.  If null and target is true, kills entities with ANY target
	 * 
	 * @return boolean: true is ent was killed, false if it lived
	 */
	private boolean KillSingle(Entity ent, Class<Entity>[] types, boolean angry, boolean color, DyeColor colorCode, boolean fire, boolean health, int healthValue, boolean mount, boolean naked, boolean owned, AnimalTamer[] owner, boolean size, int sizeValue, boolean target, Player[] targets)
	{
		Class<? extends Entity> type = ent.getClass();
		try
		{
			if (hasClass(types, type))
			{
				Method ownerMethod;
				//CULLING STAGE - each test returns false if it fails to meet it.
				
				//ANGRY (default is to kill either way)
				if (angry)
				{
					Method angryMethod = null;
					try
					{
						angryMethod = type.getMethod("isAngry");
						if (!(Boolean)angryMethod.invoke(ent))
							return false;
					} catch (NoSuchMethodException e)
					{
						try
						{
							angryMethod = type.getMethod("isPowered");
							if (!(Boolean)angryMethod.invoke(ent))
								return false;
						} catch (NoSuchMethodException f){return false;};//yeah, we have to rely on Exceptions to find out if it has a method or not, how sad is that?
					}
				}
				
				//COLOR (default is to kill either way)
				
				if (color)
				{
					Method colorMethod;
					try
					{
						colorMethod = type.getMethod("getColor");
						if (colorCode!=colorMethod.invoke(ent))
							return false;
					} catch (NoSuchMethodException e){return false;}
				}
				
				//FIRE (default is to kill either way)
				
				if (fire)
					if (ent.getFireTicks() < 1)
						return false;
				
				//HEALTH (default is to kill either way)
				
				if (health)
				{
					try
					{
						Method healthMethod = type.getMethod("getHealth");
						if ((Integer)healthMethod.invoke(ent)!=healthValue)
							return false;
						if (ent instanceof ExperienceOrb)
							if (((ExperienceOrb)ent).getExperience()!=healthValue)
								return false;
					} catch (NoSuchMethodException e){return false;}
				}

				//MOUNT (default is to leave mounted ents alone)

				Method mountMethod;
				try
				{
					mountMethod = type.getMethod("hasSaddle");
					info (((Boolean)mount).toString());
					info (mountMethod.invoke(ent).toString());
					if (mount != (Boolean)mountMethod.invoke(ent))
						return false;
				} catch (NoSuchMethodException e){if (mount) return false;}
				
				//NAKED (default is to leave naked ents alone)
				
				Method shearMethod;
				try
				{
					shearMethod = type.getMethod("isSheared");
					if (naked != (Boolean)shearMethod.invoke(ent))
						return false;
				} catch (NoSuchMethodException e){if (naked) return false;}
					
				//OWNER (default is to leave owned ents alone)
				
				try
				{
					ownerMethod = type.getMethod("getOwner");
					AnimalTamer entOwner = (AnimalTamer) ownerMethod.invoke(ent); // If Bukkit ever adds a getOwner that does not return this, it will break.
					if (owned)
					{
						if (entOwner == null) //Cull all the unowned ents
							return false;
						if (owner.length > 0) //If owner is unspecified, then don't cull ANY owned ents
							if (!existsIn(entOwner, owner)) // Otherwise, cull wolves owned by someone not in the list
								return false;
					}
					else // Default is to NOT kill owned ents.  (Tamed ents with null owner will still be killed)
						if (entOwner != null)
							return false;
				} catch(NoSuchMethodException e){if (owned) return false;}
				
				//SIZE (default is to kill either way)
				
				if (size)
				{
					Method sizeMethod = null;
					try
					{
						sizeMethod = type.getMethod("getSize");
						if (sizeValue != (Integer)sizeMethod.invoke(ent, sizeValue))
							return false;
					} catch (NoSuchMethodException e){return false;};
				}

				//TARGET (default is to kill either way)

				Method targetMethod;
				try
				{
					if (target)
					{
						targetMethod = type.getMethod("getTarget");
						LivingEntity targetLiving = (LivingEntity)targetMethod.invoke(ent);
						if (targetLiving == null) // Cull all living ents without a target
							return false;
						if (targets.length > 0) // If target is unspecified, don't cull ANY mobs with targets
							if (!existsIn(targetLiving, targets))
								return false;
					}
				} catch (NoSuchMethodException e){if (target) return false;}
				
				ent.remove();
				return true;
			}
		} catch(InvocationTargetException e)
		{
			warning("Target " + type.getSimpleName() + " has a method for doing something, but threw an exception when it was invoked:");
			e.printStackTrace();
		} catch(IllegalAccessException e)
		{
			warning("Target " + type.getSimpleName() + " has a method for doing something, but threw an exception when it was invoked:");
			e.printStackTrace();
		} 
		return false;
	}
	
	/**
	 * Searches for and kills all entities that meet the specified criteria and returns a body count
	 * 
	 * @param sender: The person who sent out the hit.  Used for radius parameter.
	 * @param types: The types of entity referred to by the alias
	 * @param radius: If positive, only kills entities within radius of sender.  Will throw casting exception if positive and sender is the console.
	 * @param angry: If true, only kills angry entities
	 * @param color: If true, only kills entities of a specific colorCode 
	 * @param colorCode: Used to decide what color of entity to kill (e.g. only kill blue sheep).
	 * @param fire: If true, only kills burning entities
	 * @param health: If true, only kills entities of a specific health value
	 * @param healthValue: Used to decide exactly how healthy an entity needs to be to die
	 * @param mount: If true, will only kill mounted entities (e.g. saddled pigs)
	 * @param owned: If true, will only kill owned entities (e.g. owned wolves) (default is to IGNORE owned entities, use carefully!)
	 * @param owner: If set and owned is true, only kills entities owned by that player.  If set to null and owned is true, kills ALL owned entities
	 * @param naked: If true, only kills naked entities (e.g. sheared sheep)
	 * @param size: If true, only kills entities of a specific size
	 * @param sizeValue: Used to decide exactly how big an entity needs to be to die
	 * @param target: If true, only kills entities that currently have a target
	 * @param targets: If set and target is true, only killed entities with these targets.  If null and target is true, kills entities with ANY target
	 * 
	 * @return int: how many entities were slain
	 */
	public int Kill(CommandSender sender, Class<Entity>[] types, int radius, boolean angry, boolean color, DyeColor colorCode, boolean fire, boolean health, int healthValue, boolean mount, boolean naked, boolean owned, Player[] owner, boolean size, int sizeValue, boolean target, Player[] targets)
	{
		int bodycount=0;
		List<Entity> ents;
		if (radius > 0)
		{
			ents = ((Player)sender).getNearbyEntities(radius, radius, radius);
			for(Iterator<Entity> iterator = ents.iterator(); iterator.hasNext();)
			{
				Entity ent = iterator.next();
				if (ent.getLocation().distance(((Player)sender).getLocation()) <= radius)
					if (KillSingle(ent, types, angry, color, colorCode, fire, health, healthValue, mount, naked, owned, owner, size, sizeValue, target, targets))
						bodycount++;
			}
		}
		else
		{
			for (Iterator<World> worlditerator = getServer().getWorlds().iterator(); worlditerator.hasNext();)
			{
				ents = worlditerator.next().getEntities();
				for(Iterator<Entity> iterator = ents.iterator(); iterator.hasNext();)
				{
					Entity ent = iterator.next();
					if (KillSingle(ent, types, angry, color, colorCode, fire, health, healthValue, mount, naked, owned, owner, size, sizeValue, target, targets))
						bodycount++;
				}
			}
		}
		return bodycount;
	}
	
	/**
	 * Logs an informative message to the console, prefaced with this plugin's header
	 * @param message: String
	 */
	protected void info(String message)
	{
		log.info(header + message);
	}

	/**
	 * Logs a severe error message to the console, prefaced with this plugin's header
	 * Used to log severe problems that have prevented normal execution of the plugin
	 * @param message: String
	 */
	protected void severe(String message)
	{
		log.severe(header + message);
	}

	/**
	 * Logs a warning message to the console, prefaced with this plugin's header
	 * Used to log problems that could interfere with the plugin's ability to meet admin expectations
	 * @param message: String
	 */
	protected void warning(String message)
	{
		log.warning(message);
	}

	/**
	 * Logs a message to the console, prefaced with this plugin's header
	 * @param level: Logging level under which to send the message
	 * @param message: String
	 */
	protected void log(java.util.logging.Level level, String message)
	{
		log.log(level, header + message);
	}
}


package joren.spawn;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Keeps track of when plugins get enabled or disabled.  Used to tell the main class when this happens.
 * @author Joren Combs
 *
 */
public class PluginListener implements Listener {
	public Spawn plugin;
    public PluginListener(Spawn plugin)
    {
        this.plugin = plugin;
    }
    
    /**
     * This runs whenever a plugin is enabled.
     * @param event
     */
    @EventHandler
    public void onPluginEnable(PluginEnableEvent event)
    {
    	if(event.getPlugin().getDescription().getName().equals("Vault"))
    	{
    		RegisteredServiceProvider<Permission> permissionProvider = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
            if (permissionProvider != null) {
            	Spawn.info("Using Vault for permissions.");
                plugin.setPermissions(permissionProvider.getProvider());
            }
    	}
    }
    
    /**
     * This runs whenever a plugin is disabled.
     * @param event
     */
    @EventHandler
    public void onPluginDisable(PluginDisableEvent event)
    {
    	if(event.getPlugin().getDescription().getName().equals("Vault")) {
        	Spawn.info("Vault is being disabled; will run in ops-only mode until it comes back.");
    		plugin.setPermissions(null);
    	}
    }
}

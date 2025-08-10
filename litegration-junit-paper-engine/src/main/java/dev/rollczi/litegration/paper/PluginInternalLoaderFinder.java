package dev.rollczi.litegration.paper;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

class PluginInternalLoaderFinder {

    private static ClassLoader findPluginClassLoader(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin with name " + pluginName + " not found!");
        }
        return plugin.getClass().getClassLoader();
    }

}

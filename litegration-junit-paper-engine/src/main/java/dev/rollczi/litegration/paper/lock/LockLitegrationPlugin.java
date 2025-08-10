package dev.rollczi.litegration.paper.lock;

import java.util.concurrent.CompletableFuture;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class LockLitegrationPlugin extends JavaPlugin implements Listener {

    static final CompletableFuture<Boolean> SERVER_READY = new CompletableFuture<>();
    static final CompletableFuture<Boolean> CLIENT_READY = new CompletableFuture<>();

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    void onServerLoad(ServerLoadEvent event) {
        if (event.getType() != ServerLoadEvent.LoadType.STARTUP) {
            return;
        }

        SERVER_READY.complete(true);
        CLIENT_READY.join();
    }

}

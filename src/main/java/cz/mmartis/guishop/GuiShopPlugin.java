package cz.mmartis.guishop;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;

public class GuiShopPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Paper way: commands are registered via Brigadier in the lifecycle event,
        // not in plugin.yml. Survives /minecraft:reload.
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        Commands.literal("shop")
                                .executes(ctx -> {
                                    ctx.getSource().getSender().sendMessage(Component.text("hello"));
                                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                })
                                .build(),
                        "Otevře obchod"
                )
        );
    }
}

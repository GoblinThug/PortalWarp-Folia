package ru.goblin_thug.portalwarp;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import ru.goblin_thug.portalwarp.commands.CommandHandler;
import ru.goblin_thug.portalwarp.listeners.CompassListener;
import ru.goblin_thug.portalwarp.listeners.PortalListener;
import ru.goblin_thug.portalwarp.managers.PortalManager;

import java.util.List;

public class PortalWarp extends JavaPlugin {

    private PortalManager portalManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        portalManager = new PortalManager(this);
        portalManager.loadPortals();

        getServer().getPluginManager().registerEvents(new CompassListener(this), this);
        getServer().getPluginManager().registerEvents(new PortalListener(this), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(buildCommand(), "Portal management", List.of());
        });

        getLogger().info("PortalWarp enabled. Portals loaded: " + portalManager.getAllPortals().size());
    }

    private LiteralCommandNode<CommandSourceStack> buildCommand() {
        CommandHandler handler = new CommandHandler(this);

        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("portal")
                .requires(src -> src.getSender().hasPermission("portalwarp.use"))
                .executes(ctx -> handler.help(ctx.getSource().getSender()));

        // /portal warp create <name>
        root.then(Commands.literal("warp")
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> handler.warpCreate(ctx)))));

        // /portal connect create <name> <address>  (только админ)
        // address — string, чтобы разрешить двоеточие (ip:port)
        root.then(Commands.literal("connect")
                .requires(src -> src.getSender().hasPermission("portalwarp.admin"))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("address", StringArgumentType.string())
                                        .executes(ctx -> handler.connectCreate(ctx))))));

        // /portal delete <name>
        root.then(Commands.literal("delete")
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests((ctx, builder) -> handler.suggestPortalNames(builder))
                        .executes(ctx -> handler.delete(ctx))));

        // /portal list
        root.then(Commands.literal("list")
                .executes(ctx -> handler.list(ctx)));

        // /portal reload (только админ)
        root.then(Commands.literal("reload")
                .requires(src -> src.getSender().hasPermission("portalwarp.admin"))
                .executes(ctx -> handler.reload(ctx)));

        return root.build();
    }

    @Override
    public void onDisable() {
        if (portalManager != null) {
            portalManager.cancelAllParticleTasks();
            portalManager.savePortals();
        }
    }

    public void reload() {
        reloadConfig();
        portalManager.cancelAllParticleTasks();
        portalManager.loadPortals();
    }

    public PortalManager getPortalManager() {
        return portalManager;
    }
}

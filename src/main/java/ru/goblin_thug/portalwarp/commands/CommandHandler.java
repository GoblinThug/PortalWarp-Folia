package ru.goblin_thug.portalwarp.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.goblin_thug.portalwarp.PortalWarp;
import ru.goblin_thug.portalwarp.managers.Portal;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CommandHandler {

    private final PortalWarp plugin;

    public CommandHandler(PortalWarp plugin) {
        this.plugin = plugin;
    }

    public int help(CommandSender sender) {
        sender.sendMessage("§a=== PortalWarp ===");
        sender.sendMessage("§e/portal warp create <название> §7— создать портал (2 рамки)");
        sender.sendMessage("§e/portal connect create <название> <адрес> §7— создать серверный портал (1 рамка, админ)");
        sender.sendMessage("§e/portal delete <название> §7— удалить портал");
        sender.sendMessage("§e/portal list §7— список порталов");
        sender.sendMessage("§e/portal reload §7— перезагрузить конфиг (админ)");
        sender.sendMessage("§7Для создания портала держите в руке §dвозвратный компас");
        return 1;
    }

    public int warpCreate(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эту команду может использовать только игрок.");
            return 0;
        }

        String name = StringArgumentType.getString(ctx, "name");
        UUID uuid = player.getUniqueId();

        plugin.getPortalManager().setPendingCreation(uuid, name);
        player.sendMessage("§a[PortalWarp] §7Создание портала: §e" + name);
        player.sendMessage("§aВозьмите §dвозвратный компас §aи кликните по §eпервой §aрамке портала.");
        player.sendMessage("§7Портал-выход: §eоткуда §7будут телепортироваться игроки.");
        return 1;
    }

    public int connectCreate(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эту команду может использовать только игрок.");
            return 0;
        }

        String name = StringArgumentType.getString(ctx, "name");
        String address = StringArgumentType.getString(ctx, "address");
        UUID uuid = player.getUniqueId();

        plugin.getPortalManager().setPendingConnectCreation(uuid, name, address);
        player.sendMessage("§a[PortalWarp] §7Создание серверного портала: §e" + name + " §7-> §b" + address);
        player.sendMessage("§aВозьмите §dвозвратный компас §aи кликните по рамке портала.");
        player.sendMessage("§7Нужна только §eодна §7рамка — при входе в неё игрок уйдёт на другой сервер.");
        return 1;
    }

    public int delete(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "name");

        boolean deleted = plugin.getPortalManager().deletePortal(name);

        if (deleted) {
            sender.sendMessage("§a[PortalWarp] Портал §e" + name + " §aудалён.");
        } else {
            sender.sendMessage("§c[PortalWarp] Портал с названием §e" + name + " §cне найден.");
        }
        return 1;
    }

    public int list(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        Collection<Portal> portals = plugin.getPortalManager().getAllPortals();

        if (portals.isEmpty()) {
            sender.sendMessage("§7[PortalWarp] Порталов нет.");
            return 1;
        }

        sender.sendMessage("§a[PortalWarp] §7Порталов: §e" + portals.size());
        for (Portal portal : portals) {
            if (portal.isCrossServer()) {
                sender.sendMessage("§7- §e" + portal.getName() + " §7(-> §b" + portal.getServerAddress() + "§7) §d[серверный]");
            } else {
                String w1 = portal.getFrame1().getWorld().getName();
                String w2 = portal.getFrame2().getWorld().getName();
                sender.sendMessage("§7- §e" + portal.getName() + " §7(" + w1 + " <-> " + w2 + ")");
            }
        }
        return 1;
    }

    public int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        plugin.reload();
        sender.sendMessage("§a[PortalWarp] Конфиг перезагружен. Порталов: §e"
                + plugin.getPortalManager().getAllPortals().size());
        return 1;
    }

    public CompletableFuture<Suggestions> suggestPortalNames(SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase();

        for (Portal portal : plugin.getPortalManager().getAllPortals()) {
            String name = portal.getName().toLowerCase();
            if (name.startsWith(input)) {
                builder.suggest(portal.getName());
            }
        }

        return builder.buildFuture();
    }
}

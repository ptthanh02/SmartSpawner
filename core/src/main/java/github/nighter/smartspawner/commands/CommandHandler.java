package github.nighter.smartspawner.commands;

import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.commands.give.GiveCommand;
import github.nighter.smartspawner.commands.hologram.HologramCommand;
import github.nighter.smartspawner.commands.list.ListCommand;
import github.nighter.smartspawner.commands.reload.ReloadCommand;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private final ReloadCommand reloadCommand;
    private final GiveCommand giveCommand;
    private final ListCommand listCommand;
    private final HologramCommand hologramCommand;
    private final SmartSpawner plugin;

    public CommandHandler(SmartSpawner plugin) {
        this.plugin = plugin;
        this.reloadCommand = new ReloadCommand(plugin);
        this.giveCommand = new GiveCommand(plugin);
        this.listCommand = new ListCommand(plugin);
        this.hologramCommand = new HologramCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getLanguageManager().getMessageWithPrefix("command.usage"));
            return true;
        }

        if(sender instanceof ConsoleCommandSender && args[0].equalsIgnoreCase("give")) {
            return giveCommand.executeCommand(args);
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                return reloadCommand.onCommand(sender, command, label, args);
            case "give":
                return giveCommand.executeCommand(sender, args);
            case "list":
                listCommand.openWorldSelectionGUI(player);
                return true;
            case "hologram":
                return hologramCommand.onCommand(sender, command, label, args);
            default:
                sender.sendMessage(plugin.getLanguageManager().getMessageWithPrefix("command.usage"));
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("smartspawner.reload")) {
                completions.add("reload");
            }
            if (sender.hasPermission("smartspawner.give")) {
                completions.add("give");
            }
            if (sender.hasPermission("smartspawner.list")) {
                completions.add("list");
            }
            if (sender.hasPermission("smartspawner.hologram")) {
                completions.add("hologram");
            }
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Handle subcommand tab completions
        if (args[0].equalsIgnoreCase("reload")) {
            return reloadCommand.onTabComplete(sender, command, alias, args);
        } else if (args[0].equalsIgnoreCase("give")) {
            return giveCommand.tabComplete(sender, args);
        }

        return Collections.emptyList();
    }
}

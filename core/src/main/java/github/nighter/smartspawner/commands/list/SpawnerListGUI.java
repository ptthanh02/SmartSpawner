package github.nighter.smartspawner.commands.list;

import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.language.LanguageManager;
import github.nighter.smartspawner.language.MessageService;
import github.nighter.smartspawner.spawner.properties.SpawnerData;
import github.nighter.smartspawner.spawner.properties.SpawnerManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SpawnerListGUI implements Listener {
    private final LanguageManager languageManager;
    private final MessageService messageService;
    private final SpawnerManager spawnerManager;
    private final ListCommand listCommand;
    private static final Set<Material> SPAWNER_MATERIALS = EnumSet.of(
            Material.PLAYER_HEAD, Material.SPAWNER, Material.ZOMBIE_HEAD,
            Material.SKELETON_SKULL, Material.WITHER_SKELETON_SKULL,
            Material.CREEPER_HEAD, Material.PIGLIN_HEAD
    );
    private static final String patternString = "#([A-Za-z0-9]+)";

    public SpawnerListGUI(SmartSpawner plugin) {
        this.languageManager = plugin.getLanguageManager();
        this.messageService = plugin.getMessageService();
        this.spawnerManager = plugin.getSpawnerManager();
        this.listCommand = plugin.getListCommand();
    }

    @EventHandler
    public void onWorldSelectionClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof WorldSelectionHolder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!player.hasPermission("smartspawner.list")) {
            messageService.sendMessage(player, "no_permission");
            return;
        }
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;

        String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        // Check for original layout slots first (for backward compatibility)
        if (event.getSlot() == 11 && displayName.equals(ChatColor.stripColor(languageManager.getGuiTitle("world_buttons.overworld.name")))) {
            listCommand.openSpawnerListGUI(player, "world", 1);
            return;
        } else if (event.getSlot() == 13 && displayName.equals(ChatColor.stripColor(languageManager.getGuiTitle("world_buttons.nether.name")))) {
            listCommand.openSpawnerListGUI(player, "world_nether", 1);
            return;
        } else if (event.getSlot() == 15 && displayName.equals(ChatColor.stripColor(languageManager.getGuiTitle("world_buttons.end.name")))) {
            listCommand.openSpawnerListGUI(player, "world_the_end", 1);
            return;
        }

        // For custom layout or any other slots, determine world by name
        if (displayName.equals(ChatColor.stripColor(languageManager.getGuiTitle("world_buttons.overworld.name")))) {
            listCommand.openSpawnerListGUI(player, "world", 1);
        } else if (displayName.equals(ChatColor.stripColor(languageManager.getGuiTitle("world_buttons.nether.name")))) {
            listCommand.openSpawnerListGUI(player, "world_nether", 1);
        } else if (displayName.equals(ChatColor.stripColor(languageManager.getGuiTitle("world_buttons.end.name")))) {
            listCommand.openSpawnerListGUI(player, "world_the_end", 1);
        } else {
            // For custom worlds, find the matching world
            for (World world : Bukkit.getWorlds()) {
                String worldDisplayName = formatWorldName(world.getName());

                if (spawnerManager.countSpawnersInWorld(world.getName()) > 0 &&
                        displayName.contains(worldDisplayName)) {
                    listCommand.openSpawnerListGUI(player, world.getName(), 1);
                    break;
                }
            }
        }
    }

    // Helper method to format world name (same as in ListCommand)
    private String formatWorldName(String worldName) {
        // Convert something like "my_custom_world" to "My Custom World"
        return Arrays.stream(worldName.replace('_', ' ').split(" "))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    @EventHandler
    public void onSpawnerListClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof SpawnerListHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!player.hasPermission("smartspawner.list")) {
            messageService.sendMessage(player, "no_permission");
            return;
        }
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        // Get current state
        String worldName = holder.getWorldName();
        int currentPage = holder.getCurrentPage();
        int totalPages = holder.getTotalPages();
        ListCommand.FilterOption currentFilter = holder.getFilterOption();
        ListCommand.SortOption currentSort = holder.getSortType();

        // Handle filter button click
        if (event.getSlot() == 48) {
            // Cycle to next filter option
            ListCommand.FilterOption nextFilter = currentFilter.getNextOption();

            // Save user preference when they change filter
            listCommand.saveUserPreference(player, worldName, nextFilter, currentSort);

            listCommand.openSpawnerListGUI(player, worldName, 1, nextFilter, currentSort);
            return;
        }

        // Handle sort button click
        if (event.getSlot() == 50) {
            // Cycle to next sort option
            ListCommand.SortOption nextSort = currentSort.getNextOption();

            // Save user preference when they change sort
            listCommand.saveUserPreference(player, worldName, currentFilter, nextSort);

            listCommand.openSpawnerListGUI(player, worldName, 1, currentFilter, nextSort);
            return;
        }

        // Handle navigation
        if (event.getSlot() == 45 && currentPage > 1) {
            // Previous page
            listCommand.openSpawnerListGUI(player, worldName, currentPage - 1, currentFilter, currentSort);
            return;
        }

        if (event.getSlot() == 49) {
            // Save preference before going back to world selection
            listCommand.saveUserPreference(player, worldName, currentFilter, currentSort);

            // Back to world selection
            listCommand.openWorldSelectionGUI(player);
            return;
        }

        if (event.getSlot() == 53 && currentPage < totalPages) {
            // Next page
            listCommand.openSpawnerListGUI(player, worldName, currentPage + 1, currentFilter, currentSort);
            return;
        }


        // Handle spawner item click (teleport functionality)
        if (isSpawnerItemSlot(event.getSlot()) && isSpawnerItem(event.getCurrentItem())) {
            handleSpawnerItemClick(player, event.getCurrentItem());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof SpawnerListHolder holder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        // Save user preferences when closing the inventory
        String worldName = holder.getWorldName();
        ListCommand.FilterOption currentFilter = holder.getFilterOption();
        ListCommand.SortOption currentSort = holder.getSortType();

        // Save preference when they close the GUI
        listCommand.saveUserPreference(player, worldName, currentFilter, currentSort);
    }

    private boolean isSpawnerItemSlot(int slot) {
        // Check if slot is in the spawner display area (first 5 rows, excluding borders)
        return slot < 45;
    }

    private boolean isSpawnerItem(ItemStack item) {
        // Check if item is a spawner or mob head (used for spawner display)
        return item != null && SPAWNER_MATERIALS.contains(item.getType()) &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName();
    }

    private void handleSpawnerItemClick(Player player, ItemStack item) {
        // Extract spawner ID from the item name
        String displayName = item.getItemMeta().getDisplayName();
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(displayName);

        if (matcher.find()) {
            String spawnerId = matcher.group(1);
            SpawnerData spawner = spawnerManager.getSpawnerById(spawnerId);

            if (spawner != null) {
                // Check if player has teleport permission
                if (player.hasPermission("smartspawner.list.teleport")) {
                    // Teleport player to spawner location
                    Location loc = spawner.getSpawnerLocation().clone().add(0.5, 1, 0.5);
                    player.teleportAsync(loc);
                    messageService.sendMessage(player, "teleported_to_spawner");
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                } else {
                    messageService.sendMessage(player, "no_permission_teleport");
                }
            } else {
                messageService.sendMessage(player, "spawner_not_found");
            }
        }
    }
}

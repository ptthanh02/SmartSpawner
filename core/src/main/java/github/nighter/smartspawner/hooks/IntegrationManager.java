package github.nighter.smartspawner.hooks;

import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.hooks.protections.api.Lands;
import github.nighter.smartspawner.hooks.protections.api.SuperiorSkyblock2;
import github.nighter.smartspawner.hooks.rpg.AuraSkillsIntegration;
import lombok.Getter;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider;
import fr.xyness.SCS.SimpleClaimSystem;

import java.util.logging.Level;

@Getter
public class IntegrationManager {
    private final SmartSpawner plugin;

    // Protection plugin flags
    private boolean hasTowny = false;
    private boolean hasLands = false;
    private boolean hasWorldGuard = false;
    private boolean hasGriefPrevention = false;
    private boolean hasSuperiorSkyblock2 = false;
    private boolean hasBentoBox = false;
    private boolean hasSimpleClaimSystem = false;
    private boolean hasRedProtect = false;
    private boolean hasMinePlots = false;

    // Integration plugin flags
    private boolean hasAuraSkills = false;

    // Integration instances
    public AuraSkillsIntegration auraSkillsIntegration;

    public IntegrationManager(SmartSpawner plugin) {
        this.plugin = plugin;
    }

    public void initializeIntegrations() {
        checkProtectionPlugins();
        checkIntegrationPlugins();
    }

    private void checkProtectionPlugins() {
        hasWorldGuard = checkPlugin("WorldGuard", () -> {
            Plugin worldGuardPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
            return worldGuardPlugin != null && worldGuardPlugin.isEnabled();
        }, true);

        hasGriefPrevention = checkPlugin("GriefPrevention", () -> {
            Plugin griefPlugin = Bukkit.getPluginManager().getPlugin("GriefPrevention");
            return griefPlugin instanceof GriefPrevention;
        }, true);

        hasLands = checkPlugin("Lands", () -> {
            Plugin landsPlugin = Bukkit.getPluginManager().getPlugin("Lands");
            if (landsPlugin != null) {
                new Lands(plugin);
                return true;
            }
            return false;
        }, true);

        hasTowny = checkPlugin("Towny", () -> {
            try {
                Class.forName("com.palmergames.bukkit.towny.TownyAPI");
                return com.palmergames.bukkit.towny.TownyAPI.getInstance() != null;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }, true);

        hasSuperiorSkyblock2 = checkPlugin("SuperiorSkyblock2", () -> {
            Plugin superiorSkyblock2 = Bukkit.getPluginManager().getPlugin("SuperiorSkyblock2");
            if(superiorSkyblock2 != null) {
                SuperiorSkyblock2 ssb2 = new SuperiorSkyblock2();
                Bukkit.getPluginManager().registerEvents(ssb2, plugin);
                return true;
            }
            return false;
        }, true);

        hasBentoBox = checkPlugin("BentoBox", () -> {
            Plugin bentoPlugin = Bukkit.getPluginManager().getPlugin("BentoBox");
            return bentoPlugin != null;
        }, true);

        hasSimpleClaimSystem = checkPlugin("SimpleClaimSystem", () -> {
            Plugin simpleClaimPlugin = Bukkit.getPluginManager().getPlugin("SimpleClaimSystem");
            if (simpleClaimPlugin == null || !simpleClaimPlugin.isEnabled()) {
                return false;
            }
            SimpleClaimSystemAPI_Provider.initialize((SimpleClaimSystem) simpleClaimPlugin);
            return SimpleClaimSystemAPI_Provider.getAPI() != null;
        }, true);

        hasRedProtect = checkPlugin("RedProtect", () -> {
            Plugin pRP = Bukkit.getPluginManager().getPlugin("RedProtect");
            return pRP != null && pRP.isEnabled();
        }, true);

        hasMinePlots = checkPlugin("MinePlots", () -> {
            Plugin pRP = Bukkit.getPluginManager().getPlugin("MinePlots");
            return pRP != null && pRP.isEnabled();
        }, true);
    }

    private void checkIntegrationPlugins() {
        hasAuraSkills = checkPlugin("AuraSkills", () -> {
            Plugin auraSkillsPlugin = Bukkit.getPluginManager().getPlugin("AuraSkills");
            if (auraSkillsPlugin != null && auraSkillsPlugin.isEnabled()) {
                this.auraSkillsIntegration = new AuraSkillsIntegration(plugin);
                return true;
            } else {
                this.auraSkillsIntegration = null;
                return false;
            }
        }, true);
    }

    private boolean checkPlugin(String pluginName, PluginCheck checker, boolean logSuccess) {
        try {
            if (checker.check()) {
                if (logSuccess) {
                    plugin.getLogger().info(pluginName + " integration enabled successfully!");
                }
                return true;
            }
        } catch (NoClassDefFoundError | NullPointerException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to initialize " + pluginName + " integration", e);
        }
        return false;
    }

    public void reload() {
        if (auraSkillsIntegration != null) {
            auraSkillsIntegration.reloadConfig();
        }
    }

    @FunctionalInterface
    private interface PluginCheck {
        boolean check();
    }
}
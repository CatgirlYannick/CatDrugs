package dev.catgirlyannick.catdrugs;

import dev.catgirlyannick.catdrugs.command.CatDrugsCommand;
import dev.catgirlyannick.catdrugs.config.ConfigManager;
import dev.catgirlyannick.catdrugs.config.DrugRegistry;
import dev.catgirlyannick.catdrugs.gui.DrugCatalogMenu;
import dev.catgirlyannick.catdrugs.integration.CatItemsAddonService;
import dev.catgirlyannick.catdrugs.integration.CatItemsBridge;
import dev.catgirlyannick.catdrugs.item.DrugItemFactory;
import dev.catgirlyannick.catdrugs.listener.GameplayListener;
import dev.catgirlyannick.catdrugs.listener.GuiListener;
import dev.catgirlyannick.catdrugs.service.ConsumptionService;
import dev.catgirlyannick.catdrugs.service.ConsumptionAnimationService;
import dev.catgirlyannick.catdrugs.service.AdvancedGameplayService;
import dev.catgirlyannick.catdrugs.service.DoseReactionService;
import dev.catgirlyannick.catdrugs.service.MessageService;
import dev.catgirlyannick.catdrugs.service.RecipeService;
import dev.catgirlyannick.catdrugs.service.RealisticEffectService;
import dev.catgirlyannick.catdrugs.service.VillageDealerService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class CatDrugsPlugin extends JavaPlugin {
    private ConfigManager configs;
    private DrugRegistry registry;
    private MessageService messages;
    private ConsumptionService consumption;
    private VillageDealerService dealers;
    private RecipeService recipes;
    private GameplayListener gameplayListener;
    private DrugCatalogMenu catalogMenu;
    private RealisticEffectService realisticEffects;
    private DoseReactionService doseReactions;
    private AdvancedGameplayService advancedGameplay;
    private ConsumptionAnimationService consumptionAnimations;

    @Override
    public void onEnable() {
        configs = new ConfigManager(this);
        registry = new DrugRegistry(this);
        if (!configs.load() || !registry.reload()) {
            getLogger().severe("CatDrugs was safely disabled because its configuration is invalid.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (!configs.main().getBoolean("general.enabled", true)) {
            getLogger().info("CatDrugs is disabled through general.enabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        messages = new MessageService(configs.messages());
        CatItemsBridge catItems = new CatItemsBridge(this);
        CatItemsAddonService catItemsAddon = new CatItemsAddonService(this, catItems);
        if (catItems.available() && configs.main().getBoolean("catitems.addon.auto-install", true)) {
            boolean overwrite = configs.main().getBoolean("catitems.addon.overwrite-customized-files", false);
            CatItemsAddonService.InstallResult result = catItemsAddon.install(registry.all(), overwrite);
            if (result.successful()) {
                getLogger().info("CatItems addon: wrote " + result.itemsWritten() + " item definitions and "
                        + result.assetsWritten() + " assets; " + result.conflicts().size() + " conflicts.");
                if (result.changed() && result.conflicts().isEmpty()
                        && configs.main().getBoolean("catitems.addon.rebuild-after-install", true)) {
                    if (!catItemsAddon.rebuild()) {
                        getLogger().warning("CatItems could not be reloaded after the addon installation.");
                    }
                }
            }
        }
        DrugItemFactory itemFactory = new DrugItemFactory(this, registry, catItems, messages);
        realisticEffects = new RealisticEffectService(this, messages, configs.realisticEffects());
        doseReactions = new DoseReactionService(this, messages, configs.main());
        dealers = new VillageDealerService(this, registry, itemFactory, messages, configs.survival());
        advancedGameplay = new AdvancedGameplayService(this, itemFactory, dealers, messages, configs.survival());
        consumptionAnimations = new ConsumptionAnimationService(this, catItems, configs.main());
        consumption = new ConsumptionService(this, messages, configs.main(), realisticEffects, doseReactions,
                advancedGameplay, consumptionAnimations);
        advancedGameplay.bindConsumption(consumption);
        recipes = new RecipeService(this, registry, itemFactory);
        gameplayListener = new GameplayListener(this, itemFactory, consumption, dealers, advancedGameplay,
                messages, configs.survival());
        catalogMenu = new DrugCatalogMenu(registry, itemFactory, messages, configs.gui());

        getServer().getPluginManager().registerEvents(gameplayListener, this);
        getServer().getPluginManager().registerEvents(advancedGameplay, this);
        getServer().getPluginManager().registerEvents(new GuiListener(itemFactory, messages, catalogMenu), this);

        CatDrugsCommand executor = new CatDrugsCommand(this, registry, itemFactory, messages, consumption,
                dealers, advancedGameplay, catItems, catItemsAddon, catalogMenu);
        PluginCommand command = Objects.requireNonNull(getCommand("catdrugs"), "catdrugs command missing from plugin.yml");
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        int recipeCount = recipes.reload(configs.survival());
        getServer().getScheduler().runTask(this, () -> getServer().getWorlds().forEach(world ->
                java.util.Arrays.stream(world.getLoadedChunks()).forEach(dealers::scanChunk)));
        getLogger().info("CatDrugs " + getPluginMeta().getVersion() + " is active: " + registry.consumables().size()
                + " substances, " + recipeCount + " recipes, CatItems="
                + catItemsAddon.status(registry.all()) + ", use animations="
                + consumptionAnimations.providerStatus() + ".");
    }

    public boolean reloadCatDrugs() {
        if (!configs.load() || !configs.main().getBoolean("general.enabled", true) || !registry.reload()) {
            return false;
        }
        messages.reload(configs.messages());
        realisticEffects.reload(configs.realisticEffects());
        doseReactions.reload(configs.main());
        consumption.reload(configs.main());
        dealers.reload(configs.survival());
        advancedGameplay.reload(configs.survival());
        gameplayListener.reload(configs.survival());
        catalogMenu.reload(configs.gui());
        recipes.reload(configs.survival());
        return true;
    }

    @Override
    public void onDisable() {
        if (realisticEffects != null) {
            realisticEffects.clearAll();
        }
        if (doseReactions != null) {
            doseReactions.clearAll();
        }
        if (advancedGameplay != null) {
            advancedGameplay.shutdown();
        }
    }
}

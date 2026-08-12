package dev.catgirlyannick.catdrugs.integration;

import dev.catgirlyannick.catdrugs.model.DrugDefinition;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class CatItemsBridge {
    private final JavaPlugin plugin;
    private Plugin cachedPlugin;
    private Object cachedProvider;
    private boolean providerResolved;
    private Method findMethod;
    private Method createMethod;
    private Method identifyMethod;
    private Method playAnimationMethod;
    private Method stopAnimationMethod;

    public CatItemsBridge(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean available() {
        return provider() != null;
    }

    public List<String> missingItems(Collection<DrugDefinition> definitions) {
        Object api = provider();
        if (api == null) {
            return configuredIds(definitions);
        }
        return definitions.stream()
                .filter(DrugDefinition::enabled)
                .map(DrugDefinition::customItemId)
                .filter(id -> !id.isBlank())
                .filter(id -> invokeOptional(api, findMethod, id).isEmpty())
                .sorted()
                .toList();
    }

    public Optional<ItemStack> createItem(String namespacedId, int amount) {
        if (namespacedId == null || namespacedId.isBlank()) {
            return Optional.empty();
        }
        Object api = provider();
        if (api == null) {
            return Optional.empty();
        }
        try {
            if (createMethod == null) {
                return Optional.empty();
            }
            Object result = createMethod.invoke(api, namespacedId, amount);
            return result instanceof ItemStack itemStack ? Optional.of(itemStack) : Optional.empty();
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            plugin.getLogger().warning("CatItems item could not be loaded ('" + namespacedId + "'): "
                    + rootMessage(exception));
            return Optional.empty();
        }
    }

    public Optional<String> identify(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Optional.empty();
        }
        Object api = provider();
        if (api == null) {
            return Optional.empty();
        }
        Optional<?> result = invokeOptional(api, identifyMethod, item);
        return result.filter(String.class::isInstance).map(String.class::cast);
    }

    public boolean supportsUseAnimations() {
        return provider() != null && playAnimationMethod != null;
    }

    public boolean playUseAnimation(Player player, String preset, int durationTicks) {
        Object api = provider();
        if (api == null || playAnimationMethod == null) {
            return false;
        }
        try {
            Object result = playAnimationMethod.invoke(api, player, preset, durationTicks);
            return result instanceof Boolean played && played;
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            return false;
        }
    }

    public void stopUseAnimation(Player player) {
        Object api = provider();
        if (api == null || stopAnimationMethod == null) {
            return;
        }
        try {
            stopAnimationMethod.invoke(api, player);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException ignored) {
            // Older CatItems builds have no animation API; the CatDrugs fallback needs no explicit stop.
        }
    }

    public String status(Collection<DrugDefinition> definitions) {
        if (!available()) {
            return "not installed (vanilla fallback)";
        }
        List<String> missing = missingItems(definitions);
        return missing.isEmpty() ? "ready" : "active, " + missing.size() + " IDs missing";
    }

    private Object provider() {
        if (providerResolved && cachedPlugin != null) {
            if (cachedPlugin.isEnabled()) {
                return cachedProvider;
            }
            clearCache();
        }
        Plugin catItems = Bukkit.getPluginManager().getPlugin("CatItems");
        if (catItems == null || !catItems.isEnabled()) {
            return null;
        }
        clearCache();
        cachedPlugin = catItems;
        providerResolved = true;
        try {
            Class<?> apiClass = Class.forName("dev.catgirlyannick.catitems.api.CatItemsApi", true,
                    catItems.getClass().getClassLoader());
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object provider = Bukkit.getServicesManager().load((Class) apiClass);
            if (provider == null) {
                return null;
            }
            cachedProvider = provider;
            Class<?> providerClass = provider.getClass();
            findMethod = method(providerClass, "find", String.class);
            createMethod = method(providerClass, "create", String.class, int.class);
            identifyMethod = method(providerClass, "identify", ItemStack.class);
            playAnimationMethod = method(providerClass, "playUseAnimation", Player.class, String.class, int.class);
            stopAnimationMethod = method(providerClass, "stopUseAnimation", Player.class);
            return cachedProvider;
        } catch (ClassNotFoundException | LinkageError exception) {
            plugin.getLogger().warning("CatItems is active but does not provide a compatible CatItemsApi.");
            return null;
        }
    }

    private Optional<?> invokeOptional(Object target, Method method, Object... arguments) {
        if (method == null) {
            return Optional.empty();
        }
        try {
            Object result = method.invoke(target, arguments);
            return result instanceof Optional<?> optional ? optional : Optional.empty();
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    private static Method method(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException | RuntimeException exception) {
            return null;
        }
    }

    private void clearCache() {
        cachedPlugin = null;
        cachedProvider = null;
        providerResolved = false;
        findMethod = null;
        createMethod = null;
        identifyMethod = null;
        playAnimationMethod = null;
        stopAnimationMethod = null;
    }

    private List<String> configuredIds(Collection<DrugDefinition> definitions) {
        return definitions.stream().filter(DrugDefinition::enabled).map(DrugDefinition::customItemId)
                .filter(id -> !id.isBlank()).sorted().toList();
    }

    private String rootMessage(Exception exception) {
        Throwable cause = exception instanceof InvocationTargetException invocation && invocation.getCause() != null
                ? invocation.getCause() : exception;
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}

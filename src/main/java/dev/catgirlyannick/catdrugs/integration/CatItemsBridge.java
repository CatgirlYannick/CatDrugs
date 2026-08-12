package dev.catgirlyannick.catdrugs.integration;

import dev.catgirlyannick.catdrugs.model.DrugDefinition;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class CatItemsBridge {
    private final JavaPlugin plugin;

    public CatItemsBridge(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean available() {
        return provider().isPresent();
    }

    public List<String> missingItems(Collection<DrugDefinition> definitions) {
        Object api = provider().orElse(null);
        if (api == null) {
            return configuredIds(definitions);
        }
        return definitions.stream()
                .filter(DrugDefinition::enabled)
                .map(DrugDefinition::customItemId)
                .filter(id -> !id.isBlank())
                .filter(id -> invokeOptional(api, "find", new Class<?>[]{String.class}, id).isEmpty())
                .sorted()
                .toList();
    }

    public Optional<ItemStack> createItem(String namespacedId, int amount) {
        if (namespacedId == null || namespacedId.isBlank()) {
            return Optional.empty();
        }
        Object api = provider().orElse(null);
        if (api == null) {
            return Optional.empty();
        }
        try {
            Object result = api.getClass().getMethod("create", String.class, int.class)
                    .invoke(api, namespacedId, amount);
            return result instanceof ItemStack itemStack ? Optional.of(itemStack) : Optional.empty();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            plugin.getLogger().warning("CatItems item could not be loaded ('" + namespacedId + "'): "
                    + rootMessage(exception));
            return Optional.empty();
        }
    }

    public Optional<String> identify(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Optional.empty();
        }
        Object api = provider().orElse(null);
        if (api == null) {
            return Optional.empty();
        }
        Optional<?> result = invokeOptional(api, "identify", new Class<?>[]{ItemStack.class}, item);
        return result.filter(String.class::isInstance).map(String.class::cast);
    }

    public String status(Collection<DrugDefinition> definitions) {
        if (!available()) {
            return "not installed (vanilla fallback)";
        }
        List<String> missing = missingItems(definitions);
        return missing.isEmpty() ? "ready" : "active, " + missing.size() + " IDs missing";
    }

    private Optional<Object> provider() {
        Plugin catItems = Bukkit.getPluginManager().getPlugin("CatItems");
        if (catItems == null || !catItems.isEnabled()) {
            return Optional.empty();
        }
        try {
            Class<?> apiClass = Class.forName("dev.catgirlyannick.catitems.api.CatItemsApi", true,
                    catItems.getClass().getClassLoader());
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object provider = Bukkit.getServicesManager().load((Class) apiClass);
            return Optional.ofNullable(provider);
        } catch (ClassNotFoundException | LinkageError exception) {
            plugin.getLogger().warning("CatItems is active but does not provide a compatible CatItemsApi.");
            return Optional.empty();
        }
    }

    private Optional<?> invokeOptional(Object target, String method, Class<?>[] types, Object... arguments) {
        try {
            Object result = target.getClass().getMethod(method, types).invoke(target, arguments);
            return result instanceof Optional<?> optional ? optional : Optional.empty();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            return Optional.empty();
        }
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

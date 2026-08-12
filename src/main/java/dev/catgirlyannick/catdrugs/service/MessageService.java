package dev.catgirlyannick.catdrugs.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class MessageService {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final YamlConfiguration bundledDefaults;
    private YamlConfiguration config;

    public MessageService(YamlConfiguration config) {
        this.config = config;
        this.bundledDefaults = loadBundledDefaults();
    }

    public void reload(YamlConfiguration config) {
        this.config = config;
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Map.of());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(component(path, placeholders));
    }

    public Component component(String path, Map<String, String> placeholders) {
        String prefix = resolve("prefix",
                "<dark_gray>[<light_purple>CatDrugs</light_purple>]</dark_gray> ");
        String value = resolve(path, "<red>Missing message: " + path + "</red>");
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", escape(entry.getValue()));
        }
        return miniMessage.deserialize(prefix + value);
    }

    String resolve(String path, String finalFallback) {
        String configured = config.getString(path);
        if (configured != null) {
            return configured;
        }
        return bundledDefaults.getString(path, finalFallback);
    }

    public Component raw(String text) {
        return miniMessage.deserialize(text);
    }

    private String escape(String value) {
        return value.replace("<", "\\<").replace(">", "\\>");
    }

    private YamlConfiguration loadBundledDefaults() {
        InputStream stream = MessageService.class.getClassLoader().getResourceAsStream("messages.yml");
        if (stream == null) {
            return new YamlConfiguration();
        }
        try (InputStream input = stream;
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (java.io.IOException exception) {
            return new YamlConfiguration();
        }
    }
}

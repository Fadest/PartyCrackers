package dev.fadest.partycrackers.utils;

import dev.fadest.partycrackers.PartyCrackersPlugin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

@Getter
@AllArgsConstructor
public enum Messages {

    COMMAND_HELP("command.help",
            """
                    <gradient:#03dbfc:#035afc>Party Crackers</gradient> <gray>help
                                
                    <#035afc>/partycracker give <#03dbfc><player> <type> <#035afc>(amount) <dark_gray>» <gray>Give a player a certain amount of a Party Cracker type
                    <#035afc>/partycracker list <dark_gray>» <gray>List all Party Crackers
                    <#035afc>/partycracker reload <dark_gray>» <gray>Reload the messages and party crackers (WARNING: using this command will forcefully end the current party crackers)
                    <#035afc>/partycracker info <#03dbfc><type> <dark_gray>» <gray>Get information about a Party Cracker type
                    """
    ),
    COMMAND_ERROR_NOT_PERMISSIONS("command.error.not-permission", "<red>You don't have permissions to use this command"),
    COMMAND_ERROR_PLAYER_NOT_FOUND("command.error.player-not-found", "<red>{player} is not online"),
    COMMAND_ERROR_PARTY_CRACKER_NOT_FOUND("command.error.party-cracker-not-found", "<red>The <gradient:#03dbfc:#035afc>Party Crackers</gradient> <gray>'{id}' <red>does not exists."),

    COMMAND_GIVE_USAGE("command.give.usage", "<red>Correct Usage: <#035afc>/partycracker give <#03dbfc><player> <type> <#035afc>(amount)"),

    COMMAND_GIVE_GIVEN("command.give.given", "<gray>You gave x<dark_aqua>{amount} <white>{party_cracker} <gray>party cracker(s) to <green>{player}"),
    COMMAND_GIVE_RECEIVED("command.give.received", "<gray>You received x<dark_aqua>{amount} <white>{party_cracker} <gray>party cracker(s)"),
    COMMAND_RELOAD_RELOADED("command.reload.reloaded", "<gray>Messages and Party Crackers have been reloaded (<green>{loaded} loaded<gray>, <red>{failed} failed<gray>)"),
    COMMAND_PARTY_CRACKER_LIST("command.party-cracker-list",
            """
                    <gray>Available <gradient:#03dbfc:#035afc>Party Crackers</gradient> <gray>(<#9334eb>{party_crackers}<gray>)
                                        
                    <gray><hover:show_text:'<gray>Click to get more information about this Party Cracker<newline><newline><gray>Name: <#9334eb>{name}<newline><gray>Seconds to explode: <#9334eb>{seconds_to_explode}<newline><gray>Ticking Sound: <#9334eb>{ticking_sound}<newline><gray>Explosion Sounds: <#9334eb>{explosion_sounds}<newline><gray>Explosion Particles: <#9334eb>{explosion_particles}<newline><gray>Rewards: <#9334eb>{rewards}'><click:run_command:/partycracker info {id}><gradient:#9334eb:#eb34ae>{id}</gradient></click></hover>
                    """
    ),
    COMMAND_INFORMATION_USAGE("command.information.usage", "<red>Correct Usage: <#035afc>/partycracker info <#03dbfc><type>"),
    COMMAND_INFORMATION_INFORMATION("command.information.information",
            """
                    <gradient:#03dbfc:#035afc>{id}</gradient>
                                        
                    <gray>Name: <#035afc>{name}
                    <gray>Seconds to explode: <#035afc>{seconds_to_explode}
                    <gray>Ticking Sound: <#035afc>{ticking_sound}
                    <gray>Explosion Sounds: <#035afc>{explosion_sounds}
                    <gray>Explosion Particles: <#035afc>{explosion_particles}
                    <gray>Rewards: <#035afc>{reward_list}
                    """
    ),
    ERROR_DROP_PARTY_CRACKER_NOT_FOUND("error.drop.not-found", "<red>An error has occurred when dropping your party cracker, contact an admin or wait few seconds before trying again"),
    ERROR_DROP_PARTY_CRACKER_COOL_DOWN("error.drop.cooldown", "<red>You need to wait until your last party cracker has exploded before throwing another one");


    /**
     * The MiniMessage deserializer for the messages
     */
    public final static MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .tags(TagResolver.builder()
                    .resolver(StandardTags.defaults())
                    .resolvers(TagResolver.resolver("res", Tag.preProcessParsed(
                            "<b:false><i:false><u:false><st:false><obf:false>"
                    )))
                    .build())
            .build();

    private final String node;
    @Setter
    private String message;

    /**
     * Returns the provided Message text parsed into MiniMessage with placeholders
     *
     * @param placeholders The placeholders to replace the message with
     * @return The message parsed with MiniMessage tags
     */
    public static String toMessage(@NotNull String message, @NotNull Map<String, Object> placeholders) {
        String baseMessage = legacyToMiniMessage(message);

        // Placeholders
        for (Map.Entry<String, Object> placeholder : placeholders.entrySet()) {
            String toReplace = Matcher.quoteReplacement(placeholder.getKey());

            String parsedPlaceholder;

            if (placeholder.getValue() instanceof Component component) {
                parsedPlaceholder = MINI_MESSAGE.serialize(component);
            } else {
                parsedPlaceholder = legacyToMiniMessage(placeholder.getValue().toString());
            }

            baseMessage = baseMessage.replaceAll("\\{" + toReplace + "\\}", parsedPlaceholder);
        }

        return baseMessage;
    }

    /**
     * Returns the provided Message text parsed into a MiniMessage Component without placeholders
     *
     * @return The message parsed with MiniMessage tags
     */
    public static Component toComponent(@NotNull String message) {
        return toComponent(message, new HashMap<>());
    }

    /**
     * Returns the provided Message text parsed into a MiniMessage Component with placeholders
     *
     * @param placeholders The placeholders to replace the message with
     * @return The message parsed with MiniMessage tags
     */
    public static Component toComponent(@NotNull String message, @NotNull Map<String, Object> placeholders) {
        return MINI_MESSAGE.deserialize(toMessage(message, placeholders));
    }

    /**
     * Convert all legacy color codes to MiniMessage tags
     *
     * @param text The text to convert
     * @return The text with the color codes converted
     */
    private static String legacyToMiniMessage(String text) {
        text = replaceLegacyCode(text, "r", "<res>");
        text = replaceLegacyCode(text, "k", "<obfuscated>");
        text = replaceLegacyCode(text, "l", "<bold>");
        text = replaceLegacyCode(text, "m", "<strikethrough>");
        text = replaceLegacyCode(text, "n", "<underlined>");
        text = replaceLegacyCode(text, "o", "<italic>");
        text = replaceLegacyCode(text, "0", "<res><black>");
        text = replaceLegacyCode(text, "1", "<res><dark_blue>");
        text = replaceLegacyCode(text, "2", "<res><dark_green>");
        text = replaceLegacyCode(text, "3", "<res><dark_aqua>");
        text = replaceLegacyCode(text, "4", "<res><dark_red>");
        text = replaceLegacyCode(text, "5", "<res><dark_purple>");
        text = replaceLegacyCode(text, "6", "<res><gold>");
        text = replaceLegacyCode(text, "7", "<res><gray>");
        text = replaceLegacyCode(text, "8", "<res><dark_gray>");
        text = replaceLegacyCode(text, "9", "<res><blue>");
        text = replaceLegacyCode(text, "a", "<res><green>");
        text = replaceLegacyCode(text, "b", "<res><aqua>");
        text = replaceLegacyCode(text, "c", "<res><red>");
        text = replaceLegacyCode(text, "d", "<res><light_purple>");
        text = replaceLegacyCode(text, "e", "<res><yellow>");
        text = replaceLegacyCode(text, "f", "<res><white>");

        return text;
    }

    /**
     * Replace all the legacy codes in the provided text
     *
     * @param text        Input text
     * @param character   Legacy code
     * @param replacement What to replace with
     * @return "Sanitised" legacy string
     */
    private static String replaceLegacyCode(String text, String character, String replacement) {
        String lowerCaseCharacter = character.toLowerCase(Locale.ROOT);
        String upperCaseCharacter = character.toUpperCase(Locale.ROOT);

        // We need to replace both lowercase and uppercase versions of the character since Adventure may be using
        // the legacy codes in a case-insensitive manner

        return text.replaceAll("&" + lowerCaseCharacter, replacement)
                .replaceAll("&" + upperCaseCharacter, replacement)
                .replaceAll("§" + lowerCaseCharacter, replacement)
                .replaceAll("§" + upperCaseCharacter, replacement);
    }

    /**
     * Reload all the messages
     */
    public static void reload() {
        try {
            final File file = new File(PartyCrackersPlugin.getInstance().getDataFolder(), "messages.yml");
            if (!file.exists()) {
                file.createNewFile();
            }

            final FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);
            boolean needToSave = false;
            for (Messages message : Messages.values()) {
                final String node = message.getNode();

                if (fileConfiguration.isSet(node)) {
                    if (fileConfiguration.isString(node)) {
                        message.setMessage(fileConfiguration.getString(node));
                        continue;
                    }

                    message.setMessage(String.join("\n", fileConfiguration.getStringList(node)));
                    continue;
                }

                needToSave = true;

                if (!message.getMessage().contains("\n")) {
                    fileConfiguration.set(node, message.getMessage());
                    continue;
                }

                fileConfiguration.set(node, message.getMessage().split("\\n"));
            }

            if (needToSave)
                fileConfiguration.save(file);
        } catch (IOException ignored) {

        }

    }

    /**
     * Returns this Message text parsed into MiniMessage without placeholders
     *
     * @return The message parsed with MiniMessage tags
     */
    public String toMessage() {
        return toMessage(new HashMap<>());
    }

    /**
     * Returns this Message text parsed into MiniMessage with placeholders
     *
     * @param placeholders The placeholders to replace the message with
     * @return The message parsed with MiniMessage tags
     */
    public String toMessage(@NotNull Map<String, Object> placeholders) {
        return toMessage(this.message, placeholders);
    }

    /**
     * Returns this Message text parsed into a MiniMessage Component without placeholders
     *
     * @return The message parsed with MiniMessage tags
     */
    public Component toComponent() {
        return toComponent(this.message);
    }

    /**
     * Returns this Message text parsed into a MiniMessage Component with placeholders
     *
     * @param placeholders The placeholders to replace the message with
     * @return The message parsed with MiniMessage tags
     */
    public Component toComponent(@NotNull Map<String, Object> placeholders) {
        return toComponent(this.message, placeholders);
    }

}

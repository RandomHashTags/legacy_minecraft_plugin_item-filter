package me.randomhashtags.itemfilter.universal;

import com.sun.istack.internal.NotNull;
import me.randomhashtags.itemfilter.ItemFilterSpigot;
import me.randomhashtags.itemfilter.addon.FilterCategory;
import me.randomhashtags.itemfilter.util.Versionable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.util.*;

public interface UVersionable extends Versionable {
    HashMap<String, FilterCategory> FILTER_CATEGORIES = new HashMap<>();
    File DATA_FOLDER = ItemFilterSpigot.getPlugin.getDataFolder();
    String SEPARATOR = File.separator;

    ItemFilterSpigot ITEM_FILTER = ItemFilterSpigot.getPlugin;
    FileConfiguration ITEM_FILTER_CONFIG = ITEM_FILTER.getConfig();
    PluginManager PLUGIN_MANAGER = Bukkit.getPluginManager();
    Server SERVER = Bukkit.getServer();
    Random RANDOM = new Random();

    BukkitScheduler SCHEDULER = Bukkit.getScheduler();
    ConsoleCommandSender CONSOLE = Bukkit.getConsoleSender();

    HashMap<FileConfiguration, HashMap<String, List<String>>> FEATURE_MESSAGES = new HashMap<>();
    HashMap<FileConfiguration, HashMap<String, String>> FEATURE_STRINGS = new HashMap<>();

    default void registerFilterCategory(@NotNull FilterCategory category) {
        FILTER_CATEGORIES.put(category.getIdentifier(), category);
    }
    default FilterCategory getFilterCategory(@NotNull String categoryIdentifier) {
        return FILTER_CATEGORIES.getOrDefault(categoryIdentifier, null);
    }

    default List<String> getStringList(FileConfiguration yml, String identifier) {
        if(!FEATURE_MESSAGES.containsKey(yml)) {
            FEATURE_MESSAGES.put(yml, new HashMap<>());
        }
        final HashMap<String, List<String>> messages = FEATURE_MESSAGES.get(yml);
        if(!messages.containsKey(identifier)) {
            messages.put(identifier, colorizeListString(yml.getStringList(identifier)));
        }
        return messages.get(identifier);
    }
    default String getString(FileConfiguration yml, String identifier) {
        if(!FEATURE_STRINGS.containsKey(yml)) {
            FEATURE_STRINGS.put(yml, new HashMap<>());
        }
        final HashMap<String, String> strings = FEATURE_STRINGS.get(yml);
        if(!strings.containsKey(identifier)) {
            strings.put(identifier, colorize(yml.getString(identifier)));
        }
        return strings.get(identifier);
    }


    default void save(String folder, String file) {
        final boolean hasFolder = folder != null && !folder.equals("");
        final File f = new File(DATA_FOLDER + SEPARATOR + (hasFolder ? folder + SEPARATOR : ""), file);
        if(!f.exists()) {
            f.getParentFile().mkdirs();
            ITEM_FILTER.saveResource(hasFolder ? folder + SEPARATOR + file : file, false);
        }
    }

    default ItemStack getClone(ItemStack is) {
        return getClone(is, null);
    }
    default ItemStack getClone(ItemStack is, ItemStack def) {
        return is != null ? is.clone() : def;
    }

    default File[] getFilesInFolder(@NotNull String folder) {
        final File f = new File(folder);
        return f.exists() ? f.listFiles() : new File[]{};
    }
    default HashSet<String> getConfigurationSectionKeys(FileConfiguration yml, String key, boolean includeKeys, String...excluding) {
        final ConfigurationSection section = yml.getConfigurationSection(key);
        if(section != null) {
            final HashSet<String> set = new HashSet<>(section.getKeys(includeKeys));
            set.removeAll(Arrays.asList(excluding));
            return set;
        } else {
            return new HashSet<>();
        }
    }

    default void sendConsoleMessage(String msg) {
        CONSOLE.sendMessage(colorize(msg));
    }
    default void sendConsoleDidLoadFeature(String what, long started) {
        sendConsoleMessage("&6[Item Filter] &aLoaded " + what + " &e(took " + (System.currentTimeMillis()-started) + "ms)");
    }

    default int getRemainingInt(String string) {
        string = ChatColor.stripColor(colorize(string)).replaceAll("\\p{L}", "").replaceAll("\\s", "").replaceAll("\\p{P}", "").replaceAll("\\p{S}", "");
        return string.isEmpty() ? -1 : Integer.parseInt(string);
    }

    default List<String> colorizeListString(List<String> input) {
        final List<String> i = new ArrayList<>();
        if(input != null) {
            for(String s : input) {
                i.add(colorize(s));
            }
        }
        return i;
    }
    default String colorize(String input) {
        return input != null ? ChatColor.translateAlternateColorCodes('&', input) : "NULL";
    }
    default void sendStringListMessage(CommandSender sender, List<String> message, HashMap<String, String> replacements) {
        if(message != null && message.size() > 0 && !message.get(0).equals("")) {
            final boolean papi = ITEM_FILTER.placeholderapi, isPlayer = sender instanceof Player;
            final Player player = isPlayer ? (Player) sender : null;
            for(String s : message) {
                if(replacements != null) {
                    for(String r : replacements.keySet()) {
                        final String replacement = replacements.get(r);
                        s = s.replace(r, replacement != null ? replacement : "null");
                    }
                }
                if(s != null) {
                    if(papi && isPlayer) {
                        s = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, s);
                    }
                    sender.sendMessage(colorize(s));
                }
            }
        }
    }

    default ItemStack createItemStack(FileConfiguration config, String path) {
        ItemStack item = new ItemStack(Material.APPLE);
        if(config == null && path != null || config != null && config.get(path + ".item") != null) {
            final String itemPath = config == null ? path : config.getString(path + ".item");
            String itemPathLC = itemPath.toLowerCase();

            int amount = config != null && config.get(path + ".amount") != null ? config.getInt(path + ".amount") : 1;
            if(itemPathLC.contains(";amount=")) {
                final String amountString = itemPathLC.split("=")[1];
                final boolean isRange = itemPathLC.contains("-");
                final int min = isRange ? Integer.parseInt(amountString.split("-")[0]) : 0;
                amount = isRange ? min+RANDOM.nextInt(Integer.parseInt(amountString.split("-")[1])-min+1) : Integer.parseInt(amountString);
                path = path.split(";amount=")[0];
                itemPathLC = itemPathLC.split(";")[0];
            }
            final boolean hasChance = itemPathLC.contains("chance=");
            if(hasChance && RANDOM.nextInt(100) > Integer.parseInt(itemPathLC.split("chance=")[1].split(";")[0])) {
                return null;
            }

            String name = config != null ? config.getString(path + ".name") : null;
            final String[] material = itemPathLC.toUpperCase().split(":");
            final String mat = material[0];
            final byte data = material.length == 2 ? Byte.parseByte(material[1]) : 0;
            final UMaterial umaterial = UMaterial.match(mat + (data != 0 ? ":" + data : ""));
            try {
                item = umaterial.getItemStack();
            } catch (Exception e) {
                System.out.println("UMaterial null itemstack. mat=" + mat + ";data=" + data + ";versionName=" + (umaterial != null ? umaterial.getVersionName() : null) + ";getMaterial()=" + (umaterial != null ? umaterial.getMaterial() : null));
                return null;
            }

            final ItemMeta itemMeta = item.getItemMeta();
            final List<String> lore = config != null ? colorizeListString(config.getStringList(path + ".lore")) : null;
            final HashMap<Enchantment, Integer> enchants = new HashMap<>();
            if(lore != null) {
                final List<String> l = new ArrayList<>();
                for(String s : lore) {
                    if(s.toLowerCase().startsWith("enchants{")) {
                        final String[] values = s.split("\\{")[1].split("}")[0].split(";");
                        for(String enchant : values) {
                            final Enchantment enchantment = getEnchantment(enchant);
                            if(enchantment != null) {
                                enchants.put(enchantment, getRemainingInt(enchant));
                            }
                        }
                    } else {
                        l.add(s);
                    }
                }
                itemMeta.setLore(l);
            }

            if(!item.getType().equals(Material.AIR)) {
                item.setAmount(amount);
                itemMeta.setDisplayName(name != null ? colorize(name) : null);
                item.setItemMeta(itemMeta);
                for(Enchantment enchant : enchants.keySet()) {
                    item.addUnsafeEnchantment(enchant, enchants.get(enchant));
                }
            }
        }
        return item;
    }

    default Enchantment getEnchantment(@NotNull String string) {
        if(string != null) {
            string = string.toLowerCase().replace("_", "");
            for(Enchantment enchant : Enchantment.values()) {
                final String name = enchant != null ? enchant.getName() : null;
                if(name != null && string.startsWith(name.toLowerCase().replace("_", ""))) {
                    return enchant;
                }
            }
            if(string.startsWith("po")) { return Enchantment.ARROW_DAMAGE; // Power
            } else if(string.startsWith("fl")) { return Enchantment.ARROW_FIRE; // Flame
            } else if(string.startsWith("i")) { return Enchantment.ARROW_INFINITE; // Infinity
            } else if(string.startsWith("pu")) { return Enchantment.ARROW_KNOCKBACK; // Punch
            } else if(string.startsWith("bi") && !EIGHT && !NINE && !TEN) { return Enchantment.getByName("BINDING_CURSE"); // Binding Curse
            } else if(string.startsWith("sh")) { return Enchantment.DAMAGE_ALL; // Sharpness
            } else if(string.startsWith("ba")) { return Enchantment.DAMAGE_ARTHROPODS; // Bane of Arthropods
            } else if(string.startsWith("sm")) { return Enchantment.DAMAGE_UNDEAD; // Smite
            } else if(string.startsWith("de")) { return Enchantment.DEPTH_STRIDER; // Depth Strider
            } else if(string.startsWith("e")) { return Enchantment.DIG_SPEED; // Efficiency
            } else if(string.startsWith("u")) { return Enchantment.DURABILITY; // Unbreaking
            } else if(string.startsWith("firea")) { return Enchantment.FIRE_ASPECT; // Fire Aspect
            } else if(string.startsWith("fr") && !EIGHT) { return Enchantment.getByName("FROST_WALKER"); // Frost Walker
            } else if(string.startsWith("k")) { return Enchantment.KNOCKBACK; // Knockback
            } else if(string.startsWith("fo")) { return Enchantment.LOOT_BONUS_BLOCKS; // Fortune
            } else if(string.startsWith("lo")) { return Enchantment.LOOT_BONUS_MOBS; // Looting
            } else if(string.startsWith("luc")) { return Enchantment.LUCK; // Luck
            } else if(string.startsWith("lur")) { return Enchantment.LURE; // Lure
            } else if(string.startsWith("m") && !EIGHT) { return Enchantment.getByName("MENDING"); // Mending
            } else if(string.startsWith("r")) { return Enchantment.OXYGEN; // Respiration
            } else if(string.startsWith("prot")) { return Enchantment.PROTECTION_ENVIRONMENTAL; // Protection
            } else if(string.startsWith("bl") || string.startsWith("bp")) { return Enchantment.PROTECTION_EXPLOSIONS; // Blast Protection
            } else if(string.startsWith("ff") || string.startsWith("fe")) { return Enchantment.PROTECTION_FALL; // Feather Falling
            } else if(string.startsWith("fp") || string.startsWith("firep")) { return Enchantment.PROTECTION_FIRE; // Fire Protection
            } else if(string.startsWith("pp") || string.startsWith("proj")) { return Enchantment.PROTECTION_PROJECTILE; // Projectile Protection
            } else if(string.startsWith("si")) { return Enchantment.SILK_TOUCH; // Silk Touch
            } else if(string.startsWith("th")) { return Enchantment.THORNS; // Thorns
            } else if(string.startsWith("v") && !EIGHT && !NINE && !TEN) { return Enchantment.getByName("VANISHING_CURSE"); // Vanishing Curse
            } else if(string.startsWith("aa") || string.startsWith("aq")) { return Enchantment.WATER_WORKER; // Aqua Affinity
            } else { return null; }
        }
        return null;
    }

}

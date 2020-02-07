package me.randomhashtags.itemfilter.addon;

import me.randomhashtags.itemfilter.universal.UInventory;
import org.bukkit.inventory.Inventory;

import java.io.File;

public class FileFilterCategory extends FilterAddon implements FilterCategory {
    private UInventory gui;
    public FileFilterCategory(File f) {
        load(f);
        register(Feature.FILTER_CATEGORY, this);
    }
    public String getIdentifier() { return getYamlName(); }

    public String getTitle() {
        return colorize(yml.getString("title"));
    }
    public UInventory getInventory() {
        if(gui == null) {
            gui = new UInventory(null, yml.getInt("size"), getTitle());
            final Inventory i = gui.getInventory();
            for(String s : yml.getConfigurationSection("gui").getKeys(false)) {
                i.setItem(yml.getInt("gui." + s + ".slot"), createItemStack(yml, "gui." + s));
            }
        }
        return gui;
    }
}

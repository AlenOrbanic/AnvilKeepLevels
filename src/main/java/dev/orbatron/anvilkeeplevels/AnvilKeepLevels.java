package dev.orbatron.anvilkeeplevels;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public class AnvilKeepLevels extends JavaPlugin implements Listener {

    private boolean allowConflicts;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        allowConflicts = getConfig().getBoolean("allow-conflicts", false);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);
        ItemStack result = event.getResult();

        if (left == null || right == null || result == null) return;
        if (left.getType().isAir() || right.getType().isAir() || result.getType().isAir()) return;

        Map<Enchantment, Integer> rightEnchants = readEnchants(right);
        if (rightEnchants.isEmpty()) return;

        boolean resultIsBook = result.getItemMeta() instanceof EnchantmentStorageMeta;

        // Start from what the left item already has.
        Map<Enchantment, Integer> desired = new LinkedHashMap<>(readEnchants(left));

        for (Map.Entry<Enchantment, Integer> entry : rightEnchants.entrySet()) {
            Enchantment ench = entry.getKey();
            int level = entry.getValue();

            Integer existing = desired.get(ench);
            if (existing != null) {
                // Same enchant on both sides: keep the higher level, never add one.
                desired.put(ench, Math.max(existing, level));
                continue;
            }

            // Skip enchants that don't fit the item (books accept everything).
            if (!resultIsBook && !ench.canEnchantItem(result)) continue;

            // Skip conflicting enchants unless allowed.
            if (!allowConflicts && conflictsWithAny(ench, desired)) continue;

            desired.put(ench, level);
        }

        ItemStack newResult = result.clone();
        ItemMeta meta = newResult.getItemMeta();
        if (meta == null) return;

        // Clear existing vanilla enchants, then write the desired ones.
        if (meta instanceof EnchantmentStorageMeta esm) {
            for (Enchantment e : Map.copyOf(esm.getStoredEnchants()).keySet()) {
                esm.removeStoredEnchant(e);
            }
            for (Map.Entry<Enchantment, Integer> e : desired.entrySet()) {
                esm.addStoredEnchant(e.getKey(), e.getValue(), true);
            }
        } else {
            for (Enchantment e : Map.copyOf(meta.getEnchants()).keySet()) {
                meta.removeEnchant(e);
            }
            for (Map.Entry<Enchantment, Integer> e : desired.entrySet()) {
                meta.addEnchant(e.getKey(), e.getValue(), true);
            }
        }

        newResult.setItemMeta(meta);
        event.setResult(newResult);
    }

    private Map<Enchantment, Integer> readEnchants(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta esm) {
            return new LinkedHashMap<>(esm.getStoredEnchants());
        }
        return new LinkedHashMap<>(item.getEnchantments());
    }

    private boolean conflictsWithAny(Enchantment ench, Map<Enchantment, Integer> current) {
        for (Enchantment other : current.keySet()) {
            if (!other.equals(ench) && ench.conflictsWith(other)) return true;
        }
        return false;
    }
}
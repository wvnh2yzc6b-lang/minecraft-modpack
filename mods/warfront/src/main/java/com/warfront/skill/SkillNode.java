package com.warfront.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One node of a skill path, read from {@code data/<ns>/warfront_skill_paths/<path>.json}. Passive nodes carry
 * vanilla (or Iron's Spells) attribute modifiers and tuning numbers that Warfront's event hooks read; active
 * nodes go in a skill slot and have a cooldown (and maybe charges).
 *
 * @param parents any one of these unlocks the node (empty for a path's root)
 */
public record SkillNode(String id, String path, String name, String desc, int col, int row, List<String> parents,
                        int cost, boolean active, int cooldown, int charges, int duration,
                        List<AttributeBonus> attributes, Map<String, Double> values) {

    /** An attribute modifier a passive node adds while it is learned. Unknown attributes are skipped. */
    public record AttributeBonus(String attribute, double amount, String operation) {}

    public double value(String key, double fallback) {
        Double v = values.get(key);
        return v == null ? fallback : v;
    }

    public boolean isRoot() {
        return parents.isEmpty();
    }

    public static SkillNode parse(String path, JsonObject o) {
        List<String> parents = new ArrayList<>();
        for (JsonElement e : GsonHelper.getAsJsonArray(o, "parents", new JsonArray())) parents.add(e.getAsString());
        JsonArray pos = GsonHelper.getAsJsonArray(o, "pos");
        List<AttributeBonus> attrs = new ArrayList<>();
        for (JsonElement e : GsonHelper.getAsJsonArray(o, "attributes", new JsonArray())) {
            JsonObject a = e.getAsJsonObject();
            attrs.add(new AttributeBonus(GsonHelper.getAsString(a, "attribute"), GsonHelper.getAsDouble(a, "amount"),
                    GsonHelper.getAsString(a, "operation", "add_value")));
        }
        Map<String, Double> values = new LinkedHashMap<>();
        if (o.has("values")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("values").entrySet()) values.put(e.getKey(), e.getValue().getAsDouble());
        }
        return new SkillNode(GsonHelper.getAsString(o, "id"), path, GsonHelper.getAsString(o, "name"),
                GsonHelper.getAsString(o, "desc", ""), pos.get(0).getAsInt(), pos.get(1).getAsInt(), List.copyOf(parents),
                GsonHelper.getAsInt(o, "cost", 1), GsonHelper.getAsBoolean(o, "active", false),
                GsonHelper.getAsInt(o, "cooldown", 0), Math.max(1, GsonHelper.getAsInt(o, "charges", 1)),
                GsonHelper.getAsInt(o, "duration", 0), List.copyOf(attrs), Map.copyOf(values));
    }

    /** One line for the client: id|path|name|desc|col|row|parent,parent|cost|active|cooldown|charges. */
    public String encode() {
        return String.join("|", id, path, name, desc.replace('|', '/'), Integer.toString(col), Integer.toString(row),
                String.join(",", parents), Integer.toString(cost), active ? "1" : "0", Integer.toString(cooldown),
                Integer.toString(charges));
    }

    public static SkillNode decode(String line) {
        String[] f = line.split("\\|", -1);
        List<String> parents = f[6].isEmpty() ? List.of() : List.of(f[6].split(","));
        return new SkillNode(f[0], f[1], f[2], f[3], Integer.parseInt(f[4]), Integer.parseInt(f[5]), parents,
                Integer.parseInt(f[7]), f[8].equals("1"), Integer.parseInt(f[9]), Integer.parseInt(f[10]), 0, List.of(), Map.of());
    }
}

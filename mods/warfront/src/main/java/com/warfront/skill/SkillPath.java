package com.warfront.skill;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.warfront.faction.Race;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * A skill path: Blade, Bulwark, Marksman, Arcane, Skirmisher, Commander (shared by all races). Each race favors
 * two of them ({@code favored_by}): its first few nodes there cost nothing.
 */
public record SkillPath(String id, String name, int order, int color, String icon, String description,
                        List<String> favoredBy, List<SkillNode> nodes) {

    public boolean favors(Race race) {
        return race != null && favoredBy.contains(race.id());
    }

    public static SkillPath parse(String id, JsonObject o) {
        List<String> favored = new ArrayList<>();
        if (o.has("favored_by")) for (JsonElement e : o.getAsJsonArray("favored_by")) favored.add(e.getAsString());
        List<SkillNode> nodes = new ArrayList<>();
        for (JsonElement e : GsonHelper.getAsJsonArray(o, "nodes")) nodes.add(SkillNode.parse(id, e.getAsJsonObject()));
        String color = GsonHelper.getAsString(o, "color", "#C9A45C").replace("#", "");
        return new SkillPath(id, GsonHelper.getAsString(o, "name", id), GsonHelper.getAsInt(o, "order", 99),
                Integer.parseInt(color, 16), GsonHelper.getAsString(o, "icon", "minecraft:book"),
                GsonHelper.getAsString(o, "description", ""), List.copyOf(favored), List.copyOf(nodes));
    }

    /** One header line for the client: id|name|order|color|icon|description|race,race. */
    public String encode() {
        return String.join("|", id, name, Integer.toString(order), Integer.toString(color), icon,
                description.replace('|', '/'), String.join(",", favoredBy));
    }

    public static SkillPath decode(String line, List<SkillNode> nodes) {
        String[] f = line.split("\\|", -1);
        return new SkillPath(f[0], f[1], Integer.parseInt(f[2]), Integer.parseInt(f[3]), f[4], f[5],
                f[6].isEmpty() ? List.of() : List.of(f[6].split(",")), nodes);
    }
}

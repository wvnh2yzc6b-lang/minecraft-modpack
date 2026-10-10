package com.warfront.skill;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.warfront.Warfront;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The skill paths, loaded from data packs ({@code data/<ns>/warfront_skill_paths/*.json}) so nodes can be tuned
 * without code. The client keeps its own copy, sent with the Skill Tree screen.
 */
public final class SkillRegistry extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static volatile List<SkillPath> paths = List.of();
    private static volatile Map<String, SkillNode> nodes = Map.of();

    public SkillRegistry() {
        super(GSON, "warfront_skill_paths");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        List<SkillPath> loaded = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
            try {
                loaded.add(SkillPath.parse(e.getKey().getPath(), e.getValue().getAsJsonObject()));
            } catch (RuntimeException ex) {
                Warfront.LOGGER.error("Warfront: bad skill path {}: {}", e.getKey(), ex.getMessage());
            }
        }
        set(loaded);
        Warfront.LOGGER.info("Warfront: loaded {} skill paths with {} nodes", paths.size(), nodes.size());
    }

    /** Replaces the known paths (data reload on the server, the screen's copy on the client). */
    public static void set(List<SkillPath> list) {
        List<SkillPath> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparingInt(SkillPath::order).thenComparing(SkillPath::id));
        Map<String, SkillNode> byId = new LinkedHashMap<>();
        for (SkillPath p : sorted) for (SkillNode n : p.nodes()) byId.put(n.id(), n);
        paths = List.copyOf(sorted);
        nodes = byId;
    }

    public static List<SkillPath> paths() {
        return paths;
    }

    @Nullable
    public static SkillNode node(String id) {
        return nodes.get(id);
    }

    @Nullable
    public static SkillPath path(String id) {
        for (SkillPath p : paths) if (p.id().equals(id)) return p;
        return null;
    }

    public static Iterable<SkillNode> allNodes() {
        return nodes.values();
    }

    /** Everything the client needs to draw the tree: a path line, then its node lines, path by path. */
    public static List<String> encode() {
        List<String> out = new ArrayList<>();
        for (SkillPath p : paths) {
            out.add("P|" + p.encode());
            for (SkillNode n : p.nodes()) out.add("N|" + n.encode());
        }
        return out;
    }

    public static List<SkillPath> decode(List<String> lines) {
        List<SkillPath> out = new ArrayList<>();
        String header = null;
        List<SkillNode> current = new ArrayList<>();
        for (String line : lines) {
            if (line.startsWith("P|")) {
                if (header != null) out.add(SkillPath.decode(header, List.copyOf(current)));
                header = line.substring(2);
                current = new ArrayList<>();
            } else if (line.startsWith("N|")) {
                current.add(SkillNode.decode(line.substring(2)));
            }
        }
        if (header != null) out.add(SkillPath.decode(header, List.copyOf(current)));
        return out;
    }
}

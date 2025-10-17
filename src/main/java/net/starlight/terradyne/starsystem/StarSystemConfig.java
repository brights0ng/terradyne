package net.starlight.terradyne.starsystem;

import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for a star system loaded from datapack
 * Lightweight data structure before building full StarSystemModel
 */
public class StarSystemConfig {

    private final Identifier identifier;
    private final String name;
    private final HierarchyNode rootStar;

    public StarSystemConfig(Identifier identifier, String name, HierarchyNode rootStar) {
        this.identifier = identifier;
        this.name = name;
        this.rootStar = rootStar;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }

    public HierarchyNode getRootStar() {
        return rootStar;
    }

    /**
     * Get all celestial objects in this star system (flattened)
     */
    public List<Identifier> getAllObjects() {
        List<Identifier> objects = new ArrayList<>();
        collectObjects(rootStar, objects);
        return objects;
    }

    private void collectObjects(HierarchyNode node, List<Identifier> objects) {
        objects.add(node.objectId);
        for (HierarchyNode child : node.orbiting) {
            collectObjects(child, objects);
        }
    }

    /**
     * Node in the orbital hierarchy
     */
    public static class HierarchyNode {
        public final Identifier objectId;
        public final List<HierarchyNode> orbiting;

        public HierarchyNode(Identifier objectId) {
            this.objectId = objectId;
            this.orbiting = new ArrayList<>();
        }

        public void addOrbiting(HierarchyNode child) {
            orbiting.add(child);
        }
    }
}

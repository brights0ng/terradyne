// Update PlanetModelRegistry.java
package net.starlight.terradyne.planet.physics;

import net.minecraft.util.Identifier;
import net.starlight.terradyne.Terradyne;
import net.starlight.terradyne.planet.physics.PlanetModel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class PlanetModelRegistry {
    private static final Map<Identifier, PlanetModel> PLANET_MODELS = new ConcurrentHashMap<>();

    public static void register(Identifier dimensionId, PlanetModel planetModel) {
        PLANET_MODELS.put(dimensionId, planetModel);
        Terradyne.LOGGER.debug("Registered planet model for: {}", dimensionId);
    }

    public static PlanetModel get(Identifier dimensionId) {
        return PLANET_MODELS.get(dimensionId);
    }

    public static boolean isRegistered(Identifier dimensionId) {
        return PLANET_MODELS.containsKey(dimensionId);
    }

    public static void clear() {
        Terradyne.LOGGER.debug("Clearing planet model registry");
        PLANET_MODELS.clear();
    }

    // Debug method
    public static int size() {
        return PLANET_MODELS.size();
    }
}
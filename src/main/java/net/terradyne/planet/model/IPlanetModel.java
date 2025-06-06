package net.terradyne.planet.model;

import net.terradyne.planet.PlanetType;
import net.terradyne.planet.config.IPlanetConfig;

public interface IPlanetModel {
    IPlanetConfig getConfig();
    PlanetType getType();
    float getGravity();
    float getAtmosphericPressure();

    // === UNIVERSAL TERRAIN PROPERTIES ===
    float getErosionRate();                        // How fast terrain changes
    boolean hasLooseMaterialFormations();          // Generic version of "hasDunes"
    float getLooseMaterialFormationHeight();       // How tall formations can get
    float getSolidMaterialExposure();              // How much bedrock/solid material shows
}
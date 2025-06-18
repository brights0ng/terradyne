// DimensionTypeProvider.java
package net.starlight.terradyne.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;

import net.starlight.terradyne.planet.world.PlanetDimensionType;

import java.util.concurrent.CompletableFuture;

/**
 * Generates dimension type JSON files using Fabric's data generation system
 */
public class DimensionTypeProvider extends FabricDynamicRegistryProvider {

    public DimensionTypeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
        System.out.println("Configuring Terradyne dimension types...");

        // Register our planet dimension type
        entries.add(PlanetDimensionType.PLANET_DIMENSION_TYPE,
                PlanetDimensionType.createPlanetDimensionType());

        System.out.println("Added planet dimension type: " + PlanetDimensionType.PLANET_DIMENSION_TYPE.getValue());
    }

    @Override
    public String getName() {
        return "Terradyne Dimension Types";
    }
}
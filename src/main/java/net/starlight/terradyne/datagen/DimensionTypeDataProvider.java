package net.starlight.terradyne.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.dimension.DimensionType;
import net.starlight.terradyne.planet.dimension.DimensionTypeFactory;
import net.starlight.terradyne.planet.dimension.ModDimensionTypes;

import java.util.concurrent.CompletableFuture;

/**
 * Generates dimension type JSON files using Fabric's data generation system
 * This creates the JSON files that Minecraft loads at startup
 */
public class DimensionTypeDataProvider extends FabricDynamicRegistryProvider {

    public DimensionTypeDataProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
        // Register each custom dimension type
        
        // HABITABLE dimension type
        entries.add(ModDimensionTypes.TERRADYNE_HABITABLE, DimensionTypeFactory.createHabitableDimension());
        
        // ULTRAWARM dimension type
        entries.add(ModDimensionTypes.TERRADYNE_ULTRAWARM, DimensionTypeFactory.createUltrawarmDimension());
        
        // THICK_ATMOSPHERE dimension type
        entries.add(ModDimensionTypes.TERRADYNE_THICK_ATMOSPHERE, DimensionTypeFactory.createThickAtmosphereDimension());
        
        // STANDARD dimension type
        entries.add(ModDimensionTypes.TERRADYNE_STANDARD, DimensionTypeFactory.createStandardDimension());
        
        // TOXIC dimension type
        entries.add(ModDimensionTypes.TERRADYNE_TOXIC, DimensionTypeFactory.createToxicDimension());
    }

    @Override
    public String getName() {
        return "Terradyne Dimension Types";
    }
}
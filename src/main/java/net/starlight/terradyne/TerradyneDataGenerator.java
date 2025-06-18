package net.starlight.terradyne;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.starlight.terradyne.datagen.DimensionTypeProvider;

/**
 * Data generation for Terradyne mod.
 * Handles registration of dimension types and other registry content.
 */
public class TerradyneDataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        System.out.println("Initializing Terradyne data generation...");

        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

        // Add our dimension type provider
        pack.addProvider(DimensionTypeProvider::new);

        System.out.println("Added Terradyne dimension type provider");
    }
}
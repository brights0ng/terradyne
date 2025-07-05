// Clean BiomeColorsMixin.java
package net.starlight.terradyne.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.biome.ColorResolver;
import net.starlight.terradyne.planet.biology.VegetationPalette;
import net.starlight.terradyne.planet.physics.PlanetModel;
import net.starlight.terradyne.planet.physics.PlanetModelRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(BiomeColors.class)
public class BiomeColorsMixin {

    @Inject(method = "getColor", at = @At("HEAD"), cancellable = true)
    private static void getAtmosphericColor(BlockRenderView world, BlockPos pos, ColorResolver resolver,
                                            CallbackInfoReturnable<Integer> cir) {
        try {
            PlanetModel planetModel = getPlanetModelFromWorld(world);
            if (planetModel != null) {
                var atmosphere = planetModel.getConfig().getAtmosphereComposition();
                VegetationPalette palette = VegetationPalette.fromAtmosphereComposition(atmosphere);

                if (resolver == BiomeColors.GRASS_COLOR) {
                    cir.setReturnValue(palette.getGrassColor(null));
                } else if (resolver == BiomeColors.FOLIAGE_COLOR) {
                    cir.setReturnValue(palette.getFoliageColor(null));
                }
            }
        } catch (Exception e) {
            // Silently fall back to vanilla
        }
    }

    private static PlanetModel getPlanetModelFromWorld(BlockRenderView world) {
        try {
            net.minecraft.world.World actualWorld = null;

            if (world instanceof net.minecraft.world.World directWorld) {
                actualWorld = directWorld;
            } else if (world instanceof net.minecraft.client.render.chunk.ChunkRendererRegion chunkRegion) {
                try {
                    java.lang.reflect.Field worldField = chunkRegion.getClass().getDeclaredField("world");
                    worldField.setAccessible(true);
                    actualWorld = (net.minecraft.world.World) worldField.get(chunkRegion);
                } catch (Exception e) {
                    for (java.lang.reflect.Field field : chunkRegion.getClass().getDeclaredFields()) {
                        if (net.minecraft.world.World.class.isAssignableFrom(field.getType())) {
                            field.setAccessible(true);
                            actualWorld = (net.minecraft.world.World) field.get(chunkRegion);
                            break;
                        }
                    }
                }
            }

            if (actualWorld != null) {
                var dimensionKey = actualWorld.getRegistryKey();
                if (dimensionKey.getValue().getNamespace().equals("terradyne")) {
                    return PlanetModelRegistry.get(dimensionKey.getValue());
                }
            }
        } catch (Exception e) {
            // Silently fail
        }
        return null;
    }
}
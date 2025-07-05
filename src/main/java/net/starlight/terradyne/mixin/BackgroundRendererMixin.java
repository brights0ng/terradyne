// Updated BackgroundRendererMixin.java - handle both fog types
package net.starlight.terradyne.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.starlight.terradyne.planet.physics.PlanetModel;
import net.starlight.terradyne.planet.physics.PlanetModelRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {

    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void applyAtmosphericFog(Camera camera, BackgroundRenderer.FogType fogType,
                                            float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        try {
            System.out.println("FOG DEBUG: applyFog TAIL called with fogType=" + fogType);

            ClientWorld world = (ClientWorld) camera.getFocusedEntity().getWorld();
            PlanetModel planetModel = getPlanetModelFromWorld(world);

            if (planetModel != null) {
                var atmosphere = planetModel.getConfig().getAtmosphereComposition();

                // Handle both fog types
                if (fogType == BackgroundRenderer.FogType.FOG_SKY) {
                    System.out.println("FOG DEBUG: Applying sky fog");
                    float[] skyFogColor = getAtmosphericFogColor(atmosphere, true); // true = sky fog
                    com.mojang.blaze3d.systems.RenderSystem.setShaderFogColor(
                            skyFogColor[0], skyFogColor[1], skyFogColor[2]
                    );
                } else if (fogType == BackgroundRenderer.FogType.FOG_TERRAIN) {
                    System.out.println("FOG DEBUG: Applying terrain fog");
                    float[] terrainFogColor = getAtmosphericFogColor(atmosphere, false); // false = terrain fog
                    com.mojang.blaze3d.systems.RenderSystem.setShaderFogColor(
                            terrainFogColor[0], terrainFogColor[1], terrainFogColor[2]
                    );
                }
            }
        } catch (Exception e) {
            System.out.println("FOG DEBUG: Exception in applyFog: " + e.getMessage());
        }
    }

    private static float[] getAtmosphericFogColor(net.starlight.terradyne.planet.physics.AtmosphereComposition atmosphere,
                                                  boolean isSkyFog) {
        // Sky fog should blend more with sky color, terrain fog can be different
        float intensity = isSkyFog ? 0.9f : 0.7f; // Sky fog more intense

        return switch (atmosphere) {
            case OXYGEN_RICH -> new float[]{0.7f * intensity, 0.8f * intensity, 1.0f * intensity};
            case CARBON_DIOXIDE -> new float[]{1.0f * intensity, 0.6f * intensity, 0.3f * intensity};
            case NITROGEN_RICH -> new float[]{0.8f * intensity, 0.7f * intensity, 0.9f * intensity};
            case METHANE -> new float[]{0.9f * intensity, 0.7f * intensity, 0.4f * intensity};
            case WATER_VAPOR_RICH -> new float[]{0.9f * intensity, 0.9f * intensity, 0.9f * intensity};
            case HYDROGEN_SULFIDE -> new float[]{1.0f * intensity, 1.0f * intensity, 0.6f * intensity};
            case NOBLE_GAS_MIXTURE -> new float[]{0.9f * intensity, 0.9f * intensity, 1.0f * intensity};
            case TRACE_ATMOSPHERE -> new float[]{0.4f * intensity, 0.4f * intensity, 0.4f * intensity};
            case VACUUM -> new float[]{0.1f * intensity, 0.1f * intensity, 0.1f * intensity};
        };
    }

    private static PlanetModel getPlanetModelFromWorld(ClientWorld world) {
        try {
            var dimensionKey = world.getRegistryKey();
            if (dimensionKey.getValue().getNamespace().equals("terradyne")) {
                return PlanetModelRegistry.get(dimensionKey.getValue());
            }
        } catch (Exception e) {
            // Silently fail
        }
        return null;
    }
}
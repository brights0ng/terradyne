// src/main/java/net/starlight/terradyne/mixin/ClientWorldSkyMixin.java
package net.starlight.terradyne.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HeightLimitView;
import net.starlight.terradyne.planet.physics.PlanetModel;
import net.starlight.terradyne.planet.physics.PlanetModelRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ClientWorld.class)
public class ClientWorldSkyMixin {

    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void getAtmosphericSkyColor(Vec3d cameraPos, float tickDelta, 
                                      CallbackInfoReturnable<Vec3d> cir) {
        try {
            ClientWorld world = (ClientWorld) (Object) this;
            PlanetModel planetModel = getPlanetModelFromWorld(world);
            
            if (planetModel != null) {
                var atmosphere = planetModel.getConfig().getAtmosphereComposition();
                Vec3d atmosphericSkyColor = calculateAtmosphericSkyColor(atmosphere, tickDelta);
                cir.setReturnValue(atmosphericSkyColor);
            }
        } catch (Exception e) {
            // Silently fall back to vanilla
        }
    }

    @Unique
    private PlanetModel getPlanetModelFromWorld(ClientWorld world) {
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

    @Unique
    private Vec3d calculateAtmosphericSkyColor(net.starlight.terradyne.planet.physics.AtmosphereComposition atmosphere, float tickDelta) {
        // Use your existing atmospheric color logic from BiomeDataProvider
        int skyColorInt = getAtmosphericSkyColor(atmosphere, 15.0f); // Use default temperature for now
        
        // Convert int color to Vec3d (RGB components 0.0-1.0)
        float red = ((skyColorInt >> 16) & 0xFF) / 255.0f;
        float green = ((skyColorInt >> 8) & 0xFF) / 255.0f;
        float blue = (skyColorInt & 0xFF) / 255.0f;
        
        return new Vec3d(red, green, blue);
    }

    @Unique
    private int getAtmosphericSkyColor(net.starlight.terradyne.planet.physics.AtmosphereComposition atmosphereComposition, float temperature) {
        // Copy this logic from your BiomeDataProvider.getAtmosphericSkyColor() method
        int baseColor = switch (atmosphereComposition) {
            case OXYGEN_RICH -> 0x78A7FF;           // Earth-like blue
            case CARBON_DIOXIDE -> 0xFFA500;        // Orange (greenhouse/Mars-like)
            case NITROGEN_RICH -> 0x9370DB;         // Purple-blue (nitrogen dominance)
            case METHANE -> 0xD2691E;               // Orange-brown (Titan-like)
            case WATER_VAPOR_RICH -> 0xF5F5DC;     // Pale/white (very cloudy steam)
            case HYDROGEN_SULFIDE -> 0xB8860B;     // Yellow-brown (sulfurous)
            case NOBLE_GAS_MIXTURE -> 0xE6E6FA;    // Very pale lavender (inert/clear)
            case TRACE_ATMOSPHERE -> 0x2F2F2F;     // Dark gray (thin atmosphere)
            case VACUUM -> 0x000000;               // Black (space)
        };

        // Apply temperature variation if needed
        if (temperature > 40) {
            return tintColor(baseColor, 0xFFB347, 0.2f);
        } else if (temperature < -20) {
            return tintColor(baseColor, 0x87CEEB, 0.15f);
        }

        return baseColor;
    }

    @Unique
    private int tintColor(int baseColor, int tintColor, float factor) {
        int baseR = (baseColor >> 16) & 0xFF;
        int baseG = (baseColor >> 8) & 0xFF;
        int baseB = baseColor & 0xFF;

        int tintR = (tintColor >> 16) & 0xFF;
        int tintG = (tintColor >> 8) & 0xFF;
        int tintB = tintColor & 0xFF;

        int resultR = (int) (baseR * (1 - factor) + tintR * factor);
        int resultG = (int) (baseG * (1 - factor) + tintG * factor);
        int resultB = (int) (baseB * (1 - factor) + tintB * factor);

        return (resultR << 16) | (resultG << 8) | resultB;
    }

    @Inject(method = "getCloudsColor", at = @At("HEAD"), cancellable = true)
    private void getAtmosphericCloudsColor(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        try {
            ClientWorld world = (ClientWorld) (Object) this;
            PlanetModel planetModel = getPlanetModelFromWorld(world);

            if (planetModel != null) {
                var atmosphere = planetModel.getConfig().getAtmosphereComposition();
                Vec3d atmosphericCloudsColor = calculateAtmosphericSkyColor(atmosphere, tickDelta);
                cir.setReturnValue(atmosphericCloudsColor);
                System.out.println("CLOUDS DEBUG: Set atmospheric clouds color");
            }
        } catch (Exception e) {
            System.out.println("CLOUDS DEBUG: Exception: " + e.getMessage());
        }
    }
}
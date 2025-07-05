// src/main/java/net/starlight/terradyne/mixin/WorldRendererMixin.java
package net.starlight.terradyne.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.starlight.terradyne.planet.physics.PlanetModel;
import net.starlight.terradyne.planet.physics.PlanetModelRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Shadow private ClientWorld world;

    // Try targeting renderSky method
    @Inject(method = "renderSky", at = @At("HEAD"))
    private void onRenderSkyStart(MatrixStack matrices, org.joml.Matrix4f projectionMatrix, float tickDelta,
                                  net.minecraft.client.render.Camera camera, boolean bl, Runnable runnable, CallbackInfo ci) {
        System.out.println("SKY DEBUG: WorldRenderer.renderSky called");

        PlanetModel planetModel = getPlanetModelFromWorld(world);
        if (planetModel != null) {
            System.out.println("SKY DEBUG: Found planet model in WorldRenderer: " + planetModel.getConfig().getAtmosphereComposition());
        }
    }

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
}
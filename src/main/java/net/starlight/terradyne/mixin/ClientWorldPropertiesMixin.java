// Create new file: ClientWorldPropertiesMixin.java
package net.starlight.terradyne.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.starlight.terradyne.planet.physics.PlanetModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ClientWorld.Properties.class)
public class ClientWorldPropertiesMixin {

//    @Inject(method = "getHorizonShadingRatio", at = @At("HEAD"), cancellable = true)
//    private void getAtmosphericHorizonShading(CallbackInfoReturnable<Float> cir) {
//        try {
//            System.out.println("HORIZON DEBUG: getHorizonShadingRatio called");
//
//            // Try to get the world context - this might be tricky
//            // We need to access the parent ClientWorld somehow
//
//            // For now, let's test with a fixed value
//            cir.setReturnValue(0.0f); // 0.0 = no horizon shading, 1.0 = full shading
//            System.out.println("HORIZON DEBUG: Set horizon shading to 0.0");
//
//        } catch (Exception e) {
//            System.out.println("HORIZON DEBUG: Exception: " + e.getMessage());
//        }
//    }

//    @Inject(method = "getSkyDarknessHeight", at = @At("HEAD"), cancellable = true)
//    private void getAtmosphericSkyDarknessHeight(CallbackInfoReturnable<Double> cir) {
//        try {
//            System.out.println("SKY DARKNESS DEBUG: getSkyDarknessHeight called");
//
//            // Test with different values
//            cir.setReturnValue(63.0); // Default Minecraft sea level
//            System.out.println("SKY DARKNESS DEBUG: Set sky darkness height to 63.0");
//
//        } catch (Exception e) {
//            System.out.println("SKY DARKNESS DEBUG: Exception: " + e.getMessage());
//        }
//    }
}
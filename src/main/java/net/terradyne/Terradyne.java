package net.terradyne; // Assuming the package remains net.terradyne

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.terradyne.planet.biome.ModBiomes;
import net.terradyne.util.CommandRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Terradyne implements ModInitializer { // Class name changed
	public static final String MOD_ID = "terradyne";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			CommandRegistry.init(dispatcher);
		});

		ModBiomes.init();

		LOGGER.info("Terradyne initialized!");	}
}
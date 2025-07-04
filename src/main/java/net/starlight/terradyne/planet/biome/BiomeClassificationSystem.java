package net.starlight.terradyne.planet.biome;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.starlight.terradyne.planet.physics.PlanetModel;

/**
 * Implements the complete Terradyne biome classification system
 * Uses physics data to determine appropriate biome for any location
 */
public class BiomeClassificationSystem {

    private final PlanetModel planetModel;
    
    public BiomeClassificationSystem(PlanetModel planetModel) {
        this.planetModel = planetModel;
    }

    /**
     * Classify biome at world coordinates using complete physics-based decision tree
     */
    public RegistryKey<Biome> classifyBiome(int worldX, int worldZ) {
        // Sample all physics data for this location
        double terrainHeight = planetModel.getTerrainHeight(worldX, worldZ);
        double seaLevel = planetModel.getPlanetData().getSeaLevel();
        double temperature = planetModel.getTemperature(worldX, worldZ);
        double humidity = planetModel.getMoisture(worldX, worldZ);
        double habitability = planetModel.getPlanetData().getHabitability();
        int volatility = planetModel.getVolatilityAt(worldX, worldZ);
        double windSpeed = planetModel.getNoiseSystem().sampleWindSpeed(worldX, worldZ);
        double elevationAboveSeaLevel = terrainHeight - seaLevel;

        // === EXTREME TEMPERATURE OVERRIDES ===
        if (temperature < -50.0) {
            return ModBiomes.EXTREME_FROZEN_WASTELAND;
        }
        if (temperature > 100.0) {
            return ModBiomes.MOLTEN_WASTELAND;
        }

        // === PRIMARY CLASSIFICATION: WATER VS LAND ===
        if (terrainHeight <= seaLevel) {
            return classifyWaterBiome(temperature, habitability);
        } else {
            return classifyLandBiome(temperature, humidity, habitability, volatility,
                                   windSpeed, elevationAboveSeaLevel);
        }
    }
    
    /**
     * Classify water biomes based on temperature and habitability
     */
    private RegistryKey<Biome> classifyWaterBiome(double temperature, double habitability) {
        if (temperature < -10.0) {
            return ModBiomes.FROZEN_OCEAN;
        }
        
        if (temperature < 5.0) {
            return ModBiomes.FRIGID_OCEAN;
        }
        
        if (temperature <= 25.0) {
            if (habitability < 0.3) {
                return ModBiomes.DEAD_OCEAN;
            } else if (habitability < 0.6) {
                return ModBiomes.OCEAN;
            } else if (habitability < 0.9) {
                return ModBiomes.WARM_OCEAN;
            } else {
                return ModBiomes.CORAL_OCEAN;
            }
        }
        
        if (temperature <= 60.0) {
            return ModBiomes.TROPICAL_OCEAN;
        }
        
        return ModBiomes.BOILING_OCEAN;
    }
    
    /**
     * Classify land biomes using volatility-based hierarchy
     */
    private RegistryKey<Biome> classifyLandBiome(double temperature, double humidity, double habitability,
                                                int volatility, double windSpeed, double elevation) {
        
        // === EXTREME ELEVATION OVERRIDE ===
        if (elevation > 150.0) {
            return classifyMountainBiome(temperature, elevation, habitability);
        }
        
        // === VOLATILITY-BASED CLASSIFICATION ===
        if (volatility >= 4) {
            // High volatility = Mountain biomes
            return classifyMountainBiome(temperature, elevation, habitability);
        } else if (volatility >= 2) {
            // Moderate volatility = Highland biomes
            return classifyHighlandBiome(temperature, humidity, habitability);
        } else {
            // Low volatility = Continental biomes
            return classifyContinentalBiome(temperature, humidity, habitability, windSpeed, elevation);
        }
    }
    
    /**
     * Classify mountain biomes (volatility 4-5 or extreme elevation)
     */
    private RegistryKey<Biome> classifyMountainBiome(double temperature, double elevation, double habitability) {
        if (temperature < -15.0) {
            return ModBiomes.FROZEN_PEAKS;
        }
        
        if (temperature <= 25.0) {
            if (elevation < 30.0) {
                return ModBiomes.MOUNTAIN_FOOTHILLS;
            } else if (elevation < 80.0) {
                return ModBiomes.MOUNTAIN_PEAKS;
            } else {
                return ModBiomes.ALPINE_PEAKS;
            }
        }
        
        // Hot mountains
        if (habitability < 0.4) {
            return ModBiomes.VOLCANIC_WASTELAND;
        } else {
            return ModBiomes.VOLCANIC_MOUNTAINS;
        }
    }
    
    /**
     * Classify highland biomes (volatility 2-3)
     */
    private RegistryKey<Biome> classifyHighlandBiome(double temperature, double humidity, double habitability) {
        if (habitability < 0.4) {
            return ModBiomes.BARREN_HIGHLANDS;
        }
        
        if (habitability <= 0.7) {
            if (humidity < 0.4) {
                return ModBiomes.WINDSWEPT_HILLS;
            } else {
                return ModBiomes.ROLLING_HILLS;
            }
        }
        
        // High habitability highlands
        if (temperature < 5.0) {
            return ModBiomes.HIGHLAND_TUNDRA;
        } else if (temperature <= 25.0) {
            return ModBiomes.FORESTED_HILLS;
        } else {
            return ModBiomes.TROPICAL_HIGHLANDS;
        }
    }
    
    /**
     * Classify continental biomes (volatility 0-1) - most complex classification
     */
    private RegistryKey<Biome> classifyContinentalBiome(double temperature, double humidity, double habitability,
                                                       double windSpeed, double elevation) {
        
        if (habitability < 0.4) {
            return classifyHostileContinental(temperature, windSpeed, elevation);
        } else if (habitability <= 0.7) {
            return classifyMarginalContinental(temperature, humidity);
        } else {
            return classifyThrivingContinental(temperature, humidity, windSpeed, elevation);
        }
    }
    
    /**
     * Classify hostile continental biomes (habitability < 0.4)
     */
    private RegistryKey<Biome> classifyHostileContinental(double temperature, double windSpeed, double elevation) {
        if (windSpeed < 0.3) {
            if (temperature < -20.0) {
                return ModBiomes.FROZEN_WASTELAND;
            } else if (temperature <= 40.0) {
                return ModBiomes.ROCKY_DESERT;
            } else {
                return ModBiomes.SCORCHED_PLAINS;
            }
        } else {
            // High wind speed
            if (temperature < 0.0) {
                return ModBiomes.WINDSWEPT_TUNDRA;
            } else if (temperature <= 40.0) {
                if (elevation < 40.0) {
                    return ModBiomes.SANDY_DESERT;
                } else {
                    return ModBiomes.DESERT_MESA;
                }
            } else {
                return ModBiomes.DUST_BOWL;
            }
        }
    }
    
    /**
     * Classify marginal continental biomes (habitability 0.4-0.7)
     */
    private RegistryKey<Biome> classifyMarginalContinental(double temperature, double humidity) {
        if (temperature < -10.0) {
            return ModBiomes.COLD_STEPPES;
        }
        
        if (temperature <= 5.0) {
            if (humidity < 0.4) {
                return ModBiomes.TUNDRA;
            } else {
                return ModBiomes.BOREAL_PLAINS;
            }
        }
        
        if (temperature <= 30.0) {
            if (humidity < 0.3) {
                return ModBiomes.DRY_STEPPES;
            } else if (humidity <= 0.6) {
                return ModBiomes.TEMPERATE_STEPPES;
            } else {
                return ModBiomes.MEADOWS;
            }
        }
        
        // Hot marginal
        if (humidity < 0.4) {
            return ModBiomes.SAVANNA;
        } else {
            return ModBiomes.TROPICAL_GRASSLAND;
        }
    }
    
    /**
     * Classify thriving continental biomes (habitability > 0.7) - most diverse
     */
    private RegistryKey<Biome> classifyThrivingContinental(double temperature, double humidity, 
                                                          double windSpeed, double elevation) {
        
        if (temperature < 0.0) {
            return classifyThrivingCold(humidity, elevation);
        } else if (temperature <= 25.0) {
            return classifyThrivingTemperate(humidity, elevation);
        } else if (temperature <= 40.0) {
            return classifyThrivingWarm(humidity, windSpeed, elevation);
        } else {
            return classifyThrivingHot(humidity);
        }
    }
    
    /**
     * Cold thriving biomes (temp < 0°C, habitability > 0.7)
     */
    private RegistryKey<Biome> classifyThrivingCold(double humidity, double elevation) {
        if (elevation < 30.0) {
            return ModBiomes.SNOWY_PLAINS;
        } else if (elevation <= 80.0) {
            if (humidity < 0.5) {
                return ModBiomes.TAIGA;
            } else {
                return ModBiomes.SNOW_FOREST;
            }
        } else {
            return ModBiomes.ALPINE_MEADOWS;
        }
    }
    
    /**
     * Temperate thriving biomes (0-25°C, habitability > 0.7)
     */
    private RegistryKey<Biome> classifyThrivingTemperate(double humidity, double elevation) {
        if (elevation < 30.0) {
            if (humidity < 0.4) {
                return ModBiomes.PLAINS;
            } else if (humidity <= 0.7) {
                return ModBiomes.MIXED_PLAINS;
            } else {
                return ModBiomes.WETLANDS;
            }
        } else if (elevation <= 80.0) {
            if (humidity < 0.4) {
                return ModBiomes.OAK_FOREST;
            } else if (humidity <= 0.7) {
                return ModBiomes.MIXED_FOREST;
            } else {
                return ModBiomes.DENSE_FOREST;
            }
        } else {
            return ModBiomes.MOUNTAIN_FOREST;
        }
    }
    
    /**
     * Warm thriving biomes (25-40°C, habitability > 0.7)
     */
    private RegistryKey<Biome> classifyThrivingWarm(double humidity, double windSpeed, double elevation) {
        if (humidity < 0.4) {
            if (windSpeed < 0.3) {
                return ModBiomes.HOT_SHRUBLAND;
            } else {
                return ModBiomes.WINDY_STEPPES;
            }
        } else if (humidity <= 0.7) {
            if (elevation < 50.0) {
                return ModBiomes.TEMPERATE_RAINFOREST;
            } else {
                return ModBiomes.CLOUD_FOREST;
            }
        } else {
            if (elevation < 50.0) {
                return ModBiomes.JUNGLE;
            } else {
                return ModBiomes.TROPICAL_RAINFOREST;
            }
        }
    }
    
    /**
     * Hot thriving biomes (temp > 40°C, habitability > 0.7)
     */
    private RegistryKey<Biome> classifyThrivingHot(double humidity) {
        if (humidity < 0.4) {
            return ModBiomes.HOT_DESERT;
        } else {
            return ModBiomes.TROPICAL_SWAMP;
        }
    }
}
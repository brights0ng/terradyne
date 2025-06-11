# Terradyne - Procedural Planet Generation for Minecraft

A sophisticated Minecraft Fabric mod that generates realistic alien planets with scientifically-inspired terrain systems, custom biomes, and physically-based world generation.

## 🌍 Overview

Terradyne creates explorable alien worlds with realistic planetary characteristics, diverse biomes, and scientifically-grounded terrain generation. Each planet type features unique atmospheric conditions, geological processes, and specialized terrain features.

## ✨ Features

### 🪐 Planet Types
Currently implemented planet types with full terrain generation:

- **🏜️ Desert Bodies** - Arid worlds with sand dunes, mesas, canyons, and salt flats. [NOT YET IMPLEMENTED]
- **🌊 Oceanic Bodies** - Water-rich worlds with deep oceans and varied landmasses. [NOT YET IMPLEMENTED]
- **🌑 Rocky Bodies** - Airless or thin-atmosphere worlds with cratered surfaces. [NOT YET IMPLEMENTED]
- **🌋 Volcanic Bodies** - Inhospitable worlds with volcanos and igneous rocks. [NOT YET IMPLEMENTED]
- **💎 Carbon Bodies** - Dark worlds rich with carbonic rocks, including graphite fields and diamond mountains. [NOT YET IMPLEMENTED]
- **🧊 Icy Bodies** - Frigid worlds made of solid ice and rock. [NOT YET IMPLEMENTED]
- **🪼 Subsurface Oceanic Bodies** - Glacial worlds covered in a thick crust of ice, with deep oceans beneath possibly containing marine life. [NOT YET IMPLEMENTED]
- **🧲 Iron Bodies** - Metallic worlds covered in ferrous rock containing lots of simple metals -- essentially the core of another world. [NOT YET IMPLEMENTED]

### 🗺️ Biome System

#### Desert Planet Biomes (4 Specialized Types)
Each desert planet contains all four biomes distributed based on planetary characteristics:

1. **Dune Sea** (25%) - Rolling sand dunes with wind-sculpted patterns
2. **Granite Mesas** (25%) - Flat-topped plateaus with steep cliff faces
3. **Limestone Canyons** (25%) - Deep carved valleys and gorge systems
4. **Salt Flats** (25%) - Crystalline plains with hexagonal crack patterns

### 🔬 Advanced Terrain Generation

#### Physics-Based Generation System
- **Master Noise Provider** - Single unified noise source prevents terrain artifacts
- **Octave Physics Engine** - Simulates real geological and atmospheric processes
- **Pass-Based Placement** - Separates physics calculations from block placement

#### Specialized Terrain Features
- **Wind Erosion Patterns** - Directional erosion based on atmospheric conditions
- **Mesa Formation** - Geological uplift with stratified layering and erosion
- **Canyon Carving** - Water erosion creating meandering channel networks
- **Salt Crystallization** - Evaporation patterns forming geometric surface features
- **Dune Dynamics** - Wind-driven sand accumulation and transport

### 🎮 Commands & Gameplay

Complete command system for planet creation and exploration:

```
/terradyne create <name> [type] [subtype]
/terradyne visit <name>
/terradyne list
/terradyne info <name>
/terradyne types
/terradyne help
```

#### Planet Creation Examples
```bash
# Create different planet types
/terradyne create Tatooine desert
/terradyne create Kamino oceanic earthlike
/terradyne create Luna rocky moonlike

# Planet subtypes
/terradyne create Arrakis desert hot
/terradyne create Atlantica oceanic tropical
/terradyne create Ceres rocky asteroid
```

## Technical Architecture

### Core Systems

#### 1. Planet Configuration System
- **Planet Models** - Calculate derived characteristics (gravity, atmosphere, etc.)
- **Factory Pattern** - Standardized planet creation with realistic parameter sets
- **Age-Based Evolution** - Planetary characteristics change based on age (Young/Mature/Ancient)

#### 2. Universal Chunk Generator
- **Pass-Based Generation** - Modular terrain construction system
- **Physics Integration** - Real geological process simulation
- **Biome-Specific Terrain** - Each biome uses different generation passes

#### 3. Dynamic Dimension System
- **Custom Dimension Types** - Planet-specific environmental conditions
- **Atmospheric Simulation** - Pressure, temperature, and sky effects
- **Gravitational Variation** - Different gravity based on planet characteristics

#### 4. Data-Driven Biomes
- **JSON Generation** - Custom biomes generated via Fabric data generation
- **Graceful Fallbacks** - Uses vanilla biomes if custom biomes unavailable
- **Color Palettes** - Planet-appropriate sky, fog, and vegetation colors

### Generation Pipeline

1. **Foundation Pass** - Places base terrain using physics octaves
2. **Formation Passes** - Build major features (mesas, dunes, limestone layers)
3. **Carving Passes** - Remove terrain (canyon erosion, wind carving)
4. **Detail Passes** - Add surface texture and fine details

### Physics Octaves

Each octave simulates specific geological/atmospheric processes:

- **FoundationOctave** - Continental-scale base terrain
- **RollingTerrainOctave** - Local terrain variation and badlands
- **DuneFormationOctave** - Wind-driven sand transport and deposition
- **MesaFormationOctave** - Geological uplift and plateau formation
- **WaterErosionOctave** - River/canyon carving through water flow
- **WindErosionOctave** - Atmospheric erosion patterns
- **SaltDepositionOctave** - Evaporation and crystallization physics

## Getting Started

### Prerequisites
- Minecraft 1.20.x
- Fabric Loader
- Fabric API

### Installation
1. Download the latest Terradyne mod file
2. Place in your `.minecraft/mods/` folder
3. Launch Minecraft with Fabric

### Quick Start
1. Create a new world or join a server with Terradyne
2. Use `/terradyne create MyPlanet desert` to create your first planet
3. Use `/terradyne visit MyPlanet` to explore your new world
4. Use `/terradyne help` for complete command documentation

### Custom Biome Generation
For enhanced visual experience with custom biomes:

1. Run `./gradlew runDatagen` in the mod development environment
2. This generates custom biome JSON files with planet-appropriate colors
3. Without data generation, the mod uses vanilla biomes with custom terrain

## Configuration

### Planet Characteristics

Each planet type has configurable parameters affecting terrain generation:

#### Desert Planets
- **Temperature** - Affects erosion rates and material behavior
- **Humidity** - Determines vegetation and weathering patterns  
- **Wind Strength** - Controls dune formation and erosion intensity
- **Sand Density** - Ratio of loose material to solid rock
- **Rock Type** - Underlying geology (Granite, Limestone, Sandstone, Volcanic)

#### Oceanic Planets
- **Ocean Coverage** - Percentage of surface covered by water
- **Ocean Depth** - Average depth affecting sea level and continental shelves
- **Continent Count** - Number of major landmasses
- **Atmospheric Humidity** - Weather intensity and storm frequency
- **Crustal Activity** - Tectonic processes affecting coastline complexity

#### Rocky Planets
- **Atmospheric Density** - Affects erosion, temperature variation, and sky visibility
- **Crater Density** - Impact history and surface roughness
- **Geological Activity** - Current volcanic/seismic processes
- **Surface Composition** - Material types (Regolith, Basaltic, Metallic, etc.)

## Development Roadmap

### Implemented Features ✅
- ✅ Desert planet generation with 4 specialized biomes
- ✅ Oceanic planet generation with Earth-like characteristics
- ✅ Rocky planet generation with airless/atmospheric variants
- ✅ Physics-based terrain generation system
- ✅ Dynamic dimension creation and management
- ✅ Complete command system with planet management
- ✅ Custom biome data generation system

### Planned Features 🔮
- 🔮 Volcanic planets with lava flows and eruption systems
- 🔮 Icy planets with frozen surfaces and subsurface oceans
- 🔮 Iron planets with metallic terrain and unique properties
- 🔮 Carbon planets with diamond formations and coal deposits
- 🔮 Atmospheric effects and weather systems
- 🔮 Planet-specific resources and materials
- 🔮 Alien flora and fauna ecosystems
- 🔮 Interplanetary travel mechanics

## Technical Details

### Architecture Principles

#### Separation of Physics and Placement
- **Physics Octaves** calculate where terrain features should form
- **Generation Passes** place actual blocks based on physics results
- This separation allows complex terrain while maintaining performance

#### Master Noise System
- Single noise source shared across all terrain generation
- Eliminates noise layer conflicts and visible generation seams
- Enables realistic terrain feature interactions

#### Biome-Driven Generation
- Each biome defines its own generation passes and parameters
- Same physics octaves used differently per biome type
- Allows diverse terrain while maintaining code reusability

### Performance Optimizations
- **Concurrent Generation** - Thread-safe terrain generation
- **Efficient Noise Sampling** - Shared noise samplers reduce computation
- **Chunk-Based Processing** - Generation optimized for Minecraft's chunk system
- **Lazy Loading** - Planets generated only when visited

### Mod Compatibility
- **Fabric API Integration** - Uses standard Fabric mod development patterns
- **Data Generation Support** - Compatible with Fabric's data generation system
- **Registry Management** - Proper registration of dimensions, biomes, and commands

## 📚 Code Structure

```
src/main/java/net/starlight/terradyne/
├── planet/
│   ├── PlanetType.java                    # Planet type enumeration and classification
│   ├── biome/                             # Biome definitions and management
│   │   ├── DesertBiomeType.java          # Desert biome implementations
│   │   ├── DesertBiomeSource.java        # Desert biome distribution logic
│   │   ├── ModBiomes.java                # Biome registry keys
│   │   └── IBiomeType.java               # Biome interface for generation passes
│   ├── config/                           # Planet configuration classes
│   │   ├── DesertConfig.java             # Desert planet parameters
│   │   ├── OceanicConfig.java            # Oceanic planet parameters
│   │   └── RockyConfig.java              # Rocky planet parameters
│   ├── model/                            # Planet model calculations
│   │   ├── DesertModel.java              # Desert planet derived properties
│   │   ├── OceanicModel.java             # Oceanic planet calculations
│   │   └── RockyModel.java               # Rocky planet characteristics
│   ├── factory/                          # Planet creation factories
│   │   ├── DesertPlanetFactory.java      # Desert planet presets
│   │   ├── OceanicPlanetFactory.java     # Oceanic planet presets
│   │   └── RockyPlanetFactory.java       # Rocky planet presets
│   ├── dimension/                        # Dimension management
│   │   ├── PlanetDimensionManager.java   # Planet world creation and management
│   │   ├── DimensionTypeFactory.java     # Custom dimension type creation
│   │   └── ModDimensionTypes.java        # Dimension type registry
│   ├── chunk/                            # World generation
│   │   └── UniversalChunkGenerator.java  # Pass-based chunk generation
│   └── terrain/                          # Terrain generation system
│       ├── MasterNoiseProvider.java      # Unified noise source
│       ├── OctaveRegistry.java           # Physics octave management
│       ├── octave/                       # Physics simulation octaves
│       │   ├── FoundationOctave.java     # Continental base terrain
│       │   ├── DuneFormationOctave.java  # Wind-driven sand physics
│       │   ├── MesaFormationOctave.java  # Geological uplift simulation
│       │   ├── WaterErosionOctave.java   # Erosion and canyon carving
│       │   └── [other octaves...]        # Additional physics simulations
│       └── pass/                         # Block placement passes
│           ├── PassRegistry.java         # Generation pass management
│           ├── TerrainFoundationPass.java # Base terrain placement
│           ├── DuneConstructionPass.java # Dune construction
│           ├── MesaConstructionPass.java # Mesa formation
│           └── [other passes...]         # Additional placement passes
├── datagen/                              # Data generation for custom content
│   └── BiomeDataProvider.java           # Custom biome JSON generation
├── util/                                 # Utility classes
│   ├── CommandRegistry.java             # Command system implementation
│   └── ModEnums.java                    # Shared enumerations
└── Terradyne.java                       # Main mod initialization
```

## 🤝 Contributing

### Development Setup
1. Clone the repository
2. Open in IntelliJ IDEA or your preferred IDE
3. Run `./gradlew build` to compile
4. Run `./gradlew runClient` to test in development environment

### Adding New Planet Types
1. Create config class in `planet.config`
2. Create model class in `planet.model` 
3. Add factory methods in `planet.factory`
4. Define biome types implementing `IBiomeType`
5. Add planet type to `PlanetType` enum
6. Update dimension creation in `PlanetDimensionManager`

### Code Style
- Follow Java naming conventions
- Document complex terrain generation algorithms
- Include parameter documentation for octaves and passes
- Use descriptive variable names for physics calculations

## 📄 License

[Add your license information here]

## 🙏 Acknowledgments

- Inspired by real planetary science and geological processes
- Built with the Fabric modding framework
- Uses Minecraft's world generation system as a foundation

## 📞 Support

- **Issues**: [GitHub Issues](your-repo-link)
- **Documentation**: [Wiki](your-wiki-link)
- **Discord**: [Community Server](your-discord-link)

---

*Generate infinite worlds, explore alien landscapes, and discover the wonders of procedural planet creation with Terradyne.*

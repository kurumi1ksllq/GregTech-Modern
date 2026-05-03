# PROJECT KNOWLEDGE BASE

**Generated:** 2026-04-30
**Commit:** N/A (local build)
**Branch:** N/A (development)

## LANGUAGE PRIORITY (语言优先级)

**CRITICAL: 用户语言要求 > 系统默认语言**

- 如果用户要求使用某种语言，**所有输出**都必须使用该语言，包括思考过程 (thinking)
- 不要因为系统提示词、示例、术语是英文就默认输出英文
- 用户明确要求 "thinking过程说中文" → 所有思考过程必须使用中文
- 用户明确要求 "用中文回答" → 所有输出使用中文

## OVERVIEW
GTCEU (GregTech CE Unofficial) - NeoForge 1.20.1 mod with Registrate, Lombok, complex machine system. Version 7.5.3. 10 subpackages deep.

## STRUCTURE
```
gtceu_tng/
├── src/main/java/com/gregtechceu/gtceu/
│   ├── api/          # Core abstractions (registry, recipe, machine traits)
│   ├── client/       # Client-only code (ClientProxy, renderers)
│   ├── common/       # Shared content (machines, blocks, covers)
│   ├── config/       # Config holder
│   ├── core/         # Mixins, core functionality
│   ├── data/         # Datagen (lang, tags, recipes)
│   ├── forge/        # Forge-specific integrations
│   ├── integration/  # Third-party mod integrations (AE2, KJS, Create)
│   ├── syncdata/     # Network sync utilities
│   └── utils/        # Helpers, formatting
├── src/test/        # GameTest framework tests
├── .github/workflows/ # CI pipelines (12 workflows)
└── gradle/          # Version catalogs, scripts
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Mod entry point | `GTCEu.java` | @Mod(GTCEu.MOD_ID), proxy init |
| Common init | `common/CommonProxy.java` | Material registration, registry freeze |
| Recipe staging | `api/recipe/staging/` | beginStaging/completeStaging |
| Machine definition | `api/machine/` | MetaMachine hierarchy |
| Data generation | `data/GregTechDatagen.java` | Blockstate, tags, lang |
| Cover system | `common/cover/` | Cover handlers and factories |
| Multiblock | `common/machine/multiblock/` | Controller + part pattern |
| Integration hooks | `integration/` | AE2, KJS, Create, CCT |

## CODE MAP
| Symbol | Type | Location | Refs | Role |
|--------|------|----------|------|------|
| GTCEu | class | GTCEu.java | - | Main mod class, proxy dispatcher |
| CommonProxy | class | common/CommonProxy.java | 355 | Material init, registry freeze, data loading |
| GTCEuAPI | class | api/GTCEuAPI.java | 84 | High-tier flag, materialManager, registry events |
| Registrate | class | common/registry/GTRegistration.java | - | Custom Registrate wrapper |
| GregTechDatagen | class | data/GregTechDatagen.java | 33 | Data generator entry |

## CONVENTIONS (THIS PROJECT)
- **Registry freeze**: Must call `unfreezeRegistries()` before adding materials, `freezeRegistries()` after
- **Recipe staging**: Use `beginStaging()/addStaging()/completeStaging()` for test recipes
- **Proxy pattern**: DistExecutor.unsafeRunForDist for client/server split
- **Mod ID constant**: `GTCEu.MOD_ID = "gtceu"`
- **Spotless**: Code formatted with spotless, regions use `// spotless:off/on`
- **Material icon**: Use MaterialIconSet, MaterialIconType for texture mapping

## ANTI-PATTERNS (THIS PROJECT)
- **NEVER call cover methods directly** - use CoverUIFactory
- **NEVER use pump frequency > 800** - causes server lag
- **NEVER use optical pipe in fluid regulation** - not supported
- **NEVER register items/blocks before material registry freeze** - crashes

## UNIQUE STYLES
- Uses `@ApiStatus.Internal` for internal-only methods
- Custom `GTRecipeType` with `InputSeparation` support
- Recipe lookup uses `MapIngredientTypeManager` for multi-ingredient matching
- Machine owner system for permission checking

## COMMANDS
```bash
./gradlew runClient          # Launch dev client
./gradlew runServer          # Launch dev server
./gradlew runGameTestServer  # Run GameTest tests
./gradlew data               # Generate datagen
./gradlew spotlessCheck      # Check code format
./gradlew spotlessApply      # Fix code format
./gradlew build              # Full build
./gradlew publish             # Publish to Maven
```

## NOTES
- **Material system**: Complex - has material properties, icon sets, flags, and generated items
- **Machine tiers**: Uses GTValues tier system (LV=0 to Max=16)
- **Config holder**: `ConfigHolder.INSTANCE` for runtime config access
- **KubeJS integration**: MaterialModificationEventJS for addon material tweaking
- **High-tier content**: Controlled by `ConfigHolder.INSTANCE.machines.highTierContent` or `IGTAddon.requiresHighTier()`
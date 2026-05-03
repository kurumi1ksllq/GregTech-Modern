# api/ AGENTS.md

## OVERVIEW
Core abstractions layer - registries, recipes, machine definitions, capabilities.

## KEY ENTRY POINTS
- `GTCEuAPI.java` - High-tier flag, materialManager, coil/filter/battery registries
- `GTValues.java` - Tier constants (LV=0 to Max=16), mod IDs
- `Registry.java` - Custom GTRegistry<K,V> wrapper

## SUBPACKAGES
| Package | Purpose |
|---------|---------|
| `block/` | ICoilType, IFilterType, block interfaces |
| `blockentity/` | MetaMachine block entities |
| `capability/` | GTCapability, recipe capabilities (item, fluid, energy) |
| `cover/` | Cover system interfaces and handlers |
| `data/chemical/` | Material system (events, managers, info, prefix) |
| `machine/` | MetaMachine hierarchy, trait system, multiblock |
| `recipe/` | Recipe types, builders, staging, lookup |
| `registry/` | GTRegistry, Registrate wrapper |
| `transfer/` | Pipe/net transfer handlers |

## CORE PATTERNS

### Registry Freeze Pattern
```java
// In MaterialRegistryManager:
unfreezeRegistries();    // BEFORE adding materials
// ... add materials ...
closeRegistries();       // AFTER adding materials
freezeRegistries();      // FINAL - no more materials
```

### Recipe Staging (for tests)
```java
type.getAdditionHandler().beginStaging();
type.getAdditionHandler().addStaging(recipe);
type.getAdditionHandler().completeStaging();
```

### Definition-Instance Pattern
- `DefinitionPump.java` / `PumpMachine.java` - Definition + Instance
- Machine definitions hold static data, instances hold state

## CONVENTIONS
- `@ApiStatus.Internal` marks internal-only APIs
- All registry lookups go through GTRegistries class
- Recipe conditions use GTRecipeCondition base
- Machine traits extend `MachineTrait`

## ANTI-PATTERNS
- NEVER instantiate MetaMachine directly - use `MachineRegistry.createMachine()`
- NEVER access cover methods directly - use CoverUIFactory
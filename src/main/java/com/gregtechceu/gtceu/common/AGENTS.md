# common/ AGENTS.md

## OVERVIEW
Largest package - machines, blocks, items, covers, recipes, entity systems.

## KEY ENTRY POINT
`CommonProxy.java` (355 lines)
- Constructor: Event bus registration, materialManager init, config init
- `init()`: UIFactory, GregTechDatagen, material init, registry freeze, cover/block/item/recipe registration

## SUBDIRECTORIES
| Directory | Content |
|-----------|---------|
| `block/` | GTBlocks, GTFluids, materials |
| `blockentity/` | Machine block entities (Tank, Machine, Crafter) |
| `cover/` | Cover implementations (Conveyor, Pump, RobotArm, SolarPanel, etc.) |
| `data/` | Recipe loaders, crafting components, loot |
| `machine/` | Machine definitions + multiblock controllers |
| `recipe/` | Recipe type definitions, condition implementations |
| `registry/` | GTRegistration (Registrate wrapper) |
| `unification/` | Material unification (ore, ingot, plate, etc.) |

## REGISTRATION ORDER
1. `initMaterials()` - unfreeze → GTMaterials.init() → freeze
2. `GTBlocks/GTFluids/GTItems/GTBlockEntities`
3. `GTRecipeTypes/GTRecipeCategories`
4. `GTMachines/GTMachineUtils`

## MULTIBLOCK PATTERN
```
MultiblockControllerMachine (abstract)
├── creates Controller
├── has PartMachine instances
└── structureDefinition.fromParts()
```

## COVER SYSTEM
- Covers registered via `GTCovers.init()`
- Never call cover methods directly - use CoverUIFactory
- Cover behaviors: Conveyor, Pump, RobotArm, SolarPanel, Detector, Regurator

## TEST RECIPES
Use RecipeStaging:
```java
GTRecipeTypes.RESEARCH.onRecipeAdded("research", recipe -> {
    recipe.getAdditionHandler().beginStaging();
    recipe.getAdditionHandler().addStaging(raw);
    recipe.getAdditionHandler().completeStaging();
});
```
# data/ AGENTS.md

## OVERVIEW
Data generation layer - lang files, tags, recipes, blockstates, models, loot tables.

## KEY ENTRY POINT
`GregTechDatagen.java`
- `initPre()`: Registers custom blockstate provider
- `initPost()`: Registers tag and lang data generators

## SUBDIRECTORIES
| Directory | Content |
|-----------|---------|
| `forge/` | Forge-specific data generators |
| `lang/` | Language file generators (MaterialLangGenerator) |
| `loader/` | Data loaders (DungeonLootLoader, GTCraftingComponents) |
| `loot/` | Loot table generators |
| `model/` | Blockstate/model loaders |
| `pack/` | Dynamic resource/data pack generation |
| `recipe/` | Recipe generators (GTRecipes) |
| `tags/` | Tag file generators (Block, Item, Fluid, Entity) |

## DATAGEN REGISTRATION
```java
// GregTechDatagen.initPost():
GTRegistration.REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, BlockTagLoader::init);
GTRegistration.REGISTRATE.addDataGenerator(ProviderType.ITEM_TAGS, ItemTagLoader::init);
GTRegistration.REGISTRATE.addDataGenerator(ProviderType.LANG, LangHandler::init);
```

## DYNAMIC PACK SYSTEM
- `GTDynamicResourcePack` - Client resources (models, textures)
- `GTDynamicDataPack` - Server data (recipes, tags, loot)
- `GTPackSource` - Pack repository source

## RECIPE GENERATION
- `GTRecipes.recipeRemoval()` - Remove vanilla/gtceu recipes
- `GTRecipes.recipeAddition(GTDynamicDataPack::addRecipe)` - Add recipes to pack
- `GTCraftingComponents.init()` - Shared crafting component recipes

## TEST RESOURCES
- `src/test/resources/data/gtceu/structures/` - GameTest templates (.nbt files)
- `TestUtils.java` in gametest/util/ - Test helper methods
package com.gregtechceu.gtceu.api.item.tool;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.api.sound.ExistingSoundEntry;
import com.gregtechceu.gtceu.api.sound.SoundEntry;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;
import com.gregtechceu.gtceu.common.data.item.GTToolActions;
import com.gregtechceu.gtceu.common.item.tool.behavior.*;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.UnaryOperator;

public class GTToolType {

    @Getter
    private static final Map<String, GTToolType> types = new HashMap<>();

    public static final GTToolType PICKAXE = GTToolType.builder("pickaxe")
            .toolTag(ToolItemTagType.MATCH, ItemTags.PICKAXES)
            .toolTag(ItemTags.CLUSTER_MAX_HARVESTABLES)
            .harvestTag(BlockTags.MINEABLE_WITH_PICKAXE)
            .toolStats(b -> b.blockBreaking().attackDamage(1.0F).attackSpeed(-2.8F)
                    .behaviors(TorchPlaceBehavior.INSTANCE))
            .toolClassNames("pickaxe")
            .defaultActions(ToolActions.DEFAULT_PICKAXE_ACTIONS)
            .materialAmount(3 * GTValues.M)
            .build();
    public static final GTToolType SHOVEL = GTToolType.builder("shovel")
            .toolTag(ToolItemTagType.MATCH, ItemTags.SHOVELS)
            .harvestTag(BlockTags.MINEABLE_WITH_SHOVEL)
            .toolStats(b -> b.blockBreaking().attackDamage(1.5F).attackSpeed(-3.0F)
                    .behaviors(GrassPathBehavior.INSTANCE, DouseCampfireBehavior.INSTANCE))
            .constructor(GTShovelItem::new)
            .toolClassNames("shovel")
            .defaultActions(ToolActions.SHOVEL_DIG)
            .materialAmount(GTValues.M)
            .build();
    public static final GTToolType AXE = GTToolType.builder("axe")
            .toolTag(ToolItemTagType.MATCH, ItemTags.AXES)
            .harvestTag(BlockTags.MINEABLE_WITH_AXE)
            .toolStats(b -> b.blockBreaking()
                    .attackDamage(5.0F).attackSpeed(-3.2F).baseEfficiency(2.0F)
                    .behaviors(DisableShieldBehavior.INSTANCE, TreeFellingBehavior.INSTANCE,
                            LogStripBehavior.INSTANCE,
                            ScrapeBehavior.INSTANCE, WaxOffBehavior.INSTANCE))
            .constructor(GTAxeItem::new)
            .toolClassNames("axe")
            .materialAmount(3 * GTValues.M)
            .defaultActions(ToolActions.AXE_DIG)
            .build();
    public static final GTToolType SPADE = GTToolType.builder("spade")
            .toolTag(ToolItemTagType.MATCH, CustomTags.SPADES)
            .harvestTag(BlockTags.MINEABLE_WITH_SHOVEL)
            .toolStats(b -> b.blockBreaking().aoe(1, 1, 0)
                    .efficiencyMultiplier(0.4F).attackDamage(1.5F).attackSpeed(-3.2F)
                    .durabilityMultiplier(3.0F)
                    .behaviors(AOEConfigUIBehavior.INSTANCE, GrassPathBehavior.INSTANCE,
                            DouseCampfireBehavior.INSTANCE))
            .constructor(GTShovelItem::new)
            .toolClasses(GTToolType.SHOVEL)
            .defaultActions(ToolActions.SHOVEL_DIG)
            .materialAmount(3 * GTValues.M)
            .build();
    public static final GTToolType HARD_HAMMER = GTToolType.builder("hammer")
            .toolTag(ToolItemTagType.CRAFTING, CustomTags.CRAFTING_HAMMERS)
            .toolTag(ToolItemTagType.MATCH, CustomTags.HAMMERS)
            .harvestTag(CustomTags.MINEABLE_WITH_HAMMER)
            .harvestTag(BlockTags.MINEABLE_WITH_PICKAXE)
            .toolStats(b -> b.blockBreaking().crafting().damagePerCraftingAction(2)
                    .attackDamage(1.0F).attackSpeed(-2.8F)
                    .behaviors(new EntityDamageBehavior(2.0F, IronGolem.class), ProspectingBehavior.INSTANCE))
            .sound(GTSoundEntries.FORGE_HAMMER)
            .symbol('h')
            .toolClasses(GTToolType.PICKAXE)
            .defaultActions(ToolActions.DEFAULT_PICKAXE_ACTIONS)
            .defaultActions(GTToolActions.DEFAULT_HAMMER_ACTIONS)
            .materialAmount(6 * GTValues.M)
            .build();
    public static final GTToolType SOFT_MALLET = GTToolType.builder("mallet")
            .toolTag(ToolItemTagType.CRAFTING, CustomTags.CRAFTING_MALLETS)
            .toolTag(ToolItemTagType.MATCH, CustomTags.MALLETS)
            .toolStats(b -> b.crafting().cannotAttack().attackSpeed(-2.4F).sneakBypassUse()
                    .behaviors(ToolModeSwitchBehavior.INSTANCE))
            .sound(GTSoundEntries.SOFT_MALLET_TOOL)
            .symbol('r')
            .defaultActions(GTToolActions.DEFAULT_MALLET_ACTIONS, GTToolActions.INTERACT_WITH_COVER)
            .materialAmount(6 * GTValues.M)
            .build();
    public static final GTToolType WRENCH = GTToolType.builder("wrench")
            .toolTag(ToolItemTagType.CRAFTING, CustomTags.CRAFTING_WRENCHES)
            .toolTag(ToolItemTagType.MATCH, CustomTags.WRENCHES)
            .toolTag(ToolItemTagType.MATCH, CustomTags.WRENCH)
            .harvestTag(CustomTags.MINEABLE_WITH_WRENCH)
            .toolStats(b -> b.blockBreaking().crafting().sneakBypassUse()
                    .attackDamage(1.0F).attackSpeed(-2.8F)
                    .behaviors(BlockRotatingBehavior.INSTANCE, new EntityDamageBehavior(3.0F, IronGolem.class),
                            ToolModeSwitchBehavior.INSTANCE))
            .sound(GTSoundEntries.WRENCH_TOOL, true)
            .symbol('w')
            .defaultActions(GTToolActions.WRENCH_DIG, GTToolActions.WRENCH_DISMANTLE, GTToolActions.WRENCH_CONNECT)
            .materialAmount(4 * GTValues.M)
            .build();
    public static final GTToolType FILE = GTToolType.builder("file")
            .toolTag(ToolItemTagType.CRAFTING, CustomTags.CRAFTING_FILES)
            .toolTag(ToolItemTagType.MATCH, CustomTags.FILES)
            .toolStats(b -> b.crafting().damagePerCraftingAction(4)
                    .cannotAttack().attackSpeed(-2.4F))
            .sound(GTSoundEntries.FILE_TOOL)
            .symbol('f')
            .materialAmount(2 * GTValues.M)
            .build();
    public static final GTToolType CROWBAR = GTToolType.builder("crowbar")
            .toolTag(ToolItemTagType.CRAFTING, CustomTags.CRAFTING_CROWBARS)
            .toolTag(ToolItemTagType.MATCH, CustomTags.CROWBARS)
            .harvestTag(CustomTags.MINEABLE_WITH_CROWBAR)
            .toolStats(b -> b.blockBreaking().crafting()
                    .attackDamage(2.0F).attackSpeed(-2.4F)
                    .sneakBypassUse().behaviors(RotateRailBehavior.INSTANCE))
            .sound(new ExistingSoundEntry(SoundEvents.ITEM_BREAK, SoundSource.BLOCKS), true)
            .symbol('c')
            .defaultActions(GTToolActions.CROWBAR_DIG, GTToolActions.CROWBAR_REMOVE_COVER)
            .materialAmount(3 * GTValues.M / 2)
            .build();
    public static final GTToolType SCREWDRIVER = GTToolType.builder("screwdriver")
            .toolTag(ToolItemTagType.CRAFTING, CustomTags.CRAFTING_SCREWDRIVERS)
            .toolTag(ToolItemTagType.MATCH, CustomTags.SCREWDRIVERS)
            .toolStats(b -> b.crafting().damagePerCraftingAction(4).sneakBypassUse()
                    .attackDamage(-1.0F).attackSpeed(3.0F)
                    .behaviors(new EntityDamageBehavior(3.0F, Spider.class)))
            .sound(GTSoundEntries.SCREWDRIVER_TOOL)
            .symbol('d')
            .defaultActions(GTToolActions.DEFAULT_SCREWDRIVER_ACTIONS)
            .materialAmount(GTValues.M)
            .build();
    public static final GTToolType MORTAR = GTToolType.builder("mortar")
            .toolTag(ToolItemTagType.CRAFTING, CustomTags.CRAFTING_MORTARS)
            .toolTag(ToolItemTagType.MATCH, CustomTags.MORTARS)
            .toolStats(b -> b.crafting().damagePerCraftingAction(2).cannotAttack().attackSpeed(-2.4F))
            .sound(GTSoundEntries.MORTAR_TOOL)
            .symbol('m')
            .materialAmount(2 * GTValues.M)
            .build();
    public static final GTToolType WIRE_CUTTER = GTToolType.builder("wire_cutter")
            .toolTag(ToolItemTagType.CRAFTING, CustomTags.CRAFTING_WIRE_CUTTERS)
            .toolTag(ToolItemTagType.MATCH, CustomTags.WIRE_CUTTERS)
            .harvestTag(CustomTags.MINEABLE_WITH_WIRE_CUTTER)
            .toolStats(b -> b.blockBreaking().crafting().sneakBypassUse()
                    .damagePerCraftingAction(4).attackDamage(-1.0F).attackSpeed(-2.4F))
            .sound(GTSoundEntries.WIRECUTTER_TOOL, true)
            .symbol('x')
            .defaultActions(GTToolActions.DEFAULT_WIRE_CUTTER_ACTIONS)
            .materialAmount(4 * GTValues.M) // 3 plates + 2 rods
            .build();
    public static final GTToolType KNIFE = GTToolType.builder("knife")
            .toolTag(ToolItemTagType.CRAFTING, CustomTags.CRAFTING_KNIVES)
            .toolTag(ToolItemTagType.MATCH, CustomTags.KNIVES)
            .harvestTag(CustomTags.MINEABLE_WITH_KNIFE)
            .toolStats(b -> b.crafting().attacking().attackSpeed(3.0F))
            .constructor(GTSwordItem::new)
            .symbol('k')
            .defaultActions(GTToolActions.KNIFE_DIG)
            .materialAmount(GTValues.M)
            .build();
    public static final GTToolType SAW = GTToolType.builder("saw")
            .toolTag(ToolItemTagType.CRAFTING, CustomTags.CRAFTING_SAWS)
            .toolTag(ToolItemTagType.MATCH, CustomTags.SAWS)
            .harvestTag(CustomTags.MINEABLE_WITH_SAW)
            .toolStats(b -> b.crafting().attacking().attackSpeed(3.0F))
            .constructor(GTSwordItem::new)
            .symbol('s')
            .defaultActions(GTToolActions.SAW_DIG)
            .materialAmount(GTValues.M)
            .build();
    // public static GTToolType GRAFTER = new GTToolType("grafter", 1, 1, GTCEu.id("item/tools/handle_hammer"),
    // GTCEu.id("item/tools/hammer"));
    public static final GTToolType PLUNGER = GTToolType.builder("plunger")
            .toolTag(ToolItemTagType.MATCH, CustomTags.PLUNGERS)
            .toolStats(b -> b.cannotAttack().attackSpeed(-2.4F).sneakBypassUse()
                    .behaviors(PlungerBehavior.INSTANCE))
            .sound(GTSoundEntries.PLUNGER_TOOL)
            .build();
    public static final GTToolType SHEARS = GTToolType.builder("shears")
            .toolTag(ToolItemTagType.MATCH, CustomTags.SHEARS)
            .harvestTag(CustomTags.MINEABLE_WITH_SHEARS)
            .toolStats(b -> b)
            .defaultActions(ToolActions.DEFAULT_SHEARS_ACTIONS)
            .build();

    public final String name;
    public final String idFormat;
    // at least one has to be set. first one MUST be the main tag.
    public final List<TagKey<Item>> itemTags;
    public final List<TagKey<Item>> matchTags;
    public final List<TagKey<Item>> craftingTags;
    public final List<TagKey<Block>> harvestTags;
    public final Set<ToolAction> defaultAbilities;
    public final ResourceLocation modelLocation;
    public final Set<String> toolClassNames;
    public final Set<GTToolType> toolClasses;
    @Nullable
    public final SoundEntry soundEntry;
    public final boolean playSoundOnBlockDestroy;
    public final char symbol;
    public final long materialAmount;
    public final IGTToolDefinition toolDefinition;
    public final ToolConstructor constructor;
    public final int electricTier;

    public GTToolType(String name, String idFormat, char symbol,
                      Set<GTToolType> toolClasses, ToolConstructor constructor, IGTToolDefinition toolDefinition,
                      List<TagKey<Item>> itemTags, List<TagKey<Item>> matchTags, List<TagKey<Item>> craftingTags,
                      List<TagKey<Block>> harvestTags, Set<ToolAction> defaultAbilities,
                      Set<String> toolClassNames, ResourceLocation modelLocation,
                      @Nullable SoundEntry soundEntry, boolean playSoundOnBlockDestroy,
                      int electricTier, long materialAmount) {
        this.name = name;
        this.idFormat = idFormat;
        this.symbol = symbol;
        toolClasses.add(this);
        this.toolClasses = toolClasses;
        this.toolDefinition = toolDefinition;
        this.constructor = constructor;
        this.itemTags = itemTags;
        this.matchTags = matchTags;
        this.craftingTags = craftingTags;
        this.harvestTags = harvestTags;
        this.defaultAbilities = defaultAbilities;
        this.modelLocation = modelLocation;
        this.toolClassNames = toolClassNames;
        this.soundEntry = soundEntry;
        this.playSoundOnBlockDestroy = playSoundOnBlockDestroy;
        this.electricTier = electricTier;
        this.materialAmount = materialAmount;

        types.put(name, this);
    }

    public boolean is(ItemStack itemStack) {
        return ToolHelper.is(itemStack, this);
    }

    public String getUnlocalizedName() {
        return "item.gtceu.tool." + name;
    }

    public enum ToolItemTagType {
        NONE,
        MATCH,
        CRAFTING;
    }

    @FunctionalInterface
    public interface ToolConstructor {

        IGTTool apply(GTToolType type, MaterialToolTier tier, Material material, IGTToolDefinition definition,
                      Item.Properties properties);
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    @Accessors(fluent = true, chain = true)
    public static class Builder {

        private final String name;
        @Setter
        private String idFormat;

        private final List<TagKey<Item>> itemTags = new ArrayList<>();
        private final List<TagKey<Item>> matchTags = new ArrayList<>();
        private final List<TagKey<Item>> craftingTags = new ArrayList<>();
        private final List<TagKey<Block>> harvestTags = new ArrayList<>();
        private final Set<ToolAction> defaultAbilities = Sets.newIdentityHashSet();
        @Setter
        private Set<String> toolClassNames = new HashSet<>();
        private final Set<GTToolType> toolClasses = new HashSet<>();
        @Setter
        private IGTToolDefinition toolStats;
        @Setter
        private long materialAmount;
        @Setter
        private int tier = -1;
        @Setter
        private char symbol = ' ';
        @Setter
        private ToolConstructor constructor = GTToolItem::new;
        @Setter
        private ResourceLocation modelLocation;
        private SoundEntry sound;
        private boolean playSoundOnBlockDestroy;

        public Builder(String name) {
            this.name = name;
            this.idFormat = "%s_" + name;
            this.modelLocation = GTCEu.id("item/tools/" + name);
        }

        @SafeVarargs
        public final Builder toolTag(TagKey<Item>... tags) {
            return toolTag(ToolItemTagType.NONE, tags);
        }

        @SafeVarargs
        public final Builder toolTag(ToolItemTagType tagType, TagKey<Item>... tags) {
            itemTags.addAll(Arrays.asList(tags));
            if (tagType == ToolItemTagType.MATCH) {
                matchTags.addAll(Arrays.asList(tags));
            }
            if (tagType == ToolItemTagType.CRAFTING) {
                craftingTags.addAll(Arrays.asList(tags));
            }
            return this;
        }

        @SafeVarargs
        public final Builder harvestTag(TagKey<Block>... tags) {
            harvestTags.addAll(Arrays.asList(tags));
            return this;
        }

        public Builder defaultActions(ToolAction... abilities) {
            defaultAbilities.addAll(Arrays.asList(abilities));
            return this;
        }

        public Builder defaultActions(Collection<ToolAction> abilities) {
            defaultAbilities.addAll(abilities);
            return this;
        }

        public Builder defaultActions(Collection<ToolAction> abilities, ToolAction... extra) {
            defaultAbilities.addAll(abilities);
            defaultAbilities.addAll(Arrays.asList(extra));
            return this;
        }

        @Tolerate
        public Builder toolClasses(GTToolType... classes) {
            this.toolClasses.addAll(Arrays.asList(classes));
            this.toolClassNames.addAll(Arrays.stream(classes).map(type -> type.name).toList());
            return this;
        }

        @Tolerate
        public Builder toolClassNames(String... classes) {
            this.toolClassNames.addAll(Arrays.asList(classes));
            return this;
        }

        @Tolerate
        public Builder toolStats(UnaryOperator<ToolDefinitionBuilder> builder) {
            this.toolStats = builder.apply(new ToolDefinitionBuilder()).build();
            return this;
        }

        public Builder sound(SoundEntry sound) {
            return this.sound(sound, false);
        }

        public Builder sound(SoundEntry sound, boolean playSoundOnBlockDestroy) {
            this.sound = sound;
            this.playSoundOnBlockDestroy = playSoundOnBlockDestroy;
            return this;
        }

        public Builder electric(int tier) {
            return tier(tier);
        }

        private GTToolType get() {
            return new GTToolType(name, idFormat, symbol,
                    toolClasses, constructor, toolStats,
                    itemTags, matchTags, craftingTags, harvestTags, defaultAbilities,
                    toolClassNames, modelLocation,
                    sound, playSoundOnBlockDestroy,
                    tier, materialAmount);
        }

        public GTToolType build() {
            if (toolClassNames.isEmpty()) {
                toolClassNames.add(name);
            }
            if (this.symbol == ' ') {
                return get();
            }
            GTToolType existing = ToolHelper.getToolFromSymbol(this.symbol);
            if (existing != null) {
                throw new IllegalArgumentException(
                        String.format("Symbol %s has been taken by %s already!", symbol, existing));
            }
            GTToolType supplied = get();
            ToolHelper.registerToolSymbol(this.symbol, supplied);
            return supplied;
        }
    }
}

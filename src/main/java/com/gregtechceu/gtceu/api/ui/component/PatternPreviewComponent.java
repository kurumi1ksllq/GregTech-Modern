package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.container.FlowLayout;
import com.gregtechceu.gtceu.api.ui.container.ScrollContainer;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.texture.TextTexture;
import com.gregtechceu.gtceu.api.ui.texture.UITextures;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemStackHandler;

import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.ItemStackKey;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.screen.RecipeScreen;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.shedaniel.rei.impl.client.gui.screen.AbstractDisplayViewingScreen;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author KilaBash
 * @date 2023/3/5
 * @implNote PatterShapeInfoWidget
 */
@OnlyIn(Dist.CLIENT)
public class PatternPreviewComponent extends FlowLayout {

    private boolean isLoaded;
    private static TrackedDummyWorld LEVEL;
    private static BlockPos LAST_POS = new BlockPos(0, 50, 0);
    private static final Map<MultiblockMachineDefinition, MBPattern[]> CACHE = new HashMap<>();
    private final SceneComponent sceneComponent;
    private final FlowLayout scrollContainer;
    public final MultiblockMachineDefinition controllerDefinition;
    public final MBPattern[] patterns;
    private final List<SimplePredicate> predicates;
    private int index;
    public int layer;
    private SlotComponent[] slotWidgets;
    private SlotComponent[] candidates;

    protected PatternPreviewComponent(MultiblockMachineDefinition controllerDefinition) {
        super(Sizing.fixed(160), Sizing.fixed(160), Algorithm.HORIZONTAL);
        this.padding(Insets.of(4));
        this.controllerDefinition = controllerDefinition;
        predicates = new ArrayList<>();
        layer = -1;

        child(sceneComponent = (SceneComponent) new SceneComponent(Sizing.fixed(150), Sizing.fixed(150), LEVEL)
                .onSelected(this::onPosSelected)
                .renderFacing(false)
                .positioning(Positioning.absolute(0, 0)));

        scrollContainer = UIContainers.horizontalFlow(Sizing.fill(), Sizing.fill());
        child(UIContainers.horizontalScroll(Sizing.fixed(154), Sizing.fixed(22), scrollContainer)
                .scrollbarThickness(4)
                .scrollbar(ScrollContainer.Scrollbar.custom(
                        GuiTextures.SLIDER_BACKGROUND.imageLocation,
                        GuiTextures.BUTTON.imageLocation))
                .positioning(Positioning.absolute(0, 129)));

        if (ConfigHolder.INSTANCE.client.useVBO) {
            if (!RenderSystem.isOnRenderThread()) {
                RenderSystem.recordRenderCall(sceneComponent::useCacheBuffer);
            } else {
                sceneComponent.useCacheBuffer();
            }
        }

        child(UIComponents.texture(UITextures.text(Component.translatable(controllerDefinition.getDescriptionId()))
                .textType(TextTexture.TextType.ROLL)
                .width(170)
                .dropShadow(true))
                .positioning(Positioning.absolute(1, 1))
                .sizing(Sizing.fill(), Sizing.fixed(10)));

        this.patterns = CACHE.computeIfAbsent(controllerDefinition, definition -> {
            HashSet<ItemStackKey> drops = new HashSet<>();
            drops.add(new ItemStackKey(this.controllerDefinition.asStack()));
            return controllerDefinition.getMatchingShapes().stream()
                    .map(it -> initializePattern(it, drops))
                    .filter(Objects::nonNull)
                    .toArray(MBPattern[]::new);
        });

        child(UIComponents.button(Component.empty(),
                (x) -> setPage((index + 1 >= patterns.length) ? 0 : index + 1))
                .renderer(ButtonComponent.Renderer.texture(
                        UITextures.group(
                                Color.T_GRAY.rectTexture(),
                                UITextures.text(Component.literal("1"))
                                        .textSupplier(() -> Component.literal("P:" + index)))))
                .sizing(Sizing.fixed(18))
                .positioning(Positioning.absolute(138, 30)));

        child(UIComponents.button(Component.empty(),
                cd -> updateLayer())
                .renderer(ButtonComponent.Renderer.texture(UITextures.group(
                        Color.T_GRAY.rectTexture(),
                        UITextures.text(Component.literal("1")).textSupplier(() -> Component.literal(layer >= 0 ?
                                "L:" + layer : "ALL")))))
                .sizing(Sizing.fixed(18))
                .positioning(Positioning.absolute(138, 50)));

        setPage(0);
    }

    private void updateLayer() {
        MBPattern pattern = patterns[index];
        if (layer + 1 >= -1 && layer + 1 <= pattern.maxY - pattern.minY) {
            layer += 1;
            if (pattern.controllerBase.isFormed()) {
                onFormedSwitch(false);
            }
        } else {
            layer = -1;
            if (!pattern.controllerBase.isFormed()) {
                onFormedSwitch(true);
            }
        }
        setupScene(pattern);
    }

    private void setupScene(MBPattern pattern) {
        Stream<BlockPos> stream = pattern.blockMap.keySet().stream()
                .filter(pos -> layer == -1 || layer + pattern.minY == pos.getY());
        if (pattern.controllerBase.isFormed()) {
            LongSet set = pattern.controllerBase.getMultiblockState().getMatchContext().getOrDefault("renderMask",
                    LongSets.EMPTY_SET);
            Set<BlockPos> modelDisabled = set.stream().map(BlockPos::of).collect(Collectors.toSet());
            if (!modelDisabled.isEmpty()) {
                sceneComponent.renderedCore(
                        stream.filter(pos -> !modelDisabled.contains(pos)).collect(Collectors.toList()), null);
            } else {
                sceneComponent.renderedCore(stream.toList(), null);
            }
        } else {
            sceneComponent.renderedCore(stream.toList(), null);
        }
    }

    public static PatternPreviewComponent getPatternWidget(MultiblockMachineDefinition controllerDefinition) {
        if (LEVEL == null) {
            if (Minecraft.getInstance().level == null) {
                GTCEu.LOGGER.error("Try to init pattern previews before level load");
                throw new IllegalStateException();
            }
            LEVEL = new TrackedDummyWorld();
        }
        return new PatternPreviewComponent(controllerDefinition);
    }

    public void setPage(int index) {
        if (index >= patterns.length || index < 0) return;
        this.index = index;
        this.layer = -1;
        MBPattern pattern = patterns[index];
        setupScene(pattern);
        if (slotWidgets != null) {
            for (SlotComponent slotWidget : slotWidgets) {
                scrollContainer.removeChild(slotWidget);
            }
        }
        slotWidgets = new SlotComponent[Math.min(pattern.parts.size(), 18)];
        var itemHandler = new CycleItemStackHandler(pattern.parts);
        for (int i = 0; i < slotWidgets.length; i++) {
            slotWidgets[i] = (SlotComponent) UIComponents.slot(itemHandler, i)
                    .canExtract(false)
                    .canInsert(false)
                    .ingredientIO(IO.IN)
                    .positioning(Positioning.absolute(4 + i * 18, 0));
            scrollContainer.child(slotWidgets[i]);
        }
    }

    private void onFormedSwitch(boolean isFormed) {
        MBPattern pattern = patterns[index];
        IMultiController controllerBase = pattern.controllerBase;
        if (isFormed) {
            this.layer = -1;
            loadControllerFormed(pattern.blockMap.keySet(), controllerBase);
        } else {
            sceneComponent.renderedCore(pattern.blockMap.keySet(), null);
            controllerBase.onStructureInvalid();
        }
    }

    private void onPosSelected(BlockPos pos, Direction facing) {
        if (index >= patterns.length || index < 0) return;
        TraceabilityPredicate predicate = patterns[index].predicateMap.get(pos);
        if (predicate != null) {
            predicates.clear();
            predicates.addAll(predicate.common);
            predicates.addAll(predicate.limited);
            predicates.removeIf(p -> p == null || p.candidates == null); // why it happens?
            if (candidates != null) {
                for (SlotComponent candidate : candidates) {
                    removeChild(candidate);
                }
            }
            List<List<ItemStack>> candidateStacks = new ArrayList<>();
            List<List<Component>> predicateTips = new ArrayList<>();
            for (SimplePredicate simplePredicate : predicates) {
                List<ItemStack> itemStacks = simplePredicate.getCandidates();
                if (!itemStacks.isEmpty()) {
                    candidateStacks.add(itemStacks);
                    predicateTips.add(simplePredicate.getToolTips(predicate));
                }
            }
            candidates = new SlotComponent[candidateStacks.size()];
            CycleItemStackHandler itemHandler = new CycleItemStackHandler(candidateStacks);
            int maxCol = (160 - (((slotWidgets.length - 1) / 9 + 1) * 18) - 35) % 18;
            for (int i = 0; i < candidateStacks.size(); i++) {
                candidates[i] = (SlotComponent) UIComponents.slot(itemHandler, i)
                        .canExtract(false)
                        .canInsert(false)
                        .ingredientIO(IO.IN)
                        .positioning(Positioning.absolute(3 + (i / maxCol) * 18, 3 + (i % maxCol) * 18))
                        .tooltip(predicateTips.get(i));
                child(candidates[i]);
            }
        }
    }

    public static BlockPos locateNextRegion(int range) {
        BlockPos pos = LAST_POS;
        LAST_POS = LAST_POS.offset(range, 0, range);
        return pos;
    }

    @Override
    protected void parentUpdate(float delta, int mouseX, int mouseY) {
        super.parentUpdate(delta, mouseX, mouseY);
        // I can only think of this way
        if (!isLoaded && GTCEu.Mods.isEMILoaded() && Minecraft.getInstance().screen instanceof RecipeScreen) {
            setPage(0);
            isLoaded = true;
        } else if (!isLoaded && GTCEu.Mods.isREILoaded() &&
                Minecraft.getInstance().screen instanceof AbstractDisplayViewingScreen) {
                    setPage(0);
                    isLoaded = true;
                }
    }

    @Override
    public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        RenderSystem.enableBlend();
        super.draw(graphics, mouseX, mouseY, partialTicks, delta);
    }

    private MBPattern initializePattern(MultiblockShapeInfo shapeInfo, HashSet<ItemStackKey> blockDrops) {
        Map<BlockPos, BlockInfo> blockMap = new HashMap<>();
        IMultiController controllerBase = null;
        BlockPos multiPos = locateNextRegion(500);

        BlockInfo[][][] blocks = shapeInfo.getBlocks();
        for (int x = 0; x < blocks.length; x++) {
            BlockInfo[][] aisle = blocks[x];
            for (int y = 0; y < aisle.length; y++) {
                BlockInfo[] column = aisle[y];
                for (int z = 0; z < column.length; z++) {
                    BlockState blockState = column[z].getBlockState();
                    BlockPos pos = multiPos.offset(x, y, z);
                    if (column[z].getBlockEntity(pos) instanceof IMachineBlockEntity holder &&
                            holder.getMetaMachine() instanceof IMultiController controller) {
                        holder.getSelf().setLevel(LEVEL);
                        controllerBase = controller;
                    }
                    blockMap.put(pos, BlockInfo.fromBlockState(blockState));
                }
            }
        }

        LEVEL.addBlocks(blockMap);
        if (controllerBase != null) {
            LEVEL.setInnerBlockEntity(controllerBase.self().holder.getSelf());
        }

        Map<ItemStackKey, PartInfo> parts = gatherBlockDrops(blockMap);
        blockDrops.addAll(parts.keySet());

        Map<BlockPos, TraceabilityPredicate> predicateMap = new HashMap<>();
        if (controllerBase != null) {
            loadControllerFormed(predicateMap.keySet(), controllerBase);
            predicateMap = controllerBase.getMultiblockState().getMatchContext().get("predicates");
        }
        return controllerBase == null ? null : new MBPattern(blockMap, parts.values().stream().sorted((one, two) -> {
            if (one.isController) return -1;
            if (two.isController) return +1;
            if (one.isTile && !two.isTile) return -1;
            if (two.isTile && !one.isTile) return +1;
            if (one.blockId != two.blockId) return two.blockId - one.blockId;
            return two.amount - one.amount;
        }).map(PartInfo::getItemStack).filter(list -> !list.isEmpty()).collect(Collectors.toList()), predicateMap,
                controllerBase);
    }

    private void loadControllerFormed(Collection<BlockPos> poses, IMultiController controllerBase) {
        BlockPattern pattern = controllerBase.getPattern();
        if (pattern != null && pattern.checkPatternAt(controllerBase.getMultiblockState(), true)) {
            controllerBase.onStructureFormed();
        }
        if (controllerBase.isFormed()) {
            LongSet set = controllerBase.getMultiblockState().getMatchContext().getOrDefault("renderMask",
                    LongSets.EMPTY_SET);
            Set<BlockPos> modelDisabled = set.stream().map(BlockPos::of).collect(Collectors.toSet());
            if (!modelDisabled.isEmpty()) {
                sceneComponent.renderedCore(
                        poses.stream().filter(pos -> !modelDisabled.contains(pos)).collect(Collectors.toList()), null);
            } else {
                sceneComponent.renderedCore(poses, null);
            }
        } else {
            GTCEu.LOGGER.warn("Pattern formed checking failed: {}", controllerBase.self().getDefinition());
        }
    }

    private Map<ItemStackKey, PartInfo> gatherBlockDrops(Map<BlockPos, BlockInfo> blocks) {
        Map<ItemStackKey, PartInfo> partsMap = new Object2ObjectOpenHashMap<>();
        for (Map.Entry<BlockPos, BlockInfo> entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState blockState = ((Level) PatternPreviewComponent.LEVEL).getBlockState(pos);
            ItemStack itemStack = blockState.getBlock().getCloneItemStack(PatternPreviewComponent.LEVEL, pos,
                    blockState);

            if (itemStack.isEmpty() && !blockState.getFluidState().isEmpty()) {
                Fluid fluid = blockState.getFluidState().getType();
                itemStack = fluid.getBucket().getDefaultInstance();
            }

            ItemStackKey itemStackKey = new ItemStackKey(itemStack);
            partsMap.computeIfAbsent(itemStackKey, key -> new PartInfo(key, entry.getValue())).amount++;
        }
        return partsMap;
    }

    private static class PartInfo {

        final ItemStackKey itemStackKey;
        boolean isController = false;
        boolean isTile = false;
        final int blockId;
        int amount = 0;

        PartInfo(final ItemStackKey itemStackKey, final BlockInfo blockInfo) {
            this.itemStackKey = itemStackKey;
            this.blockId = Block.getId(blockInfo.getBlockState());
            this.isTile = blockInfo.hasBlockEntity();

            if (blockInfo.getBlockState().getBlock() instanceof MetaMachineBlock block) {
                if (block.definition instanceof MultiblockMachineDefinition)
                    this.isController = true;
            }
        }

        public List<ItemStack> getItemStack() {
            return Arrays.stream(itemStackKey.getItemStack())
                    .map(itemStack -> {
                        var item = itemStack.copy();
                        item.setCount(amount);
                        return item;
                    }).filter(item -> !item.isEmpty()).toList();
        }
    }

    public static class MBPattern {

        @NotNull
        final List<List<ItemStack>> parts;
        @NotNull
        final Map<BlockPos, TraceabilityPredicate> predicateMap;
        @NotNull
        final Map<BlockPos, BlockInfo> blockMap;
        @NotNull
        final IMultiController controllerBase;
        final int maxY, minY;

        public MBPattern(@NotNull Map<BlockPos, BlockInfo> blockMap, @NotNull List<List<ItemStack>> parts,
                         @NotNull Map<BlockPos, TraceabilityPredicate> predicateMap,
                         @NotNull IMultiController controllerBase) {
            this.parts = parts;
            this.blockMap = blockMap;
            this.predicateMap = predicateMap;
            this.controllerBase = controllerBase;
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            for (BlockPos pos : blockMap.keySet()) {
                min = Math.min(min, pos.getY());
                max = Math.max(max, pos.getY());
            }
            minY = min;
            maxY = max;
        }
    }
}

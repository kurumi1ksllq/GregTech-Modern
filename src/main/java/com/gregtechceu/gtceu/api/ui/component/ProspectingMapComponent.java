package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.container.*;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.misc.PacketProspecting;
import com.gregtechceu.gtceu.api.ui.misc.ProspectorMode;
import com.gregtechceu.gtceu.api.ui.texture.ProspectingTexture;
import com.gregtechceu.gtceu.api.ui.texture.TextTexture;
import com.gregtechceu.gtceu.api.ui.texture.UITexture;
import com.gregtechceu.gtceu.common.item.ProspectorScannerBehavior;
import com.gregtechceu.gtceu.integration.map.WaypointManager;
import com.gregtechceu.gtceu.integration.map.cache.client.GTClientCache;
import com.gregtechceu.gtceu.integration.map.cache.server.ServerCache;
import com.gregtechceu.gtceu.integration.map.layer.builtin.OreRenderLayer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ProspectingMapComponent extends StackLayout implements SearchComponent.IComponentSearch<Object> {

    private final int chunkRadius;
    private final ProspectorMode mode;
    private final int scanTick;
    @Getter
    private boolean darkMode = false;
    private final FlowLayout itemList;
    private ProspectingTexture texture;
    private int playerChunkX;
    private int playerChunkZ;
    // runtime
    private int chunkIndex = 0;
    private final Queue<PacketProspecting> packetQueue = new LinkedBlockingQueue<>();
    private final Set<Object> items = new CopyOnWriteArraySet<>();
    private final Map<String, SelectableFlowLayout> selectedMap = new ConcurrentHashMap<>();

    public ProspectingMapComponent(Sizing horizontalSizing, Sizing verticalSizing, int chunkRadius,
                                   @NotNull ProspectorMode mode, int scanTick) {
        super(horizontalSizing, verticalSizing);
        this.chunkRadius = chunkRadius;
        this.mode = mode;
        this.scanTick = scanTick;
        int imageWidth = (chunkRadius * 2 - 1) * 16;
        int imageHeight = (chunkRadius * 2 - 1) * 16;
        child(UIComponents.texture(GuiTextures.BACKGROUND_INVERSE)
                .positioning(Positioning.absolute(0, (height - imageHeight) / 2 - 4))
                .sizing(Sizing.fixed(imageWidth + 8), Sizing.fixed(imageHeight + 8)));
        var group = (StackLayout) UIContainers.stack(Sizing.fixed(width - (imageWidth + 10)), Sizing.fill())
                .surface(Surface.UI_BACKGROUND_INVERSE)
                .padding(Insets.both(8, 32))
                .positioning(Positioning.absolute(imageWidth + 10, 0));

        itemList = UIContainers.verticalFlow(Sizing.fill(), Sizing.fill())
                .configure(c -> {
                    c.padding(Insets.of(4));
                });
        group.child(UIContainers.verticalScroll(Sizing.fill(), Sizing.fill(), itemList)
                .scrollbarThickness(2).scrollbar(ScrollContainer.Scrollbar.flat(Color.T_WHITE)));

        group.child(new SearchComponent<>(Sizing.fixed(group.width() - 12), Sizing.fixed(18), this)
                .positioning(Positioning.absolute(-2, -26))
                .sizing(Sizing.fill()));
        child(group);

        // FIXME MAKE TRANSLATABLE
        addNewItem("[all]", Component.translatable("all resources"), UITexture.EMPTY, -1);
    }

    /*
     * @Override
     * public void writeInitialData(FriendlyByteBuf buffer) {
     * super.writeInitialData(buffer);
     * buffer.writeVarInt(playerChunkX = gui.entityPlayer.chunkPosition().x);
     * buffer.writeVarInt(playerChunkZ = gui.entityPlayer.chunkPosition().z);
     * buffer.writeVarInt(gui.entityPlayer.getBlockX());
     * buffer.writeVarInt(gui.entityPlayer.getBlockZ());
     * }
     * 
     * @Override
     * public void readInitialData(FriendlyByteBuf buffer) {
     * super.readInitialData(buffer);
     * texture = new ProspectingTexture(
     * buffer.readVarInt(),
     * buffer.readVarInt(),
     * buffer.readVarInt(),
     * buffer.readVarInt(),
     * gui.entityPlayer.getVisualRotationYInDegrees(), mode, chunkRadius, darkMode);
     * }
     */

    @Override
    public void init() {
        super.init();
        var player = player();
        playerChunkX = player.chunkPosition().x;
        playerChunkZ = player.chunkPosition().z;

        texture = new ProspectingTexture(
                playerChunkX,
                playerChunkZ,
                player.getBlockX(),
                player.getBlockZ(),
                player().getVisualRotationYInDegrees(), mode, chunkRadius, darkMode);
    }

    public void setDarkMode(boolean mode) {
        if (darkMode != mode) {
            darkMode = mode;
            texture.setDarkMode(darkMode);
        }
    }

    private void addOresToList(Object[][][] data) {
        var newItems = new HashSet<>();
        for (int x = 0; x < mode.cellSize; x++) {
            for (int z = 0; z < mode.cellSize; z++) {
                for (var item : data[x][z]) {
                    newItems.add(item);
                    addNewItem(mode.getUniqueID(item), mode.getDescription(item), mode.getItemIcon(item),
                            mode.getItemColor(item));
                }
            }
        }
        items.addAll(newItems);
    }

    private void addNewItem(String uniqueID, Component renderingName, UITexture icon, int color) {
        if (!selectedMap.containsKey(uniqueID)) {
            int width = itemList.width() - 4;
            SelectableFlowLayout selectable = new SelectableFlowLayout(Sizing.fixed(width), Sizing.fixed(15),
                    FlowLayout.Algorithm.HORIZONTAL, () -> !Objects.equals(texture.getSelected(), uniqueID))
                    .onSelected(c -> texture.setSelected(uniqueID))
                    .selectedTexture(Color.WHITE.borderTexture(-1));

            selectable.child(UIComponents.texture(icon)
                    .sizing(Sizing.fixed(15)));
            selectable.child(UIComponents.label(renderingName)
                    .maxWidth(width - 15)
                    .textType(TextTexture.TextType.LEFT_HIDE)
                    .positioning(Positioning.absolute(15, 0))
                    .sizing(Sizing.fill(), Sizing.fixed(15)));
            itemList.child(selectable);
            selectedMap.put(uniqueID, selectable);
        }
    }

    @Override
    protected void parentUpdate(float delta, int mouseX, int mouseY) {
        super.parentUpdate(delta, mouseX, mouseY);

        if (packetQueue != null) {
            int max = 10;
            while (max-- > 0 && !packetQueue.isEmpty()) {
                var packet = packetQueue.poll();
                texture.updateTexture(packet);
                addOresToList(packet.data);
            }
        }

        var player = player();
        var world = player.level();
        if (player.level().getGameTime() % scanTick == 0 &&
                chunkIndex < (chunkRadius * 2 - 1) * (chunkRadius * 2 - 1)) {

            int row = chunkIndex / (chunkRadius * 2 - 1);
            int column = chunkIndex % (chunkRadius * 2 - 1);

            int ox = column - chunkRadius + 1;
            int oz = row - chunkRadius + 1;

            var chunk = world.getChunk(playerChunkX + ox, playerChunkZ + oz);
            if (mode == ProspectorMode.ORE) {
                ServerCache.instance.prospectAllInChunk(world.dimension(), chunk.getPos(), (ServerPlayer) player);
            }
            PacketProspecting packet = new PacketProspecting(playerChunkX + ox, playerChunkZ + oz, this.mode);
            mode.scan(packet.data, chunk);
            //sendMessage(-1, packet::writePacketData);
            chunkIndex++;
        }
        var held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.getItem() instanceof IComponentItem componentItem) {
            for (var component : componentItem.getComponents()) {
                if (component instanceof ProspectorScannerBehavior prospector) {
                    if (!player.isCreative() && !prospector.drainEnergy(held, false)) {
                        player.closeContainer();
                    }
                }
            }
        }

        var x = x() + 3;
        var y = y() + (height() - texture.getImageHeight()) / 2 - 1;
        int cX = (mouseX - x) / 16;
        int cZ = (mouseY - y) / 16;
        if (cX >= 0 && cZ >= 0 && cX < chunkRadius * 2 - 1 && cZ < chunkRadius * 2 - 1) {
            // draw hover layer
            List<Component> tooltips = new ArrayList<>();
            tooltips.add(Component.translatable(mode.unlocalizedName));
            List<Object[]> items = new ArrayList<>();
            for (int i = 0; i < mode.cellSize; i++) {
                for (int j = 0; j < mode.cellSize; j++) {
                    assert texture != null;
                    if (texture.data[cX * mode.cellSize + i][cZ * mode.cellSize + j] != null) {
                        items.add(texture.data[cX * mode.cellSize + i][cZ * mode.cellSize + j]);
                    }
                }
            }
            mode.appendTooltips(items, tooltips, texture.getSelected());
            this.tooltip(tooltips);
        }
    }

    private void addPacketToQueue(PacketProspecting packet) {
        packetQueue.add(packet);

        var player = player();
        if (mode == ProspectorMode.FLUID && packet.data[0][0].length > 0) {
            GTClientCache.instance.addFluid(player.level().dimension(), packet.chunkX, packet.chunkZ,
                    (ProspectorMode.FluidInfo) packet.data[0][0][0]);
        }
    }

    @Override
    public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        super.draw(graphics, mouseX, mouseY, partialTicks, delta);
        // draw background
        var x = x() + 3;
        var y = y() + (height() - texture.getImageHeight()) / 2 - 1;
        texture.draw(graphics, x, y);
        int cX = (mouseX - x) / 16;
        int cZ = (mouseY - y) / 16;
        if (cX >= 0 && cZ >= 0 && cX < chunkRadius * 2 - 1 && cZ < chunkRadius * 2 - 1) {
            // draw hover layer
            graphics.fill(cX * 16 + x, cZ * 16 + y, 16, 16, 0x4B6C6C6C);
        }
    }

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        var clickedItem = getClickedVein(mouseX, mouseY);
        if (clickedItem == null) {
            return super.onMouseDown(mouseX, mouseY, button);
        }
        var player = player();
        WaypointManager.setWaypoint(new ChunkPos(clickedItem.position).toString(),
                clickedItem.name.getString(),
                clickedItem.color,
                player.level().dimension(),
                clickedItem.position.getX(), clickedItem.position.getY(), clickedItem.position.getZ());
        player.displayClientMessage(
                Component.translatable("behavior.prospector.added_waypoint", clickedItem.name), true);
        UIComponent.playButtonClickSound();
        return true;
    }

    private WaypointItem getClickedVein(double mouseX, double mouseY) {
        var x = x() + 3;
        var y = y() + (height() - texture.getImageHeight()) / 2 - 1;

        int cX = (int) (mouseX - x) / 16;
        int cZ = (int) (mouseY - y) / 16;
        int offsetX = Math.abs((int) (mouseX - x) % 16);
        int offsetZ = Math.abs((int) (mouseY - y) % 16);
        int xDiff = cX - (chunkRadius - 1);
        int zDiff = cZ - (chunkRadius - 1);

        var player = player();
        int xPos = ((player.chunkPosition().x + xDiff) << 4) + offsetX;
        int zPos = ((player.chunkPosition().z + zDiff) << 4) + offsetZ;

        var blockPos = new BlockPos(xPos, player.level().getHeight(Heightmap.Types.WORLD_SURFACE, xPos, zPos),
                zPos);
        if (cX < 0 || cZ < 0 || cX >= chunkRadius * 2 - 1 || cZ >= chunkRadius * 2 - 1) {
            return null;
        }

        // If the ores are filtered use its name
        if (!texture.getSelected().equals(ProspectingTexture.SELECTED_ALL)) {
            for (var item : items) {
                if (!texture.getSelected().equals(mode.getUniqueID(item))) continue;
                var name = mode.getDescription(item);
                var color = mode.getItemColor(item);
                return new WaypointItem(blockPos, name, color);
            }
        }

        // If the cursor is over an ore use its name
        var hoveredItem = texture.data[cX * mode.cellSize + (offsetX * mode.cellSize / 16)][cZ * mode.cellSize +
                (offsetZ * mode.cellSize / 16)];
        if (hoveredItem != null && hoveredItem.length != 0) {
            var name = mode.getDescription(hoveredItem[0]);
            var color = mode.getItemColor(hoveredItem[0]);
            return new WaypointItem(blockPos, name, color);
        }

        // If all else fails see if there's a nearby vein and use the vein's name
        var vein = GTClientCache.instance.getNearbyVeins(player.level().dimension(), blockPos, 32);
        if (!vein.isEmpty()) {
            vein.sort((o1, o2) -> (int) (o1.center().distToCenterSqr(xPos, o1.center().getY(), zPos) -
                    o2.center().distToCenterSqr(xPos, o2.center().getY(), zPos)));
            var name = OreRenderLayer.getName(vein.get(0));
            var materials = vein.get(0).definition().veinGenerator().getAllMaterials();
            var mostCommonItem = materials.get(materials.size() - 1);
            var color = mostCommonItem.getMaterialRGB();
            return new WaypointItem(blockPos, name, color);
        }

        // FIXME MAKE TRANSLATABLE
        return new WaypointItem(blockPos, Component.translatable("Depleted Vein"), 0x990000);
    }

    @Override
    public Component resultDisplay(Object value) {
        return mode.getDescription(value);
    }

    @Override
    public void selectResult(Object item) {
        var uid = mode.getUniqueID(item);
        texture.setSelected(uid);
        var selected = selectedMap.get(uid);
        selected.onSelected();
    }

    @Override
    public void search(String s, Consumer<Object> consumer) {
        var added = new HashSet<String>();
        for (var item : this.items) {
            if (Thread.currentThread().isInterrupted()) return;
            var id = mode.getUniqueID(item);
            if (!added.contains(id)) {
                added.add(id);
                var localized = resultDisplay(item);
                if (item.toString().toLowerCase(Locale.ROOT).contains(s.toLowerCase(Locale.ROOT)) ||
                        localized.getString().toLowerCase(Locale.ROOT).contains(s.toLowerCase(Locale.ROOT))) {
                    consumer.accept(item);
                }
            }
        }
    }

    private record WaypointItem(BlockPos position, Component name, int color) {

    }
}

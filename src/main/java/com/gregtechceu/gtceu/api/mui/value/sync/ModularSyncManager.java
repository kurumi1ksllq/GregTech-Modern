package com.gregtechceu.gtceu.api.mui.value.sync;

import com.gregtechceu.gtceu.client.mui.screen.ModularContainerMenu;
import com.gregtechceu.gtceu.common.mui.widgets.slot.SlotGroup;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.PlayerInvWrapper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.util.Map;

public class ModularSyncManager {

    public static final String AUTO_SYNC_PREFIX = "auto_sync:";
    protected static final String PLAYER_INVENTORY = "player_inventory";
    private static final String CURSOR_KEY = makeSyncKey("cursor_slot", 255255);

    private final Map<String, PanelSyncManager> panelSyncManagerMap = new Object2ObjectOpenHashMap<>();
    private PanelSyncManager mainPSM;
    private final ModularContainerMenu menu;
    private final CursorSlotSyncHandler cursorSlotSyncHandler = new CursorSlotSyncHandler();

    public ModularSyncManager(ModularContainerMenu menu) {
        this.menu = menu;
    }

    @ApiStatus.Internal
    public void construct(String mainPanelName, PanelSyncManager mainPSM) {
        this.mainPSM = mainPSM;
        if (this.mainPSM.getSlotGroup(PLAYER_INVENTORY) == null) {
            this.mainPSM.bindPlayerInventory(getPlayer());
        }
        open(mainPanelName, mainPSM);
        mainPSM.syncValue(CURSOR_KEY, this.cursorSlotSyncHandler);
    }

    public PanelSyncManager getMainPSM() {
        return mainPSM;
    }

    public void detectAndSendChanges(boolean init) {
        this.panelSyncManagerMap.values().forEach(psm -> psm.detectAndSendChanges(init));
    }

    public void onClose() {
        this.panelSyncManagerMap.values().forEach(PanelSyncManager::onClose);
    }

    public void onOpen() {
        this.panelSyncManagerMap.values().forEach(PanelSyncManager::onOpen);
    }

    public PanelSyncManager getPanelSyncManager(String panelName) {
        PanelSyncManager psm = this.panelSyncManagerMap.get(panelName);
        if (psm != null) return psm;
        throw new NullPointerException("No PanelSyncManager found for name '" + panelName + "'!");
    }

    public SyncHandler getSyncHandler(String panelName, String syncKey) {
        return getPanelSyncManager(panelName).getSyncHandler(syncKey);
    }

    public SlotGroup getSlotGroup(String panelName, String slotGroupName) {
        return getPanelSyncManager(panelName).getSlotGroup(slotGroupName);
    }

    public ItemStack getCursorItem() {
        return getPlayer().inventoryMenu.getCarried();
    }

    public void setCursorItem(ItemStack item) {
        getPlayer().inventoryMenu.setCarried(item);
        this.cursorSlotSyncHandler.sync();
    }

    public void open(String name, PanelSyncManager syncManager) {
        this.panelSyncManagerMap.put(name, syncManager);
        syncManager.initialize(name, this);
    }

    public void close(String name) {
        PanelSyncManager psm = this.panelSyncManagerMap.remove(name);
        if (psm != null) psm.onClose();
    }

    public boolean isOpen(String panelName) {
        return this.panelSyncManagerMap.containsKey(panelName);
    }

    public void receiveWidgetUpdate(String panelName, String mapKey, int id, FriendlyByteBuf buf) throws IOException {
        getPanelSyncManager(panelName).receiveWidgetUpdate(mapKey, id, buf);
    }

    public Player getPlayer() {
        return this.menu.getPlayer();
    }

    public ModularContainerMenu getMenu() {
        return menu;
    }

    public boolean isClient() {
        return this.menu.isClient();
    }

    private static boolean isPlayerSlot(Slot slot) {
        if (slot == null) return false;
        if (slot.container instanceof Inventory) {
            return slot.getSlotIndex() >= 0 && slot.getSlotIndex() < 36;
        }
        if (slot instanceof SlotItemHandler slotItemHandler) {
            IItemHandler iItemHandler = slotItemHandler.getItemHandler();
            if (iItemHandler instanceof PlayerMainInvWrapper || iItemHandler instanceof PlayerInvWrapper) {
                return slot.getSlotIndex() >= 0 && slot.getSlotIndex() < 36;
            }
        }
        return false;
    }

    public static String makeSyncKey(String name, int id) {
        return name + ":" + id;
    }
}

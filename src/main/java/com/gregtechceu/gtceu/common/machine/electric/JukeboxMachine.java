package com.gregtechceu.gtceu.common.machine.electric;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IWorkable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;

// TODO: Copy Lang
// TODO: Better tooltips
// TODO: Actual textures
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class JukeboxMachine extends TieredEnergyMachine implements IFancyUIMachine, IWorkable {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(FisherMachine.class,
            TieredEnergyMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    private final NotifiableItemStackHandler inventory;

    @Getter
    @Persisted
    @DescSynced
    private boolean isWorkingEnabled = true;

    @DescSynced
    @Persisted
    @Getter
    @RequireRerender
    private boolean active = false;


    private final long energyPerTick;
    private static final int INVENTORY_SIZE = 21;
    @Persisted
    private int selectedDiscIndex;
    @Persisted
    private int discProgress;
    private int discLength;

    public JukeboxMachine(IMachineBlockEntity holder, int tier, Object... args) {
        super(holder, tier, args);
        this.energyPerTick = calculateEut();
        this.inventory = new NotifiableItemStackHandler(this, INVENTORY_SIZE, IO.BOTH);
        this.selectedDiscIndex = 0;
        this.discProgress = 0;
    }

//    private void updateDisc() {
//        if (discProgress >= discLength) {
//            {
//            } else {
//                this.discLength = -1;
//            }
//        } else {
//            discProgress++;
//        }
//    }

    private void playDisc() {
        if (!this.isWorkingEnabled) {
            return;
        }

        active = true;
        Item item = inventory.getStackInSlot(selectedDiscIndex).getItem();
        if (item instanceof RecordItem record) {
            this.discLength = record.getLengthInTicks();
            this.discProgress = 0;
            Level level = this.getLevel();
            if (level != null && !level.isClientSide) {
                record.getSound().getLocation();
//                level.playSound(null, getPos(), );
            }
        }
    }

    // TODO: continue on load
    @Override
    public void onLoad() {
        super.onLoad();
        Item item = inventory.getStackInSlot(selectedDiscIndex).getItem();
        if (item instanceof RecordItem record) {
            this.discLength = record.getLengthInTicks();
        } else {
            this.discLength = -1;
        }
    }

    public long calculateEut() {
        if (this.tier <= GTValues.ULV) {
            return 2;
        } else {
            return GTValues.V[this.tier] / 16;
        }
    }

    /// ----------- UI -----------
    private void onPlayClick(ClickData data) {
        playDisc();
    }

    @Override
    public Widget createUIWidget() {
        WidgetGroup widgetGroup = new WidgetGroup(0,0,176, 60);
        int x = 0;
        int y = 0;
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            SlotWidget slotWidget = new SlotWidget(inventory, slot, x * 18 + 7, y * 18);
            if (slot == selectedDiscIndex) {
                slotWidget.setBackgroundTexture(GuiTextures.SLOT_DARK);
            } else {
                slotWidget.setBackgroundTexture(GuiTextures.SLOT);
            }
            widgetGroup.addWidget(slotWidget);
            x++;
            if (x == 7) {
                x = 0;
                y++;
            }
        }
        widgetGroup.addWidget(new ButtonWidget(135, 35, 18, 18, GuiTextures.BUTTON_POWER, this::onPlayClick));
        return widgetGroup;
    }

    @Override
    public int getProgress() {
        return this.discProgress;
    }

    @Override
    public int getMaxProgress() {
        return this.discLength;
    }


    @Override
    public void setWorkingEnabled(boolean workingEnabled) {
        isWorkingEnabled = workingEnabled;
    }
}

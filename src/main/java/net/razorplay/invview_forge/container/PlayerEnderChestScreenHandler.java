package net.razorplay.invview_forge.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class PlayerEnderChestScreenHandler extends Container {

    private final ServerPlayerEntity player;
    private final EnderChestInventory enderChestInventory;

    public PlayerEnderChestScreenHandler(int syncId, ServerPlayerEntity player, PlayerInventory viewInventory) {
        super(ContainerType.GENERIC_9x3, syncId);
        this.player = player;
        this.enderChestInventory = viewInventory.player.getEnderChestInventory();
        PlayerInventory playerInventory = player.inventory;

        int rows = 3;
        int i = (rows - 4) * 18;
        int n;
        int m;
        for (n = 0; n < rows; ++n) {
            for (m = 0; m < 9; ++m) {
                this.addSlot(new Slot(enderChestInventory, m + n * 9, 8 + m * 18, 18 + n * 18));
            }
        }

        for (n = 0; n < 3; ++n) {
            for (m = 0; m < 9; ++m) {
                this.addSlot(new Slot(playerInventory, m + n * 9 + 9, 8 + m * 18, 103 + n * 18 + i));
            }
        }

        for (n = 0; n < 9; ++n) {
            this.addSlot(new Slot(playerInventory, n, 8 + n * 18, 161 + i));
        }
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int index) {
        this.resendInventory();
        return new ItemStack(Items.OAK_PLANKS);
    }

    @Override
    public ItemStack clicked(int i, int j, ClickType actionType, PlayerEntity playerEntity) {
        if (i > 40 && i < 45) {
            resendInventory();
            return ItemStack.EMPTY;
        }

        return super.clicked(i, j, actionType, playerEntity);
    }

    private void resendInventory() {
        player.refreshContainer(this, this.getItems());
    }

}

package net.razorplay.invview_forge.container;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.razorplay.invview_forge.util.ITargetPlayerContainer;
import net.razorplay.invview_forge.InvView_Forge;
import net.razorplay.invview_forge.util.InventoryLockManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PlayerInventoryScreenHandler extends AbstractContainerMenu implements ITargetPlayerContainer {

    private static final int TARGET_INVENTORY_ROWS = 5;
    private static final int TARGET_INVENTORY_COLUMNS = 9;
    private static final int PLAYER_INVENTORY_ROWS = 3;
    private static final int PLAYER_INVENTORY_COLUMNS = 9;
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INVENTORY_Y_OFFSET = 103;
    private static final int HOTBAR_Y_OFFSET = 161;

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMNS * PLAYER_INVENTORY_ROWS;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
    private static final int TE_INVENTORY_SLOT_COUNT = TARGET_INVENTORY_COLUMNS * TARGET_INVENTORY_ROWS;

    private final ServerPlayer targetPlayer;
    private final UUID targetPlayerUUID;

    public PlayerInventoryScreenHandler(int syncId, ServerPlayer player, ServerPlayer targetPlayer) {
        super(MenuType.GENERIC_9x5, syncId);
        this.targetPlayer = targetPlayer;
        this.targetPlayerUUID = targetPlayer.getUUID();

        if (!tryLockInventory(targetPlayerUUID)) {
            handleInventoryLockFailure(player);
            return;
        }

        addTargetInventorySlots(targetPlayer.getInventory());
        addPlayerInventorySlots(player.getInventory());
    }

    private boolean tryLockInventory(UUID playerUUID) {
        return InventoryLockManager.tryLock(playerUUID, InventoryLockManager.InventoryType.PLAYER_INVENTORY);
    }

    private void handleInventoryLockFailure(ServerPlayer player) {
        player.closeContainer();
        player.displayClientMessage(Component.translatable("inv_view_forge.inventory_in_use.error"), false);
    }

    private void addTargetInventorySlots(Inventory targetInventory) {
        for (int row = 0; row < TARGET_INVENTORY_ROWS; ++row) {
            for (int column = 0; column < TARGET_INVENTORY_COLUMNS; ++column) {
                this.addSlot(new Slot(targetInventory, column + row * TARGET_INVENTORY_COLUMNS, 8 + column * SLOT_SIZE, 18 + row * SLOT_SIZE));
            }
        }
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        for (int row = 0; row < PLAYER_INVENTORY_ROWS; ++row) {
            for (int column = 0; column < PLAYER_INVENTORY_COLUMNS; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * PLAYER_INVENTORY_COLUMNS + 9, 8 + column * SLOT_SIZE, PLAYER_INVENTORY_Y_OFFSET + row * SLOT_SIZE));
            }
        }

        for (int column = 0; column < HOTBAR_SLOT_COUNT; ++column) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * SLOT_SIZE, HOTBAR_Y_OFFSET));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            InvView_Forge.LOGGER.info("Invalid slotIndex:{}", index);
            return ItemStack.EMPTY;
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(player, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType actionType, Player player) {
        if (slotIndex >= 41 && slotIndex <= 44) {
            if (actionType == ClickType.QUICK_MOVE) {
                super.clicked(slotIndex, button, actionType, player);
            } else {
                player.getInventory().setItem(slotIndex, ItemStack.EMPTY);
            }
        } else {
            super.clicked(slotIndex, button, actionType, player);
        }
        targetPlayer.inventoryMenu.sendAllDataToRemote();
    }

    @Override
    public void removed(@NotNull Player player) {
        InventoryLockManager.unlock(targetPlayerUUID, InventoryLockManager.InventoryType.PLAYER_INVENTORY);
        InvView_Forge.savePlayerData(targetPlayer);
        super.removed(player);
    }

    @Override
    public ServerPlayer getTargetPlayer() {
        return targetPlayer;
    }
}
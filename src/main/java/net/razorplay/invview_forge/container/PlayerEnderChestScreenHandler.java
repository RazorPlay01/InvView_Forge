package net.razorplay.invview_forge.container;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.razorplay.invview_forge.util.ITargetPlayerContainer;
import net.razorplay.invview_forge.InvView_Forge;
import net.razorplay.invview_forge.util.InventoryLockManager;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerEnderChestScreenHandler extends AbstractContainerMenu implements ITargetPlayerContainer {

    // Constantes para tamaños de inventario
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    // Tamaño del inventario del Ender Chest
    private int TE_INVENTORY_SLOT_COUNT;

    private final ServerPlayer targetPlayer;
    private final UUID targetPlayerUUID;
    private int rows;

    public PlayerEnderChestScreenHandler(MenuType<ChestMenu> menuType, int syncId, ServerPlayer player, ServerPlayer targetPlayer) {
        super(menuType, syncId);
        this.targetPlayer = targetPlayer;
        this.targetPlayerUUID = targetPlayer.getUUID();

        if (!tryLockInventory(player)) {
            return;
        }

        initializeInventorySize();
        addEnderChestSlots();
        addPlayerInventorySlots(player);
    }

    private boolean tryLockInventory(ServerPlayer player) {
        if (!InventoryLockManager.tryLock(targetPlayerUUID, InventoryLockManager.InventoryType.ENDER_CHEST)) {
            player.closeContainer();
            player.displayClientMessage(Component.translatable("inv_view_forge.inventory_in_use.error"), false);
            return false;
        }
        return true;
    }

    private void initializeInventorySize() {
        int containerSize = targetPlayer.getEnderChestInventory().getContainerSize();
        rows = switch (containerSize) {
            case 9 -> 1;
            case 18 -> 2;
            case 36 -> 4;
            case 45 -> 5;
            case 54 -> 6;
            default -> 3;
        };
        TE_INVENTORY_SLOT_COUNT = 9 * rows;
    }

    private void addEnderChestSlots() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(targetPlayer.getEnderChestInventory(), col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
    }

    private void addPlayerInventorySlots(ServerPlayer player) {
        int yOffset = (rows - 4) * 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(player.getInventory(), col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + yOffset));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(player.getInventory(), col, 8 + col * 18, 161 + yOffset));
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player playerIn, int index) {
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

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public void removed(@NotNull Player player) {
        InventoryLockManager.unlock(targetPlayerUUID, InventoryLockManager.InventoryType.ENDER_CHEST);
        InvView_Forge.savePlayerData(targetPlayer);
        super.removed(player);
    }

    @Override
    public ServerPlayer getTargetPlayer() {
        return this.targetPlayer;
    }
}
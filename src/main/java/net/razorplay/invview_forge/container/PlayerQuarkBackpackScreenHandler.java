package net.razorplay.invview_forge.container;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.razorplay.invview_forge.InvView_Forge;
import net.razorplay.invview_forge.util.ITargetPlayerContainer;
import net.razorplay.invview_forge.util.InventoryLockManager;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerQuarkBackpackScreenHandler extends AbstractContainerMenu implements ITargetPlayerContainer {

    private static final int BACKPACK_ROWS = 3;
    private static final int BACKPACK_COLUMNS = 9;
    private static final int PLAYER_INVENTORY_ROWS = 3;
    private static final int PLAYER_INVENTORY_COLUMNS = 9;
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INVENTORY_Y_OFFSET = 103;
    private static final int HOTBAR_Y_OFFSET = 161;

    private final ServerPlayer targetPlayer;
    private final UUID targetPlayerUUID;
    private final SimpleContainer backpackInv = new SimpleContainer(BACKPACK_ROWS * BACKPACK_COLUMNS);

    public PlayerQuarkBackpackScreenHandler(int syncId, ServerPlayer player, ServerPlayer targetPlayer) {
        super(MenuType.GENERIC_9x3, syncId);
        this.targetPlayer = targetPlayer;
        this.targetPlayerUUID = targetPlayer.getUUID();

        if (!tryLockInventory(targetPlayerUUID)) {
            handleInventoryLockFailure(player);
            return;
        }

        initializeBackpackInventory(targetPlayer);
        addBackpackSlots();
        addPlayerInventorySlots(player.getInventory());
    }

    private boolean tryLockInventory(UUID playerUUID) {
        return InventoryLockManager.tryLock(playerUUID, InventoryLockManager.InventoryType.QUARK_BACKPACK);
    }

    private void handleInventoryLockFailure(ServerPlayer player) {
        player.closeContainer();
        player.displayClientMessage(Component.translatable("inv_view_forge.inventory_in_use.error"), false);
    }

    private void initializeBackpackInventory(ServerPlayer targetPlayer) {
        ItemStack backpack = targetPlayer.getItemBySlot(EquipmentSlot.CHEST);
        LazyOptional<IItemHandler> handlerOpt = backpack.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
        IItemHandler handler = handlerOpt.orElse(new ItemStackHandler());

        for (int i = 0; i < handler.getSlots(); ++i) {
            backpackInv.setItem(i, handler.getStackInSlot(i));
        }
    }

    private void addBackpackSlots() {
        for (int row = 0; row < BACKPACK_ROWS; ++row) {
            for (int column = 0; column < BACKPACK_COLUMNS; ++column) {
                this.addSlot(new Slot(backpackInv, column + row * BACKPACK_COLUMNS, 8 + column * SLOT_SIZE, 18 + row * SLOT_SIZE));
            }
        }
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        for (int row = 0; row < PLAYER_INVENTORY_ROWS; ++row) {
            for (int column = 0; column < PLAYER_INVENTORY_COLUMNS; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * PLAYER_INVENTORY_COLUMNS + 9, 8 + column * SLOT_SIZE, PLAYER_INVENTORY_Y_OFFSET + row * SLOT_SIZE));
            }
        }

        for (int column = 0; column < PLAYER_INVENTORY_COLUMNS; ++column) {
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

        if (index < backpackInv.getContainerSize()) {
            if (!moveItemStackTo(sourceStack, backpackInv.getContainerSize(), slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(sourceStack, 0, backpackInv.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        if (sourceStack.getCount() == copyOfSourceStack.getCount()) {
            return ItemStack.EMPTY;
        }

        sourceSlot.onTake(player, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType actionType, Player player) {
        if (slotIndex > backpackInv.getContainerSize() && slotIndex < (BACKPACK_ROWS * BACKPACK_COLUMNS)) {
            if (actionType == ClickType.QUICK_MOVE) {
                super.clicked(slotIndex, button, actionType, player);
            } else {
                player.getInventory().setItem(slotIndex, ItemStack.EMPTY);
            }
        } else {
            super.clicked(slotIndex, button, actionType, player);
        }
        saveBackpackInventory(targetPlayer);
    }

    private void saveBackpackInventory(ServerPlayer targetPlayer) {
        ItemStack backpack = targetPlayer.getItemBySlot(EquipmentSlot.CHEST);
        LazyOptional<IItemHandler> handlerOpt = backpack.getCapability(ForgeCapabilities.ITEM_HANDLER, null);
        IItemHandler handler = handlerOpt.orElse(new ItemStackHandler());

        for (int i = 0; i < handler.getSlots(); ++i) {
            handler.extractItem(i, handler.getStackInSlot(i).getCount(), false);
        }

        for (int i = 0; i < handler.getSlots(); ++i) {
            ItemStack stack = backpackInv.getItem(i);
            if (!stack.isEmpty()) {
                handler.insertItem(i, stack, false);
            }
        }
    }

    @Override
    public void removed(@NotNull Player player) {
        InventoryLockManager.unlock(targetPlayerUUID, InventoryLockManager.InventoryType.QUARK_BACKPACK);
        saveBackpackInventory(targetPlayer);
        InvView_Forge.savePlayerData(targetPlayer);
        super.removed(player);
    }

    @Override
    public ServerPlayer getTargetPlayer() {
        return targetPlayer;
    }
}
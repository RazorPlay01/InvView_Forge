package net.razorplay.invview_forge.container;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.razorplay.invview_forge.util.ITargetPlayerContainer;
import net.razorplay.invview_forge.InvView_Forge;
import net.razorplay.invview_forge.util.InventoryLockManager;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerCuriosInventoryScreenHandler extends AbstractContainerMenu implements ITargetPlayerContainer {
    // Constantes para tamaños de inventario
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int CURIOS_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    private final ServerPlayer targetPlayer;
    private final UUID targetPlayerUUID;
    private SimpleContainer curiosInv;
    private final List<Integer> validCuriosSlots = new ArrayList<>();
    private int inventoryRows;
    private int totalSlots;

    public PlayerCuriosInventoryScreenHandler(MenuType<ChestMenu> menuType, int syncId, ServerPlayer player, ServerPlayer targetPlayer) {
        super(menuType, syncId);
        this.targetPlayer = targetPlayer;
        this.targetPlayerUUID = targetPlayer.getUUID();

        if (!tryLockInventory(player)) {
            return;
        }

        initializeInventorySize(menuType);
        loadCuriosInventory();
        addCuriosSlots();
        addPlayerInventorySlots(player);
    }

    private boolean tryLockInventory(ServerPlayer player) {
        if (!InventoryLockManager.tryLock(targetPlayerUUID, InventoryLockManager.InventoryType.CURIOS)) {
            player.closeContainer();
            player.displayClientMessage(Component.translatable("inv_view_forge.inventory_in_use.error"), false);
            return false;
        }
        return true;
    }

    private void initializeInventorySize(MenuType<ChestMenu> menuType) {
        if (menuType.equals(MenuType.GENERIC_9x1)) {
            inventoryRows = 1;
        } else if (menuType.equals(MenuType.GENERIC_9x2)) {
            inventoryRows = 2;
        } else if (menuType.equals(MenuType.GENERIC_9x3)) {
            inventoryRows = 3;
        } else if (menuType.equals(MenuType.GENERIC_9x4)) {
            inventoryRows = 4;
        } else if (menuType.equals(MenuType.GENERIC_9x5)) {
            inventoryRows = 5;
        } else if (menuType.equals(MenuType.GENERIC_9x6)) {
            inventoryRows = 6;
        }
        totalSlots = inventoryRows * 9;
        this.curiosInv = new SimpleContainer(totalSlots);
    }

    private void loadCuriosInventory() {
        CuriosApi.getCuriosInventory(targetPlayer).ifPresent(curiosHandler -> {
            int index = 0;
            for (ICurioStacksHandler handler : curiosHandler.getCurios().values()) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    if (index < totalSlots) {
                        ItemStack item = handler.getStacks().getStackInSlot(i);
                        curiosInv.setItem(index, item);
                        validCuriosSlots.add(index);
                        index++;
                    } else {
                        break;
                    }
                }
                if (index >= totalSlots) break;
            }
        });
    }

    private void addCuriosSlots() {
        for (int row = 0; row < inventoryRows; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(curiosInv, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
    }

    private void addPlayerInventorySlots(ServerPlayer player) {
        int yOffset = (inventoryRows - 4) * 18;
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
            if (!moveItemStackTo(sourceStack, CURIOS_FIRST_SLOT_INDEX, CURIOS_FIRST_SLOT_INDEX + totalSlots, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < CURIOS_FIRST_SLOT_INDEX + totalSlots) {
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
        InventoryLockManager.unlock(targetPlayerUUID, InventoryLockManager.InventoryType.CURIOS);
        saveCuriosInventory();
        InvView_Forge.savePlayerData(targetPlayer);
        super.removed(player);
    }

    private void saveCuriosInventory() {
        CuriosApi.getCuriosInventory(targetPlayer).ifPresent(curiosHandler -> {
            int slotIndex = 0;
            for (ICurioStacksHandler handler : curiosHandler.getCurios().values()) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    if (slotIndex < totalSlots) {
                        ItemStack itemStack = curiosInv.getItem(slotIndex);
                        handler.getStacks().setStackInSlot(i, itemStack);
                        slotIndex++;
                    } else {
                        break;
                    }
                }
            }
        });
    }

    @Override
    public ServerPlayer getTargetPlayer() {
        return this.targetPlayer;
    }
}
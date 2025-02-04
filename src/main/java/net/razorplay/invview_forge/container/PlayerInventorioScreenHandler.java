package net.razorplay.invview_forge.container;

import de.rubixdev.inventorio.api.InventorioAPI;
import de.rubixdev.inventorio.player.PlayerInventoryAddon;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.razorplay.invview_forge.InvView_Forge;
import net.razorplay.invview_forge.util.ITargetPlayerContainer;
import net.razorplay.invview_forge.util.InventoryLockManager;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerInventorioScreenHandler extends AbstractContainerMenu implements ITargetPlayerContainer {

    // Constantes para tamaños de inventario
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
    private static final int TE_INVENTORY_SLOT_COUNT = 9 * 2;  // Tamaño del inventario de Inventorio

    private final SimpleContainer inventorioContainer = new SimpleContainer(TE_INVENTORY_SLOT_COUNT);
    private final ServerPlayer targetPlayer;
    private final UUID targetPlayerUUID;
    private int totalItems = 0;

    public PlayerInventorioScreenHandler(int syncId, ServerPlayer player, ServerPlayer targetPlayer) {
        super(MenuType.GENERIC_9x2, syncId);
        this.targetPlayer = targetPlayer;
        this.targetPlayerUUID = targetPlayer.getUUID();

        if (!tryLockInventory(player)) {
            return;
        }

        initializeInventorioContainer();
        addInventorioSlots();
        addPlayerInventorySlots(player);
    }

    private boolean tryLockInventory(ServerPlayer player) {
        if (!InventoryLockManager.tryLock(targetPlayerUUID, InventoryLockManager.InventoryType.INVENTORIO)) {
            player.closeContainer();
            player.displayClientMessage(Component.translatable("inv_view_forge.inventory_in_use.error"), false);
            return false;
        }
        return true;
    }

    private void initializeInventorioContainer() {
        PlayerInventoryAddon playerInventoryAddon = InventorioAPI.getInventoryAddon(targetPlayer);

        playerInventoryAddon.toolBelt.forEach(item -> {
            inventorioContainer.setItem(totalItems, item);
            totalItems++;
        });

        playerInventoryAddon.utilityBelt.forEach(item -> {
            inventorioContainer.setItem(totalItems, item);
            totalItems++;
        });
    }

    private void addInventorioSlots() {
        int rows = 2;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventorioContainer, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
    }

    private void addPlayerInventorySlots(ServerPlayer player) {
        int rows = 2;
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
    public void clicked(int slotIndex, int button, @NotNull ClickType actionType, @NotNull Player player) {
        if (slotIndex > totalItems && slotIndex < TE_INVENTORY_SLOT_COUNT) {
            if (actionType == ClickType.QUICK_MOVE) {
                super.clicked(slotIndex, button, actionType, player);
            } else {
                player.getInventory().setItem(slotIndex, ItemStack.EMPTY);
            }
        } else {
            super.clicked(slotIndex, button, actionType, player);
        }
        saveInventorio(targetPlayer);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void removed(@NotNull Player player) {
        InventoryLockManager.unlock(targetPlayerUUID, InventoryLockManager.InventoryType.INVENTORIO);
        saveInventorio(targetPlayer);
        InvView_Forge.savePlayerData(targetPlayer);
        totalItems = 0;
        super.removed(player);
    }

    private void saveInventorio(ServerPlayer targetPlayer) {
        PlayerInventoryAddon playerInventoryAddon = InventorioAPI.getInventoryAddon(targetPlayer);

        for (int i = 0; i <= 4; i++) {
            playerInventoryAddon.toolBelt.set(i, inventorioContainer.getItem(i));
        }

        for (int i = 0; i <= 7; i++) {
            playerInventoryAddon.utilityBelt.set(i, inventorioContainer.getItem(i + 5));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
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
        sourceSlot.onTake(player, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public ServerPlayer getTargetPlayer() {
        return this.targetPlayer;
    }
}
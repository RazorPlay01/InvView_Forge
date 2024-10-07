/*
package net.razorplay.invview_forge.container;

import de.rubixdev.inventorio.api.InventorioAPI;
import de.rubixdev.inventorio.player.PlayerInventoryAddon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.razorplay.invview_forge.InvView_Forge;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PlayerInventorioScreenHandler extends AbstractContainerMenu {
    public static final List<ServerPlayer> inventorioScreenTargetPlayers = new ArrayList<>();
    private final SimpleContainer inventorioContainer = new SimpleContainer(9 * 2);
    private final ServerPlayer targetPlayer;
    private int totalItems = 0;

    public PlayerInventorioScreenHandler(int syncId, ServerPlayer player, ServerPlayer targetPlayer) {
        super(MenuType.GENERIC_9x2, syncId);
        this.targetPlayer = targetPlayer;

        // Añadir el jugador objetivo a la lista de jugadores si no está ya en ella
        if (!inventorioScreenTargetPlayers.contains(targetPlayer)) {
            inventorioScreenTargetPlayers.add(targetPlayer);
        }

        PlayerInventoryAddon playerInventoryAddon = InventorioAPI.getInventoryAddon(targetPlayer);

        playerInventoryAddon.toolBelt.forEach(item -> {
            inventorioContainer.setItem(totalItems, item);
            totalItems = totalItems + 1;
        });
        playerInventoryAddon.utilityBelt.forEach(item -> {
            inventorioContainer.setItem(totalItems, item);
            totalItems = totalItems + 1;
        });

        int rows = 2;
        int i = (rows - 4) * 18;
        int n;
        int m;
        for (n = 0; n < rows; ++n) {
            for (m = 0; m < 9; ++m) {
                this.addSlot(new Slot(inventorioContainer, m + n * 9, 8 + m * 18, 18 + n * 18));
            }
        }

        for (n = 0; n < 3; ++n) {
            for (m = 0; m < 9; ++m) {
                this.addSlot(new Slot(player.getInventory(), m + n * 9 + 9, 8 + m * 18, 103 + n * 18 + i));
            }
        }

        for (n = 0; n < 9; ++n) {
            this.addSlot(new Slot(player.getInventory(), n, 8 + n * 18, 161 + i));
        }
    }

    @Override
    public void clicked(int i, int j, @NotNull ClickType actionType, @NotNull Player playerEntity) {
        if (i > this.totalItems && i < (9 * 2)) {
            if (actionType == ClickType.QUICK_MOVE) {
                super.clicked(i, j, actionType, playerEntity);
            } else {
                playerEntity.getInventory().setItem(i, ItemStack.EMPTY);
            }
        } else {
            super.clicked(i, j, actionType, playerEntity);
        }
        saveInv(targetPlayer);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void removed(@NotNull Player player) {
        saveInv(targetPlayer);
        InvView_Forge.savePlayerData(targetPlayer);

        inventorioScreenTargetPlayers.removeIf(p -> p.equals(targetPlayer));
        totalItems = 0;
        super.removed(player);
    }

    public void saveInv(ServerPlayer targetPlayer) {
        PlayerInventoryAddon playerInventoryAddon = InventorioAPI.getInventoryAddon(targetPlayer);

        for (int i = 0; i <= 4; i++) {
            playerInventoryAddon.toolBelt.set(i, inventorioContainer.getItem(i));
        }

        for (int i = 0; i <= 7; i++) {
            playerInventoryAddon.utilityBelt.set(i, inventorioContainer.getItem(i + 5));
        }

    }

    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    // must assign a slot number to each of the slots used by the GUI.
    // For this container, we can see both the tile inventory's slots as well as the player inventory slots and the hotbar.
    // Each time we add a Slot to the container, it automatically increases the slotIndex, which means
    //  0 - 8 = hotbar slots (which will map to the InventoryPlayer slot numbers 0 - 8)
    //  9 - 35 = player inventory slots (which map to the InventoryPlayer slot numbers 9 - 35)
    //  36 - 44 = TileInventory slots, which map to our TileEntity slot numbers 0 - 8)
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    // THIS YOU HAVE TO DEFINE!
    private static final int TE_INVENTORY_SLOT_COUNT = 9 * 2;  // must be the number of slots you have!

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;  //EMPTY_ITEM
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Check if the slot clicked is one of the vanilla container slots
        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX
                    + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;  // EMPTY_ITEM
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // This is a TE slot so merge the stack into the players inventory
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            System.out.println("Invalid slotIndex:" + index);
            return ItemStack.EMPTY;
        }
        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }
}
*/

package net.razorplay.invview_forge.container;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.razorplay.invview_forge.InvView_Forge;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerCuriosInventoryScreenHandler extends AbstractContainerMenu {
    public static List<ServerPlayer> curiosInvScreenTargetPlayers = new ArrayList<>();
    private final SimpleContainer curiosInv = new SimpleContainer(9 * 3);
    private final ServerPlayer targetPlayer;
    private int contador = 0;
    private final List<List<Object>> twoDimensionalList = new ArrayList<>();

    public PlayerCuriosInventoryScreenHandler(int syncId, ServerPlayer player, ServerPlayer targetPlayer) {
        super(MenuType.GENERIC_9x3, syncId);
        this.targetPlayer = targetPlayer;

        curiosInvScreenTargetPlayers.add(targetPlayer);

        // This gives access to the whole inventory
        CuriosApi.getCuriosInventory(targetPlayer).ifPresent(inv -> inv.getCurios().forEach((key, value) -> {
            for (int i = 0; i < value.getSlots(); i++) {
                ItemStack item = value.getStacks().getStackInSlot(i);

                List<Object> row1 = new ArrayList<>();
                row1.add(key);
                row1.add(item);
                twoDimensionalList.add(row1);

                curiosInv.setItem(contador, item);
                contador = contador + 1;
            }
        }));


        int rows = 3;
        int i = (rows - 4) * 18;
        int n;
        int m;
        for (n = 0; n < rows; ++n) {
            for (m = 0; m < 9; ++m) {
                this.addSlot(new Slot(curiosInv, m + n * 9, 8 + m * 18, 18 + n * 18));
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
        if (i > twoDimensionalList.size() && i < (9 * 3)) {
            if (actionType == ClickType.QUICK_MOVE) {
                super.clicked(i, j, actionType, playerEntity);
            } else {
                playerEntity.getInventory().setItem(i, ItemStack.EMPTY);
            }
        } else {
            super.clicked(i, j, actionType, playerEntity);
        }
        saveCuriosInv(targetPlayer);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void removed(@NotNull Player player) {
        saveCuriosInv(targetPlayer);
        InvView_Forge.savePlayerData(targetPlayer);


        for (int i = 0; i < curiosInvScreenTargetPlayers.size(); i++) {
            if (curiosInvScreenTargetPlayers.get(i).equals(targetPlayer)) {
                curiosInvScreenTargetPlayers.remove(i);
                break;
            }
        }

        super.removed(player);
    }

    public void saveCuriosInv(ServerPlayer targetPlayer) {
        int contador = 0;
        for (List<Object> row : twoDimensionalList) {
            String string = (String) row.get(0); // Obtener el string de la lista

            List<Object> row1 = new ArrayList<>();
            row1.add(string);
            row1.add(curiosInv.getItem(contador));
            twoDimensionalList.set(contador, row1);

            contador = contador + 1;
        }
        this.contador = 0;

        AtomicReference<String> prevString = new AtomicReference<>("");
        AtomicInteger contador2 = new AtomicInteger();
        CuriosApi.getCuriosInventory(targetPlayer).ifPresent(inv -> inv.getCurios().forEach((key, value) -> {
            for (List<Object> row : twoDimensionalList) {
                String string = (String) row.get(0);
                ItemStack itemStack = (ItemStack) row.get(1);

                if (prevString.get().equals(string)) {
                    contador2.set(contador2.get() + 1);
                } else {
                    contador2.set(0);
                }

                if (key.equals(string)) {
                    value.getStacks().setStackInSlot(contador2.get(), itemStack);
                }
                prevString.set(string);
            }
        }));
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
    private static final int TE_INVENTORY_SLOT_COUNT = 9 * 3;  // must be the number of slots you have!

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

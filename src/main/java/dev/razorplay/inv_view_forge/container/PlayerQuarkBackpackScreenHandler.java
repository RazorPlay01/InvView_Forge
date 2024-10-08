package dev.razorplay.inv_view_forge.container;

import net.minecraft.core.Direction;
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
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import dev.razorplay.inv_view_forge.InvView_Forge;

import java.util.ArrayList;
import java.util.List;

public class PlayerQuarkBackpackScreenHandler extends AbstractContainerMenu {
    public static final List<ServerPlayer> quarkBackpackScreenTargetPlayers = new ArrayList<>();
    private final ServerPlayer targetPlayer;
    private final SimpleContainer backpackInv = new SimpleContainer(9 * 3);


    public PlayerQuarkBackpackScreenHandler(int syncId, ServerPlayer player, ServerPlayer targetPlayer) {
        super(MenuType.GENERIC_9x3, syncId);
        this.targetPlayer = targetPlayer;
        Inventory playerInventory = player.getInventory();

        // Añadir el jugador objetivo a la lista de jugadores si no está ya en ella
        if (!quarkBackpackScreenTargetPlayers.contains(targetPlayer)) {
            quarkBackpackScreenTargetPlayers.add(targetPlayer);
        }

        ItemStack backpack = targetPlayer.getItemBySlot(EquipmentSlot.CHEST);
        LazyOptional<IItemHandler> handlerOpt = backpack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, (Direction) null);
        IItemHandler handler = handlerOpt.orElse(new ItemStackHandler());
        for (int i = 0; i < handler.getSlots(); ++i) {
            ItemStack inStack = handler.getStackInSlot(i);
            backpackInv.setItem(i, inStack);
        }

        int rows = 3;
        int i = (rows - 4) * 18;
        int n;
        int m;
        for (n = 0; n < rows; ++n) {
            for (m = 0; m < 9; ++m) {
                this.addSlot(new Slot(backpackInv, m + n * 9, 8 + m * 18, 18 + n * 18));
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
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Si el slot es del inventario de la mochila
        if (index < backpackInv.getContainerSize()) {
            // Intenta mover al inventario del jugador
            if (!moveItemStackTo(sourceStack, backpackInv.getContainerSize(), slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Si el slot es del inventario del jugador, intenta mover a la mochila
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

        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public void clicked(int i, int j, ClickType actionType, Player playerEntity) {
        if (i > backpackInv.getContainerSize() && i < (9 * 3)) {
            if (actionType == ClickType.QUICK_MOVE) {
                super.clicked(i, j, actionType, playerEntity);
            } else {
                playerEntity.getInventory().setItem(i, ItemStack.EMPTY);
            }
        } else {
            super.clicked(i, j, actionType, playerEntity);
        }
        saveBackpackInv(targetPlayer);
    }

    public void saveBackpackInv(ServerPlayer targetPlayer) {
        ItemStack backpack = targetPlayer.getItemBySlot(EquipmentSlot.CHEST);
        LazyOptional<IItemHandler> handlerOpt = backpack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, (Direction) null);
        IItemHandler handler = handlerOpt.orElse(new ItemStackHandler());

        // Primero, limpiamos el inventario de la mochila
        for (int i = 0; i < handler.getSlots(); ++i) {
            handler.extractItem(i, handler.getStackInSlot(i).getCount(), false);
        }

        // Luego, insertamos los items del inventario temporal
        for (int i = 0; i < handler.getSlots(); ++i) {
            ItemStack stack = backpackInv.getItem(i);
            if (!stack.isEmpty()) {
                handler.insertItem(i, stack, false);
            }
        }
    }

    @Override
    public void removed(Player player) {
        saveBackpackInv(targetPlayer);
        InvView_Forge.savePlayerData(targetPlayer);

        quarkBackpackScreenTargetPlayers.removeIf(p -> p.equals(targetPlayer));
        super.removed(player);
    }
}

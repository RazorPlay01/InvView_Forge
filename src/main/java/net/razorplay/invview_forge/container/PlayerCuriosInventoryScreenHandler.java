package net.razorplay.invview_forge.container;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.razorplay.invview_forge.InvView_Forge;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.ArrayList;
import java.util.List;

public class PlayerCuriosInventoryScreenHandler extends AbstractContainerMenu {
    private int inventoryRows = 1;
    private int totalSlots;

    private final List<Integer> validCuriosSlots = new ArrayList<>();
    public static final List<ServerPlayer> curiosInvScreenTargetPlayers = new ArrayList<>();
    private final SimpleContainer curiosInv;
    private final ServerPlayer targetPlayer;

    public PlayerCuriosInventoryScreenHandler(MenuType<ChestMenu> menuType, int syncId, ServerPlayer player, ServerPlayer targetPlayer) {
        super(menuType, syncId);
        setInventoryRows(menuType);
        this.curiosInv = new SimpleContainer(totalSlots);
        this.targetPlayer = targetPlayer;

        // Añadir el jugador objetivo a la lista de jugadores si no está ya en ella
        if (!curiosInvScreenTargetPlayers.contains(targetPlayer)) {
            curiosInvScreenTargetPlayers.add(targetPlayer);
        }

        // Cargar los ítems y registrar los slots válidos
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
            validCuriosSlots.add(index);
        });

        int rows = inventoryRows;
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

    private void setInventoryRows(MenuType<ChestMenu> menuType) {
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
    }

    @Override
    public void clicked(int slotId, int button, @NotNull ClickType actionType, @NotNull Player player) {
        if (slotId >= 0 && slotId < totalSlots) {
            if (validCuriosSlots.contains(slotId)) {
                Slot slot = this.slots.get(slotId);
                ItemStack cursorStack = player.containerMenu.getCarried();
                ItemStack slotStack = slot.getItem();

                if (actionType == ClickType.PICKUP) {
                    if (cursorStack.isEmpty() && !slotStack.isEmpty()) {
                        // Tomar el ítem del slot
                        player.containerMenu.setCarried(slotStack.copy());
                        slot.set(ItemStack.EMPTY);
                    } else if (!cursorStack.isEmpty() && slotStack.isEmpty()) {
                        // Colocar el ítem en el slot vacío
                        if (isItemValidForSlot(slotId, cursorStack)) {
                            slot.set(cursorStack.copy());
                            player.containerMenu.setCarried(ItemStack.EMPTY);
                        }
                    } else if (!cursorStack.isEmpty() && !slotStack.isEmpty()) {
                        // Intercambiar ítems
                        if (isItemValidForSlot(slotId, cursorStack)) {
                            ItemStack temp = slotStack.copy();
                            slot.set(cursorStack.copy());
                            player.containerMenu.setCarried(temp);
                        }
                    }
                } else if (actionType == ClickType.QUICK_MOVE) {
                    if (!slotStack.isEmpty()) {
                        // Mover el ítem a otro slot disponible
                        ItemStack remainder = tryMoveItemToOtherSlot(slotStack);
                        slot.set(remainder);
                    }
                }

                saveCuriosInv(targetPlayer);
            }
            // Si el slot no es válido, no hacemos nada
        } else {
            super.clicked(slotId, button, actionType, player);
        }
    }

    private boolean isItemValidForSlot(int slotId, ItemStack itemStack) {
        // Implementa la lógica para verificar si el ítem es válido para el slot de curios
        // Puedes usar CuriosApi para verificar la compatibilidad
        return true; // Cambia esto según tus necesidades
    }

    private ItemStack tryMoveItemToOtherSlot(ItemStack itemStack) {
        // Intenta mover el ítem a otro slot disponible en el inventario de curios
        for (int i = 0; i < totalSlots; i++) {
            Slot targetSlot = this.slots.get(i);
            if (targetSlot.getItem().isEmpty() && isItemValidForSlot(i, itemStack)) {
                targetSlot.set(itemStack.copy());
                return ItemStack.EMPTY;
            }
        }

        // Si no se pudo mover dentro del inventario de curios, intenta moverlo al inventario del jugador
        ItemStack remainingStack = itemStack.copy();
        if (moveItemStackTo(remainingStack, totalSlots, this.slots.size(), false)) {
            if (remainingStack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return remainingStack;
    }

    public void saveCuriosInv(ServerPlayer targetPlayer) {
        // Obtener el inventario de curios y sincronizarlo con la instancia local
        CuriosApi.getCuriosInventory(targetPlayer).ifPresent(curiosHandler -> {
            int slotIndex = 0;
            for (ICurioStacksHandler handler : curiosHandler.getCurios().values()) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    if (slotIndex < totalSlots) {
                        // Actualiza cada slot del inventario de curios del jugador objetivo
                        ItemStack itemStack = curiosInv.getItem(slotIndex);
                        handler.getStacks().setStackInSlot(i, itemStack);
                        slotIndex++;
                    } else {
                        break;  // Evitar sobrepasar el tamaño del inventario
                    }
                }
            }
        });
    }

    @Override
    public void removed(@NotNull Player player) {
        // Guardar el inventario de curios antes de salir
        saveCuriosInv(targetPlayer);
        InvView_Forge.savePlayerData(targetPlayer);

        // Eliminar al jugador de la lista de jugadores objetivo
        curiosInvScreenTargetPlayers.remove(targetPlayer);

        super.removed(player);
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

        // Define el rango de slots para el inventario de curios
        int curiosFirstSlot = 0;
        int curiosLastSlot = totalSlots - 1;

        // Define el rango de slots para el inventario del jugador
        int playerFirstSlot = totalSlots;
        int playerLastSlot = slots.size() - 1;

        if (index >= curiosFirstSlot && index <= curiosLastSlot) {
            // El slot fuente está en el inventario de curios
            // Intenta mover al inventario del jugador
            if (!this.moveItemStackTo(sourceStack, playerFirstSlot, playerLastSlot + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= playerFirstSlot && index <= playerLastSlot) {
            // El slot fuente está en el inventario del jugador
            // Intenta mover a un slot válido del inventario de curios
            boolean moved = false;
            for (int validSlot : validCuriosSlots) {
                if (isItemValidForSlot(validSlot, sourceStack) && this.moveItemStackTo(sourceStack, validSlot, validSlot + 1, false)) {
                    moved = true;
                    break;
                }
            }
            if (!moved) {
                // Si no se pudo mover a un slot de curios, intenta mover a otra parte del inventario del jugador
                if (index >= playerFirstSlot && index < playerLastSlot - 9) {
                    if (!this.moveItemStackTo(sourceStack, playerLastSlot - 8, playerLastSlot + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= playerLastSlot - 8 && index <= playerLastSlot) {
                    if (!this.moveItemStackTo(sourceStack, playerFirstSlot, playerLastSlot - 9, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        } else {
            System.out.println("Invalid slotIndex:" + index);
            return ItemStack.EMPTY;
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
}

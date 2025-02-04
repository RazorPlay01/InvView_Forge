package net.razorplay.invview_forge.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
import com.mrcrayfish.backpacked.inventory.BackpackInventory;
import com.mrcrayfish.backpacked.inventory.BackpackedInventoryAccess;
import com.mrcrayfish.backpacked.platform.Services;
import com.tiviacz.travelersbackpack.capability.CapabilityUtils;
import com.tiviacz.travelersbackpack.util.Reference;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkHooks;
import net.razorplay.invview_forge.util.InventoryLockManager;
import net.razorplay.invview_forge.container.*;
import org.jetbrains.annotations.NotNull;
import org.violetmoon.quark.addons.oddities.item.BackpackItem;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.concurrent.atomic.AtomicInteger;

import static com.mrcrayfish.backpacked.item.BackpackItem.BACKPACK_TRANSLATION;

public class InvViewCommands {
    private static final String TARGET_ID = "target";
    private static final String CURIOS_ID = "curios";
    private static final String INVENTORIO_ID = "inventorio";
    private static final String TRAVELERS_BACKPACK_ID = "travelersbackpack";
    private static final String QUARK_ID = "quark";
    private static final String BACKPACKED_ID = "backpacked";

    public InvViewCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("view").requires(player -> player.hasPermission(2))
                .then(createCommand("inv", this::executeInventoryCheck))
                .then(createCommand("echest", this::executeEnderChestCheck))
                .then(createModCommand(CURIOS_ID, CURIOS_ID, this::executeCuriosCheck))
                .then(createModCommand(CURIOS_ID, CURIOS_ID + "-cosmetic", this::executeCuriosCosmeticCheck))
                .then(createModCommand(INVENTORIO_ID, INVENTORIO_ID, this::executeInventorioCheck))
                .then(createModCommand(TRAVELERS_BACKPACK_ID, TRAVELERS_BACKPACK_ID, this::executeTravelersBackpackCheck))
                .then(createModCommand(QUARK_ID, QUARK_ID + "-backpack", this::executeQuarkBackpackCheck))
                .then(createModCommand(BACKPACKED_ID, BACKPACKED_ID + "-backpack", this::executeBackpackedBackpackCheck))
        );
    }

    private LiteralArgumentBuilder<CommandSourceStack> createCommand(String literal, Command<CommandSourceStack> command) {
        return Commands.literal(literal).then(Commands.argument(TARGET_ID, GameProfileArgument.gameProfile()).executes(command));
    }

    private LiteralArgumentBuilder<CommandSourceStack> createModCommand(String modId, String literalCommand, Command<CommandSourceStack> command) {
        return ModList.get().isLoaded(modId) ? createCommand(literalCommand, command) : Commands.literal("");
    }

    private int executeBackpackedBackpackCheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer commandExecutor = context.getSource().getPlayer();
        ServerPlayer targetPlayer = getRequestedPlayer(context);

        ItemStack backpack = Services.BACKPACK.getBackpackStack(targetPlayer);
        if (!backpack.isEmpty()) {
            BackpackInventory backpackInventory = ((BackpackedInventoryAccess) targetPlayer).backpacked$GetBackpackInventory();
            if (backpackInventory == null)
                return 1;
            com.mrcrayfish.backpacked.item.BackpackItem backpackItem = (com.mrcrayfish.backpacked.item.BackpackItem) backpack.getItem();
            Component title = backpack.hasCustomHoverName() ? backpack.getHoverName() : BACKPACK_TRANSLATION;
            int cols = backpackItem.getColumnCount();
            int rows = backpackItem.getRowCount();
            boolean owner = targetPlayer.equals(commandExecutor);
            Services.BACKPACK.openBackpackScreen(commandExecutor, backpackInventory, cols, rows, owner, title);
        }

        return 1;
    }

    private int executeQuarkBackpackCheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer commandExecutor = context.getSource().getPlayer();
        ServerPlayer targetPlayer = getRequestedPlayer(context);

        if (ModList.get().isLoaded(QUARK_ID)) {
            if (targetPlayer.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof BackpackItem) {
                if (!InventoryLockManager.isLocked(targetPlayer.getUUID(), InventoryLockManager.InventoryType.QUARK_BACKPACK)) {
                    openScreen(commandExecutor, targetPlayer, (i, inventory, player) -> new PlayerQuarkBackpackScreenHandler(i, commandExecutor, targetPlayer));
                } else {
                    sendInventoryInUseError(context);
                }
            } else {
                sendNoBackpackEquippedMessage(context);
            }
        } else {
            sendDependencyNotFoundError(context);
        }
        return 1;
    }

    private int executeTravelersBackpackCheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer commandExecutor = context.getSource().getPlayer();
        ServerPlayer targetPlayer = getRequestedPlayer(context);

        if (ModList.get().isLoaded(TRAVELERS_BACKPACK_ID)) {
            if (CapabilityUtils.isWearingBackpack(targetPlayer)) {
                if (!context.getSource().getLevel().isClientSide) {
                    NetworkHooks.openScreen(commandExecutor, CapabilityUtils.getBackpackInv(targetPlayer), packetBuffer -> packetBuffer.writeByte(Reference.WEARABLE_SCREEN_ID));
                }
            } else {
                sendNoBackpackEquippedMessage(context);
            }
        } else {
            sendDependencyNotFoundError(context);
        }
        return 1;
    }

    private int executeInventorioCheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer commandExecutor = context.getSource().getPlayer();
        ServerPlayer targetPlayer = getRequestedPlayer(context);

        if (ModList.get().isLoaded(INVENTORIO_ID)) {
            if (!InventoryLockManager.isLocked(targetPlayer.getUUID(), InventoryLockManager.InventoryType.INVENTORIO)) {
                openScreen(commandExecutor, targetPlayer, (i, inventory, player) -> new PlayerInventorioScreenHandler(i, commandExecutor, targetPlayer));
            } else {
                sendInventoryInUseError(context);
            }
        } else {
            sendDependencyNotFoundError(context);
        }
        return 1;
    }

    private int executeCuriosCheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer commandExecutor = context.getSource().getPlayer();
        ServerPlayer targetPlayer = getRequestedPlayer(context);

        if (ModList.get().isLoaded(CURIOS_ID)) {
            if (!InventoryLockManager.isLocked(targetPlayer.getUUID(), InventoryLockManager.InventoryType.CURIOS)) {
                openScreen(commandExecutor, targetPlayer, (i, inventory, player) -> {
                    AtomicInteger index = new AtomicInteger();
                    CuriosApi.getCuriosInventory(targetPlayer).ifPresent(curiosHandler -> {
                        for (ICurioStacksHandler handler : curiosHandler.getCurios().values()) {
                            for (int j = 0; j < handler.getSlots(); j++) {
                                index.getAndIncrement();
                            }
                        }
                    });
                    MenuType<ChestMenu> menuType = getMenuType(index.get());
                    return new PlayerCuriosInventoryScreenHandler(menuType, i, commandExecutor, targetPlayer);
                });
            } else {
                sendInventoryInUseError(context);
            }
        } else {
            sendDependencyNotFoundError(context);
        }
        return 1;
    }

    private int executeCuriosCosmeticCheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer commandExecutor = context.getSource().getPlayer();
        ServerPlayer targetPlayer = getRequestedPlayer(context);

        if (ModList.get().isLoaded(CURIOS_ID)) {
            if (!InventoryLockManager.isLocked(targetPlayer.getUUID(), InventoryLockManager.InventoryType.CURIOS_COSMETIC)) {
                openScreen(commandExecutor, targetPlayer, (i, inventory, player) -> {
                    AtomicInteger index = new AtomicInteger();
                    CuriosApi.getCuriosInventory(targetPlayer).ifPresent(curiosHandler -> {
                        for (ICurioStacksHandler handler : curiosHandler.getCurios().values()) {
                            for (int j = 0; j < handler.getCosmeticStacks().getSlots(); j++) {
                                index.getAndIncrement();
                            }
                        }
                    });
                    MenuType<ChestMenu> menuType = getMenuType(index.get());
                    return new PlayerCuriosCosmeticInventoryScreenHandler(menuType, i, commandExecutor, targetPlayer);
                });
            } else {
                sendInventoryInUseError(context);
            }
        } else {
            sendDependencyNotFoundError(context);
        }
        return 1;
    }

    private int executeEnderChestCheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer commandExecutor = context.getSource().getPlayer();
        ServerPlayer targetPlayer = getRequestedPlayer(context);

        if (!InventoryLockManager.isLocked(targetPlayer.getUUID(), InventoryLockManager.InventoryType.ENDER_CHEST)) {
            openScreen(commandExecutor, targetPlayer, (i, inventory, player) -> {
                MenuType<ChestMenu> menuType = getMenuType(targetPlayer.getEnderChestInventory().getContainerSize());
                return new PlayerEnderChestScreenHandler(menuType, i, commandExecutor, targetPlayer);
            });
        } else {
            sendInventoryInUseError(context);
        }
        return 1;
    }

    private int executeInventoryCheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer commandExecutor = context.getSource().getPlayer();
        ServerPlayer targetPlayer = getRequestedPlayer(context);

        if (!InventoryLockManager.isLocked(targetPlayer.getUUID(), InventoryLockManager.InventoryType.PLAYER_INVENTORY)) {
            openScreen(commandExecutor, targetPlayer, (i, inventory, player) -> new PlayerInventoryScreenHandler(i, commandExecutor, targetPlayer));
        } else {
            sendInventoryInUseError(context);
        }
        return 1;
    }

    private void openScreen(ServerPlayer commandExecutor, ServerPlayer targetPlayer, ScreenHandlerFactory factory) {
        MenuProvider screenHandlerFactory = new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return targetPlayer.getDisplayName();
            }

            @Override
            public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
                return factory.createMenu(i, inventory, player);
            }
        };

        NetworkHooks.openScreen(commandExecutor, screenHandlerFactory);
    }

    private MenuType<ChestMenu> getMenuType(int size) {
        if (size >= 0 && size <= 9) {
            return MenuType.GENERIC_9x1;
        } else if (size > 9 && size <= 18) {
            return MenuType.GENERIC_9x2;
        } else if (size > 18 && size <= 27) {
            return MenuType.GENERIC_9x3;
        } else if (size > 27 && size <= 36) {
            return MenuType.GENERIC_9x4;
        } else if (size > 36 && size <= 45) {
            return MenuType.GENERIC_9x5;
        } else {
            return MenuType.GENERIC_9x6;
        }
    }

    private void sendInventoryInUseError(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.translatable("inv_view_forge.command.error.inventory_in_use"));
    }

    private void sendDependencyNotFoundError(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.translatable("inv_view_forge.command.error.dependency_not_found"));
    }

    private void sendNoBackpackEquippedMessage(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.translatable("inv_view_forge.command.error.player_without_backpack"));
    }

    private static ServerPlayer getRequestedPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MinecraftServer minecraftServer = context.getSource().getServer();

        GameProfile requestedProfile = GameProfileArgument.getGameProfiles(context, TARGET_ID).iterator().next();
        ServerPlayer requestedPlayer = minecraftServer.getPlayerList().getPlayerByName(requestedProfile.getName());

        if (requestedPlayer == null) {
            requestedPlayer = minecraftServer.getPlayerList().getPlayerForLogin(requestedProfile);
            CompoundTag compound = minecraftServer.getPlayerList().load(requestedPlayer);
            if (compound != null) {
                ServerLevel world = minecraftServer.getLevel(
                        DimensionType.parseLegacy(new Dynamic<>(NbtOps.INSTANCE, compound.get("Dimension")))
                                .result().get());

                if (world != null) {
                    requestedPlayer.setServerLevel(world);
                }
            }
        }
        return requestedPlayer;
    }

    @FunctionalInterface
    private interface ScreenHandlerFactory {
        AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player);
    }
}
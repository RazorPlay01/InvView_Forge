package dev.razorplay.inv_view_forge.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
import com.tiviacz.travelersbackpack.capability.CapabilityUtils;
import com.tiviacz.travelersbackpack.util.Reference;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
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
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.fml.ModList;
import dev.razorplay.inv_view_forge.container.*;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import vazkii.quark.addons.oddities.item.BackpackItem;

import java.util.concurrent.atomic.AtomicInteger;

public class InvViewCommands {
    private static final String TARGET_ID = "target";
    private static final String CURIOS_ID = "curios";

    public InvViewCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("view").requires(player -> player.hasPermission(2))
                .then(Commands.literal("inv")
                        .then(Commands.argument(TARGET_ID, GameProfileArgument.gameProfile())
                                .executes(this::executeInventoryCheck)))
                .then(Commands.literal("echest")
                        .then(Commands.argument(TARGET_ID, GameProfileArgument.gameProfile())
                                .executes(this::executeEnderChestCheck)))
                .then((ModList.get().isLoaded(CURIOS_ID) ?
                        Commands.literal(CURIOS_ID)
                                .then(Commands.argument(TARGET_ID, GameProfileArgument.gameProfile())
                                        .executes(this::executeCuriosCheck)
                                ) : Commands.literal("")))
                .then((ModList.get().isLoaded("inventorio") ?
                        Commands.literal("inventorio")
                                .then(Commands.argument(TARGET_ID, GameProfileArgument.gameProfile())
                                        .executes(context -> executeInventorioCheck(context, (ServerPlayer) context.getSource().getEntity()))
                                ) : Commands.literal("")))
                .then((ModList.get().isLoaded("travelersbackpack") ?
                        Commands.literal("travelersbackpack")
                                .then(Commands.argument(TARGET_ID, EntityArgument.player())
                                        .executes(context -> executeTravelersBackPackCheck(context, EntityArgument.getPlayer(context, TARGET_ID)))
                                ) : Commands.literal("")))
                .then((ModList.get().isLoaded("quark") ?
                        Commands.literal("quark-backpack")
                                .then(Commands.argument(TARGET_ID, EntityArgument.player())
                                        .executes(context -> executeQuarkBackPackCheck(context, EntityArgument.getPlayer(context, TARGET_ID)))
                                ) : Commands.literal("")))
        );
    }

    private int executeQuarkBackPackCheck(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        if (targetPlayer.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof BackpackItem) {
            boolean canOpen = true;

            if (!PlayerQuarkBackpackScreenHandler.quarkBackpackScreenTargetPlayers.isEmpty()) {
                for (int i = 0; i < PlayerQuarkBackpackScreenHandler.quarkBackpackScreenTargetPlayers.size(); i++) {
                    if (PlayerQuarkBackpackScreenHandler.quarkBackpackScreenTargetPlayers.get(i).getDisplayName().equals(targetPlayer.getDisplayName())) {
                        canOpen = false;
                        break;
                    }
                }
            }
            if (canOpen) {
                MenuProvider screenHandlerFactory = new MenuProvider() {
                    @Override
                    public @NotNull Component getDisplayName() {
                        return targetPlayer.getDisplayName();
                    }

                    @Override
                    public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player1) {
                        return new PlayerQuarkBackpackScreenHandler(i, (ServerPlayer) context.getSource().getEntity(), targetPlayer);
                    }
                };

                NetworkHooks.openScreen((ServerPlayer) context.getSource().getEntity(), screenHandlerFactory);
            } else {
                context.getSource().sendFailure(Component.literal("ERROR: The quark backpack container is already being used by another player."));
            }
        } else {
            context.getSource().getEntity().sendSystemMessage(Component.literal("The player does not have a currently equipped backpack."));
        }
        return 1;
    }


    private int executeTravelersBackPackCheck(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        if (CapabilityUtils.isWearingBackpack(targetPlayer)) {
            if (!context.getSource().getLevel().isClientSide) {
                ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
                NetworkHooks.openScreen(player, CapabilityUtils.getBackpackInv(targetPlayer), packetBuffer -> packetBuffer.writeByte(Reference.WEARABLE_SCREEN_ID));
            }
        } else {
            context.getSource().getEntity().sendSystemMessage(Component.literal("The player does not have a currently equipped backpack."));
        }
        return 1;
    }


    private int executeInventorioCheck(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException {
        ServerPlayer targetPlayer = getRequestedPlayer(context);
        boolean canOpen = true;

        if (!PlayerInventorioScreenHandler.inventorioScreenTargetPlayers.isEmpty()) {
            for (int i = 0; i < PlayerInventorioScreenHandler.inventorioScreenTargetPlayers.size(); i++) {
                if (PlayerInventorioScreenHandler.inventorioScreenTargetPlayers.get(i).getDisplayName().equals(targetPlayer.getDisplayName())) {
                    canOpen = false;
                    break;
                }
            }
        }
        if (canOpen) {
            MenuProvider screenHandlerFactory = new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return targetPlayer.getDisplayName();
                }

                @Override
                public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player1) {
                    return new PlayerInventorioScreenHandler(i, player, targetPlayer);

                }
            };

            NetworkHooks.openScreen(player, screenHandlerFactory);
        } else {
            context.getSource().sendFailure(Component.literal("ERROR: The inventorio inventory container is already being used by another player."));
        }
        return 1;
    }

    private int executeCuriosCheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = getRequestedPlayer(context);
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();

        if (ModList.get().isLoaded(CURIOS_ID)) {
            boolean canOpen = true;

            if (!PlayerCuriosInventoryScreenHandler.curiosInvScreenTargetPlayers.isEmpty()) {
                for (int i = 0; i < PlayerCuriosInventoryScreenHandler.curiosInvScreenTargetPlayers.size(); i++) {
                    if (PlayerCuriosInventoryScreenHandler.curiosInvScreenTargetPlayers.get(i).getDisplayName().equals(targetPlayer.getDisplayName())) {
                        canOpen = false;
                        break;
                    }
                }
            }
            if (canOpen) {
                MenuProvider screenHandlerFactory = new MenuProvider() {
                    @Override
                    public @NotNull Component getDisplayName() {
                        return targetPlayer.getDisplayName();
                    }

                    @Override
                    public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player1) {
                        AtomicInteger index = new AtomicInteger();
                        CuriosApi.getCuriosHelper().getCuriosHandler(targetPlayer).ifPresent(curiosHandler -> {
                            for (ICurioStacksHandler handler : curiosHandler.getCurios().values()) {
                                for (int j = 0; j < handler.getSlots(); j++) {
                                    index.getAndIncrement();
                                }
                            }
                        });
                        MenuType<ChestMenu> menuType;
                        if (index.intValue() <= 9) {
                            menuType = MenuType.GENERIC_9x1;
                        } else if (index.intValue() <= 18) {
                            menuType = MenuType.GENERIC_9x2;
                        } else if (index.intValue() <= 36) {
                            menuType = MenuType.GENERIC_9x4;
                        } else if (index.intValue() <= 45) {
                            menuType = MenuType.GENERIC_9x5;
                        } else {
                            menuType = MenuType.GENERIC_9x6;
                        }
                        return new PlayerCuriosInventoryScreenHandler(menuType, i, player, targetPlayer);
                    }
                };

                player.openMenu(screenHandlerFactory);
            } else {
                context.getSource().sendFailure(Component.literal("ERROR: The curios inventory container is already being used by another player."));
            }
        } else {
            context.getSource().sendFailure(Component.literal("ERROR: CuriosApi dependency not found!"));
        }

        return 1;
    }

    private int executeEnderChestCheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = getRequestedPlayer(context);
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        boolean canOpen = true;

        if (!PlayerEnderChestScreenHandler.endChestScreenTargetPlayers.isEmpty()) {
            for (int i = 0; i < PlayerEnderChestScreenHandler.endChestScreenTargetPlayers.size(); i++) {
                if (PlayerEnderChestScreenHandler.endChestScreenTargetPlayers.get(i).getDisplayName().equals(targetPlayer.getDisplayName())) {
                    canOpen = false;
                    break;
                }
            }
        }
        if (canOpen) {
            MenuProvider screenHandlerFactory = new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return targetPlayer.getDisplayName();
                }

                @Override
                public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player1) {
                    MenuType<ChestMenu> menuType = switch (targetPlayer.getEnderChestInventory().getContainerSize()) {
                        case 9 -> MenuType.GENERIC_9x1;
                        case 18 -> MenuType.GENERIC_9x2;
                        case 36 -> MenuType.GENERIC_9x4;
                        case 45 -> MenuType.GENERIC_9x5;
                        case 54 -> MenuType.GENERIC_9x6;
                        default -> MenuType.GENERIC_9x3;
                    };

                    return new PlayerEnderChestScreenHandler(menuType, i, player, targetPlayer);

                }
            };
            player.openMenu(screenHandlerFactory);
        } else {
            context.getSource().sendFailure(Component.literal("ERROR: The enderchest container is already being used by another player."));
        }
        return 1;
    }

    private int executeInventoryCheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = getRequestedPlayer(context);
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();

        boolean canOpen = true;

        if (!PlayerInventoryScreenHandler.invScreenTargetPlayers.isEmpty()) {
            for (int i = 0; i < PlayerInventoryScreenHandler.invScreenTargetPlayers.size(); i++) {
                if (PlayerInventoryScreenHandler.invScreenTargetPlayers.get(i).getDisplayName().equals(targetPlayer.getDisplayName())) {
                    canOpen = false;
                    break;
                }
            }
        }
        if (canOpen) {
            MenuProvider screenHandlerFactory = new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return targetPlayer.getDisplayName();
                }

                @Override
                public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player1) {
                    return new PlayerInventoryScreenHandler(i, player, targetPlayer);

                }
            };

            player.openMenu(screenHandlerFactory);
        } else {
            context.getSource().sendFailure(Component.literal("ERROR: The inventory container is already being used by another player."));
        }
        return 1;
    }


    private static ServerPlayer getRequestedPlayer(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        MinecraftServer minecraftServer = context.getSource().getServer();

        GameProfile requestedProfile = GameProfileArgument.getGameProfiles(context, TARGET_ID).iterator().next();
        ServerPlayer requestedPlayer = minecraftServer.getPlayerList().getPlayerByName(requestedProfile.getName());

        if (requestedPlayer == null) {
            requestedPlayer = minecraftServer.getPlayerList().getPlayerForLogin(requestedProfile, null);
            CompoundTag compound = minecraftServer.getPlayerList().load(requestedPlayer);
            if (compound != null) {
                ServerLevel world = minecraftServer.getLevel(
                        DimensionType.parseLegacy(new Dynamic<>(NbtOps.INSTANCE, compound.get("Dimension")))
                                .result().get());

                if (world != null) {
                    requestedPlayer.setLevel(world);
                }
            }
        }
        return requestedPlayer;
    }
}

package net.razorplay.invview_forge.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.network.NetworkHooks;
import net.razorplay.invview_forge.container.PlayerEnderChestScreenHandler;
import net.razorplay.invview_forge.container.PlayerInventoryScreenHandler;

import javax.annotation.Nullable;

public class InvViewCommands {
    public InvViewCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("view").requires((player) -> {
                            return player.hasPermission(2);
                        })
                        .then(Commands.literal("inv")
                                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                                        .executes(context -> executeInventoryCheck(context, (ServerPlayer) context.getSource().getEntity()))))
                        .then(Commands.literal("echest")
                                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                                        .executes(context -> executeEnderChestCheck(context, (ServerPlayer) context.getSource().getEntity()))))
        );
    }

    private int executeEnderChestCheck(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException {
        ServerPlayer targetPlayer = getRequestedPlayer(context);
        MenuProvider screenHandlerFactory = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return targetPlayer.getDisplayName();
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player_) {
                return new PlayerEnderChestScreenHandler(i, player, targetPlayer);

            }
        };

        NetworkHooks.openGui(player, screenHandlerFactory);
        return 1;
    }

    private int executeInventoryCheck(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException {
        ServerPlayer targetPlayer = getRequestedPlayer(context);
        MenuProvider screenHandlerFactory = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return targetPlayer.getDisplayName();
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player_) {
                return new PlayerInventoryScreenHandler(i, player, targetPlayer);
            }
        };

        NetworkHooks.openGui(player, screenHandlerFactory);
        return 1;
    }

    private static ServerPlayer getRequestedPlayer(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        MinecraftServer minecraftServer = context.getSource().getServer();

        GameProfile requestedProfile = GameProfileArgument.getGameProfiles(context, "target").iterator().next();
        ServerPlayer requestedPlayer = minecraftServer.getPlayerList().getPlayerByName(requestedProfile.getName());

        if (requestedPlayer == null) {
            requestedPlayer = minecraftServer.getPlayerList().getPlayerForLogin(requestedProfile);
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

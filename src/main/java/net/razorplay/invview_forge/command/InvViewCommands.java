package net.razorplay.invview_forge.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.network.NetworkHooks;
import net.razorplay.invview_forge.container.PlayerCuriosInventoryScreenHandler;
import net.razorplay.invview_forge.container.PlayerEnderChestScreenHandler;
import net.razorplay.invview_forge.container.PlayerInventoryScreenHandler;

import javax.annotation.Nullable;

public class InvViewCommands {
    public InvViewCommands(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("view").requires((player) -> {
                            return player.hasPermission(2);
                        })
                        .then(Commands.literal("inv")
                                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                                        .executes(context -> executeInventoryCheck(context, (ServerPlayerEntity) context.getSource().getEntity()))))
                        .then(Commands.literal("echest")
                                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                                        .executes(context -> executeEnderChestCheck(context, (ServerPlayerEntity) context.getSource().getEntity()))))
                        .then(Commands.literal("curios")
                                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                                        .executes(context -> executeCuriosCheck(context, (ServerPlayerEntity) context.getSource().getEntity()))))
        );
    }

    private int executeCuriosCheck(CommandContext<CommandSource> context, ServerPlayerEntity player) throws CommandSyntaxException {
        ServerPlayerEntity targetPlayer = getRequestedPlayer(context);

        if (ModList.get().isLoaded("curios")) {
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
                INamedContainerProvider screenHandlerFactory = new SimpleNamedContainerProvider((syncId, inv, playerEntity) ->
                        new PlayerCuriosInventoryScreenHandler(syncId, player, targetPlayer), targetPlayer.getDisplayName()
                );

                NetworkHooks.openGui(player, screenHandlerFactory);
            } else {
                context.getSource().sendFailure(new StringTextComponent("ERROR: The curios inventory container is already being used by another player."));
            }
        } else {
            context.getSource().sendFailure(new StringTextComponent("ERROR: CuriosApi dependency not found!"));
        }

        return 1;
    }

    private int executeEnderChestCheck(CommandContext<CommandSource> context, ServerPlayerEntity player) throws CommandSyntaxException {
        ServerPlayerEntity targetPlayer = getRequestedPlayer(context);
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
            INamedContainerProvider screenHandlerFactory = new SimpleNamedContainerProvider((syncId, inv, playerEntity) ->
                    new PlayerEnderChestScreenHandler(syncId, player, targetPlayer.inventory), targetPlayer.getDisplayName()
            );
            NetworkHooks.openGui(player, screenHandlerFactory);
        } else {
            context.getSource().sendFailure(new StringTextComponent("ERROR: The enderchest container is already being used by another player."));
        }
        return 1;
    }

    private int executeInventoryCheck(CommandContext<CommandSource> context, ServerPlayerEntity player) throws CommandSyntaxException {
        ServerPlayerEntity targetPlayer = getRequestedPlayer(context);
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
            INamedContainerProvider screenHandlerFactory = new SimpleNamedContainerProvider((syncId, inv, playerEntity) ->
                    new PlayerInventoryScreenHandler(syncId, player, targetPlayer.inventory), targetPlayer.getDisplayName()
            );
            NetworkHooks.openGui(player, screenHandlerFactory);
        } else {
            context.getSource().sendFailure(new StringTextComponent("ERROR: The inventory container container is already being used by another player."));
        }
        return 1;
    }

    private static ServerPlayerEntity getRequestedPlayer(CommandContext<CommandSource> context)
            throws CommandSyntaxException {
        MinecraftServer minecraftServer = context.getSource().getServer();

        GameProfile requestedProfile = GameProfileArgument.getGameProfiles(context, "target").iterator().next();
        ServerPlayerEntity requestedPlayer = minecraftServer.getPlayerList().getPlayerByName(requestedProfile.getName());

        if (requestedPlayer == null) {
            requestedPlayer = minecraftServer.getPlayerList().getPlayerForLogin(requestedProfile);

            CompoundNBT compound = minecraftServer.getPlayerList().load(requestedPlayer);
            if (compound != null) {
                ServerWorld world = minecraftServer.getLevel(
                        DimensionType.parseLegacy(new Dynamic<>(NBTDynamicOps.INSTANCE, compound.get("Dimension")))
                                .result().get());

                if (world != null) {
                    requestedPlayer.setLevel(world);
                }
            }
        }
        return requestedPlayer;
    }
}

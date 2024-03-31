package net.razorplay.invview_forge.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraftforge.fml.network.NetworkHooks;
import net.razorplay.invview_forge.container.PlayerEnderChestScreenHandler;
import net.razorplay.invview_forge.container.PlayerInventoryScreenHandler;

public class InvViewCommands {
    public InvViewCommands(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("view").requires((player) -> {
                            return player.hasPermission(2);
                        })
                        .then(Commands.literal("inv")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> executeInventoryCheck(context.getSource(), (ServerPlayerEntity) context.getSource().getEntity(), EntityArgument.getPlayer(context, "player")))))
                        .then(Commands.literal("echest")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> executeEnderChestCheck(context.getSource(), (ServerPlayerEntity) context.getSource().getEntity(), EntityArgument.getPlayer(context, "player")))))
        );
    }

    private int executeEnderChestCheck(CommandSource source, ServerPlayerEntity player, ServerPlayerEntity targetPlayer) {
        INamedContainerProvider screenHandlerFactory = new SimpleNamedContainerProvider((syncId, inv, playerEntity) ->
                new PlayerEnderChestScreenHandler(syncId, player, targetPlayer.inventory), targetPlayer.getDisplayName()
        );
        NetworkHooks.openGui(player, screenHandlerFactory);
        return 1;
    }

    private int executeInventoryCheck(CommandSource source, ServerPlayerEntity player, ServerPlayerEntity targetPlayer) {
        INamedContainerProvider screenHandlerFactory = new SimpleNamedContainerProvider((syncId, inv, playerEntity) ->
                new PlayerInventoryScreenHandler(syncId, player, targetPlayer.inventory), targetPlayer.getDisplayName()
        );
        NetworkHooks.openGui(player, screenHandlerFactory);
        //player.openMenu(screenHandlerFactory);
        return 1;
    }
}

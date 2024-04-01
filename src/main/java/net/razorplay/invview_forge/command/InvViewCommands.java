package net.razorplay.invview_forge.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.fmllegacy.network.NetworkHooks;
import net.razorplay.invview_forge.container.PlayerEnderChestScreenHandler;
import net.razorplay.invview_forge.container.PlayerInventoryScreenHandler;

import javax.annotation.Nullable;

public class InvViewCommands {
    public InvViewCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("view").requires((player) -> {
                            return player.hasPermission(2);
                        })
                        .then(Commands.literal("inv")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> executeInventoryCheck((ServerPlayer) context.getSource().getEntity(), EntityArgument.getPlayer(context, "player")))))
                        .then(Commands.literal("echest")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> executeEnderChestCheck((ServerPlayer) context.getSource().getEntity(), EntityArgument.getPlayer(context, "player")))))
        );
    }

    private int executeEnderChestCheck(ServerPlayer player, ServerPlayer targetPlayer) {
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

    private int executeInventoryCheck(ServerPlayer player, ServerPlayer targetPlayer) {
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
}

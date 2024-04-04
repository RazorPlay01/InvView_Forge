package net.razorplay.invview_forge.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.razorplay.invview_forge.InvView_Forge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.ConfigCommand;
import net.razorplay.invview_forge.command.InvViewCommands;
import net.razorplay.invview_forge.container.PlayerEnderChestScreenHandler;
import net.razorplay.invview_forge.container.PlayerInventoryScreenHandler;

import java.util.List;

@Mod.EventBusSubscriber(modid = InvView_Forge.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        new InvViewCommands(event.getDispatcher());

        ConfigCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!isPlayerUnique(PlayerInventoryScreenHandler.invScreenTargetPlayers, (ServerPlayer) event.getEntity()) ||
                !isPlayerUnique(PlayerEnderChestScreenHandler.endChestScreenTargetPlayers, (ServerPlayer) event.getEntity())) {

            List<ServerPlayer> serverPlayers = event.getEntity().getServer().getPlayerList().getPlayers();

            serverPlayers.forEach(player -> {
                if (player.containerMenu instanceof PlayerEnderChestScreenHandler ||
                        player.containerMenu instanceof PlayerInventoryScreenHandler) {
                    player.closeContainer();
                }
            });
        }
    }

    private static boolean isPlayerUnique(List<ServerPlayer> players, ServerPlayer targetPlayer) {
        return players.stream()
                .noneMatch(player -> player.getDisplayName().equals(targetPlayer.getDisplayName()));
    }


}
package dev.razorplay.inv_view_forge.event;

import dev.razorplay.inv_view_forge.InvView_Forge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.ConfigCommand;
import dev.razorplay.inv_view_forge.command.InvViewCommands;
import dev.razorplay.inv_view_forge.container.*;

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
        if (isUniquePlayer(PlayerInventoryScreenHandler.invScreenTargetPlayers, (ServerPlayer) event.getEntity()) ||
                isUniquePlayer(PlayerEnderChestScreenHandler.endChestScreenTargetPlayers, (ServerPlayer) event.getEntity()) ||
                isUniquePlayer(PlayerCuriosInventoryScreenHandler.curiosInvScreenTargetPlayers, (ServerPlayer) event.getEntity()) /*||
                isUniquePlayer(PlayerInventorioScreenHandler.inventorioScreenTargetPlayers, (ServerPlayer) event.getEntity()) ||
                isUniquePlayer(PlayerQuarkBackpackScreenHandler.quarkBackpackScreenTargetPlayers, (ServerPlayer) event.getEntity())*/) {

            List<ServerPlayer> serverPlayers = event.getEntity().getServer().getPlayerList().getPlayers();

            serverPlayers.forEach(player -> {
                if (player.containerMenu instanceof PlayerEnderChestScreenHandler ||
                        player.containerMenu instanceof PlayerInventoryScreenHandler ||
                        player.containerMenu instanceof PlayerCuriosInventoryScreenHandler /*||
                        player.containerMenu instanceof PlayerInventorioScreenHandler ||
                        player.containerMenu instanceof PlayerQuarkBackpackScreenHandler*/) {
                    player.closeContainer();
                }
            });
        }
    }

    private static boolean isUniquePlayer(List<ServerPlayer> players, ServerPlayer targetPlayer) {
        return players.stream()
                .anyMatch(player -> player.getDisplayName().equals(targetPlayer.getDisplayName()));
    }


}
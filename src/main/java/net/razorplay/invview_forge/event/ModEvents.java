package net.razorplay.invview_forge.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.ConfigCommand;
import net.razorplay.invview_forge.util.ITargetPlayerContainer;
import net.razorplay.invview_forge.InvView_Forge;
import net.razorplay.invview_forge.util.InventoryLockManager;
import net.razorplay.invview_forge.command.InvViewCommands;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = InvView_Forge.MOD_ID)
public class ModEvents {
    private ModEvents() {
        // []
    }

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        new InvViewCommands(event.getDispatcher());
        ConfigCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer joiningPlayer) {
            UUID joiningPlayerUUID = joiningPlayer.getUUID();

            InventoryLockManager.unlockAll(joiningPlayerUUID);

            event.getEntity().getServer().getPlayerList().getPlayers().forEach(player -> {
                if (player.containerMenu instanceof ITargetPlayerContainer container &&
                        container.getTargetPlayer().getUUID().equals(joiningPlayerUUID)) {
                    player.closeContainer();
                }
            });
        }
    }
}
package net.razorplay.invview_forge.event;

import net.razorplay.invview_forge.InvView_Forge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.ConfigCommand;
import net.razorplay.invview_forge.command.InvViewCommands;

@Mod.EventBusSubscriber(modid = InvView_Forge.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        new InvViewCommands(event.getDispatcher());

        ConfigCommand.register(event.getDispatcher());
    }
}
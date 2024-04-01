package net.razorplay.invview_forge;

import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fmlserverevents.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(InvView_Forge.MOD_ID)
public class InvView_Forge {
    public static final String MOD_ID = "inv_view_forge";
    private static final Logger LOGGER = LogManager.getLogger();
    private static MinecraftServer minecraftServer;

    public InvView_Forge() {

        MinecraftForge.EVENT_BUS.register(this);
    }


    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        onLogicalServerStarting(event.getServer());

    }

    private void onLogicalServerStarting(MinecraftServer server) {
        minecraftServer = server;
    }

    public static void savePlayerData(ServerPlayer player) {
        File playerDataDir = minecraftServer.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();

        try {
            CompoundTag compoundTag = player.saveWithoutId(new CompoundTag());
            File file = File.createTempFile(player.getStringUUID() + "-", ".dat", playerDataDir);
            final FileOutputStream fos = new FileOutputStream(file);
            NbtIo.writeCompressed(compoundTag, fos);
            File file2 = new File(playerDataDir, player.getStringUUID() + ".dat");
            File file3 = new File(playerDataDir, player.getStringUUID() + ".dat_old");
            Util.safeReplaceFile(file2.toPath(), file.toPath(), file3.toPath());
        } catch (Exception var6) {
            LogManager.getLogger().warn("Failed to save player data for {}", player.getName().getString());
        }
    }
}

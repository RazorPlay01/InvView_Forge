package net.razorplay.invview_forge;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.util.Util;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(InvView_Forge.MOD_ID)
public class InvView_Forge {
    public static final String MOD_ID = "inv_view_forge";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public InvView_Forge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        MinecraftForge.EVENT_BUS.register(this);
    }

    public static void SavePlayerData(ServerPlayerEntity player) {
        File playerDataDir = player.getServer().getWorldPath(FolderName.PLAYER_DATA_DIR).toFile();
        try {
            CompoundNBT compoundTag = player.saveWithoutId(new CompoundNBT());
            File file = File.createTempFile(player.getStringUUID() + "-", ".dat", playerDataDir);
            CompressedStreamTools.writeCompressed(compoundTag, file);
            File file2 = new File(playerDataDir, player.getStringUUID() + ".dat");
            File file3 = new File(playerDataDir, player.getStringUUID() + ".dat_old");
            Util.safeReplaceFile(file2, file, file3);
        } catch (Exception var6) {
            LogManager.getLogger().warn("Failed to save player data for {}", player.getName().getString());
        }
    }
}

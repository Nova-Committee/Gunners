package team.dovecotmc.gunners.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FileUtils;
import team.dovecotmc.gunners.Gunners;
import team.dovecotmc.gunners.compat.gun.cgm.CgmHandler;
import team.dovecotmc.gunners.compat.gun.tac.TacHandler;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class CompatHandler {
    private static CompatHandler instance;
    public static final Path gunnersDir = FMLPaths.CONFIGDIR.get().resolve("gunners");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public final boolean cgmLoaded;
    public final boolean tacLoaded;
    public final boolean recruitsLoaded;
    public final boolean guardVillagersLoaded;

    public CompatHandler() {
        final Config cfg = getCompatConfig();
        cgmLoaded = getViaCfgAndClass(cfg.cgm, "com.mrcrayfish.guns.GunMod");
        tacLoaded = getViaCfgAndClass(cfg.tac, "com.tac.guns.GunMod");
        recruitsLoaded = getViaCfgAndClass(cfg.recruits, "com.talhanation.recruits.Main");
        guardVillagersLoaded = getViaCfgAndClass(cfg.guardVillagers, "tallestegg.guardvillagers.GuardVillagers");
    }

    private boolean getViaCfgAndClass(boolean cfg, String className) {
        if (!cfg) return false;
        try {
            Class.forName(className, false, this.getClass().getClassLoader());
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    public static CompatHandler getInstance() {
        if (instance == null) instance = new CompatHandler();
        return instance;
    }

    public static Optional<HumanoidModel.ArmPose> poseForAiming(ItemStack mainHand) {
        Optional<HumanoidModel.ArmPose> pose;
        if (instance.cgmLoaded) {
            pose = CgmHandler.poseForAiming(mainHand);
            if (pose.isPresent()) return pose;
        }
        if (instance.tacLoaded) {
            pose = TacHandler.poseForAiming(mainHand);
            if (pose.isPresent()) return pose;
        }
        return Optional.empty();
    }

    public static boolean isGun(ItemStack stack) {
        if (instance.cgmLoaded && CgmHandler.isGun(stack)) return true;
        if (instance.tacLoaded && TacHandler.isGun(stack)) return true;
        return false;
    }

    @Nonnull
    private static Config getCompatConfig() {
        if (!gunnersDir.toFile().isDirectory()) {
            try {
                Files.createDirectories(gunnersDir);
            } catch (IOException e) {
                Gunners.LOGGER.error("Failed to create directory for gunner compat config!", e);
                return new Config();
            }
        }
        final Path cfgPath = gunnersDir.resolve("gunners-compat.json");
        Config config = new Config();
        if (cfgPath.toFile().isFile()) {
            try {
                config = GSON.fromJson(FileUtils.readFileToString(cfgPath.toFile(),
                        StandardCharsets.UTF_8), Config.class);
                return config;
            } catch (IOException e) {
                Gunners.LOGGER.error(String.format("Failed to parse gunners compat config \"%s\"", cfgPath), e);
                FileUtils.deleteQuietly(cfgPath.toFile());
                return createCompatConfig(cfgPath, config) ? config : new Config();
            }
        } else return createCompatConfig(cfgPath, config) ? config : new Config();
    }

    private static boolean createCompatConfig(Path cfgPath, Config config) {
        try {
            FileUtils.write(cfgPath.toFile(), GSON.toJson(config), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            Gunners.LOGGER.error(String.format("Failed to create gunners compat config \"%s\"", cfgPath), e);
        }
        return false;
    }

    public static class Config {
        public boolean cgm = true;
        public boolean tac = true;
        public boolean recruits = true;
        public boolean guardVillagers = true;
    }
}

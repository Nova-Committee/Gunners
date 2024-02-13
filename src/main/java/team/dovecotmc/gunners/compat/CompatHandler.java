package team.dovecotmc.gunners.compat;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.item.ItemStack;
import team.dovecotmc.gunners.compat.gun.cgm.CgmHandler;

import java.util.Optional;

public class CompatHandler {
    private static CompatHandler instance;
    public final boolean cgmLoaded;
    public final boolean recruitsLoaded;
    public final boolean guardVillagersLoaded;

    public CompatHandler() {
        boolean cgm;
        try {
            Class.forName("com.mrcrayfish.guns.GunMod", false, this.getClass().getClassLoader());
            cgm = true;
        } catch (Exception e) {
            cgm = false;
        }
        cgmLoaded = cgm;
        boolean recruits;
        try {
            Class.forName("com.talhanation.recruits.Main", false, this.getClass().getClassLoader());
            recruits = true;
        } catch (Exception e) {
            recruits = false;
        }
        recruitsLoaded = recruits;
        boolean guardVillagers;
        try {
            Class.forName("tallestegg.guardvillagers.GuardVillagers", false, this.getClass().getClassLoader());
            guardVillagers = true;
        } catch (Exception ignored) {
            guardVillagers = false;
        }
        guardVillagersLoaded = guardVillagers;
        instance = this;
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
        return Optional.empty();
    }

    public static boolean isGun(ItemStack stack) {
        if (instance.cgmLoaded && CgmHandler.isGun(stack)) return true;
        return false;
    }
}

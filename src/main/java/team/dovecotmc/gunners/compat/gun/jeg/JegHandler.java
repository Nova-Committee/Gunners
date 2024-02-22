package team.dovecotmc.gunners.compat.gun.jeg;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.item.ItemStack;
import ttv.alanorMiga.jeg.item.GunItem;

import java.util.Optional;

public class JegHandler {
    public static Optional<HumanoidModel.ArmPose> poseForAiming(ItemStack mainHand) {
        if (!isGun(mainHand)) return Optional.empty();
        return Optional.of(HumanoidModel.ArmPose.BOW_AND_ARROW);
    }

    public static boolean isGun(ItemStack stack) {
        return stack.getItem() instanceof GunItem;
    }
}

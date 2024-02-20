package team.dovecotmc.gunners.compat.gun.tac;

import com.tac.guns.item.GunItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class TacHandler {
    public static Optional<HumanoidModel.ArmPose> poseForAiming(ItemStack mainHand) {
        if (!isGun(mainHand)) return Optional.empty();
        return Optional.of(HumanoidModel.ArmPose.BOW_AND_ARROW);
    }

    public static boolean isGun(ItemStack stack) {
        return stack.getItem() instanceof GunItem;
    }
}

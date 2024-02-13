package team.dovecotmc.gunners.compat.gun.cgm;

import com.mrcrayfish.guns.init.ModItems;
import com.mrcrayfish.guns.item.GunItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class CgmHandler {
    public static Optional<HumanoidModel.ArmPose> poseForAiming(ItemStack mainHand) {
        if (!isGun(mainHand)) return Optional.empty();
        return mainHand.is(ModItems.MINI_GUN.get()) ? Optional.of(HumanoidModel.ArmPose.EMPTY) : Optional.of(HumanoidModel.ArmPose.BOW_AND_ARROW);
    }

    public static boolean isGun(ItemStack stack) {
        return stack.getItem() instanceof GunItem;
    }
}

package team.dovecotmc.gunners.mixin.client.guardvillagers;

import com.mrcrayfish.guns.init.ModItems;
import com.mrcrayfish.guns.item.GunItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tallestegg.guardvillagers.client.renderer.GuardRenderer;
import tallestegg.guardvillagers.entities.Guard;

@Mixin(GuardRenderer.class)
public abstract class MixinGuardRenderer extends HumanoidMobRenderer<Guard, HumanoidModel<Guard>> {

    public MixinGuardRenderer(EntityRendererProvider.Context ctx, HumanoidModel<Guard> m, float f) {
        super(ctx, m, f);
    }

    @Inject(method = "getArmPose", at = @At("HEAD"), remap = false, cancellable = true)
    private void inject$getArmPose(Guard entityIn, ItemStack itemStackMain, ItemStack itemStackOff, InteractionHand handIn, CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        if (itemStackMain.getItem() instanceof GunItem)
            cir.setReturnValue(itemStackMain.is(ModItems.MINI_GUN.get()) ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.BOW_AND_ARROW);
    }
}

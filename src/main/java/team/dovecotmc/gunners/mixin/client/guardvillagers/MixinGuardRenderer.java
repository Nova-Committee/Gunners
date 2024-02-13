package team.dovecotmc.gunners.mixin.client.guardvillagers;

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
import team.dovecotmc.gunners.compat.CompatHandler;

@Mixin(GuardRenderer.class)
public abstract class MixinGuardRenderer extends HumanoidMobRenderer<Guard, HumanoidModel<Guard>> {

    public MixinGuardRenderer(EntityRendererProvider.Context ctx, HumanoidModel<Guard> m, float f) {
        super(ctx, m, f);
    }

    @Inject(method = "getArmPose", at = @At("HEAD"), remap = false, cancellable = true)
    private void inject$getArmPose(Guard entityIn, ItemStack itemStackMain, ItemStack itemStackOff, InteractionHand handIn, CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        CompatHandler.poseForAiming(itemStackMain).ifPresent(cir::setReturnValue);
    }
}

package team.dovecotmc.gunners.mixin.client;

import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import team.dovecotmc.gunners.api.IEntityCanReload;
import team.dovecotmc.gunners.compat.CompatHandler;

@Mixin(ItemInHandLayer.class)
public abstract class MixinGunItemInHandLayer {
    @Redirect(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getMainArm()Lnet/minecraft/world/entity/HumanoidArm;")
    )
    private HumanoidArm gunners$selectGunRenderArm(LivingEntity entity) {
        // Both gun mods skip left-arm rendering while their gun is in the main hand.
        if (entity instanceof IEntityCanReload && (CompatHandler.isTaczGun(entity.getMainHandItem())
                || CompatHandler.isSbwGun(entity.getMainHandItem()))) {
            return HumanoidArm.RIGHT;
        }
        return entity.getMainArm();
    }
}

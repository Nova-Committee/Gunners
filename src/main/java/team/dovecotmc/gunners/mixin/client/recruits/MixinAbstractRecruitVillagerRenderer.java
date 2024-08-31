package team.dovecotmc.gunners.mixin.client.recruits;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.client.render.RecruitVillagerRenderer;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.dovecotmc.gunners.compat.CompatHandler;

@Mixin(RecruitVillagerRenderer.class)
public abstract class MixinAbstractRecruitVillagerRenderer extends MobRenderer<AbstractRecruitEntity, HumanoidModel<AbstractRecruitEntity>> {

    public MixinAbstractRecruitVillagerRenderer(EntityRendererProvider.Context ctx, HumanoidModel<AbstractRecruitEntity> m, float f) {
        super(ctx, m, f);
    }

    @Inject(
            method = "render(Lcom/talhanation/recruits/entities/AbstractRecruitEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/MobRenderer;render(Lnet/minecraft/world/entity/Mob;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private void inject$render(AbstractRecruitEntity entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, CallbackInfo ci) {
        CompatHandler.poseForAiming(entityIn.getMainHandItem()).ifPresent(p -> getModel().rightArmPose = p);
    }
}

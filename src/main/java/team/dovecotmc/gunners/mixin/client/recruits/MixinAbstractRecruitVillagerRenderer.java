package team.dovecotmc.gunners.mixin.client.recruits;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.guns.init.ModItems;
import com.mrcrayfish.guns.item.GunItem;
import com.talhanation.recruits.client.render.AbstractRecruitVillagerRenderer;
import com.talhanation.recruits.entities.AbstractInventoryEntity;
import com.talhanation.recruits.entities.CrossBowmanEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractRecruitVillagerRenderer.class)
public abstract class MixinAbstractRecruitVillagerRenderer extends HumanoidMobRenderer<AbstractInventoryEntity, HumanoidModel<AbstractInventoryEntity>> {
    public MixinAbstractRecruitVillagerRenderer(EntityRendererProvider.Context c, HumanoidModel<AbstractInventoryEntity> m, float f) {
        super(c, m, f);
    }

    @Inject(
            method = "render(Lcom/talhanation/recruits/entities/AbstractInventoryEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/HumanoidMobRenderer;render(Lnet/minecraft/world/entity/Mob;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    )
    private void inject$render(AbstractInventoryEntity entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, CallbackInfo ci) {
        final ItemStack inHand = entityIn.getMainHandItem();
        if (entityIn instanceof CrossBowmanEntity && inHand.getItem() instanceof GunItem)
            getModel().rightArmPose = inHand.is(ModItems.MINI_GUN.get()) ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.BOW_AND_ARROW;
    }
}
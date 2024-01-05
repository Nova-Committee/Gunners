package team.dovecotmc.gunners.mixin.common.recruit;

import com.mrcrayfish.guns.item.GunItem;
import com.talhanation.recruits.entities.AbstractInventoryEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.CrossBowmanEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.dovecotmc.gunners.api.IEntityCanReload;
import team.dovecotmc.gunners.compat.recruit.ai.RecruitCgmGunAttackGoal;

@Mixin(value = CrossBowmanEntity.class, remap = false)
public abstract class MixinCrossBowmanEntity extends AbstractRecruitEntity implements IEntityCanReload {
    @Unique
    private int gunners$reloadTick;

    @Shadow
    public abstract double getMeleeStartRange();

    public MixinCrossBowmanEntity(EntityType<? extends AbstractInventoryEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "registerGoals", at = @At("HEAD"))
    private void inject$registerGoals(CallbackInfo ci) {
        this.goalSelector.addGoal(0, new RecruitCgmGunAttackGoal((CrossBowmanEntity) (Object) this, this.getMeleeStartRange()));
    }

    @Inject(method = "wantsToPickUp", at = @At("HEAD"), cancellable = true, remap = false)
    private void inject$wantsToPickup(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (itemStack.getItem() instanceof GunItem) cir.setReturnValue(true);
    }

    @Override
    public int gunners$getReloadTick() {
        return gunners$reloadTick;
    }

    @Override
    public void gunners$setReloadTick(int reloadTick) {
        this.gunners$reloadTick = reloadTick;
    }
}

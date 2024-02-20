package team.dovecotmc.gunners.mixin.common.recruits;

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
import team.dovecotmc.gunners.compat.CompatHandler;
import team.dovecotmc.gunners.compat.shooter.recruits.ai.RecruitCgmGunAttackGoal;
import team.dovecotmc.gunners.compat.shooter.recruits.ai.RecruitTacGunAttackGoal;

@Mixin(CrossBowmanEntity.class)
public abstract class MixinCrossBowmanEntity extends AbstractRecruitEntity implements IEntityCanReload {
    @Unique
    private int gunners$reloadTick;

    @Shadow(remap = false)
    public abstract double getMeleeStartRange();

    public MixinCrossBowmanEntity(EntityType<? extends AbstractInventoryEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "registerGoals", at = @At("HEAD"))
    private void inject$registerGoals(CallbackInfo ci) {
        if (CompatHandler.getInstance().cgmLoaded)
            this.goalSelector.addGoal(0, new RecruitCgmGunAttackGoal((CrossBowmanEntity) (Object) this, this.getMeleeStartRange()));
        if (CompatHandler.getInstance().tacLoaded)
            this.goalSelector.addGoal(0, new RecruitTacGunAttackGoal((CrossBowmanEntity) (Object) this, this.getMeleeStartRange()));
    }

    @Inject(method = "wantsToPickUp", at = @At("HEAD"), cancellable = true)
    private void inject$wantsToPickup(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (CompatHandler.isGun(itemStack)) cir.setReturnValue(true);
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

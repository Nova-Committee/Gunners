package team.dovecotmc.gunners.mixin.common.guardvillagers;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tallestegg.guardvillagers.entities.Guard;
import team.dovecotmc.gunners.api.IEntityCanReload;
import team.dovecotmc.gunners.compat.guardvillagers.ai.GVCgmGunAttackGoal;

@Mixin(Guard.class)
public abstract class MixinGuard extends PathfinderMob implements IEntityCanReload {
    @Unique
    private int gunners$reloadTick;

    protected MixinGuard(EntityType<? extends PathfinderMob> t, Level l) {
        super(t, l);
    }

    @Inject(method = "registerGoals", at = @At("HEAD"))
    private void inject$registerGoals(CallbackInfo ci) {
        this.goalSelector.addGoal(0, new GVCgmGunAttackGoal((Guard) (Object) this, 0.0));
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

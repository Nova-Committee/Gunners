package team.dovecotmc.gunners.mixin.common.guardvillagers;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tallestegg.guardvillagers.entities.Guard;
import tallestegg.guardvillagers.configuration.GuardConfig;
import team.dovecotmc.gunners.api.IEntityCanReload;
import team.dovecotmc.gunners.compat.CompatHandler;
import team.dovecotmc.gunners.compat.ai.TaczGunAttackGoal;
import team.dovecotmc.gunners.compat.ai.SbwGunAttackGoal;
import team.dovecotmc.gunners.compat.shooter.guardvillagers.ai.GVCgmGunAttackGoal;
import team.dovecotmc.gunners.compat.shooter.guardvillagers.ai.GVJegGunAttackGoal;

import java.util.function.Predicate;

@Mixin(Guard.class)
public abstract class MixinGuard extends PathfinderMob implements IEntityCanReload {
    @Unique
    private int gunners$reloadTick;

    protected MixinGuard(EntityType<? extends PathfinderMob> t, Level l) {
        super(t, l);
    }

    @Inject(method = "registerGoals", at = @At("HEAD"))
    private void inject$registerGoals(CallbackInfo ci) {
        if (CompatHandler.getInstance().cgmLoaded)
            this.goalSelector.addGoal(0, new GVCgmGunAttackGoal((Guard) (Object) this, 0.0));
        if (CompatHandler.getInstance().jegLoaded)
            this.goalSelector.addGoal(0, new GVJegGunAttackGoal((Guard) (Object) this, 0.0));
        CompatHandler compat = CompatHandler.getInstance();
        if (compat.taczLoaded || compat.superbWarfareLoaded) {
            Guard guard = (Guard) (Object) this;
            Predicate<LivingEntity> protects =
                    entity -> GuardConfig.FriendlyFire && (guard.isOwner(entity) || entity instanceof Guard
                            || entity.getType() == EntityType.VILLAGER || entity.getType() == EntityType.IRON_GOLEM
                            || guard.isAlliedTo(entity));
            if (compat.taczLoaded) this.goalSelector.addGoal(0,
                    new TaczGunAttackGoal(guard, () -> 0.0, () -> !guard.isFollowing(), protects));
            if (compat.superbWarfareLoaded) this.goalSelector.addGoal(0,
                    new SbwGunAttackGoal(guard, () -> 0.0, () -> !guard.isFollowing(), protects));
        }
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

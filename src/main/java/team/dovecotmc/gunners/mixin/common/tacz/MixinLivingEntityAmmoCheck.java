package team.dovecotmc.gunners.mixin.common.tacz;

import com.tacz.guns.entity.shooter.LivingEntityAmmoCheck;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.dovecotmc.gunners.api.IEntityCanReload;
import team.dovecotmc.gunners.compat.CompatHandler;

@Mixin(value = LivingEntityAmmoCheck.class, remap = false)
public abstract class MixinLivingEntityAmmoCheck {
    @Shadow @Final private LivingEntity shooter;

    @Inject(method = "needCheckAmmo", at = @At("HEAD"), cancellable = true)
    private void gunners$provideReserve(CallbackInfoReturnable<Boolean> cir) {
        if (shooter instanceof IEntityCanReload && !CompatHandler.getInstance().finiteReserveAmmo
                && CompatHandler.isTaczGun(shooter.getMainHandItem())) {
            cir.setReturnValue(false);
        }
    }
}

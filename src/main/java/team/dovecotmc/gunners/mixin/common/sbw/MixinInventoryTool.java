package team.dovecotmc.gunners.mixin.common.sbw;

import com.atsuishio.superbwarfare.tools.InventoryTool;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.dovecotmc.gunners.api.IEntityCanReload;
import team.dovecotmc.gunners.compat.CompatHandler;

@Mixin(value = InventoryTool.class, remap = false)
public abstract class MixinInventoryTool {
    @Inject(method = "hasCreativeAmmoBox(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private static void gunners$provideReserve(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof LivingEntity shooter && shooter instanceof IEntityCanReload
                && !CompatHandler.getInstance().finiteReserveAmmo
                && CompatHandler.isSbwGun(shooter.getMainHandItem())) {
            cir.setReturnValue(true);
        }
    }
}

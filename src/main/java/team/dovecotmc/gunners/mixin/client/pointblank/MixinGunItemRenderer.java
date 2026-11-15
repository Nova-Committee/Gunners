package team.dovecotmc.gunners.mixin.client.pointblank;

import com.vicmatskiv.pointblank.client.ClientEventHandler;
import com.vicmatskiv.pointblank.client.render.GunItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.dovecotmc.gunners.api.IEntityCanReload;

@Mixin(value = GunItemRenderer.class, remap = false)
public abstract class MixinGunItemRenderer {
    @Inject(method = "getPlayer", at = @At("RETURN"), cancellable = true)
    private void gunners$allowNpcGunRender(ItemDisplayContext context, CallbackInfoReturnable<Player> cir) {
        if (cir.getReturnValue() != null || (context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)) return;
        LivingEntity entity = ClientEventHandler.getCurrentEntityLiving();
        if (entity instanceof IEntityCanReload) {
            // The geometry pass needs a client context even though the stack belongs to the NPC.
            cir.setReturnValue(Minecraft.getInstance().player);
        }
    }
}

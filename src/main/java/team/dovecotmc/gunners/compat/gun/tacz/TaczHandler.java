package team.dovecotmc.gunners.compat.gun.tacz;

import com.tacz.guns.api.item.gun.AbstractGunItem;
import net.minecraft.world.item.ItemStack;

public final class TaczHandler {
    private TaczHandler() {}

    public static boolean isGun(ItemStack stack) {
        return stack.getItem() instanceof AbstractGunItem;
    }
}

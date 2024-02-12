package team.dovecotmc.gunners.api;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;

public interface IWeapon {
    Item getWeapon();

    double getMoveSpeedAmp();

    int getAttackCooldown();

    int getWeaponLoadTime();

    float getProjectileSpeed();

    SoundEvent getShootSound();

    SoundEvent getLoadSound();

    boolean isGun();

    boolean canMelee();

    void performRangedAttackIWeapon(Mob var1, double x, double y, double z, float projectileSpeed);

    boolean isLoaded();

    void setLoaded(int ammo);
}
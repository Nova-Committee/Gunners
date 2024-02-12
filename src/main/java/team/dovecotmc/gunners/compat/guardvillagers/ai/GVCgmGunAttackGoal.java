package team.dovecotmc.gunners.compat.guardvillagers.ai;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import tallestegg.guardvillagers.GuardEntityType;
import tallestegg.guardvillagers.configuration.GuardConfig;
import tallestegg.guardvillagers.entities.Guard;
import team.dovecotmc.gunners.compat.ai.CgmGunAttackGoal;

public class GVCgmGunAttackGoal extends CgmGunAttackGoal<Guard> {
    public GVCgmGunAttackGoal(Guard shooter, double stopRange) {
        super(shooter, stopRange);
    }

    @Override
    protected int consumeAmmo() {
        return weapon.consumeAmmoFromVoid();
    }

    @Override
    protected boolean canLoad() {
        return true;
    }

    @Override
    protected boolean shouldSuppress() {
        return GuardConfig.FriendlyFire && this.friendlyInLineOfSight();
    }

    private boolean friendlyInLineOfSight() {
        return this.shooter.level()
                .getEntities(this.shooter, this.shooter.getBoundingBox().inflate(5.0))
                .stream().anyMatch(target -> {
                    if (target == this.shooter.getTarget()) return false;
                    boolean isFriendly = this.shooter.getOwner() == target ||
                            target.getType() == EntityType.VILLAGER ||
                            target.getType() == GuardEntityType.GUARD.get() ||
                            target.getType() == EntityType.IRON_GOLEM;
                    if (!isFriendly) return false;
                    Vec3 vector3d = this.shooter.getLookAngle();
                    Vec3 vector3d1 = target.position().vectorTo(this.shooter.position()).normalize();
                    vector3d1 = new Vec3(vector3d1.x, vector3d1.y, vector3d1.z);
                    return vector3d1.dot(vector3d) < 1.0 && this.shooter.hasLineOfSight(target) &&
                            (double) target.distanceTo(this.shooter) <= 4.0;
                });
    }
}

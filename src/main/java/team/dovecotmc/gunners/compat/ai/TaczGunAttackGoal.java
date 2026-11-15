package team.dovecotmc.gunners.compat.ai;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;

public class TaczGunAttackGoal extends Goal {
    private static final double RANGE = 17.0;

    private final PathfinderMob shooter;
    private final DoubleSupplier minimumRange;
    private final BooleanSupplier canPursue;
    private final Predicate<LivingEntity> protects;
    private ItemStack drawnGun = ItemStack.EMPTY;
    private int retryDelay;

    public TaczGunAttackGoal(PathfinderMob shooter, DoubleSupplier minimumRange,
                             BooleanSupplier canPursue, Predicate<LivingEntity> protects) {
        this.shooter = shooter;
        this.minimumRange = minimumRange;
        this.canPursue = canPursue;
        this.protects = protects;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = shooter.getTarget();
        return shooter instanceof IGunOperator
                && shooter.getMainHandItem().getItem() instanceof AbstractGunItem
                && target != null && target.isAlive() && shooter.canAttack(target)
                && target.distanceTo(shooter) >= minimumRange.getAsDouble();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        if (shooter instanceof IGunOperator operator) {
            operator.aim(false);
            operator.cancelReload();
        }
        drawnGun = ItemStack.EMPTY;
        retryDelay = 0;
        shooter.setAggressive(false);
    }

    @Override
    public void tick() {
        LivingEntity target = shooter.getTarget();
        ItemStack held = shooter.getMainHandItem();
        if (target == null || !target.isAlive() || !(held.getItem() instanceof AbstractGunItem gun)
                || !(shooter instanceof IGunOperator operator)) return;

        if (held != drawnGun) {
            operator.draw(shooter::getMainHandItem);
            drawnGun = held;
            retryDelay = 0;
        }

        shooter.getLookControl().setLookAt(target);
        shooter.setAggressive(true);
        boolean ready = shooter.distanceToSqr(target) <= RANGE * RANGE && shooter.hasLineOfSight(target);
        if (canPursue.getAsBoolean()) {
            if (ready) shooter.getNavigation().stop();
            else shooter.getNavigation().moveTo(target, 1.0);
        }

        operator.aim(ready);
        if (retryDelay > 0) retryDelay--;
        if (operator.getSynReloadState().getStateType().isReloading() || operator.getSynIsBolting()) return;

        if (!gun.useInventoryAmmo(held) && gun.getCurrentAmmoCount(held) <= 0
                && !gun.hasBulletInBarrel(held) && retryDelay == 0 && canReload(operator, gun, held)) {
            operator.reload();
            retryDelay = 5;
            return;
        }

        Vec3 start = shooter.getEyePosition();
        Vec3 end = target.getBoundingBox().getCenter();
        if (!ready || retryDelay > 0 || hasProtectedEntityInLine(target, start, end)) return;
        Vec3 direction = end.subtract(start);
        float pitch = (float) -Math.toDegrees(Mth.atan2(direction.y, direction.horizontalDistance()));
        float yaw = (float) Math.toDegrees(Mth.atan2(-direction.x, direction.z));
        switch (operator.shoot(() -> pitch, () -> yaw)) {
            case NEED_BOLT -> operator.bolt();
            case NO_AMMO -> {
                if (canReload(operator, gun, held)) operator.reload();
                retryDelay = 5;
            }
            case NOT_DRAW -> operator.draw(shooter::getMainHandItem);
            default -> { }
        }
    }

    private boolean canReload(IGunOperator operator, AbstractGunItem gun, ItemStack held) {
        return !gun.useInventoryAmmo(held) && (!operator.needCheckAmmo() || gun.canReload(shooter, held));
    }

    private boolean hasProtectedEntityInLine(LivingEntity target, Vec3 start, Vec3 end) {
        return !shooter.level().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(1.0),
                entity -> entity != shooter && entity != target && entity.isAlive()
                        && protects.test(entity)
                        && entity.getBoundingBox().inflate(entity.getPickRadius()).clip(start, end).isPresent())
                .isEmpty();
    }
}

package team.dovecotmc.gunners.compat.ai;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.event.GunEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.server.level.ServerLevel;
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

public final class SbwGunAttackGoal extends Goal {
    private static final double RANGE = 17.0;

    private final PathfinderMob shooter;
    private final DoubleSupplier minimumRange;
    private final BooleanSupplier canPursue;
    private final Predicate<LivingEntity> protects;
    private ItemStack activeGun = ItemStack.EMPTY;
    private int shotDelay;

    public SbwGunAttackGoal(PathfinderMob shooter, DoubleSupplier minimumRange, BooleanSupplier canPursue,
                            Predicate<LivingEntity> protects) {
        this.shooter = shooter;
        this.minimumRange = minimumRange;
        this.canPursue = canPursue;
        this.protects = protects;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    public static boolean isGun(ItemStack stack) {
        return stack.getItem() instanceof GunItem;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = shooter.getTarget();
        return shooter.level() instanceof ServerLevel && isGun(shooter.getMainHandItem())
                && target != null && target.isAlive() && shooter.canAttack(target)
                && target.distanceTo(shooter) >= minimumRange.getAsDouble()
                && !GunData.from(shooter.getMainHandItem()).meleeOnly();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        stopAiming();
        activeGun = ItemStack.EMPTY;
        shotDelay = 0;
        shooter.setAggressive(false);
    }

    private void stopAiming() {
        if (isGun(activeGun)) GunData.from(activeGun).zooming.set(false);
    }

    @Override
    public void tick() {
        LivingEntity target = shooter.getTarget();
        ItemStack held = shooter.getMainHandItem();
        if (target == null || !target.isAlive() || !isGun(held)
                || !(shooter.level() instanceof ServerLevel level)) return;
        if (held != activeGun) {
            stopAiming();
            activeGun = held;
            shotDelay = 0;
        }

        GunData data = GunData.from(held);
        if (!data.initialized()) data.initialize();
        data.tick(shooter, true);
        if (shotDelay > 0) shotDelay--;
        if (data.meleeOnly()) return;
        if (data.shouldStartReloading(shooter)) GunEventHandler.INSTANCE.tryStartReload(shooter, data, true);
        if (data.shouldStartBolt()) data.startBolt();

        shooter.getLookControl().setLookAt(target);
        shooter.setAggressive(true);
        boolean ready = shooter.distanceToSqr(target) <= RANGE * RANGE && shooter.hasLineOfSight(target);
        if (canPursue.getAsBoolean()) {
            if (ready) shooter.getNavigation().stop();
            else shooter.getNavigation().moveTo(target, 1.0);
        }
        data.zooming.set(ready);
        if (!ready || shotDelay > 0 || !data.canShoot(shooter)) return;

        Vec3 start = shooter.getEyePosition();
        Vec3 end = target.getBoundingBox().getCenter();
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < 1.0E-6 || !level.getEntitiesOfClass(LivingEntity.class,
                new AABB(start, end).inflate(1.0), entity -> entity != shooter && entity != target
                        && entity.isAlive() && protects.test(entity)
                        && entity.getBoundingBox().inflate(entity.getPickRadius()).clip(start, end).isPresent())
                .isEmpty()) return;
        data.shoot(new ShootParameters(shooter, shooter, level, start, direction.normalize(),
                data, data.get(GunProp.SPREAD), true, target.getUUID(), end));
        shotDelay = (int) Math.ceil(1200.0 / Math.max(1, data.get(GunProp.RPM)));
    }
}

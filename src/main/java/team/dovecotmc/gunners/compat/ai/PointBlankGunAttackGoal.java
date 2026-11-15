package team.dovecotmc.gunners.compat.ai;

import com.vicmatskiv.pointblank.item.AmmoItem;
import com.vicmatskiv.pointblank.item.FireModeInstance;
import com.vicmatskiv.pointblank.item.GunItem;
import com.vicmatskiv.pointblank.util.HitScan;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import team.dovecotmc.gunners.compat.CompatHandler;

import java.util.EnumSet;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class PointBlankGunAttackGoal extends Goal {
    private static final double RANGE = 17;
    private static final int RELOAD_TICKS = 40;

    private final PathfinderMob shooter;
    private final DoubleSupplier minimumRange;
    private final BooleanSupplier canPursue;
    private final Predicate<LivingEntity> protects;
    private final Supplier<SimpleContainer> inventory;
    private ItemStack activeGun = ItemStack.EMPTY;
    private FireModeInstance activeMode;
    private int shotDelay;
    private int reloadTicks;

    public PointBlankGunAttackGoal(PathfinderMob shooter, DoubleSupplier minimumRange, BooleanSupplier canPursue,
                                   Predicate<LivingEntity> protects, Supplier<SimpleContainer> inventory) {
        this.shooter = shooter;
        this.minimumRange = minimumRange;
        this.canPursue = canPursue;
        this.protects = protects;
        this.inventory = inventory;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    public static boolean isGun(ItemStack stack) {
        return stack.getItem() instanceof GunItem gun && gun.isEnabled();
    }

    private static List<AmmoItem> compatibleAmmo(GunItem gun, FireModeInstance mode) {
        if (mode.isUsingDefaultAmmoPool()) return gun.getCompatibleAmmo();
        AmmoItem ammo = mode.getAmmo();
        return ammo == null ? List.of() : List.of(ammo);
    }

    private static boolean supportsMode(GunItem gun, FireModeInstance mode) {
        return mode != null && gun.getExplosion() == null
                && compatibleAmmo(gun, mode).stream().noneMatch(AmmoItem::isHasProjectile);
    }

    private static FireModeInstance selectedMode(GunItem gun, ItemStack stack) {
        if (gun.getMainFireModes().isEmpty()) return null;
        // Point Blank returns no mode for untagged stacks, including items created by commands.
        stack.getOrCreateTag();
        return GunItem.getFireModeInstance(stack);
    }

    @Override
    public boolean canUse() {
        LivingEntity target = shooter.getTarget();
        ItemStack held = shooter.getMainHandItem();
        return shooter.level() instanceof ServerLevel && isGun(held)
                && target != null && target.isAlive() && shooter.canAttack(target)
                && target.distanceTo(shooter) >= minimumRange.getAsDouble()
                && supportsMode((GunItem) held.getItem(), selectedMode((GunItem) held.getItem(), held));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        activeGun = ItemStack.EMPTY;
        activeMode = null;
        reloadTicks = 0;
        shooter.setAggressive(false);
    }

    @Override
    public void tick() {
        LivingEntity target = shooter.getTarget();
        ItemStack held = shooter.getMainHandItem();
        if (target == null || !target.isAlive() || !isGun(held)) return;
        GunItem gun = (GunItem) held.getItem();
        FireModeInstance mode = selectedMode(gun, held);
        if (!supportsMode(gun, mode)) return;
        if (held != activeGun || !mode.equals(activeMode)) {
            reloadTicks = 0;
            activeGun = held;
            activeMode = mode;
        }
        if (shotDelay > 0) shotDelay--;

        double range = mode.getMaxShootingDistance() > 0 ? Math.min(RANGE, mode.getMaxShootingDistance()) : RANGE;
        shooter.getLookControl().setLookAt(target);
        shooter.setAggressive(true);
        boolean ready = shooter.distanceToSqr(target) <= range * range && shooter.hasLineOfSight(target);
        if (canPursue.getAsBoolean()) {
            if (ready) shooter.getNavigation().stop();
            else shooter.getNavigation().moveTo(target, 1.0);
        }

        int loaded = GunItem.getAmmo(held, mode);
        if (loaded == 0) {
            reload(gun, held, mode);
            return;
        }
        reloadTicks = 0;
        if (!ready || shotDelay > 0) return;

        Vec3 start = shooter.getEyePosition();
        Vec3 direction = target.getBoundingBox().getCenter().subtract(start).normalize();
        // Use Point Blank's native trace and damage entry points without its player packet handler.
        HitResult hit = HitScan.getNearestObjectInCrosshair(shooter, start, direction, 0, range,
                block -> false, block -> false, null);
        if (!(hit instanceof EntityHitResult entityHit) || entityHit.getEntity() != target || protects.test(target)) return;

        if (loaded != GunItem.INFINITE_AMMO) GunItem.setAmmo(held, mode, loaded - 1);
        SoundEvent sound = gun.getFireSound();
        if (sound != null) shooter.playSound(sound, gun.getFireSoundVolume(), 1.0F);
        int pellets = mode.getPelletCount() > 0 ? mode.getPelletCount() : gun.getPelletCount();
        if (pellets <= 1) {
            gun.hurtEntity(shooter, entityHit, shooter, held);
        } else {
            double spread = mode.getPelletSpread() >= 0 ? mode.getPelletSpread() : gun.getPelletSpread();
            for (HitResult pellet : HitScan.getObjectsInCrosshair(shooter, start, direction, 0, range,
                    pellets, spread, shooter.getRandom().nextLong(), block -> false, block -> false, null)) {
                if (pellet instanceof EntityHitResult pelletHit && pelletHit.getEntity() == target && target.isAlive()) {
                    gun.hurtEntity(shooter, pelletHit, shooter, held);
                }
            }
        }
        int rpm = mode.getRpm() > 0 ? mode.getRpm() : gun.getRpm();
        shotDelay = (int) Math.ceil(1200.0 / Math.max(1, rpm));
    }

    private void reload(GunItem gun, ItemStack held, FireModeInstance mode) {
        boolean finite = CompatHandler.getInstance().finiteReserveAmmo;
        List<AmmoItem> ammo = compatibleAmmo(gun, mode);
        SimpleContainer supplies = inventory.get();
        if (finite && !supplies.hasAnyMatching(stack -> ammo.contains(stack.getItem()))) {
            reloadTicks = 0;
            return;
        }
        if (++reloadTicks < RELOAD_TICKS) return;
        reloadTicks = 0;

        int capacity = gun.getMaxAmmoCapacity(held, mode);
        if (capacity <= 0) return;
        int loaded = capacity;
        if (finite) {
            loaded = 0;
            for (AmmoItem item : ammo) {
                if (loaded == capacity) break;
                loaded += supplies.removeItemType(item, capacity - loaded).getCount();
            }
        }
        if (loaded > 0) {
            GunItem.setAmmo(held, mode, loaded);
            shooter.playSound(SoundEvents.CROSSBOW_LOADING_END, 1.0F, 1.0F);
        }
    }
}

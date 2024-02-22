package team.dovecotmc.gunners.compat.ai;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import team.dovecotmc.gunners.api.IEntityCanReload;
import team.dovecotmc.gunners.compat.weapon.JegWeapon;
import ttv.alanorMiga.jeg.init.ModItems;
import ttv.alanorMiga.jeg.item.GunItem;

public abstract class JegGunAttackGoal<T extends Mob> extends Goal {
    protected final T shooter;
    protected final double speedModifier;
    protected int seeTime;
    protected State state;
    protected JegWeapon weapon = new JegWeapon(ModItems.REVOLVER.get().getDefaultInstance());
    protected boolean isWeaponInHand = isWeaponInHand();
    protected ItemStack stackCache;
    protected int weaponLoadTime;
    protected final double stopRange;

    public JegGunAttackGoal(T shooter, double stopRange) {
        this.shooter = shooter;
        this.speedModifier = this.weapon.getMoveSpeedAmp();
        this.stopRange = stopRange;
    }

    @Override
    public boolean canUse() {
        LivingEntity livingentity = this.shooter.getTarget();
        if (livingentity != null && this.isWeaponInHand()) {
            return (double) livingentity.distanceTo(this.shooter) >= this.stopRange;
        } else {
            return this.isWeaponInHand() && this.weapon != null && !this.weapon.isLoaded();
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        super.start();
        this.shooter.setAggressive(true);
        this.state = State.IDLE;
        this.weaponLoadTime = this.shooter.isPassenger() ? this.weapon.getWeaponLoadTime() * 2 : this.weapon.getWeaponLoadTime();
    }

    @Override
    public void stop() {
        super.stop();
        this.seeTime = 0;
        getReloadable().gunners$setReloadTick(0);
        this.shooter.setAggressive(false);
    }

    protected boolean isWeaponInHand() {
        if (this.shooter == null) return false;
        ItemStack itemStack = this.shooter.getMainHandItem();
        if (itemStack.equals(stackCache)) return isWeaponInHand;
        stackCache = itemStack;
        if (itemStack.getItem() instanceof GunItem) {
            this.weapon = new JegWeapon(itemStack);
            isWeaponInHand = true;
        } else {
            isWeaponInHand = false;
        }
        return isWeaponInHand;
    }

    public void tick() {
        LivingEntity target = this.shooter.getTarget();
        if (target != null && target.isAlive()) {
            double distanceToTarget = target.distanceTo(this.shooter);
            boolean isFar = distanceToTarget >= 56.0;
            boolean inRange = !isFar && distanceToTarget <= 17.0;
            this.shooter.setAggressive(true);
            if (inRange) {
                this.shooter.getNavigation().stop();
            } else {
                this.shooter.getNavigation().moveTo(target, this.speedModifier);
            }
        }

        if (this.isWeaponInHand()) {
            switch (this.state) {
                case IDLE:
                    this.shooter.setAggressive(false);
                    State newState;
                    if (!this.weapon.isLoaded()) {
                        if (this.canLoad()) {
                            newState = State.RELOAD;
                        } else {
                            newState = State.IDLE;
                        }
                    } else if (target != null && target.isAlive()) {
                        newState = State.AIMING;
                    } else {
                        newState = State.IDLE;
                    }

                    this.state = newState;
                    break;
                case RELOAD:
                    getReloadable().gunners$setReloadTick(getReloadable().gunners$getReloadTick() + 1);
                    int i = getReloadable().gunners$getReloadTick();
                    if (i >= this.weaponLoadTime) {
                        getReloadable().gunners$setReloadTick(0);
                        this.shooter.playSound(this.weapon.getLoadSound(), 1.0F, 1.0F / (this.shooter.getRandom().nextFloat() * 0.4F + 0.8F));
                        this.weapon.setLoaded(this.consumeAmmo());
                        if (target != null && target.isAlive()) {
                            this.state = State.AIMING;
                        } else {
                            this.state = State.IDLE;
                        }
                    }
                    break;
                case AIMING:
                    boolean canSee = target != null && this.shooter.getSensing().hasLineOfSight(target) && target.isAlive();
                    if (canSee) {
                        this.shooter.getLookControl().setLookAt(target);
                        this.shooter.setAggressive(true);
                        ++this.seeTime;
                        if (this.seeTime >= weapon.getAttackCooldown()) {
                            this.state = State.SHOOT;
                            this.seeTime = 0;
                        }
                    } else {
                        this.shooter.setAggressive(false);
                        this.seeTime = 0;
                        this.state = State.IDLE;
                    }
                    break;
                case SHOOT:
                    if (target != null && target.isAlive() && this.shooter.canAttack(target)) {
                        this.shooter.getLookControl().setLookAt(target);
                        if (shouldSuppress()) {
                            this.state = State.AIMING;
                        } else {
                            this.shooter.level().playSound(null, this.shooter, weapon.getShootSound(), SoundSource.PLAYERS, 1.0F, 1.0F);
                            this.weapon.performRangedAttackIWeapon(this.shooter, target.getX(), (target.getEyeY() + target.getY()) / 2, target.getZ(), this.weapon.getProjectileSpeed());
                            this.state = State.IDLE;
                        }
                    }
            }
        }

    }

    protected boolean shouldSuppress() {
        return false;
    }

    protected abstract int consumeAmmo();

    protected abstract boolean canLoad();

    private IEntityCanReload getReloadable() {
        return (IEntityCanReload) shooter;
    }

    public enum State {
        IDLE,
        RELOAD,
        AIMING,
        SHOOT;
    }
}
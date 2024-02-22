package team.dovecotmc.gunners.compat.shooter.recruits.ai;

import com.talhanation.recruits.entities.CrossBowmanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import team.dovecotmc.gunners.api.IEntityCanReload;
import team.dovecotmc.gunners.compat.ai.JegGunAttackGoal;

public class RecruitJegGunAttackGoal extends JegGunAttackGoal<CrossBowmanEntity> {
    private int seeTime;

    public RecruitJegGunAttackGoal(CrossBowmanEntity shooter, double stopRange) {
        super(shooter, stopRange);
    }

    @Override
    public boolean canUse() {
        LivingEntity livingentity = this.shooter.getTarget();
        if (livingentity != null && this.isWeaponInHand()) {
            return (double) livingentity.distanceTo(this.shooter) >= this.stopRange;
        } else {
            return this.shooter.getShouldStrategicFire() || this.isWeaponInHand() && this.weapon != null && !this.weapon.isLoaded();
        }
    }

    @Override
    public void tick() {
        LivingEntity target = this.shooter.getTarget();
        if (target != null && target.isAlive()) {
            double distanceToTarget = target.distanceTo(this.shooter);
            boolean isFar = distanceToTarget >= 56.0;
            boolean inRange = !isFar && distanceToTarget <= 17.0;
            if (!this.shooter.isFollowing()) {
                this.shooter.setAggressive(true);
                if (inRange) {
                    this.shooter.getNavigation().stop();
                } else {
                    this.shooter.getNavigation().moveTo(target, this.speedModifier);
                }
            }

            if (this.shooter.getShouldHoldPos() && this.shooter.getHoldPos() != null && !this.shooter.getHoldPos().closerThan(this.shooter.getOnPos(), 5.0)) {
                this.shooter.setAggressive(true);
                this.shooter.getNavigation().moveTo(target, this.speedModifier);
            }
        }

        if (this.isWeaponInHand()) {
            if (this.shooter.getShouldStrategicFire() && target == null) {
                BlockPos pos = this.shooter.getStrategicFirePos();
                if (pos != null) {
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
                            } else {
                                newState = State.AIMING;
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
                                this.state = State.AIMING;
                            }
                            break;
                        case AIMING:
                            this.shooter.getLookControl().setLookAt(Vec3.atCenterOf(pos));
                            this.shooter.setAggressive(true);
                            ++this.seeTime;
                            if (this.seeTime >= weapon.getAttackCooldown()) {
                                this.seeTime = 0;
                                this.state = State.SHOOT;
                            }
                            break;
                        case SHOOT:
                            this.shooter.getLookControl().setLookAt(Vec3.atCenterOf(pos));
                            this.weapon.performRangedAttackIWeapon(this.shooter, pos.getX(), pos.getY(), pos.getZ(), this.weapon.getProjectileSpeed());
                            this.state = State.IDLE;
                    }
                }
            } else {
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
                        if (target != null && target.isAlive() && this.shooter.canAttack(target) && this.shooter.getState() != 3) {
                            this.shooter.getLookControl().setLookAt(target);
                            this.shooter.level().playSound(null, this.shooter, weapon.getShootSound(), SoundSource.PLAYERS, 1.0F, 1.0F);
                            this.weapon.performRangedAttackIWeapon(this.shooter, target.getX(), (target.getEyeY() + target.getY()) / 2, target.getZ(), this.weapon.getProjectileSpeed());
                        }
                        this.state = State.IDLE;
                }
            }
        }

    }

    @Override
    public int consumeAmmo() {
        return weapon.consumeAmmoInInv(this.shooter.getInventory());
    }

    @Override
    public boolean canLoad() {
        return weapon.hasAmmoInInv(this.shooter.getInventory());
    }

    private IEntityCanReload getReloadable() {
        return (IEntityCanReload) shooter;
    }
}
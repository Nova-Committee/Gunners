package team.dovecotmc.gunners.compat.recruit.ai;

import com.mrcrayfish.guns.init.ModItems;
import com.mrcrayfish.guns.item.GunItem;
import com.talhanation.recruits.entities.CrossBowmanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import team.dovecotmc.gunners.api.IEntityCanReload;
import team.dovecotmc.gunners.compat.recruit.weapon.CgmWeapon;

public class RecruitCgmGunAttackGoal extends Goal {
    private final CrossBowmanEntity crossBowman;
    private final double speedModifier;
    private int seeTime;
    private State state;
    private CgmWeapon weapon = new CgmWeapon(ModItems.PISTOL.get().getDefaultInstance());
    private boolean isWeaponInHand = isWeaponInHand();
    private ItemStack stackCache;
    private int weaponLoadTime;
    private final double stopRange;

    public RecruitCgmGunAttackGoal(CrossBowmanEntity crossBowman, double stopRange) {
        this.crossBowman = crossBowman;
        this.speedModifier = this.weapon.getMoveSpeedAmp();
        this.stopRange = stopRange;
    }

    public boolean canUse() {
        LivingEntity livingentity = this.crossBowman.getTarget();
        if (livingentity != null && this.isWeaponInHand()) {
            return (double) livingentity.distanceTo(this.crossBowman) >= this.stopRange;
        } else {
            return this.crossBowman.getShouldStrategicFire() || this.isWeaponInHand() && this.weapon != null && !this.weapon.isLoaded(this.crossBowman.getMainHandItem());
        }
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    public void start() {
        super.start();
        this.crossBowman.setAggressive(true);
        this.state = State.IDLE;
        this.weaponLoadTime = this.crossBowman.isPassenger() ? this.weapon.getWeaponLoadTime() * 2 : this.weapon.getWeaponLoadTime();
    }

    public void stop() {
        super.stop();
        this.seeTime = 0;
        getReloadable().gunners$setReloadTick(0);
        this.crossBowman.setAggressive(false);
    }

    protected boolean isWeaponInHand() {
        if (this.crossBowman == null) return false;
        ItemStack itemStack = this.crossBowman.getMainHandItem();
        if (itemStack.equals(stackCache)) return isWeaponInHand;
        stackCache = itemStack;
        if (itemStack.getItem() instanceof GunItem) {
            this.weapon = new CgmWeapon(itemStack);
            isWeaponInHand = true;
        } else {
            isWeaponInHand = false;
        }
        return isWeaponInHand;
    }

    public void tick() {
        LivingEntity target = this.crossBowman.getTarget();
        if (target != null && target.isAlive()) {
            double distanceToTarget = target.distanceTo(this.crossBowman);
            boolean isFar = distanceToTarget >= 56.0;
            boolean inRange = !isFar && distanceToTarget <= 17.0;
            if (!this.crossBowman.isFollowing()) {
                this.crossBowman.setAggressive(true);
                if (inRange) {
                    this.crossBowman.getNavigation().stop();
                } else {
                    this.crossBowman.getNavigation().moveTo(target, this.speedModifier);
                }
            }

            if (this.crossBowman.getShouldHoldPos() && this.crossBowman.getHoldPos() != null && !this.crossBowman.getHoldPos().closerThan(this.crossBowman.getOnPos(), 5.0)) {
                this.crossBowman.setAggressive(true);
                this.crossBowman.getNavigation().moveTo(target, this.speedModifier);
            }
        }

        if (this.isWeaponInHand()) {
            if (this.crossBowman.getShouldStrategicFire() && target == null) {
                BlockPos pos = this.crossBowman.getStrategicFirePos();
                if (pos != null) {
                    switch (this.state) {
                        case IDLE:
                            this.crossBowman.setAggressive(false);
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
                                this.crossBowman.playSound(this.weapon.getLoadSound(), 1.0F, 1.0F / (this.crossBowman.getRandom().nextFloat() * 0.4F + 0.8F));
                                this.weapon.setLoaded(this.crossBowman.getMainHandItem(), true);
                                this.consumeAmmo();
                                this.state = State.AIMING;
                            }
                            break;
                        case AIMING:
                            this.crossBowman.getLookControl().setLookAt(Vec3.atCenterOf(pos));
                            this.crossBowman.setAggressive(true);
                            ++this.seeTime;
                            if (this.seeTime >= weapon.getAttackCooldown()) {
                                this.seeTime = 0;
                                this.state = State.SHOOT;
                            }
                            break;
                        case SHOOT:
                            this.crossBowman.getLookControl().setLookAt(Vec3.atCenterOf(pos));
                            this.weapon.performRangedAttackIWeapon(this.crossBowman, pos.getX(), pos.getY(), pos.getZ(), this.weapon.getProjectileSpeed());
                            this.state = State.IDLE;
                    }
                }
            } else {
                switch (this.state) {
                    case IDLE:
                        this.crossBowman.setAggressive(false);
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
                            this.crossBowman.playSound(this.weapon.getLoadSound(), 1.0F, 1.0F / (this.crossBowman.getRandom().nextFloat() * 0.4F + 0.8F));
                            this.weapon.setLoaded(this.crossBowman.getMainHandItem(), true);
                            this.consumeAmmo();
                            if (target != null && target.isAlive()) {
                                this.state = State.AIMING;
                            } else {
                                this.state = State.IDLE;
                            }
                        }
                        break;
                    case AIMING:
                        boolean canSee = target != null && this.crossBowman.getSensing().hasLineOfSight(target) && target.isAlive();
                        if (canSee) {
                            this.crossBowman.getLookControl().setLookAt(target);
                            this.crossBowman.setAggressive(true);
                            ++this.seeTime;
                            if (this.seeTime >= weapon.getAttackCooldown()) {
                                this.state = State.SHOOT;
                                this.seeTime = 0;
                            }
                        } else {
                            this.crossBowman.setAggressive(false);
                            this.seeTime = 0;
                            this.state = State.IDLE;
                        }
                        break;
                    case SHOOT:
                        if (target != null && target.isAlive() && this.crossBowman.canAttack(target) && this.crossBowman.getState() != 3) {
                            this.crossBowman.getLookControl().setLookAt(target);
                            this.crossBowman.level().playSound(null, this.crossBowman, weapon.getShootSound(), SoundSource.PLAYERS, 1.0F, 1.0F);
                            this.weapon.performRangedAttackIWeapon(this.crossBowman, target.getX(), target.getEyeY(), target.getZ(), this.weapon.getProjectileSpeed());
                        }
                        this.state = State.IDLE;
                }
            }
        }

    }

    private void consumeAmmo() {
        weapon.consumeAmmoInInv(this.crossBowman.getInventory());
    }

    private boolean canLoad() {
        return weapon.hasAmmoInInv(this.crossBowman.getInventory());
    }

    private IEntityCanReload getReloadable() {
        return (IEntityCanReload) crossBowman;
    }

    enum State {
        IDLE,
        RELOAD,
        AIMING,
        SHOOT;
    }
}
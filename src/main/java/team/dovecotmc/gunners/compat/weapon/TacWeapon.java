package team.dovecotmc.gunners.compat.weapon;

import com.tac.guns.Config;
import com.tac.guns.common.Gun;
import com.tac.guns.common.ProjectileManager;
import com.tac.guns.entity.ProjectileEntity;
import com.tac.guns.init.ModEnchantments;
import com.tac.guns.init.ModItems;
import com.tac.guns.interfaces.IProjectileFactory;
import com.tac.guns.item.GunItem;
import com.tac.guns.network.PacketHandler;
import com.tac.guns.network.message.MessageBulletTrail;
import com.tac.guns.util.GunEnchantmentHelper;
import com.tac.guns.util.GunModifierHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import team.dovecotmc.gunners.api.IWeapon;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class TacWeapon implements IWeapon {
    private final ItemStack gunStack;
    private SoundEvent fireSound;
    private SoundEvent loadSound;
    private Item ammo;
    private final Gun gun;
    private int fireRate = -1;

    public TacWeapon(ItemStack stack) {
        if (stack.getItem() instanceof GunItem g) {
            this.gunStack = stack;
            this.gun = g.getModifiedGun(stack);
        } else {
            this.gunStack = ModItems.GLOCK_17.get().getDefaultInstance();
            this.gun = ((GunItem) ModItems.GLOCK_17.get()).getGun();
        }
    }

    @Override
    public Item getWeapon() {
        return gunStack.getItem();
    }

    @Override
    public double getMoveSpeedAmp() {
        return 0.4;
    }

    @Override
    public int getAttackCooldown() {
        if (fireRate == -1)
            fireRate = Math.max(1, (int) (1.0 / GunModifierHelper.getModifiedRate(gunStack, gun.getGeneral().getRate())));
        return fireRate;
    }

    @Override
    public int getWeaponLoadTime() {
        return GunModifierHelper.getAmmoCapacity(gunStack, gun) / gun.getReloads().getReloadAmount() * gun.getReloads().getReloadMagTimer();
    }

    @Override
    public float getProjectileSpeed() {
        return (float) GunModifierHelper.getModifiedProjectileSpeed(
                gunStack,
                gun.getProjectile().getSpeed() * GunEnchantmentHelper.getProjectileSpeedModifier(gunStack)
        );
    }

    @Override
    public SoundEvent getShootSound() {
        if (fireSound == null)
            fireSound = ForgeRegistries.SOUND_EVENTS.getValue(
                    GunModifierHelper.isSilencedFire(gunStack) ? gun.getSounds().getSilencedFire() : gun.getSounds().getFire()
            );
        return fireSound;
    }

    @Override
    public SoundEvent getLoadSound() {
        if (loadSound == null)
            loadSound = ForgeRegistries.SOUND_EVENTS.getValue(gun.getSounds().getReload());
        return loadSound;
    }

    @Override
    public void performRangedAttackIWeapon(Mob shooter, double x, double y, double z, float projectileSpeed) {
        final Level level = shooter.level;
        if (level.isClientSide()) return;
        int count = gun.getGeneral().getProjectileAmount();
        Gun.Projectile projectileProps = gun.getProjectile();
        ProjectileEntity[] spawnedProjectiles = new ProjectileEntity[count];
        for (int i = 0; i < count; ++i) {
            IProjectileFactory factory = ProjectileManager.getInstance().getFactory(projectileProps.getItem());
            ProjectileEntity projectileEntity = factory.create(level, shooter, gunStack, (GunItem) gunStack.getItem(), gun, .0F, .0F);
            projectileEntity.setWeapon(gunStack);
            projectileEntity.setAdditionalDamage(Gun.getAdditionalDamage(gunStack));
            final Vec3 startPos = shooter.getEyePosition();
            final float gunSpread = GunModifierHelper.getModifiedSpread(gunStack, gun.getGeneral().getSpread()) * .5F;
            final Vec3 track = new Vec3(x, y, z).subtract(startPos).normalize().add(
                    ThreadLocalRandom.current().nextFloat() * gunSpread / 100.0,
                    ThreadLocalRandom.current().nextFloat() * gunSpread / 100.0,
                    ThreadLocalRandom.current().nextFloat() * gunSpread / 100.0
            );
            projectileEntity.setPos(startPos.add(track));
            projectileEntity.setDeltaMovement(track.scale(projectileSpeed));
            level.addFreshEntity(projectileEntity);
            spawnedProjectiles[i] = projectileEntity;
            projectileEntity.tick();
        }
        consumeAmmoInGun();
        if (!projectileProps.isVisible()) {
            int x1 = (int) shooter.getX();
            int y1 = (int) (shooter.getY() + 1.0);
            int z1 = (int) shooter.getZ();
            double r = Config.COMMON.network.projectileTrackingRange.get();
            MessageBulletTrail messageBulletTrail = new MessageBulletTrail(spawnedProjectiles, projectileProps, shooter.getId(), projectileProps.getSize());
            PacketHandler.getPlayChannel().send(
                    PacketDistributor.NEAR.with(() -> PacketDistributor.TargetPoint.p(x1, y1, z1, r, level.dimension()).get()),
                    messageBulletTrail
            );
        }
    }

    @Override
    public boolean isLoaded() {
        return Gun.hasAmmo(gunStack);
    }

    @Override
    public void setLoaded(int ammo) {
        CompoundTag tag = gunStack.getOrCreateTag();
        if (tag.getBoolean("IgnoreAmmo")) return;
        tag.putInt("AmmoCount", ammo);
    }

    private void consumeAmmoInGun() {
        CompoundTag tag = gunStack.getOrCreateTag();
        if (!tag.getBoolean("IgnoreAmmo")) {
            int level = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.RECLAIMED.get(), gunStack);
            if (level == 0 || ThreadLocalRandom.current().nextInt(4 - Mth.clamp(level, 1, 2)) != 0) {
                tag.putInt("AmmoCount", Math.max(0, tag.getInt("AmmoCount") - 1));
            }
        }
    }

    public int consumeAmmoInInv(SimpleContainer inv) {
        return inv.removeItemType(getAmmo(), GunModifierHelper.getAmmoCapacity(gunStack, gun)).getCount();
    }

    public int consumeAmmoFromVoid() {
        return GunModifierHelper.getAmmoCapacity(gunStack, gun);
    }

    public boolean hasAmmoInInv(SimpleContainer inv) {
        return inv.hasAnyOf(Set.of(getAmmo()));
    }

    private Item getAmmo() {
        if (ammo == null)
            ammo = Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(gun.getProjectile().getItem()));
        return ammo;
    }
}

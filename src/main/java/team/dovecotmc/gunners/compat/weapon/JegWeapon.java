package team.dovecotmc.gunners.compat.weapon;

import com.mrcrayfish.framework.api.network.LevelLocation;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import team.dovecotmc.gunners.api.IWeapon;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.common.ReloadType;
import ttv.migami.jeg.common.ProjectileManager;
import ttv.migami.jeg.entity.projectile.ProjectileEntity;
import ttv.migami.jeg.init.ModEnchantments;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.interfaces.IProjectileFactory;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.network.PacketHandler;
import ttv.migami.jeg.network.message.S2CMessageBulletTrail;
import ttv.migami.jeg.util.GunEnchantmentHelper;
import ttv.migami.jeg.util.GunModifierHelper;

import java.util.concurrent.ThreadLocalRandom;

public class JegWeapon implements IWeapon {
    private final ItemStack gunStack;
    private SoundEvent fireSound;
    private SoundEvent loadSound;
    private final Gun gun;

    public JegWeapon(ItemStack stack) {
        if (stack.getItem() instanceof GunItem g) {
            this.gunStack = stack;
            this.gun = g.getModifiedGun(stack);
        } else {
            this.gunStack = ModItems.REVOLVER.get().getDefaultInstance();
            this.gun = ModItems.REVOLVER.get().getGun();
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
        return GunModifierHelper.getModifiedRate(gunStack, GunEnchantmentHelper.getRate(gunStack, gun));
    }

    @Override
    public int getWeaponLoadTime() {
        return GunEnchantmentHelper.getRealReloadSpeed(gunStack);
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
                    GunModifierHelper.isSilencedFire(gunStack) ? gun.getSounds().getSilencedFire() :
                            gunStack.isEnchanted() ? gun.getSounds().getEnchantedFire() :
                                    gun.getSounds().getFire()
            );
        return fireSound;
    }

    @Override
    public SoundEvent getLoadSound() {
        ResourceLocation soundId = gun.getSounds().getReloadLoad();
        if (loadSound == null && soundId != null) {
            loadSound = ForgeRegistries.SOUND_EVENTS.getValue(soundId);
        }
        return loadSound;
    }

    public void playLoadSound(Mob shooter) {
        SoundEvent sound = getLoadSound();
        if (sound != null) {
            shooter.playSound(sound, 1.0F, 1.0F / (shooter.getRandom().nextFloat() * 0.4F + 0.8F));
        }
    }

    @Override
    public void performRangedAttackIWeapon(Mob shooter, double x, double y, double z, float projectileSpeed) {
        final Level level = shooter.level();
        if (level.isClientSide()) return;
        int count = gun.getGeneral().getProjectileAmount();
        Gun.Projectile projectileProps = gun.getProjectile();
        ProjectileEntity[] spawnedProjectiles = new ProjectileEntity[count];
        for (int i = 0; i < count; ++i) {
            IProjectileFactory factory = ProjectileManager.getInstance().getFactory(projectileProps.getItem());
            ProjectileEntity projectileEntity = factory.create(level, shooter, gunStack, (GunItem) gunStack.getItem(), gun);
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
            int radius = (int) shooter.getX();
            int y1 = (int) (shooter.getY() + 1.0);
            int z1 = (int) shooter.getZ();
            double r = Config.COMMON.network.projectileTrackingRange.get();
            ParticleOptions data = GunEnchantmentHelper.getParticle(gunStack);
            S2CMessageBulletTrail messageBulletTrail = new S2CMessageBulletTrail(spawnedProjectiles, projectileProps, shooter.getId(), data);
            PacketHandler.getPlayChannel().sendToNearbyPlayers(
                    () -> LevelLocation.create(level, radius, y1, z1, r),
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
            int level = EnchantmentHelper.getTagEnchantmentLevel(ModEnchantments.RECLAIMED.get(), gunStack);
            if (level == 0 || ThreadLocalRandom.current().nextInt(4 - Mth.clamp(level, 1, 2)) != 0) {
                tag.putInt("AmmoCount", Math.max(0, tag.getInt("AmmoCount") - 1));
            }
        }
    }

    public int consumeAmmoInInv(SimpleContainer inv) {
        Item ammo = getAmmo();
        if (ammo == null) return 0;

        int capacity = consumeAmmoFromVoid();
        if (usesReloadItem()) {
            return inv.removeItemType(ammo, 1).isEmpty() ? 0 : capacity;
        }
        return inv.removeItemType(ammo, capacity).getCount();
    }

    public int consumeAmmoFromVoid() {
        return GunModifierHelper.getModifiedAmmoCapacity(gunStack, gun);
    }

    public boolean hasAmmoInInv(SimpleContainer inv) {
        Item ammo = getAmmo();
        return ammo != null && inv.hasAnyMatching(stack -> !stack.isEmpty() && stack.is(ammo));
    }

    private Item getAmmo() {
        ResourceLocation ammoId = usesReloadItem() ? gun.getReloads().getReloadItem() : gun.getProjectile().getItem();
        return ammoId == null ? null : ForgeRegistries.ITEMS.getValue(ammoId);
    }

    private boolean usesReloadItem() {
        return gun.getReloads().getReloadType() == ReloadType.SINGLE_ITEM;
    }
}

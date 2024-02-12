package team.dovecotmc.gunners.mixin.plugin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class GunnersMixinPlugin implements IMixinConfigPlugin {
    private boolean cgmLoaded;
    private boolean recruitsLoaded;
    private boolean guardVillagersLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        try {
            Class.forName("com.mrcrayfish.guns.GunMod", false, this.getClass().getClassLoader());
            cgmLoaded = true;
        } catch (Exception e) {
            cgmLoaded = false;
        }
        try {
            Class.forName("com.talhanation.recruits.Main", false, this.getClass().getClassLoader());
            recruitsLoaded = true;
        } catch (Exception e) {
            recruitsLoaded = false;
        }
        try {
            Class.forName("tallestegg.guardvillagers.GuardVillagers", false, this.getClass().getClassLoader());
            guardVillagersLoaded = true;
        } catch (Exception e) {
            guardVillagersLoaded = false;
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!cgmLoaded) return false;
        if (mixinClassName.contains("recruits")) return recruitsLoaded;
        if (mixinClassName.contains("guardvillagers")) return guardVillagersLoaded;
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}

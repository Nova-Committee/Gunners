package team.dovecotmc.gunners.mixin.plugin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import team.dovecotmc.gunners.compat.CompatHandler;

import java.util.List;
import java.util.Set;

public class GunnersMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
        CompatHandler.getInstance();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("recruits")) return CompatHandler.getInstance().recruitsLoaded;
        if (mixinClassName.contains("guardvillagers")) return CompatHandler.getInstance().guardVillagersLoaded;
        if (mixinClassName.endsWith(".MixinGunItemInHandLayer")) {
            CompatHandler compat = CompatHandler.getInstance();
            return (compat.taczLoaded || compat.superbWarfareLoaded) && (compat.recruitsLoaded || compat.guardVillagersLoaded);
        }
        if (mixinClassName.contains(".common.tacz.")) return CompatHandler.getInstance().taczLoaded;
        if (mixinClassName.contains(".common.sbw.")) return CompatHandler.getInstance().superbWarfareLoaded;
        if (mixinClassName.contains(".client.pointblank.")) return CompatHandler.getInstance().pointBlankLoaded;
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

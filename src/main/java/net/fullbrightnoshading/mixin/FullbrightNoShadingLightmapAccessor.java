package net.fullbrightnoshading.mixin;

import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LightmapTextureManager.class)
public interface FullbrightNoShadingLightmapAccessor {
    @Accessor("dirty")
    void fullbrightNoShading$setDirty(boolean dirty);
}

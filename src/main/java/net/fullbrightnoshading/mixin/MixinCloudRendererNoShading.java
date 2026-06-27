package net.fullbrightnoshading.mixin;

import net.fullbrightnoshading.FullbrightNoShading;
import net.minecraft.client.render.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CloudRenderer.class)
public class MixinCloudRendererNoShading {
    private static final int FLAG_USE_TOP_COLOR = 16;

    @ModifyVariable(method = "method_71098(Ljava/nio/ByteBuffer;IILnet/minecraft/util/math/Direction;I)V", at = @At("HEAD"), ordinal = 2, argsOnly = true)
    private int fullbrightNoShading$disableCloudShading(int flags) {
        return FullbrightNoShading.shouldShadeClouds() ? flags : flags | FLAG_USE_TOP_COLOR;
    }
}

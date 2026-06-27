package net.fullbrightnoshading.mixin;

import net.fullbrightnoshading.FullbrightNoShading;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientWorld.class)
public class MixinClientWorldNoShading {
    @ModifyVariable(method = "getBrightness(Lnet/minecraft/util/math/Direction;Z)F", at = @At("HEAD"), argsOnly = true)
    private boolean fullbrightNoShading$disableBlockShading(boolean shade) {
        return shade && FullbrightNoShading.shouldShadeBlocks();
    }
}

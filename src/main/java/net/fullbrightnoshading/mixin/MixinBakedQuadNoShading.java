package net.fullbrightnoshading.mixin;

import net.fullbrightnoshading.FullbrightNoShading;
import net.minecraft.client.render.model.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BakedQuad.class)
public class MixinBakedQuadNoShading {
    @Inject(method = "shade", at = @At("RETURN"), cancellable = true)
    private void fullbrightNoShading$disableQuadShading(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(cir.getReturnValueZ() && FullbrightNoShading.shouldShadeBlocks());
    }
}

package net.fullbrightnoshading.mixin;

import net.fullbrightnoshading.FullbrightNoShading;
import net.minecraft.client.render.block.FluidRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(FluidRenderer.class)
public class MixinFluidRendererNoShading {
    @ModifyArg(
            method = "render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/block/BlockState;Lnet/minecraft/fluid/FluidState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/BlockRenderView;getBrightness(Lnet/minecraft/util/math/Direction;Z)F"),
            index = 1
    )
    private boolean fullbrightNoShading$disableFluidShading(boolean shade) {
        return shade && FullbrightNoShading.shouldShadeBlocks();
    }
}

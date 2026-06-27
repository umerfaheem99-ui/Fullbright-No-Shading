package net.fullbrightnoshading.mixin;

import java.util.ArrayList;
import java.util.List;
import net.fullbrightnoshading.FullbrightNoShading;
import net.minecraft.client.gl.GlImportProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlImportProcessor.class)
public class MixinGlImportProcessorNoShading {
    private static final String LIGHT_IMPORT = "#moj_import <minecraft:light.glsl>";
    private static final String LEGACY_LIGHT_IMPORT = "#moj_import <light.glsl>";
    private static final String PROJECTION_IMPORT = "#moj_import <minecraft:projection.glsl>";
    private static final String OLD_ENTITY_LIGHTING = "Light0_Direction";

    @Inject(method = "readSource", at = @At("RETURN"), cancellable = true)
    private void fullbrightNoShading$disableEntityShading(String source, CallbackInfoReturnable<List<String>> cir) {
        if (FullbrightNoShading.shouldShadeEntities() || !usesEntityLighting(source)) {
            return;
        }

        List<String> patched = new ArrayList<>(cir.getReturnValue().size());
        for (String part : cir.getReturnValue()) {
            patched.add(patchLightFunctions(part));
        }
        cir.setReturnValue(patched);
    }

    private static boolean usesEntityLighting(String source) {
        return (source.contains(LIGHT_IMPORT) || source.contains(LEGACY_LIGHT_IMPORT))
                && (source.contains(PROJECTION_IMPORT) || source.contains(OLD_ENTITY_LIGHTING));
    }

    private static String patchLightFunctions(String source) {
        if (!source.contains("vec4 minecraft_mix_light(") && !source.contains("vec4 minecraft_mix_light_separate(")) {
            return source;
        }

        // Matches Simply No Shading's conditional shader mode: in-world entity
        // rendering ignores light mixing, but GUI/item-preview paths keep it.
        return source
                .replace("minecraft_mix_light(", "minecraft_mix_light_helper(")
                .replace("minecraft_mix_light_separate(", "minecraft_mix_light_separate_helper(")
                + "\n#define minecraft_mix_light(lightDir0, lightDir1, normal, color) "
                + "(ProjMat[3].x != -1 ? color : minecraft_mix_light_helper(lightDir0, lightDir1, normal, color))\n"
                + "#define minecraft_mix_light_separate(light, color) "
                + "(ProjMat[3].x != -1 ? color : minecraft_mix_light_separate_helper(light, color))\n";
    }
}

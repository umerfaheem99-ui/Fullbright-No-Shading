package net.fullbrightnoshading.mixin;

import net.fullbrightnoshading.FullbrightNoShading;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    private static final long SAVE_INTERVAL = 2000L;

    @Shadow
    public GameOptions options;
    private long lastSaveTime = 0L;

    @Inject(at = @At("HEAD"), method = "close")
    private void brightnessPlus$close(CallbackInfo info) {
        FullbrightNoShading.prepareForVanillaOptionsScreen();
        this.options.write();
        FullbrightNoShading.saveConfig();
    }

    @Inject(at = @At("HEAD"), method = "setScreen")
    private void brightnessPlus$setScreen(Screen screen, CallbackInfo info) {
        if (screen instanceof OptionsScreen && System.currentTimeMillis() - this.lastSaveTime > SAVE_INTERVAL) {
            FullbrightNoShading.prepareForVanillaOptionsScreen();
            FullbrightNoShading.saveConfig();
            this.lastSaveTime = System.currentTimeMillis();
        }
    }
}

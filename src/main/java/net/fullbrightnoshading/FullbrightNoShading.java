package net.fullbrightnoshading;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.function.DoubleConsumer;
import net.fullbrightnoshading.config.FullbrightNoShadingConfigScreen;
import net.fullbrightnoshading.mixin.FullbrightNoShadingLightmapAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class FullbrightNoShading implements ClientModInitializer {
    public static final String MOD_ID = "fullbright-no-shading";
    private static final String LOG_NAME = "Fullbright No Shading";
    private static final double VANILLA_MIN_BRIGHTNESS = 0.0d;
    private static final double VANILLA_MAX_BRIGHTNESS = 1.0d;
    private static final double DEFAULT_FULLBRIGHT = 12.0d;
    private static final Gson GSON = new Gson();
    private static final String FULLBRIGHT_CATEGORY = "category.fullbright-no-shading.main";
    private static Method inputUtilIsKeyPressedMethod;
    private static boolean inputUtilIsKeyPressedLookedUp = false;

    private static KeyBinding toggleBind;
    private static KeyBinding raiseBind;
    private static KeyBinding lowerBind;
    private static KeyBinding openGuiBind;
    private static MinecraftClient client;
    private static boolean enabled = false;
    private static double brightness = DEFAULT_FULLBRIGHT;
    private static double step = 0.5d;
    private static boolean shadeBlocks = false;
    private static boolean shadeClouds = false;
    private static boolean shadeEntities = false;
    private static double vanillaBrightnessBeforeEnable = VANILLA_MAX_BRIGHTNESS;
    private static boolean vanillaBrightnessCaptured = false;

    @Override
    public void onInitializeClient() {
        toggleBind = KeyBindingHelper.registerKeyBinding(createKeyBinding("key.fullbright-plus.toggle", InputUtil.GLFW_KEY_B));
        raiseBind = KeyBindingHelper.registerKeyBinding(createKeyBinding("key.fullbright-plus.raise", InputUtil.GLFW_KEY_EQUAL));
        lowerBind = KeyBindingHelper.registerKeyBinding(createKeyBinding("key.fullbright-plus.lower", InputUtil.GLFW_KEY_MINUS));
        openGuiBind = KeyBindingHelper.registerKeyBinding(createKeyBinding("key.fullbright-plus.open_gui", InputUtil.GLFW_KEY_G));
        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);
        loadConfig();
        client = MinecraftClient.getInstance();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        if (enabled == value) {
            return;
        }
        if (value && client != null && client.options != null) {
            captureVanillaBrightness();
            vanillaBrightnessBeforeEnable = clamp(client.options.getGamma().getValue(), VANILLA_MIN_BRIGHTNESS, VANILLA_MAX_BRIGHTNESS);
        }
        enabled = value;
        applyToVanillaGamma();
        markLightmapDirty();
        saveConfig();
    }

    public static double getBrightness() {
        return brightness;
    }

    public static void setBrightness(double value) {
        double clamped = clamp(value, VANILLA_MIN_BRIGHTNESS, DEFAULT_FULLBRIGHT);
        if (Math.abs(brightness - clamped) <= 1.0E-6d) {
            return;
        }
        brightness = clamped;
        if (enabled) {
            applyToVanillaGamma();
            markLightmapDirty();
        }
        saveConfig();
    }

    public static double getStep() {
        return step;
    }

    public static void setStep(double value) {
        double clamped = clamp(value, 0.1d, 2.0d);
        if (Math.abs(step - clamped) <= 1.0E-6d) {
            return;
        }
        step = clamped;
        saveConfig();
    }

    public static boolean shouldShadeBlocks() {
        return shadeBlocks;
    }

    public static boolean shouldShadeClouds() {
        return shadeClouds;
    }

    public static boolean shouldShadeEntities() {
        return shadeEntities;
    }

    public static boolean isBlockShadingDisabled() {
        return !shadeBlocks;
    }

    public static void setBlockShadingDisabled(boolean disabled) {
        if (shadeBlocks == !disabled) {
            return;
        }
        shadeBlocks = !disabled;
        refreshTerrain();
        saveConfig();
    }

    public static boolean isCloudShadingDisabled() {
        return !shadeClouds;
    }

    public static void setCloudShadingDisabled(boolean disabled) {
        if (shadeClouds == !disabled) {
            return;
        }
        shadeClouds = !disabled;
        saveConfig();
    }

    public static boolean isEntityShadingDisabled() {
        return !shadeEntities;
    }

    public static void setEntityShadingDisabled(boolean disabled) {
        if (shadeEntities == !disabled) {
            return;
        }
        shadeEntities = !disabled;
        // Entity/item lighting lives in loaded shader resources, so this only reloads
        // when the actual entity shading option changes.
        reloadResources();
        saveConfig();
    }

    public static void openConfigScreen(Screen parent) {
        MinecraftClient.getInstance().setScreen(FullbrightNoShadingConfigScreen.create(parent));
    }

    public static boolean isFullbrightEnabled() {
        return enabled;
    }

    public static double getRenderBrightness() {
        return enabled ? brightness : VANILLA_MAX_BRIGHTNESS;
    }

    public static void prepareForVanillaOptionsScreen() {
        if (enabled && client != null && client.options != null) {
            client.options.getGamma().setValue(VANILLA_MAX_BRIGHTNESS);
        }
    }

    public static void saveConfig() {
        JsonObject config = new JsonObject();
        config.addProperty("enabled", enabled);
        config.addProperty("brightness", brightness);
        config.addProperty("step", step);
        config.addProperty("shadeBlocks", shadeBlocks);
        config.addProperty("shadeClouds", shadeClouds);
        config.addProperty("shadeEntities", shadeEntities);
        try {
            Files.write(getConfigPath(), GSON.toJson(config).getBytes(), new OpenOption[0]);
        } catch (IOException ex) {
            logException(ex, "Failed to save " + LOG_NAME + " config");
        }
    }

    public static void logException(Exception ex, String message) {
        System.err.printf("[%s] %s (%s: %s)%n", LOG_NAME, message, ex.getClass().getSimpleName(), ex.getLocalizedMessage());
    }

    private void loadConfig() {
        try {
            Path configPath = Files.exists(getConfigPath()) ? getConfigPath() : getLegacyConfigPath();
            JsonObject config = GSON.fromJson(Files.readString(configPath), JsonObject.class);
            asBoolean(config.get("enabled"), value -> enabled = value);
            asDouble(config.get("brightness"), value -> brightness = clamp(value, VANILLA_MIN_BRIGHTNESS, DEFAULT_FULLBRIGHT));
            asDouble(config.get("step"), value -> step = clamp(value, 0.1d, 2.0d));
            asBoolean(config.get("shadeBlocks"), value -> shadeBlocks = value);
            asBoolean(config.get("shadeClouds"), value -> shadeClouds = value);
            asBoolean(config.get("shadeEntities"), value -> shadeEntities = value);
            // Migrate the old brightness slot value if this is a previous Brightness Plus config.
            asDouble(config.get(String.valueOf(Math.max(1, getOldSelectedIndex(config)))), value -> brightness = clamp(value, VANILLA_MIN_BRIGHTNESS, DEFAULT_FULLBRIGHT));
        } catch (IOException | JsonSyntaxException | NullPointerException ex) {
            logException(ex, "Failed to load " + LOG_NAME + " config");
        }
    }

    private void onEndTick(MinecraftClient tickClient) {
        client = tickClient;
        if (tickClient.options == null) {
            return;
        }
        captureVanillaBrightness();
        while (openGuiBind.wasPressed()) {
            if (tickClient.currentScreen == null) {
                openConfigScreen(null);
            }
        }
        while (toggleBind.wasPressed()) {
            setEnabled(!enabled);
            showOverlay(tickClient);
        }
        while (raiseBind.wasPressed()) {
            setEnabled(true);
            adjustBrightness(scaledStep(tickClient));
            showOverlay(tickClient);
        }
        while (lowerBind.wasPressed()) {
            setEnabled(true);
            adjustBrightness(-scaledStep(tickClient));
            showOverlay(tickClient);
        }
        if (enabled && tickClient.currentScreen instanceof OptionsScreen) {
            prepareForVanillaOptionsScreen();
            return;
        }
        if (enabled) {
            if (Math.abs(tickClient.options.getGamma().getValue() - VANILLA_MAX_BRIGHTNESS) > 1.0E-6d) {
                applyToVanillaGamma();
            }
            markLightmapDirty();
        }
    }

    private static void applyToVanillaGamma() {
        if (client == null || client.options == null) {
            return;
        }
        double gamma = enabled ? VANILLA_MAX_BRIGHTNESS : vanillaBrightnessBeforeEnable;
        client.options.getGamma().setValue(clamp(gamma, VANILLA_MIN_BRIGHTNESS, VANILLA_MAX_BRIGHTNESS));
    }

    private static void adjustBrightness(double delta) {
        brightness = clamp(brightness + delta, VANILLA_MIN_BRIGHTNESS, DEFAULT_FULLBRIGHT);
        applyToVanillaGamma();
        markLightmapDirty();
        saveConfig();
    }

    private static void markLightmapDirty() {
        if (client != null && client.gameRenderer != null) {
            ((FullbrightNoShadingLightmapAccessor) client.gameRenderer.getLightmapTextureManager()).fullbrightNoShading$setDirty(true);
        }
    }

    private static void refreshTerrain() {
        if (client != null && client.worldRenderer != null) {
            client.worldRenderer.scheduleTerrainUpdate();
        }
    }

    private static void reloadResources() {
        if (client != null) {
            client.reloadResources();
        }
    }

    private static KeyBinding createKeyBinding(String translationKey, int keyCode) {
        for (Constructor<?> constructor : KeyBinding.class.getConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length == 4
                    && parameters[0] == String.class
                    && parameters[1] == InputUtil.Type.class
                    && parameters[2] == int.class
                    && parameters[3] != String.class) {
                try {
                    Object category = createKeyBindingCategory(parameters[3]);
                    return (KeyBinding) constructor.newInstance(translationKey, InputUtil.Type.KEYSYM, keyCode, category);
                } catch (ReflectiveOperationException ignored) {
                    // Fall through to the older string-category constructor below.
                }
            }
        }

        try {
            Constructor<KeyBinding> constructor = KeyBinding.class.getConstructor(String.class, InputUtil.Type.class, int.class, String.class);
            return constructor.newInstance(translationKey, InputUtil.Type.KEYSYM, keyCode, FULLBRIGHT_CATEGORY);
        } catch (ReflectiveOperationException oldCategoryMissing) {
            throw new IllegalStateException("Unable to create key binding for this Minecraft version", oldCategoryMissing);
        }
    }

    private static Object createKeyBindingCategory(Class<?> categoryClass) throws ReflectiveOperationException {
        try {
            Constructor<?> constructor = categoryClass.getConstructor(Identifier.class);
            return constructor.newInstance(Identifier.of(MOD_ID, "main"));
        } catch (NoSuchMethodException constructorMissing) {
            for (Method method : categoryClass.getMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (method.getReturnType() == categoryClass && parameters.length == 1 && parameters[0] == Identifier.class) {
                    return method.invoke(null, Identifier.of(MOD_ID, "main"));
                }
            }
            throw constructorMissing;
        }
    }

    private static void captureVanillaBrightness() {
        if (!vanillaBrightnessCaptured && client != null && client.options != null) {
            vanillaBrightnessBeforeEnable = clamp(client.options.getGamma().getValue(), VANILLA_MIN_BRIGHTNESS, VANILLA_MAX_BRIGHTNESS);
            vanillaBrightnessCaptured = true;
            if (enabled) {
                applyToVanillaGamma();
            }
        }
    }

    private double scaledStep(MinecraftClient tickClient) {
        boolean shift = isKeyPressed(tickClient, InputUtil.GLFW_KEY_LEFT_SHIFT) || isKeyPressed(tickClient, InputUtil.GLFW_KEY_RIGHT_SHIFT);
        boolean ctrl = isKeyPressed(tickClient, InputUtil.GLFW_KEY_LEFT_CONTROL) || isKeyPressed(tickClient, InputUtil.GLFW_KEY_RIGHT_CONTROL);
        double scaled = step;
        if (shift) {
            scaled *= 5.0d;
        }
        if (ctrl) {
            scaled *= 0.2d;
        }
        return scaled;
    }

    private boolean isKeyPressed(MinecraftClient tickClient, int keyCode) {
        Method method = getInputUtilIsKeyPressedMethod(tickClient);
        if (method == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(method.invoke(null, tickClient.getWindow(), keyCode));
        } catch (IllegalAccessException | InvocationTargetException ex) {
            return false;
        }
    }

    private Method getInputUtilIsKeyPressedMethod(MinecraftClient tickClient) {
        if (inputUtilIsKeyPressedLookedUp) {
            return inputUtilIsKeyPressedMethod;
        }
        inputUtilIsKeyPressedLookedUp = true;
        try {
            inputUtilIsKeyPressedMethod = InputUtil.class.getMethod("isKeyPressed", tickClient.getWindow().getClass(), int.class);
        } catch (NoSuchMethodException ex) {
            inputUtilIsKeyPressedMethod = null;
        }
        return inputUtilIsKeyPressedMethod;
    }

    private void showOverlay(MinecraftClient tickClient) {
        String translationKey = enabled ? "overlay.fullbright-plus.enabled" : "overlay.fullbright-plus.disabled";
        tickClient.inGameHud.setOverlayMessage(Text.translatable(translationKey, Math.round(brightness * 100.0d)).formatted(Formatting.YELLOW), false);
    }

    private void asDouble(JsonElement element, DoubleConsumer onSuccess) {
        if (element != null && element.isJsonPrimitive() && ((JsonPrimitive) element).isNumber()) {
            onSuccess.accept(element.getAsDouble());
        }
    }

    private void asBoolean(JsonElement element, BooleanConsumer onSuccess) {
        if (element != null && element.isJsonPrimitive() && ((JsonPrimitive) element).isBoolean()) {
            onSuccess.accept(element.getAsBoolean());
        }
    }

    private int getOldSelectedIndex(JsonObject config) {
        JsonElement selected = config.get("selected");
        if (selected != null && selected.isJsonPrimitive() && ((JsonPrimitive) selected).isNumber()) {
            return selected.getAsInt();
        }
        return 2;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("fullbright-no-shading.json");
    }

    private static Path getLegacyConfigPath() {
        Path fullbrightPlus = FabricLoader.getInstance().getConfigDir().resolve("fullbrightplus.json");
        return Files.exists(fullbrightPlus) ? fullbrightPlus : FabricLoader.getInstance().getConfigDir().resolve("brightnessplus.json");
    }

    @FunctionalInterface
    private interface BooleanConsumer {
        void accept(boolean value);
    }
}

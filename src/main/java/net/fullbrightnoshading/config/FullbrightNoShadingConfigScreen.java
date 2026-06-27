package net.fullbrightnoshading.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fullbrightnoshading.FullbrightNoShading;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class FullbrightNoShadingConfigScreen {
    private static final int MAX_BRIGHTNESS_PERCENT = 1200;
    private static final int DEFAULT_BRIGHTNESS_PERCENT = 1200;

    private FullbrightNoShadingConfigScreen() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("screen.fullbright-plus.title_with_author"))
                .setSavingRunnable(FullbrightNoShading::saveConfig);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("category.fullbright-plus.general"));
        ConfigCategory noShading = builder.getOrCreateCategory(Text.translatable("category.fullbright-plus.no_shading"));

        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("option.fullbright-plus.enabled"), FullbrightNoShading.isEnabled())
                .setDefaultValue(false)
                .setTooltip(Text.translatable("option.fullbright-plus.enabled.tooltip"))
                .setSaveConsumer(FullbrightNoShading::setEnabled)
                .build());

        general.addEntry(entryBuilder.startIntSlider(Text.translatable("option.fullbright-plus.brightness"), toPercent(FullbrightNoShading.getBrightness()), 0, MAX_BRIGHTNESS_PERCENT)
                .setDefaultValue(DEFAULT_BRIGHTNESS_PERCENT)
                .setTextGetter(value -> Text.translatable("option.fullbright-plus.percent_value", value))
                .setTooltip(Text.translatable("option.fullbright-plus.brightness.tooltip"))
                .setSaveConsumer(value -> FullbrightNoShading.setBrightness(value / 100.0d))
                .build());

        noShading.addEntry(entryBuilder.startBooleanToggle(Text.translatable("option.fullbright-plus.no_block_shading"), FullbrightNoShading.isBlockShadingDisabled())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("option.fullbright-plus.no_block_shading.tooltip"))
                .setSaveConsumer(FullbrightNoShading::setBlockShadingDisabled)
                .build());

        noShading.addEntry(entryBuilder.startBooleanToggle(Text.translatable("option.fullbright-plus.no_cloud_shading"), FullbrightNoShading.isCloudShadingDisabled())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("option.fullbright-plus.no_cloud_shading.tooltip"))
                .setSaveConsumer(FullbrightNoShading::setCloudShadingDisabled)
                .build());

        noShading.addEntry(entryBuilder.startBooleanToggle(Text.translatable("option.fullbright-plus.no_entity_shading"), FullbrightNoShading.isEntityShadingDisabled())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("option.fullbright-plus.no_entity_shading.tooltip"))
                .setSaveConsumer(FullbrightNoShading::setEntityShadingDisabled)
                .build());

        return builder.build();
    }

    private static int toPercent(double value) {
        return (int) Math.round(value * 100.0d);
    }
}

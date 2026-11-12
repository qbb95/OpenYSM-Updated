package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.config.ExtraPlayerRenderConfig;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.config.LoadingStateConfig;
import dev.architectury.platform.Platform;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import rip.ysm.gui.Option;
import rip.ysm.gui.OptionGroup;
import rip.ysm.gui.OptionRow;
import rip.ysm.gui.OptionScreen;
import rip.ysm.gui.components.BooleanOptionRow;
import rip.ysm.gui.components.EnumOptionRow;
import rip.ysm.gui.components.SliderOptionRow;
import rip.ysm.gui.components.StatusOptionRow;

public class ExtraPlayerConfigScreen extends OptionScreen {

    /**
     * When this mod is present the camera passthrough feature is active on its own,
     * so the row is a status readout rather than a switch.
     */
    private static final String PERSPECTIVE_MOD_ID = "non_animation_perspective";

    private Option<Boolean> realCameraBindOption;
    private OptionRow<?>[] realCameraDependentRows = new OptionRow<?>[0];

    public ExtraPlayerConfigScreen(@Nullable PlayerModelScreen modelScreen) {
        super(Component.literal("OpenYSM"), modelScreen);
    }

    @Override
    protected void registerGroups() {
        OptionGroup general = new OptionGroup("general")
                .add(new SliderOptionRow(0, 0, 0, 22, Option.ofDouble("sound_volume", GeneralConfig.SOUND_VOLUME), 0.0d, 100.0d, 1.0d, "%"))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_self_model", GeneralConfig.DISABLE_SELF_MODEL)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_other_model", GeneralConfig.DISABLE_OTHER_MODEL)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_self_hands", GeneralConfig.DISABLE_SELF_HANDS)));

        OptionGroup rendering = new OptionGroup("rendering")
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_player_render", ExtraPlayerRenderConfig.DISABLE_PLAYER_RENDER)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_projectile_model", GeneralConfig.DISABLE_PROJECTILE_MODEL)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_vehicle_model", GeneralConfig.DISABLE_VEHICLE_MODEL)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_external_first_person_anim", GeneralConfig.DISABLE_EXTERNAL_FP_ANIM)));

        OptionGroup performance = new OptionGroup("performance")
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("use_compatibility_renderer", GeneralConfig.USE_COMPATIBILITY_RENDERER)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("use_gpu_renderer", GeneralConfig.USE_GPU_RENDERER)));

        OptionGroup misc = new OptionGroup("misc")
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("print_animation_roulette_msg", GeneralConfig.PRINT_ANIMATION_ROULETTE_MSG)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_loading_state_screen", LoadingStateConfig.DISABLE_LOADING_STATE_SCREEN)))
                .add(new EnumOptionRow<>(0, 0, 0, 22, Option.ofEnum("loading_state_position", LoadingStateConfig.LOADING_STATE_POSITION), LoadingStateConfig.Position.values()))
                .add(new EnumOptionRow<>(0, 0, 0, 22, Option.ofEnum("roulette_settings_mode", GeneralConfig.ROULETTE_SETTINGS_MODE), GeneralConfig.RouletteSettingsMode.values()))
                .add(new EnumOptionRow<>(0, 0, 0, 22, Option.ofEnum("roulette_mode", GeneralConfig.ROULETTE_MODE), GeneralConfig.RouletteMode.values()))
                .add(new EnumOptionRow<>(0, 0, 0, 22, Option.ofEnum("texture_screen_mode", GeneralConfig.TEXTURE_SCREEN_MODE), GeneralConfig.TextureScreenMode.values()))
                .add(new EnumOptionRow<>(0, 0, 0, 22, Option.ofEnum("model_info_screen_mode", GeneralConfig.MODEL_INFO_SCREEN_MODE), GeneralConfig.ModelInfoScreenMode.values()))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("blur_gui", GeneralConfig.BLUR_GUI)));

        // RealCamera has its own group: the automatic binding is the master switch,
        // everything below it only matters while that binding is active.
        realCameraBindOption = Option.ofBoolean("realcamera_bind_rotation", GeneralConfig.REALCAMERA_BIND_ROTATION);
        OptionRow<?> viewModeRow = new EnumOptionRow<>(0, 0, 0, 22,
                Option.ofEnum("realcamera_view_mode", GeneralConfig.REALCAMERA_VIEW_MODE), GeneralConfig.RealCameraViewMode.values());
        OptionRow<?> dollyRow = new BooleanOptionRow(0, 0, 0, 22,
                Option.ofBoolean("realcamera_scroll_dolly", GeneralConfig.REALCAMERA_SCROLL_DOLLY));
        OptionRow<?> passthroughRow = new StatusOptionRow(0, 0, 0, 22,
                new Option<Boolean>("camera_passthrough", () -> Platform.isModLoaded(PERSPECTIVE_MOD_ID), value -> {
                }));

        OptionGroup realCamera = new OptionGroup("realcamera")
                .add(new BooleanOptionRow(0, 0, 0, 22, realCameraBindOption))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("hide_head_in_first_person", GeneralConfig.HIDE_HEAD_IN_FIRST_PERSON)))
                .add(viewModeRow)
                .add(dollyRow)
                .add(passthroughRow);
        realCameraDependentRows = new OptionRow<?>[]{viewModeRow, dollyRow, passthroughRow};

        groups.add(general);
        groups.add(rendering);
        groups.add(performance);
        groups.add(misc);
        groups.add(realCamera);
    }

    /**
     * Keeps the rows that only work inside the automatic binding greyed out while it
     * is switched off. Reads the pending value so the greying reacts before "apply".
     */
    @Override
    protected void renderExtras(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (realCameraBindOption == null) return;
        boolean bindingActive = Boolean.TRUE.equals(realCameraBindOption.get());
        for (OptionRow<?> row : realCameraDependentRows) {
            row.setRowEnabled(bindingActive);
        }
    }

    @Override
    protected void renderBlurredBackground(net.minecraft.client.gui.GuiGraphics guiGraphics) {

    }

}

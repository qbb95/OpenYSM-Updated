package com.elfmcys.yesstevemodel.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class GeneralConfig {

    public static ForgeConfigSpec.BooleanValue DISCLAIMER_SHOW;

    public static ForgeConfigSpec.BooleanValue PRINT_ANIMATION_ROULETTE_MSG;

    public static ForgeConfigSpec.BooleanValue DISABLE_SELF_MODEL;

    public static ForgeConfigSpec.BooleanValue DISABLE_OTHER_MODEL;

    public static ForgeConfigSpec.BooleanValue DISABLE_SELF_HANDS;

    public static ForgeConfigSpec.BooleanValue DISABLE_PROJECTILE_MODEL;

    public static ForgeConfigSpec.BooleanValue DISABLE_VEHICLE_MODEL;

    public static ForgeConfigSpec.BooleanValue DISABLE_EXTERNAL_FP_ANIM;

    public static ForgeConfigSpec.BooleanValue HIDE_HEAD_IN_FIRST_PERSON;

    public static ForgeConfigSpec.BooleanValue USE_COMPATIBILITY_RENDERER;

    public static ForgeConfigSpec.DoubleValue SOUND_VOLUME;

    public static ForgeConfigSpec.BooleanValue SHOW_MODEL_ID_FIRST;

    public static ForgeConfigSpec.BooleanValue SOPHISTICATEDBACKPACK;

    public static ForgeConfigSpec.BooleanValue PARCOOL;

    public static ForgeConfigSpec.BooleanValue REALCAMERA_BIND_ROTATION;

    public static ForgeConfigSpec.EnumValue<RealCameraViewMode> REALCAMERA_VIEW_MODE;

    public static ForgeConfigSpec.BooleanValue REALCAMERA_SCROLL_DOLLY;

    public static ForgeConfigSpec.DoubleValue REALCAMERA_SCROLL_DOLLY_MAX;

    public static ForgeConfigSpec.DoubleValue REALCAMERA_SCROLL_DOLLY_STEP;

    public static ForgeConfigSpec.BooleanValue USE_GPU_RENDERER;

    public static ForgeConfigSpec.EnumValue<RouletteSettingsMode> ROULETTE_SETTINGS_MODE;

    public static ForgeConfigSpec.EnumValue<RouletteMode> ROULETTE_MODE;

    public static ForgeConfigSpec.BooleanValue BLUR_GUI;

    public static ForgeConfigSpec.EnumValue<TextureScreenMode> TEXTURE_SCREEN_MODE;

    public static ForgeConfigSpec.EnumValue<ModelInfoScreenMode> MODEL_INFO_SCREEN_MODE;

    public enum RouletteSettingsMode {
        MODERN,
        CLASSIC
    }

    public enum RouletteMode {
        MODERN,
        CLASSIC
    }

    public enum TextureScreenMode {
        MODERN,
        CLASSIC
    }

    public enum ModelInfoScreenMode {
        MODERN,
        CLASSIC
    }

    /**
     * RealCamera 绑定模式第一人称在 NoN 之类骨骼动画下的朝向算法。
     * <ul>
     *   <li>{@link #MOUSE_ABSOLUTE}：镜头朝向完全由鼠标决定（和普通第一人称一样），
     *       动画只额外提供身体位置与侧滚 roll。任何姿势下上下都能看满 ±90°。</li>
     *   <li>{@link #FACE_RELATIVE}：镜头以动画脸的朝向为基准再叠加鼠标俯仰，
     *       镜头会跟着脸一起倾斜；脸朝下时向上角度会被脸本身吃掉一部分。</li>
     * </ul>
     */
    public enum RealCameraViewMode {
        MOUSE_ABSOLUTE,
        FACE_RELATIVE
    }

    public static boolean effectiveModernRoulette() {
        if (ROULETTE_MODE == null || ROULETTE_SETTINGS_MODE == null) return false;
        return ROULETTE_MODE.get() == RouletteMode.MODERN && ROULETTE_SETTINGS_MODE.get() == RouletteSettingsMode.MODERN;
    }

    public static ForgeConfigSpec buildSpec() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        defineGeneral(builder);
        ExtraPlayerRenderConfig.define(builder);
        LoadingStateConfig.define(builder);
        return builder.build();
    }

    public static void defineGeneral(ForgeConfigSpec.Builder builder) {
        builder.push("general");
        builder.comment("Whether to display disclaimer GUI");
        DISCLAIMER_SHOW = builder.define("DisclaimerShow", true);
        builder.comment("Whether to print animation roulette play message");
        PRINT_ANIMATION_ROULETTE_MSG = builder.define("PrintAnimationRouletteMsg", false);
        builder.comment("Prevents rendering of self player's model");
        DISABLE_SELF_MODEL = builder.define("DisableSelfModel", false);
        builder.comment("Prevents rendering of other player's model");
        DISABLE_OTHER_MODEL = builder.define("DisableOtherModel", false);
        builder.comment("Prevents rendering of self player's hand");
        DISABLE_SELF_HANDS = builder.define("DisableSelfHands", false);
        builder.comment("Prevents rendering of projectile model");
        DISABLE_PROJECTILE_MODEL = builder.define("DisableProjectileModel", false);
        builder.comment("Prevents rendering of vehicle model");
        DISABLE_VEHICLE_MODEL = builder.define("DisableVehicleModel", false);
        builder.comment("Disable first person animation from other mods.");
        DISABLE_EXTERNAL_FP_ANIM = builder.define("DisableExternalFirstPersonAnim", false);
        builder.comment("Hide the YSM model's head in first-person view. The extra player (paper doll) overlay keeps the head.");
        HIDE_HEAD_IN_FIRST_PERSON = builder.define("HideHeadInFirstPerson", true);
        builder.comment("If rendering errors occur, try turning on this.");
        USE_COMPATIBILITY_RENDERER = builder.define("UseCompatibilityRenderer", false);
        builder.comment("Test renderer.");
        USE_GPU_RENDERER = builder.define("UseGpuRenderer", true);
        ROULETTE_SETTINGS_MODE = builder.defineEnum("RouletteSettingsMode", RouletteSettingsMode.MODERN);
        ROULETTE_MODE = builder.defineEnum("RouletteMode", RouletteMode.CLASSIC);
        BLUR_GUI = builder.define("BlurGui", true);
        TEXTURE_SCREEN_MODE = builder.defineEnum("TextureScreenMode", TextureScreenMode.MODERN);
        MODEL_INFO_SCREEN_MODE = builder.defineEnum("ModelInfoScreenMode", ModelInfoScreenMode.MODERN);
        builder.comment("The amount of volume when the animation is played.");
        SOUND_VOLUME = builder.defineInRange("SoundVolume", 100.0d, 0.0d, 100.0d);
        builder.comment("Whether to display model ID first in the model selection screen, instead of the model name filled in by the model author.");
        SHOW_MODEL_ID_FIRST = builder.define("ShowModelIdFirst", false);
        builder.pop();
        builder.push("Integration");
        SOPHISTICATEDBACKPACK = builder.define("SophisticatedBackpack", true);
        PARCOOL = builder.define("Parcool", true);
        builder.comment("Let RealCamera's binding mode follow the YSM model's head orientation (including roll).");
        REALCAMERA_BIND_ROTATION = builder.define("RealCameraBindRotation", true);
        builder.comment("How the binding-mode first person camera is aimed during skeletal animations (e.g. needsofnature).",
                "MOUSE_ABSOLUTE: camera direction is fully driven by the mouse like normal first person; the animation only",
                "  contributes body position and roll, so up/down stay full ±90° in any pose.",
                "FACE_RELATIVE: camera is based on the animated face direction with the mouse as an offset, so the camera",
                "  tilts with the face; looking up is limited when the face already points down.");
        REALCAMERA_VIEW_MODE = builder.defineEnum("RealCameraViewMode", RealCameraViewMode.MOUSE_ABSOLUTE);
        builder.comment("Allow the mouse wheel to dolly the binding-mode first person camera while a triggered animation",
                "(animation roulette / needsofnature) is playing. Scroll up moves the camera forward along the",
                "face direction, scroll down pulls it back. The view direction and face plane are unchanged.",
                "The wheel is consumed while this is engaged, so it no longer switches hotbar slots.");
        REALCAMERA_SCROLL_DOLLY = builder.define("RealCameraScrollDolly", true);
        builder.comment("Maximum camera dolly distance in blocks (1 block = 16 model pixels).");
        REALCAMERA_SCROLL_DOLLY_MAX = builder.defineInRange("RealCameraScrollDollyMax", 2.5d, 0.05d, 8.0d);
        builder.comment("Camera distance change per mouse wheel notch, in blocks.");
        REALCAMERA_SCROLL_DOLLY_STEP = builder.defineInRange("RealCameraScrollDollyStep", 0.1d, 0.01d, 1.0d);
        builder.pop();
    }
}
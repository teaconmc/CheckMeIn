package org.teacon.checkin.configs;

import net.minecraftforge.common.ForgeConfigSpec.*;
import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {
    public static final ClientConfig INSTANCE;
    public static final ForgeConfigSpec CONFIG_SPEC;

    static {
        var pair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        INSTANCE = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

    public final IntValue checkPointRenderDistance;
    public final IntValue pathNaviRenderDistance;
    public final BooleanValue pathNaviArrowAlwaysShow;

    private ClientConfig(ForgeConfigSpec.Builder builder) {
        checkPointRenderDistance = builder
                .comment("Max render distance of check points when holding corresponding blocks or Path Planner")
                .translation("configs.check_in.check_point_render_distance")
                .defineInRange("check_point_render_distance", 32, 0, 1024);
        pathNaviRenderDistance = builder.comment("Max render distance of check points when holding Path Navigator")
                .translation("configs.check_in.path_navi_render_distance")
                .defineInRange("path_navi_render_distance", 16, 0, 1024);
        pathNaviArrowAlwaysShow = builder
                .comment("Always show arrow no matter if Path Navigator is holding in either hand")
                .translation("configs.check_in.path_navi_arrow_always_show")
                .define("path_navi_arrow_always_show", false);
    }

}

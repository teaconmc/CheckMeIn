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

    private ClientConfig(ForgeConfigSpec.Builder builder) {
        checkPointRenderDistance = builder.comment("Max render distance of check points with holding corresponding blocks or Path Planner")
                .translation("configs.check_in.check_point_render_distance")
                .defineInRange("check_point_render_distance", 32, 0, 1024);
    }

}

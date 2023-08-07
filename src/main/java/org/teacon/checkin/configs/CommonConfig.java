package org.teacon.checkin.configs;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

public class CommonConfig {
    public static final CommonConfig INSTANCE;
    public static final ForgeConfigSpec CONFIG_SPEC;

    static {
        var pair = new Builder().configure(CommonConfig::new);
        INSTANCE = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

    public final IntValue uniquePointCheckInRange;
    public final IntValue pathPointCheckInRange;

    private CommonConfig(Builder builder) {
        uniquePointCheckInRange = builder.comment("Range in which a player can check a Photography Check-in Point (Chebyshev distance)")
                .translation("configs.check_in.point_unique_check_range")
                .defineInRange("point_unique_check_range", 3, 0, 64);
        pathPointCheckInRange = builder.comment("Range in which a player can check a Path Check-in Point (Chebyshev distance)")
                .translation("configs.check_in.point_path_check_range")
                .defineInRange("point_path_check_range", 2, 0, 64);
    }

}

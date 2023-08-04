package org.teacon.checkin.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.client.events.ScreenshotNoGuiTaker;
import org.teacon.checkin.configs.ServerConfig;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.network.capability.CheckProgress;
import org.teacon.checkin.network.capability.UniquePointData;
import org.teacon.checkin.utils.MathHelper;

import java.util.Comparator;

public class Checker extends Item {
    public static final String NAME = "checker";

    public Checker(Properties prop) {
        super(prop);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var itemstack = player.getItemInHand(hand);
        player.getCooldowns().addCooldown(this, 20);

        if (!level.isClientSide) {
            CheckInPoints.of(level).ifPresent(points -> CheckProgress.of((ServerPlayer) player).ifPresent(progress -> {
                final BlockPos playerPos = player.blockPosition();
                final int r = ServerConfig.INSTANCE.uniquePointCheckInRange.get();
                points.getAllUniquePoints().stream()
                        .filter(data -> MathHelper.chebyshevDist(data.pos(), playerPos) <= r && !progress.checked(data))
                        .min(Comparator.comparing(data -> data.pos().distSqr(playerPos)))
                        .ifPresent(closest -> checkUniquePoint(level, progress, (ServerPlayer) player, closest));
            }));

        } else {
            ScreenshotNoGuiTaker.requestShot();
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    private static void checkUniquePoint(Level level, CheckProgress progress, ServerPlayer player, UniquePointData pointData) {
        if (progress.checkUniquePoint(pointData)) {
            player.sendSystemMessage(Component.translatable("item.check_in.checker.checked",
                                 pointData.teamID(), pointData.pointName()));
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    CheckMeIn.CHECKER_SHOT.get(), SoundSource.NEUTRAL,
                    0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        }
    }
}

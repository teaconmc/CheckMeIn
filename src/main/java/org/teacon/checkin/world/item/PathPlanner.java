package org.teacon.checkin.world.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.network.capability.PathPointData;
import org.teacon.checkin.world.level.block.entity.PointPathBlockEntity;

public class PathPlanner extends Item {
    public static final String NAME = "path_planner";
    public static final String PLANNER_PROPERTY_KEY = "PlannerProperty";
    public static final String TEAM_ID_KEY = "TeamID";
    public static final String PATH_ID_KEY = "PathID";
    public static final String LAST_DIM_KEY = "LastDim";
    public static final String LAST_POS_KEY = "LastPos";
    public static final String NEXT_ORD_KEY = "NextOrd";

    // TODO: make this range configurable
    private static final int CHECK_IN_RANGE = 2;


    public PathPlanner(Properties prop) {
        super(prop);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) return InteractionResult.sidedSuccess(true);
        var level = (ServerLevel) context.getLevel();
        var dim = level.dimensionTypeId().location().toString();
        var player = (ServerPlayer) context.getPlayer();
        if (player == null || !player.canUseGameMasterBlocks()) return InteractionResult.FAIL;
        var clickedPos = context.getClickedPos();
        if (!level.getBlockState(clickedPos).is(CheckMeIn.POINT_PATH_BLOCK.get()) || !(level.getBlockEntity(clickedPos) instanceof PointPathBlockEntity pointPathBlockEntity))
            return InteractionResult.FAIL;
        var capOpt = CheckInPoints.of(level).resolve();
        if (capOpt.isEmpty()) return InteractionResult.FAIL;
        var cap = capOpt.get();
        var data = cap.getPathPoint(clickedPos);

        if (data == null) {
            CheckMeIn.LOGGER.warn("Block {} at {}, {}, {} ({}) does not have data in level capability",
                    CheckMeIn.POINT_PATH_BLOCK.get(), clickedPos.getX(), clickedPos.getY(), clickedPos.getZ(),
                    level.dimensionTypeId().location());
            pointPathBlockEntity.removeIfInvalid();
            return InteractionResult.FAIL;
        }

        var compoundTag = context.getItemInHand().getOrCreateTagElement(PLANNER_PROPERTY_KEY);

        if (player.isShiftKeyDown()) {
            if (data.ord() == null) {
                player.sendSystemMessage(Component.translatable("item.check_in.path_planner.no_ord"), true);
                return InteractionResult.FAIL;
            }

            int ord = data.ord(), ordNext = ord + 1;
            compoundTag.putString(TEAM_ID_KEY, data.teamID());
            compoundTag.putString(PATH_ID_KEY, data.pathID());
            compoundTag.putString(LAST_DIM_KEY, level.dimensionTypeId().location().toString());
            compoundTag.put(LAST_POS_KEY, NbtUtils.writeBlockPos(data.pos()));
            compoundTag.putInt(NEXT_ORD_KEY, ordNext);

            player.sendSystemMessage(Component.translatable("item.check_in.path_planner.begin",
                    data.pathID(), ord, ordNext), true);

            return InteractionResult.SUCCESS;
        } else if (!compoundTag.contains(TEAM_ID_KEY, CompoundTag.TAG_STRING) || !compoundTag.contains(PATH_ID_KEY, CompoundTag.TAG_STRING)
                || !compoundTag.contains(LAST_DIM_KEY, CompoundTag.TAG_STRING) || !compoundTag.contains(LAST_POS_KEY, CompoundTag.TAG_COMPOUND)
                || !compoundTag.contains(NEXT_ORD_KEY, CompoundTag.TAG_INT)) {
            // no path is selected (fresh planner), or item data is missing
            player.sendSystemMessage(Component.translatable("item.check_in.path_planner.unselected"), true);
            return InteractionResult.FAIL;
        } else if (!compoundTag.getString(TEAM_ID_KEY).equals(data.teamID()) || !compoundTag.getString(PATH_ID_KEY).equals(data.pathID())) {
            // clicking in a different path
            player.sendSystemMessage(Component.translatable("item.check_in.path_planner.different_team",
                    data.teamID(), data.pathID(), compoundTag.getString(TEAM_ID_KEY), compoundTag.getString(PATH_ID_KEY)), true);
            return InteractionResult.FAIL;
        } else if (/* not clicking on the last block */ !compoundTag.getString(LAST_DIM_KEY).equals(dim)
                || !NbtUtils.readBlockPos(compoundTag.getCompound(LAST_POS_KEY)).equals(data.pos())) {
            var ordNew = compoundTag.getInt(NEXT_ORD_KEY);

            // if there is duplication of ordinal number, set the number of duplicating data to null
            for (var lvl : level.getServer().getAllLevels()) {
                var capTmp = CheckInPoints.of(lvl).resolve();
                if (capTmp.isEmpty()) continue;
                var dupData = capTmp.get().getPathPoint(data.teamID(), data.pathID(), ordNew);
                // nonnull and different from the clicked block
                if (dupData != null && (!dupData.pos().equals(data.pos()) || lvl != level)) {
                    capTmp.get().removePathPoint(dupData.pos());
                    capTmp.get().addPathPointIfAbsent(new PathPointData(dupData.pos(), dupData.teamID(), dupData.pointName(), dupData.pathID(), null));
                }
            }

            cap.removePathPoint(data.pos());
            cap.addPathPointIfAbsent(new PathPointData(data.pos(), data.teamID(), data.pointName(), data.pathID(), compoundTag.getInt(NEXT_ORD_KEY)));

            var lastDim = compoundTag.getString(LAST_DIM_KEY);
            var lastPos = NbtUtils.readBlockPos(compoundTag.getCompound(LAST_POS_KEY));
            compoundTag.putString(LAST_DIM_KEY, level.dimensionTypeId().location().toString());
            compoundTag.put(LAST_POS_KEY, NbtUtils.writeBlockPos(data.pos()));
            compoundTag.putInt(NEXT_ORD_KEY, ordNew + 1);

            if (!lastDim.equals(dim)) {
                player.sendSystemMessage(Component.translatable("item.check_in.path_planner.set_different_dim", ordNew), true);
            } else {
                var dist = Math.sqrt(lastPos.distSqr(data.pos()));
                if (dist < CHECK_IN_RANGE * 2)
                    player.sendSystemMessage(Component.translatable("item.check_in.path_planner.set_too_close", ordNew, dist), true);
                else
                    player.sendSystemMessage(Component.translatable("item.check_in.path_planner.set", ordNew, dist), true);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }


}

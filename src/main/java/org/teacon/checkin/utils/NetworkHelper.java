package org.teacon.checkin.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public class NetworkHelper {
    public static void writeAllBlockPosOptimized(List<BlockPos> list, FriendlyByteBuf buf, boolean noSize) {
        if (!noSize) buf.writeInt(list.size());
        if (canOptimize(list)) {
            buf.writeBoolean(true);

            var first = list.get(0);
            buf.writeBlockPos(first);
            var prev = first;
            for (var curr : list.subList(1, list.size())) {
                var d = curr.subtract(prev);
                buf.writeByte(d.getX());
                buf.writeByte(d.getY());
                buf.writeByte(d.getZ());
                prev = curr;
            }
        } else {
            buf.writeBoolean(false);

            list.forEach(buf::writeBlockPos);
        }
    }

    private static boolean canOptimize(List<BlockPos> list) {
        if (list.size() > 1) {
            int max = 0;
            var prev = list.get(0);
            for (var curr : list.subList(1, list.size())) {
                var p = curr.subtract(prev);
                max |= Math.abs(p.getX()) | Math.abs(p.getY()) | Math.abs(p.getZ());
                prev = curr;
            }
            return (~127 & max) == 0;// all involved bits are in the last 7 bits.
        }
        return false;
    }

    public static List<BlockPos> readAllBlockPosOptimized(FriendlyByteBuf buf, boolean noSize) {
        var ret = new ArrayList<BlockPos>();
        var size = noSize ? -1 : buf.readInt();
        var optimized = buf.readBoolean();
        if (optimized) {
            if (noSize && buf.isReadable()) {
                var prev = buf.readBlockPos();
                ret.add(prev);
                while (buf.isReadable()) {
                    var curr = prev.offset(new BlockPos(buf.readByte(), buf.readByte(), buf.readByte()));
                    ret.add(curr);
                    prev = curr;
                }
            } else if (!noSize && size > 0) {
                var prev = buf.readBlockPos();
                ret.add(prev);
                for (int i = 1; i < size; i++) {
                    var curr = prev.offset(new BlockPos(buf.readByte(), buf.readByte(), buf.readByte()));
                    ret.add(curr);
                    prev = curr;
                }
            }
        } else {
            if (noSize) while (buf.isReadable()) ret.add(buf.readBlockPos());
            else for (int i = 0; i < size; i++) ret.add(buf.readBlockPos());
        }
        return ret;
    }
}

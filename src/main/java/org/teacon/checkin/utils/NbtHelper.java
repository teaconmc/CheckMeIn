package org.teacon.checkin.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.stream.Collector;

public class NbtHelper {
    public static Collector<CompoundTag, ListTag, ListTag> toListTag() {
        return Collector.of(ListTag::new, ListTag::add, (l1, l2) -> {
            l1.add(l2);
            return l1;
        });
    }
}

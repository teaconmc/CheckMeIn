package org.teacon.checkin;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.teacon.checkin.world.item.PointUniqueItem;
import org.teacon.checkin.world.level.block.PointUniqueBlock;
import org.teacon.checkin.world.level.block.entity.PointUniqueBlockEntity;

@Mod(CheckMeIn.MODID)
public class CheckMeIn {
    public static final String MODID = "check_in";
    private static final Logger LOGGER = LogUtils.getLogger();


    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);


    public static final RegistryObject<Block> POINT_UNIQUE_BLOCk = BLOCKS.register(
            PointUniqueBlock.NAME, () -> new PointUniqueBlock(BlockBehaviour.Properties.of()
                    .strength(-1, 3600000)
                    .noLootTable()
                    .noOcclusion()
                    .isValidSpawn((a, b, c, d) -> false)
                    .noParticlesOnBreak()
                    .pushReaction(PushReaction.BLOCK)));

    public static final RegistryObject<BlockEntityType<PointUniqueBlockEntity>> POINT_UNIQUE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            PointUniqueBlock.NAME, () -> BlockEntityType.Builder.of(PointUniqueBlockEntity::new, POINT_UNIQUE_BLOCk.get()).build(null));
    public static final RegistryObject<Item> POINT_UNIQUE_ITEM = ITEMS.register(
            PointUniqueBlock.NAME, () -> new PointUniqueItem(POINT_UNIQUE_BLOCk.get(), new Item.Properties()
                    .rarity(Rarity.EPIC)));

    public static final RegistryObject<CreativeModeTab> CHECK_IN_TAB = CREATIVE_MODE_TABS.register("check_in_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.OP_BLOCKS)
            .icon(() -> POINT_UNIQUE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(POINT_UNIQUE_ITEM.get()))
            .title(Component.translatable("itemGroup.check_in"))
            .build());

    public CheckMeIn() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
        }
    }
}

package org.teacon.checkin;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.teacon.checkin.client.gui.screens.inventory.PointPathScreen;
import org.teacon.checkin.client.gui.screens.inventory.PointUniqueScreen;
import org.teacon.checkin.client.renderer.blockentity.CheckPointBlockRenderer;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.network.protocol.game.*;
import org.teacon.checkin.server.commands.CheckMeInCommand;
import org.teacon.checkin.world.inventory.PointPathMenu;
import org.teacon.checkin.world.inventory.PointUniqueMenu;
import org.teacon.checkin.world.item.PathPlanner;
import org.teacon.checkin.world.item.PointPathItem;
import org.teacon.checkin.world.item.PointUniqueItem;
import org.teacon.checkin.world.level.block.PointPathBlock;
import org.teacon.checkin.world.level.block.PointUniqueBlock;
import org.teacon.checkin.world.level.block.entity.PointPathBlockEntity;
import org.teacon.checkin.world.level.block.entity.PointUniqueBlockEntity;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mod(CheckMeIn.MODID)
public class CheckMeIn {
    public static final String MODID = "check_in";
    public static final Logger LOGGER = LogUtils.getLogger();


    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);

    private static final String NETWORK_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CheckMeIn.MODID, "network"),
            () -> NETWORK_VERSION, NETWORK_VERSION::equals, NETWORK_VERSION::equals);


    private static final BlockBehaviour.Properties CHECK_POINT_BLOCK_PROPERTIES = BlockBehaviour.Properties.of()
            .strength(-1, 3600000)
            .noLootTable()
            .noOcclusion()
            .isValidSpawn((a, b, c, d) -> false)
            .noParticlesOnBreak()
            .pushReaction(PushReaction.BLOCK);
    public static final RegistryObject<Block> POINT_UNIQUE_BLOCK = BLOCKS.register(PointUniqueBlock.NAME, () -> new PointUniqueBlock(CHECK_POINT_BLOCK_PROPERTIES));
    public static final RegistryObject<Block> POINT_PATH_BLOCK = BLOCKS.register(PointPathBlock.NAME, () -> new PointPathBlock(CHECK_POINT_BLOCK_PROPERTIES));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<PointUniqueBlockEntity>> POINT_UNIQUE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            PointUniqueBlock.NAME, () -> BlockEntityType.Builder.of(PointUniqueBlockEntity::new, POINT_UNIQUE_BLOCK.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<PointPathBlockEntity>> POINT_PATH_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            PointPathBlock.NAME, () -> BlockEntityType.Builder.of(PointPathBlockEntity::new, POINT_PATH_BLOCK.get()).build(null));
    public static final RegistryObject<Item> POINT_UNIQUE_ITEM = ITEMS.register(
            PointUniqueBlock.NAME, () -> new PointUniqueItem(POINT_UNIQUE_BLOCK.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> POINT_PATH_ITEM = ITEMS.register(
            PointPathBlock.NAME, () -> new PointPathItem(POINT_PATH_BLOCK.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> PATH_PLANNER = ITEMS.register(
            PathPlanner.NAME, () -> new PathPlanner(new Item.Properties().rarity(Rarity.EPIC)));

    @SuppressWarnings("unused")
    public static final RegistryObject<CreativeModeTab> CHECK_IN_TAB = CREATIVE_MODE_TABS.register("check_in_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.OP_BLOCKS)
            .icon(() -> POINT_UNIQUE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.acceptAll(Stream.of(
                    POINT_UNIQUE_ITEM, POINT_PATH_ITEM, PATH_PLANNER
            ).map(RegistryObject::get).map(ItemStack::new).collect(Collectors.toList())))
            .title(Component.translatable("itemGroup.check_in"))
            .build());

    public static final RegistryObject<MenuType<PointUniqueMenu>> POINT_UNIQUE_MENU = MENU_TYPES.register(
            PointUniqueBlock.NAME, () -> IForgeMenuType.create(PointUniqueMenu::new));
    public static final RegistryObject<MenuType<PointPathMenu>> POINT_PATH_MENU = MENU_TYPES.register(
            PointPathBlock.NAME, () -> IForgeMenuType.create(PointPathMenu::new));

    public CheckMeIn() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MENU_TYPES.register(modEventBus);

        MinecraftForge.EVENT_BUS.addListener(CheckMeIn::registerCommands);
        MinecraftForge.EVENT_BUS.addGenericListener(Level.class, CheckMeIn::attachCapabilities);
    }

    public static void registerCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        var buildContext = event.getBuildContext();
        CheckMeInCommand.register(dispatcher, buildContext);
    }

    public static void attachCapabilities(AttachCapabilitiesEvent<Level> event) {
        event.addCapability(CheckInPoints.ID, new CheckInPoints.Provider());
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class CommonModEvents {
        @SubscribeEvent
        @SuppressWarnings({"ReassignedVariable", "UnusedAssignment"})
        public static void commonSetup(FMLCommonSetupEvent event) {
            var packId = 0;
            CHANNEL.registerMessage(packId++, PointUniqueSetDataPacket.class,
                    PointUniqueSetDataPacket::write, PointUniqueSetDataPacket::new, PointUniqueSetDataPacket::handle);
            CHANNEL.registerMessage(packId++, PointUniqueScreenDataPacket.class,
                    PointUniqueScreenDataPacket::write, PointUniqueScreenDataPacket::new, PointUniqueScreenDataPacket::handle);

            CHANNEL.registerMessage(packId++, PointPathScreenDataPacket.class,
                    PointPathScreenDataPacket::write, PointPathScreenDataPacket::new, PointPathScreenDataPacket::handle);
            CHANNEL.registerMessage(packId++, PointPathSetDataPacket.class,
                    PointPathSetDataPacket::write, PointPathSetDataPacket::new, PointPathSetDataPacket::handle);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            MenuScreens.register(POINT_UNIQUE_MENU.get(), PointUniqueScreen::new);
            MenuScreens.register(POINT_PATH_MENU.get(), PointPathScreen::new);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(POINT_UNIQUE_BLOCK_ENTITY.get(), context ->
                    new CheckPointBlockRenderer<>(new ResourceLocation(CheckMeIn.MODID, "textures/item/point_unique.png")));
            event.registerBlockEntityRenderer(POINT_PATH_BLOCK_ENTITY.get(), context ->
                    new CheckPointBlockRenderer<>(new ResourceLocation(CheckMeIn.MODID, "textures/item/point_path.png")));
        }
    }
}

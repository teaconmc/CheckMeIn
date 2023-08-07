package org.teacon.checkin;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntIterators;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.teacon.checkin.client.gui.screens.inventory.PathNavigatorScreen;
import org.teacon.checkin.client.gui.screens.inventory.PointPathScreen;
import org.teacon.checkin.client.gui.screens.inventory.PointUniqueScreen;
import org.teacon.checkin.client.renderer.blockentity.CheckPointBlockRenderer;
import org.teacon.checkin.configs.ClientConfig;
import org.teacon.checkin.configs.CommonConfig;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.network.capability.CheckProgress;
import org.teacon.checkin.network.capability.GuidingManager;
import org.teacon.checkin.network.protocol.game.*;
import org.teacon.checkin.server.commands.CheckMeInCommand;
import org.teacon.checkin.server.commands.PointPathArgument;
import org.teacon.checkin.server.commands.PointUniqueArgument;
import org.teacon.checkin.world.inventory.PathNavigatorMenu;
import org.teacon.checkin.world.inventory.PointPathMenu;
import org.teacon.checkin.world.inventory.PointUniqueMenu;
import org.teacon.checkin.world.item.*;
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
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPEs = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, MODID);


    private static final String NETWORK_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(CheckMeIn.MODID, "network"),
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

    private static final Item.Properties ITEM_PROPS_EPIC = new Item.Properties().rarity(Rarity.EPIC);
    public static final RegistryObject<Item> POINT_UNIQUE_ITEM = ITEMS.register(PointUniqueBlock.NAME, () -> new PointUniqueItem(POINT_UNIQUE_BLOCK.get(), ITEM_PROPS_EPIC));
    public static final RegistryObject<Item> POINT_PATH_ITEM = ITEMS.register(PointPathBlock.NAME, () -> new PointPathItem(POINT_PATH_BLOCK.get(), ITEM_PROPS_EPIC));
    public static final RegistryObject<Item> PATH_PLANNER = ITEMS.register(PathPlanner.NAME, () -> new PathPlanner(ITEM_PROPS_EPIC));
    public static final RegistryObject<Item> CHECKER = ITEMS.register(Checker.NAME, () -> new Checker(ITEM_PROPS_EPIC));
    public static final RegistryObject<Item> NVG_PATH = ITEMS.register(PathNavigator.NAME, () -> new PathNavigator(ITEM_PROPS_EPIC));

    @SuppressWarnings("unused")
    public static final RegistryObject<CreativeModeTab> CHECK_IN_TAB = CREATIVE_MODE_TABS.register("check_in_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.OP_BLOCKS, CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> CHECKER.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.acceptAll(Stream.of(
                    POINT_UNIQUE_ITEM, POINT_PATH_ITEM, PATH_PLANNER, CHECKER, NVG_PATH
            ).map(RegistryObject::get).map(Item::getDefaultInstance).collect(Collectors.toList())))
            .title(Component.translatable("itemGroup.check_in"))
            .build());

    public static final RegistryObject<MenuType<PointUniqueMenu>> POINT_UNIQUE_MENU = MENU_TYPES.register(
            PointUniqueBlock.NAME, () -> IForgeMenuType.create(PointUniqueMenu::new));
    public static final RegistryObject<MenuType<PointPathMenu>> POINT_PATH_MENU = MENU_TYPES.register(
            PointPathBlock.NAME, () -> IForgeMenuType.create(PointPathMenu::new));
    public static final RegistryObject<MenuType<PathNavigatorMenu>> PATH_NAVIGATOR_MENU = MENU_TYPES.register(
            PathNavigator.NAME, () -> IForgeMenuType.create(PathNavigatorMenu::new));

    public static final RegistryObject<SoundEvent> CHECKER_SHOT = SOUND_EVENTS.register("item.check_in.checker.shot", () ->
            SoundEvent.createVariableRangeEvent(new ResourceLocation(CheckMeIn.MODID, "item.check_in.checker.shot")));
    public static final RegistryObject<SoundEvent> CHECK_PATH = SOUND_EVENTS.register("entity.check_in.player.check_path", () ->
            SoundEvent.createVariableRangeEvent(new ResourceLocation(CheckMeIn.MODID, "entity.check_in.player.check_path")));

    @SuppressWarnings("unused")
    public static final RegistryObject<ArgumentTypeInfo<?, ?>> POINT_UNIQUE_ARGUMENT = COMMAND_ARGUMENT_TYPEs.register("point_unique", () ->
            ArgumentTypeInfos.registerByClass(PointUniqueArgument.class, SingletonArgumentInfo.contextFree(PointUniqueArgument::new)));
    @SuppressWarnings("unused")
    public static final RegistryObject<ArgumentTypeInfo<?, ?>> POINT_PATH_ARGUMENT = COMMAND_ARGUMENT_TYPEs.register("point_path", () ->
            ArgumentTypeInfos.registerByClass(PointPathArgument.class, SingletonArgumentInfo.contextFree(PointPathArgument::new)));

    public CheckMeIn() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.CONFIG_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.CONFIG_SPEC);

        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        COMMAND_ARGUMENT_TYPEs.register(modEventBus);

        MinecraftForge.EVENT_BUS.addListener(CheckMeIn::registerCommands);
        MinecraftForge.EVENT_BUS.addGenericListener(Level.class, CheckMeIn::attachLevelCapabilities);
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, CheckMeIn::attachEntityCapabilities);
    }

    public static void registerCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        var buildContext = event.getBuildContext();
        CheckMeInCommand.register(dispatcher, buildContext);
    }

    public static void attachLevelCapabilities(AttachCapabilitiesEvent<Level> event) {
        if (!event.getObject().isClientSide) event.addCapability(CheckInPoints.ID, new CheckInPoints.Provider());
    }

    public static void attachEntityCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(GuidingManager.ID, new GuidingManager.Provider());
            event.addCapability(CheckProgress.ID, new CheckProgress.Provider());
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class CommonModEvents {
        @SubscribeEvent
        public static void commonSetup(FMLCommonSetupEvent event) {
            var packId = IntIterators.fromTo(0, Integer.MAX_VALUE);
            CHANNEL.registerMessage(packId.nextInt(), PointUniqueSetDataPacket.class, PointUniqueSetDataPacket::write, PointUniqueSetDataPacket::new, PointUniqueSetDataPacket::handle);
            CHANNEL.registerMessage(packId.nextInt(), PointPathSetDataPacket.class, PointPathSetDataPacket::write, PointPathSetDataPacket::new, PointPathSetDataPacket::handle);

            CHANNEL.registerMessage(packId.nextInt(), PathPlannerGuidePacket.class, PathPlannerGuidePacket::write, PathPlannerGuidePacket::new, PathPlannerGuidePacket::handle);

            CHANNEL.registerMessage(packId.nextInt(), PathNaviPageRequestPacket.class, PathNaviPageRequestPacket::write, PathNaviPageRequestPacket::new, PathNaviPageRequestPacket::handle);
            CHANNEL.registerMessage(packId.nextInt(), PathNaviPageResponsePacket.class, PathNaviPageResponsePacket::write, PathNaviPageResponsePacket::new, PathNaviPageResponsePacket::handle);
            CHANNEL.registerMessage(packId.nextInt(), PathNaviTpBackPacket.class, PathNaviTpBackPacket::write, PathNaviTpBackPacket::new, PathNaviTpBackPacket::handle);
            CHANNEL.registerMessage(packId.nextInt(), PathNaviErasePacket.class, PathNaviErasePacket::write, PathNaviErasePacket::new, PathNaviErasePacket::handle);
            CHANNEL.registerMessage(packId.nextInt(), PathNaviSetGuidingPathPacket.class, PathNaviSetGuidingPathPacket::write, PathNaviSetGuidingPathPacket::new, PathNaviSetGuidingPathPacket::handle);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                // FIXME: server should send client the globalpos of the next point in the path
                ItemProperties.register(NVG_PATH.get(), new ResourceLocation("angle"), new CompassItemPropertyFunction(
                        (level, itemStack, entity) -> null));

                MenuScreens.register(POINT_UNIQUE_MENU.get(), PointUniqueScreen::new);
                MenuScreens.register(POINT_PATH_MENU.get(), PointPathScreen::new);
                MenuScreens.register(PATH_NAVIGATOR_MENU.get(), PathNavigatorScreen::new);
            });
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

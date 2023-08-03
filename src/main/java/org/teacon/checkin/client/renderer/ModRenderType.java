package org.teacon.checkin.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.teacon.checkin.CheckMeIn;

import java.util.OptionalDouble;
import java.util.function.Function;

import static org.teacon.checkin.client.renderer.ModRenderType.RenderStateShardAccessor.*;

public class ModRenderType {
    public static final Function<ResourceLocation, RenderType> CHECK_POINT = Util.memoize(texture ->
            RenderType.create(CheckMeIn.MODID + ":checkpoint", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false,
                    RenderType.CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENTITY_CUTOUT_SHADER)
                            .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                            .setTransparencyState(NO_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .setDepthTestState(NO_DEPTH_TEST)
                            .createCompositeState(true)));
    public static final Function<OptionalDouble, RenderType> GUIDE_LINE = Util.memoize(width ->
            RenderType.create(CheckMeIn.MODID + ":guide_line", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 256, false, false,
                    RenderType.CompositeState.builder()
                            .setShaderState(RENDERTYPE_LINES_SHADER)
                            .setLineState(lineStateShard(width))
                            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setOutputState(ITEM_ENTITY_TARGET)
                            .setWriteMaskState(COLOR_DEPTH_WRITE)
                            .setCullState(NO_CULL)
                            .setDepthTestState(NO_DEPTH_TEST)
                            .createCompositeState(false)));

    protected static abstract class RenderStateShardAccessor extends RenderStateShard {
        protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_CUTOUT_SHADER = RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_SHADER;
        protected static final RenderStateShard.ShaderStateShard RENDERTYPE_LINES_SHADER = RenderStateShard.RENDERTYPE_LINES_SHADER;
        protected static final RenderStateShard.TransparencyStateShard NO_TRANSPARENCY = RenderStateShard.NO_TRANSPARENCY;
        protected static final RenderStateShard.TransparencyStateShard TRANSLUCENT_TRANSPARENCY = RenderStateShard.TRANSLUCENT_TRANSPARENCY;
        protected static final RenderStateShard.CullStateShard NO_CULL = RenderStateShard.NO_CULL;
        protected static final RenderStateShard.OverlayStateShard OVERLAY = RenderStateShard.OVERLAY;
        protected static final RenderStateShard.DepthTestStateShard NO_DEPTH_TEST = RenderStateShard.NO_DEPTH_TEST;
        protected static final RenderStateShard.LightmapStateShard LIGHTMAP = RenderStateShard.LIGHTMAP;
        protected static final RenderStateShard.LayeringStateShard VIEW_OFFSET_Z_LAYERING = RenderStateShard.VIEW_OFFSET_Z_LAYERING;
        protected static final RenderStateShard.OutputStateShard ITEM_ENTITY_TARGET = RenderStateShard.ITEM_ENTITY_TARGET;
        protected static final RenderStateShard.WriteMaskStateShard COLOR_DEPTH_WRITE = RenderStateShard.COLOR_DEPTH_WRITE;

        @SuppressWarnings({"OptionalUsedAsFieldOrParameterType"})
        protected static RenderStateShard.LineStateShard lineStateShard(OptionalDouble width) {
            return new RenderStateShard.LineStateShard(width);
        }

        protected RenderStateShardAccessor(String name, Runnable setup, Runnable clear) {super(name, setup, clear);}
    }
}

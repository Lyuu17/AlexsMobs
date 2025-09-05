package com.github.alexthe666.alexsmobs.renderer;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;

import java.awt.*;

@Environment(EnvType.CLIENT)
public class OctopusColorRegistry {

    public static final BlockState FALLBACK_BLOCK = Blocks.SAND.getDefaultState();
    public static Object2IntMap<String> TEXTURES_TO_COLOR = new Object2IntOpenHashMap<>();

    public static int getBlockColor(BlockState stack) {
        String blockName = stack.toString();
        if (TEXTURES_TO_COLOR.containsKey(blockName)) {
            return TEXTURES_TO_COLOR.getInt(blockName);
        } else {
            int colorizer = -1;
            try{
                colorizer = MinecraftClient.getInstance().getBlockColors().getColor(stack, null, null, 0);
            }catch (Exception e){
                AlexsMobs.LOGGER.warn("Another mod did not use block colorizers correctly.");
            }
            int color = 0XFFFFFF;
            if(colorizer == -1){
                try {
                    Color texColour = getAverageColour(getTextureAtlas(stack));
                    color = texColour.getRGB();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }else{
                color = colorizer;
            }
            TEXTURES_TO_COLOR.put(blockName, color);
            return color;
        }
    }

    private static Color getAverageColour(Sprite image) {
        float red = 0;
        float green = 0;
        float blue = 0;
        float count = 0;
        int uMax = image.getContents().getWidth();
        int vMax = image.getContents().getHeight();
        for (float i = 0; i < uMax; i++)
            for (float j = 0; j < vMax; j++) {
                int alpha = getPixelRGBA(image, 0, (int) i, (int) j) >> 24 & 0xFF;
                if (alpha == 0) {
                    continue;
                }
                red += getPixelRGBA(image, 0, (int) i, (int) j) & 0xFF;
                green += getPixelRGBA(image, 0, (int) i, (int) j) >> 8 & 0xFF;
                blue += getPixelRGBA(image, 0, (int) i, (int) j) >> 16 & 0xFF;
                count++;
            }
        //Average color
        return new Color((int) (red / count), (int) (green / count), (int) (blue / count));
    }

    private static Sprite getTextureAtlas(BlockState state) {
        return MinecraftClient.getInstance().getBlockRenderManager().getModels().getModel(state).getParticleSprite();
    }

    /**
     * @see <a href="https://github.com/MinecraftForge/MinecraftForge/blob/1.20.x/patches/minecraft/net/minecraft/client/renderer/texture/TextureAtlasSprite.java.patch">...</a>
     */
    private static int getPixelRGBA(Sprite image, int frameIndex, int x, int y) {
        if (image.getContents().animation != null) {
            x += image.getContents().animation.getFrameX(frameIndex) * image.getContents().getWidth();
            y += image.getContents().animation.getFrameY(frameIndex) * image.getContents().getHeight();
        }
        return image.getContents().image.getColor(x, y);
    }
}

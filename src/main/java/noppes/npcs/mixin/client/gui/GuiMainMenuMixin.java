package noppes.npcs.mixin.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.mixin.client.gui.IGuiMainMenuMixin;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(value = GuiMainMenu.class, priority = 499)
public class GuiMainMenuMixin implements IGuiMainMenuMixin {

    @Shadow
    private ResourceLocation backgroundTexture;

    @Final
    @Shadow
    private static ResourceLocation[] TITLE_PANORAMA_PATHS;

    @Unique
    public int cnpc$variant = new Random().nextInt(CustomNpcs.PanoramaNumbers);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void npcs$onConstructor(CallbackInfo ci) {
        for (int i = 0; i < 6; i++) {
            TITLE_PANORAMA_PATHS[i] = new ResourceLocation(CustomNpcs.MODID, "textures/gui/title/background/"+cnpc$variant+"/panorama_"+i+".png");
        }
    }

    /**
     * @author BetaZavr
     * @reason remove white mask
     */
    @Overwrite
    private void rotateAndBlurSkybox() {
        Minecraft.getMinecraft().getTextureManager().bindTexture(backgroundTexture);
        GlStateManager.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, 256, 256);
    }

    @Override
    public ResourceLocation[] npcs$getImages() { return TITLE_PANORAMA_PATHS; }

}

package noppes.npcs.client.renderer.blocks;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.model.blocks.ModelCarpentryBench;

public class BlockCarpentryBenchRenderer<T extends TileEntity> extends TileEntitySpecialRenderer<T> {
	private static ResourceLocation TEXTURE = new ResourceLocation(CustomNpcs.MODID,
			"textures/models/carpentrybench.png");
	private ModelCarpentryBench model;

	public BlockCarpentryBenchRenderer() {
		this.model = new ModelCarpentryBench();
	}

	public void render(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		int rotation = 0;
		if (te != null && te.getPos() != BlockPos.ORIGIN) {
			rotation = te.getBlockMetadata() % 4;
		}
		GlStateManager.pushMatrix();
		GlStateManager.disableBlend();
		GlStateManager.enableLighting();
		GlStateManager.translate(x + 0.5f, y + 1.4f, z + 0.5f);
		GlStateManager.scale(0.95f, 0.95f, 0.95f);
		GlStateManager.rotate(180.0f, 0.0f, 0.0f, 1.0f);
		GlStateManager.rotate((90 * rotation), 0.0f, 1.0f, 0.0f);
		this.bindTexture(BlockCarpentryBenchRenderer.TEXTURE);
		this.model.render(null, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0625f);
		GlStateManager.popMatrix();
	}

}

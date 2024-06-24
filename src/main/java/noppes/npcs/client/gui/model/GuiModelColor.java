package noppes.npcs.client.gui.model;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.imageio.ImageIO;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.gui.select.GuiTextureSelection;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.ITextfieldListener;
import noppes.npcs.client.gui.util.SubGuiInterface;

public class GuiModelColor extends SubGuiInterface implements ITextfieldListener {

	public interface ColorCallback {
		void color(int p0);
	}

	private static ResourceLocation colorgui = new ResourceLocation("moreplayermodels:textures/gui/color_gui.png");
	private static ResourceLocation colorPicker = new ResourceLocation("moreplayermodels:textures/gui/color.png");
	private ResourceLocation npcSkin;
	private ColorCallback callback;
	public int color, colorX, colorY, hover;
	public boolean hovered;
	public GuiScreen parent;
	private BufferedImage bufferColor, bufferSkin;

	private GuiNpcTextField textfield;

	public GuiModelColor(GuiScreen parent, int color, ColorCallback callback) {
		this.parent = parent;
		this.callback = callback;
		this.ySize = 230;
		this.closeOnEsc = false;
		this.color = color;
		this.background = GuiModelColor.colorgui;
		this.npcSkin = null;
		this.hover = 0;
		this.hovered = false;
	}

	@Override
	protected void actionPerformed(GuiButton guibutton) {
		if (guibutton.id == 66) {
			this.close();
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		
		if (this.npcSkin != null) {
			// back
			this.mc.renderEngine.bindTexture(GuiModelColor.colorgui);
			int xs = this.colorX + 128;
			int ys = this.colorY;
			this.drawTexturedModalRect(xs + 3, ys - 5, 11, 0, 134, 1);
			this.drawTexturedModalRect(xs + 2, ys - 4, 10, 1, 135, 1);
			this.drawTexturedModalRect(xs + 1, ys - 3, 9, 2, 136, 122);
			this.drawTexturedModalRect(xs, ys + 119, 8, 169, 137, 4);
			
			GlStateManager.pushMatrix();
			GlStateManager.translate(xs + 4, ys, 0.0f);
			GlStateManager.scale(0.46f, 0.46f, 0.46f);
			this.mc.renderEngine.bindTexture(this.npcSkin);
			
			Gui.drawRect(-1, -1, 258, 258, GuiTextureSelection.dark ? 0xFFE0E0E0 : 0xFF202020);
			Gui.drawRect(0, 0, 256, 256, GuiTextureSelection.dark ? 0xFF000000 : 0xFFFFFFFF);
			int g = 16;
			for (int u = 0; u < 16; u ++) {
				for (int v = 0; v < 16; v ++) {
					if (u % 2 == (v % 2 == 0 ? 1 : 0)) {
						Gui.drawRect(u * g, v * g, u * g + g, v * g + g, GuiTextureSelection.dark ? 0xFF343434 : 0xFFCCCCCC);
					}
				}
			}
			
			this.drawTexturedModalRect(0, 0, 0, 0, 256, 256);
			GlStateManager.popMatrix();

		} else if (this.npc != null && this.bufferSkin == null && !this.npc.display.getSkinTexture().isEmpty()) {
			this.npcSkin = new ResourceLocation(this.npc.display.getSkinTexture());
			InputStream stream = null;
			try {
				IResource resource = this.mc.getResourceManager().getResource(this.npcSkin);
				this.bufferSkin = ImageIO.read(stream = resource.getInputStream());
			} catch (IOException ex) {
			} finally {
				if (stream != null) {
					try {
						stream.close();
					} catch (IOException ex2) {
					}
				}
			}
		}
		this.hovered = false;
		if (this.bufferColor == null) {
			InputStream stream = null;
			try {
				IResource resource = this.mc.getResourceManager().getResource(GuiModelColor.colorPicker);
				this.bufferColor = ImageIO.read(stream = resource.getInputStream());
			}
			catch (IOException ex) { }
			finally {
				if (stream != null) {
					try { stream.close(); }
					catch (IOException ex2) { }
				}
			}
		}

		int x = this.colorX + 4, y = this.colorY;
		this.drawGradientRect(x - 2, y - 2, x + 120, y + 119, 0xFFF0F0F0, 0xFF202020);
		this.drawGradientRect(x - 1, y - 1, x + 119, y + 118, 0xFF202020, 0xFFF0F0F0);
		this.mc.renderEngine.bindTexture(GuiModelColor.colorPicker);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		this.drawTexturedModalRect(x, y, 0, 1, 120, 120);
		
		if (this.bufferColor != null && isMouseHover(mouseX, mouseY, x, y, 117, 117)) {
			int xb = (mouseX - x) * 4;
			int yb = (mouseY - y + 1) * 4;
			this.hover = this.bufferColor.getRGB(xb, yb) & 0xFFFFFF;
			this.hovered = true;
		}
		else {
			x = this.colorX + 132;
			if (this.bufferSkin != null && isMouseHover(mouseX, mouseY, x, y, 118, 118)) {
				float xb = (float) (mouseX - x) / 0.458823f;
				float yb = (float) (mouseY - y) / 0.458823f;
				float w = 256.0f / (float) this.bufferSkin.getWidth();
				float h = 256.0f / (float) this.bufferSkin.getHeight();
				try { this.hover = this.bufferSkin.getRGB((int) ((float) xb / w), (int) ((float) yb / h)) & 0xFFFFFF; }
				catch (Exception e) { }
				this.hovered = true;
			}
		}
		
		GlStateManager.pushMatrix();
		GlStateManager.translate(this.colorX + 5, this.colorY - 25, 1.0f);
		int c = this.hovered ? this.hover : this.color;
		this.drawGradientRect(-1, -1, 21, 21, 0x80000000, 0x80000000);
		this.drawGradientRect(0, 0, 20, 20, 0xFF000000 + c, 0xFF000000 + c);
		this.drawGradientRect(0, 0, 20, 20, c, c);
		GlStateManager.popMatrix();
		
		if (!CustomNpcs.ShowDescriptions) { return; }
		if (this.getTextField(0) != null && this.getTextField(0).isMouseOver()) {
			this.setHoverText(new TextComponentTranslation("hover.set.color").getFormattedText());
		} else if (this.getButton(66) != null && this.getButton(66).isMouseOver()) {
			this.setHoverText(new TextComponentTranslation("hover.back").getFormattedText());
		}
		if (this.hoverText != null) {
			this.drawHoveringText(Arrays.asList(this.hoverText), mouseX, mouseY, this.fontRenderer);
			this.hoverText = null;
		}
	}

	public String getColor() {
		String str;
		for (str = Integer.toHexString(this.color); str.length() < 6; str = "0" + str) {
		}
		return str;
	}

	@Override
	public void initGui() {
		super.initGui();
		this.colorX = this.guiLeft + 4;
		this.colorY = this.guiTop + 50;
		this.addTextField(this.textfield = new GuiNpcTextField(0, this, this.guiLeft + 35, this.guiTop + 25, 60, 20, this.getColor()));
		this.addButton(new GuiNpcButton(66, this.guiLeft + 107, this.guiTop + 8, 20, 20, "X"));
	}

	@Override
	public void keyTyped(char c, int i) {
		String prev = this.textfield.getText();
		super.keyTyped(c, i);
		String newText = this.textfield.getText();
		if (newText.equals(prev)) {
			return;
		}
		try {
			this.color = Integer.parseInt(this.textfield.getText(), 16);
			this.callback.color(this.color);
		} catch (NumberFormatException e) {
			this.textfield.setText(prev);
		}
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseBottom) {
		super.mouseClicked(mouseX, mouseY, mouseBottom);
		if (this.hovered && this.hover != 0) {
			this.color = this.hover;
			this.callback.color(this.hover);
			this.textfield.setText(this.getColor());
		}
	}

	@Override
	public void save() {
	}

	@Override
	public void unFocused(GuiNpcTextField textfield) {
		try {
			this.color = Integer.parseInt(textfield.getText(), 16);
		} catch (NumberFormatException e) {
			this.color = 0;
		}
		this.callback.color(this.color);
	}

}

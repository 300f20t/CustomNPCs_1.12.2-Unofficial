package noppes.npcs.config;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.util.LRUHashMap;

public class TrueTypeFont {

	static class Glyph {
		public boolean isColor = false;
		GlyphType type;
		int color;
		int x;
		int y;
		int height;
		int width;
		int texture;
		char c;

		Glyph(char c) {
			this.type = GlyphType.NORMAL;
			this.color = -1;
			this.c = c;
		}
	}

	static class GlyphCache {
		public int width;
		public int height;
		List<Glyph> glyphs;

		GlyphCache() {
			this.glyphs = new ArrayList<>();
		}
	}

	enum GlyphType {
		NORMAL, COLOR, RANDOM, BOLD, STRIKETHROUGH, UNDERLINE, ITALIC, RESET, OTHER
	}

	static class TextureCache {
		int x;
		int y;
		int textureId;
		BufferedImage bufferedImage;
		Graphics2D g;
		boolean full;

		TextureCache() {
			this.textureId = GlStateManager.generateTexture();
			this.bufferedImage = new BufferedImage(512, 512, 2);
			this.g = (Graphics2D) this.bufferedImage.getGraphics();
		}
	}

	private static final List<Font> allFonts = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts());

	private final List<Font> usedFonts;
	private final LinkedHashMap<String, GlyphCache> textcache;
	private final Map<Character, Glyph> glyphcache;
	private final List<TextureCache> textures;
	private final Font font;
	private int lineHeight;
	private final Graphics2D globalG;
	public float scale;
	private char specialChar;

	public TrueTypeFont(Font font, float scale) {
		this.usedFonts = new ArrayList<>();
		this.textcache = new LRUHashMap<>(100);
		this.glyphcache = new HashMap<>();
		this.textures = new ArrayList<>();
		this.lineHeight = 1;
		this.globalG = (Graphics2D) new BufferedImage(1, 1, 2).getGraphics();
        this.specialChar = (char) 167;
		this.font = font;
		this.scale = scale;
		this.globalG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		this.lineHeight = this.globalG.getFontMetrics(font).getHeight();
	}

	public TrueTypeFont(ResourceLocation resource, int fontSize, float scale) throws IOException, FontFormatException {
		this.usedFonts = new ArrayList<>();
		this.textcache = new LRUHashMap<>(100);
		this.glyphcache = new HashMap<>();
		this.textures = new ArrayList<>();
		this.lineHeight = 1;
		this.globalG = (Graphics2D) new BufferedImage(1, 1, 2).getGraphics();
		this.scale = 1.0f;
		this.specialChar = 167;
		InputStream stream = Minecraft.getMinecraft().getResourceManager().getResource(resource).getInputStream();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		Font font = Font.createFont(0, stream);
		ge.registerFont(font);
		this.font = font.deriveFont(Font.PLAIN, fontSize);
		this.scale = scale;
		this.globalG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		this.lineHeight = this.globalG.getFontMetrics(font).getHeight();
	}

	public boolean hasFont() { return font != null; }

	public void dispose() {
		for (TextureCache cache : this.textures) { GlStateManager.deleteTexture(cache.textureId); }
		this.textcache.clear();
	}

	public void draw(String text, float x, float y, int color) {
		GlyphCache cache = getOrCreateCache(text);
		float r = (color >> 16 & 0xFF) / 255.0f;
		float g = (color >> 8 & 0xFF) / 255.0f;
		float b = (color & 0xFF) / 255.0f;
		GlStateManager.color(r, g, b, 1.0f);
		GlStateManager.enableBlend();
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, 0.0f);
		GlStateManager.scale(this.scale, this.scale, 1.0f);
		float i = 0.0f;
		for (Glyph gl : cache.glyphs) {
			if (gl.type != GlyphType.NORMAL) {
				if (gl.type == GlyphType.RESET) {
					GlStateManager.color(r, g, b, 1.0f);
				} else {
					if (gl.type != GlyphType.COLOR) {
						continue;
					}
					GlStateManager.color((gl.color >> 16 & 0xFF) / 255.0f, (gl.color >> 8 & 0xFF) / 255.0f,
							(gl.color & 0xFF) / 255.0f, 1.0f);
				}
			} else {
				GlStateManager.bindTexture(gl.texture);
				this.drawTexturedModalRect(i, 0.0f, gl.x * this.textureScale(), gl.y * this.textureScale(),
						gl.width * this.textureScale(), gl.height * this.textureScale());
				i += gl.width * this.textureScale();
			}
		}
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
	}

	public void drawTexturedModalRect(float x, float y, float textureX, float textureY, float width, float height) {
		float f = 0.00390625f;
		float f2 = 0.00390625f;
		int zLevel = 0;
		BufferBuilder tessellator = Tessellator.getInstance().getBuffer();
		tessellator.begin(7, DefaultVertexFormats.POSITION_TEX);
		tessellator.noColor();
		tessellator.pos(x, (y + height), zLevel).tex((textureX * f), ((textureY + height) * f2)).endVertex();
		tessellator.pos((x + width), (y + height), zLevel).tex(((textureX + width) * f), ((textureY + height) * f2))
				.endVertex();
		tessellator.pos((x + width), y, zLevel).tex(((textureX + width) * f), (textureY * f2)).endVertex();
		tessellator.pos(x, y, zLevel).tex((textureX * f), (textureY * f2)).endVertex();
		Tessellator.getInstance().draw();
	}

	private TextureCache getCurrentTexture() {
		TextureCache cache = null;
		for (TextureCache t : this.textures) {
			if (!t.full) {
				cache = t;
				break;
			}
		}
		if (cache == null) {
			this.textures.add(cache = new TextureCache());
		}
		return cache;
	}

	private Font getFontForChar(char c) {
		if (this.font.canDisplay(c)) {
			return this.font;
		}
		for (Font f : this.usedFonts) {
			if (f.canDisplay(c)) {
				return f;
			}
		}
		Font fa = new Font("Arial Unicode MS", Font.PLAIN, this.font.getSize());
		if (fa.canDisplay(c)) {
			return fa;
		}
		for (Font f2 : TrueTypeFont.allFonts) {
			if (f2.canDisplay(c)) {
				this.usedFonts.add(f2 = f2.deriveFont(Font.PLAIN, this.font.getSize()));
				return f2;
			}
		}
		return this.font;
	}

	public String getFontName() {
		return this.font.getFontName();
	}

	private GlyphCache getOrCreateCache(String text) {
		GlyphCache cache = this.textcache.get(text);
		if (cache != null) {
			return cache;
		}
		cache = new GlyphCache();
		Font font = this.getFontForChar(' ');
		FontMetrics metrics = this.globalG.getFontMetrics(font);
		int tabStep = metrics.charWidth((char) 32) * 6;
		for (int i = 0; i < text.length(); ++i) {
			char c = text.charAt(i);
			if (c == this.specialChar && i + 1 < text.length()) {
				char next = text.toLowerCase(Locale.ENGLISH).charAt(i + 1);
				int index = "0123456789abcdefklmnor".indexOf(next);
				if (index >= 0) {
					Glyph g = new Glyph(c);
					if (index < 16) {
						g.type = GlyphType.COLOR;
						g.color = Minecraft.getMinecraft().fontRenderer.getColorCode(next);
					} else if (index == 16) {
						g.type = GlyphType.RANDOM;
					} else if (index == 17) {
						g.type = GlyphType.BOLD;
					} else if (index == 18) {
						g.type = GlyphType.STRIKETHROUGH;
					} else if (index == 19) {
						g.type = GlyphType.UNDERLINE;
					} else if (index == 20) {
						g.type = GlyphType.ITALIC;
					} else {
						g.type = GlyphType.RESET;
					}
					g.isColor = true;
					cache.glyphs.add(g);
					++i;
					continue;
				}
			}
			Glyph g2 = this.getOrCreateGlyph(c);
			cache.glyphs.add(g2);
			if (c == '	') { // Tabulation
				int nw = (int) (Math.ceil((double) cache.width / (double) tabStep) * (double) tabStep);
				if (cache.width == nw) {
					cache.width += tabStep;
					g2.width = tabStep;
				} else {
					int gw = nw - cache.width;
					cache.width += gw;
					g2.width = gw;
				}
			} else {
				cache.width += g2.width;
			}
			cache.height = Math.max(cache.height, g2.height);
		}
		textcache.put(text, cache);
		return cache;
	}

	private Glyph getOrCreateGlyph(char c) {
		Glyph g = this.glyphcache.get(c);
		if (g != null && c != '	') {
			return g;
		}
		Font font = this.getFontForChar(c);
		FontMetrics metrics = this.globalG.getFontMetrics(font);
		g = new Glyph(c);
		g.width = Math.max(metrics.charWidth(c), 1);
		g.height = Math.max(metrics.getHeight(), 1);
		if (c == '	') {
			g.width = metrics.charWidth((char) 32) * 6;
		} // Tabulation
		TextureCache cache = this.getCurrentTexture();
		if (cache.x + g.width >= 512) {
			cache.x = 0;
			TextureCache textureCache = cache;
			textureCache.y += this.lineHeight + 1;
			if (cache.y >= 512) {
				cache.full = true;
				cache = this.getCurrentTexture();
			}
		}
		g.x = cache.x;
		g.y = cache.y;
		cache.x += g.width + 3;
		this.lineHeight = Math.max(this.lineHeight, g.height);
		cache.g.setFont(font);
		cache.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		cache.g.drawString(c + "", g.x, g.y + metrics.getAscent());
		g.texture = cache.textureId;
		TextureUtil.uploadTextureImage(cache.textureId, cache.bufferedImage);
		glyphcache.put(c, g);
		return g;
	}

	public synchronized int height(String text) {
		if (text == null || text.trim().isEmpty()) {
			return (int) (this.lineHeight * this.scale * this.textureScale());
		}
		GlyphCache cache = this.getOrCreateCache(text);
		return (int) Math.max(1, (cache.height * this.scale * this.textureScale()));
	}

	public void setSpecial(char c) {
		this.specialChar = c;
	}

	private float textureScale() {
		return 0.5f;
	}

	public int width(String text) {
		GlyphCache cache = this.getOrCreateCache(text);
		return (int) (cache.width * this.scale * this.textureScale());
	}

}

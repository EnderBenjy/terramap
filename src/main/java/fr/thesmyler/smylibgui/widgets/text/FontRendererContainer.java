package fr.thesmyler.smylibgui.widgets.text;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import fr.thesmyler.smylibgui.SmyLibGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

/**
 * Q: Why?
 * A: Because some at Mojang though nobody would ever want to render a transparent String, yet I do
 * 
 * Q: Yes, but why?
 * A: I didn't want to just extend FontRenderer as I prefer to use the vanilla Mincraft::fontRenderer
 * 
 * Q: Still, why?
 * A: Ok, I admit it, I wanted to experiment with ObfuscationReflectionHelper. I know, right?
 *
 */
public class FontRendererContainer {

	private static final String SRG_renderStringAtPos = "func_78255_a";
	private static Method renderStringAtPos;
	private static final String SRG_bidiReorder = "func_147647_b";
	private static Method bidiReorder;
	private static final String SRG_resetStyles = "func_78265_b";
	private static Method resetStyles;
	private static final String SRG_trimStringNewline = "func_78273_d";
	private static Method trimStringNewline;
	private static final String SRG_posX = "field_78295_j";
	private static final String SRG_posY = "field_78296_k";
	private static final String SRG_red = "field_78291_n";
	private static final String SRG_green = "field_78306_p";
	private static final String SRG_blue = "field_78292_o";
	private static final String SRG_alpha = "field_78305_q";
	private static final String SRG_textColor = "field_78304_r";
	private static final String SRG_randomStyle = "field_78303_s";
	private static final String SRG_boldStyle = "field_78302_t";
	private static final String SRG_strikethroughStyle = "field_78299_w";
	private static final String SRG_underlineStyle = "field_78300_v";
	private static final String SRG_italicStyle = "field_78301_u";
	private static final String SRG_colorCode = "field_78285_g";

	public final int FONT_HEIGHT;

	public FontRenderer font;

	public FontRendererContainer(FontRenderer font) {
		this.font = font;
		this.FONT_HEIGHT = font.FONT_HEIGHT;
	}

	/**
	 * Draws the specified string.
	 */
	public int drawString(String text, float x, float y, int color, boolean dropShadow) {
		try {
			this.enableAlpha();
			GlStateManager.enableBlend();
			this.resetStyles();
			int i;
			if (dropShadow) {
				i = this.renderString(text, x + 1.0F, y + 1.0F, color, true);
				i = Math.max(i, this.renderString(text, x, y, color, false));
			} else {
				i = this.renderString(text, x, y, color, false);
			}
			return i;
		} catch (Exception e) {
			if(SmyLibGui.debug) {
				SmyLibGui.logger.error("Failed to use custom drawString in custom font renderer!");
				SmyLibGui.logger.catching(e);
			}
			return this.font.drawString(text, x, y, color, dropShadow);
		}
	}
	
	public void drawCenteredString(float x, float y, String str, int color, boolean shadow) {
		int w = this.getStringWidth(str);
		this.drawString(str, x - w/2, y, color, shadow);
	}

	/**
	 * Splits and draws a String with wordwrap
	 */
	public void drawSplitString(String str, int x, int y, int wrapWidth, int textColor) {
		try {
			this.resetStyles();
			this.setTextColor(textColor);
			String s = this.trimStringNewline(str);
			this.renderSplitString(s, x, y, wrapWidth, false);
		} catch (Exception e) {
			if(SmyLibGui.debug) {
				SmyLibGui.logger.error("Failed to use custom drawSplitString in custom font renderer!");
				SmyLibGui.logger.catching(e);
			}
			this.font.drawSplitString(str, x, y, wrapWidth, textColor);
		}
	}

	protected int renderString(String text, float x, float y, int color, boolean dropShadow) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (text == null) {
			return 0;
		} else {
			String t = text;
			if (this.font.getBidiFlag()) {
				t = this.bidiReorder(text);
			}
			
			int shadowedColor = color;
			
            if (dropShadow) {
            	shadowedColor = (color & 16579836) >> 2 | color & -16777216;
            }

			float red = (float)(shadowedColor >> 16 & 255) / 255.0F;
			float green = (float)(shadowedColor >> 8 & 255) / 255.0F;
			float blue = (float)(shadowedColor & 255) / 255.0F;
			float alpha = (float)(shadowedColor >> 24 & 255) / 255.0F;
			this.setRed(red);
			this.setGreen(green);
			this.setBlue(blue);
			this.setAlpha(alpha);
			this.setColor(red, green, blue, alpha);
			this.setPosX(x);
			this.setPosY(y);
			this.renderStringAtPos(t, dropShadow);
			return (int)this.getPosX();
		}
	}

    /**
     * Perform actual work of rendering a multi-line string with wordwrap and with darker drop shadow color if flag is
     * set
     * @throws InvocationTargetException 
     * @throws IllegalArgumentException 
     * @throws IllegalAccessException 
     */
    protected void renderSplitString(String str, int x, int y, int wrapWidth, boolean addShadow) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    	int t = y;
        for (String s : this.listFormattedStringToWidth(str, wrapWidth)) {
            this.renderStringAligned(s, x, t, wrapWidth, this.getTextColor(), addShadow);
            t += this.FONT_HEIGHT;
        }
    }
    
    /**
     * Render string either left or right aligned depending on bidiFlag
     * @throws InvocationTargetException 
     * @throws IllegalArgumentException 
     * @throws IllegalAccessException 
     */
    protected int renderStringAligned(String text, int x, int y, int width, int color, boolean dropShadow) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    	int t = x;
        if (this.font.getBidiFlag()) {
            int i = this.getStringWidth(this.bidiReorder(text));
            t = t + width - i;
        }

        return this.renderString(text, (float)t, (float)y, color, dropShadow);
    }
    
	protected void renderStringAtPos(String text, boolean shadow) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if(renderStringAtPos == null) 
			renderStringAtPos = ObfuscationReflectionHelper.findMethod(FontRenderer.class, SRG_renderStringAtPos, Void.TYPE, String.class, Boolean.TYPE);
		renderStringAtPos.invoke(this.font, text, shadow);
	}
    
	protected String trimStringNewline(String t) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if(trimStringNewline == null)
			trimStringNewline = ObfuscationReflectionHelper.findMethod(FontRenderer.class, SRG_trimStringNewline, String.class, String.class);
		return (String) trimStringNewline.invoke(this.font, t);
	}
	
	protected void resetStyles() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if(resetStyles == null)
			resetStyles = ObfuscationReflectionHelper.findMethod(FontRenderer.class, SRG_resetStyles, Void.TYPE);
		resetStyles.invoke(this.font);
	}

	protected String bidiReorder(String text) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if(bidiReorder == null)
			bidiReorder = ObfuscationReflectionHelper.findMethod(FontRenderer.class, SRG_bidiReorder, String.class, String.class);
		return (String) bidiReorder.invoke(this.font, text);
	}

	protected int getTextColor() throws IllegalArgumentException, IllegalAccessException {
		return ObfuscationReflectionHelper.getPrivateValue(FontRenderer.class, this.font, SRG_textColor);
	}

	protected void setTextColor(int color) throws IllegalArgumentException, IllegalAccessException {
		ObfuscationReflectionHelper.setPrivateValue(FontRenderer.class, this.font, color, SRG_textColor);
	}
	
	protected float getRed() throws IllegalArgumentException, IllegalAccessException {
		return ObfuscationReflectionHelper.getPrivateValue(FontRenderer.class, this.font, SRG_red);
	}

	protected void setRed(float red) throws IllegalArgumentException, IllegalAccessException {
		ObfuscationReflectionHelper.setPrivateValue(FontRenderer.class, this.font, red, SRG_red);
	}

	protected float getGreen() throws IllegalArgumentException, IllegalAccessException {
		return ObfuscationReflectionHelper.getPrivateValue(FontRenderer.class, this.font, SRG_green);
	}

	protected void setGreen(float green) throws IllegalArgumentException, IllegalAccessException {
		ObfuscationReflectionHelper.setPrivateValue(FontRenderer.class, this.font, green, SRG_green);
	}

	protected float getBlue() throws IllegalArgumentException, IllegalAccessException {
		return ObfuscationReflectionHelper.getPrivateValue(FontRenderer.class, this.font, SRG_blue);
	}

	protected void setBlue(float blue) throws IllegalArgumentException, IllegalAccessException {
		ObfuscationReflectionHelper.setPrivateValue(FontRenderer.class, this.font, blue, SRG_blue);
	}
	
	protected float getAlpha() throws IllegalArgumentException, IllegalAccessException {
		return ObfuscationReflectionHelper.getPrivateValue(FontRenderer.class, this.font, SRG_alpha);
	}

	protected void setAlpha(float alpha) throws IllegalArgumentException, IllegalAccessException {
		ObfuscationReflectionHelper.setPrivateValue(FontRenderer.class, this.font, alpha, SRG_alpha);
	}
	
	protected float getPosX() throws IllegalArgumentException, IllegalAccessException {
		return ObfuscationReflectionHelper.getPrivateValue(FontRenderer.class, this.font, SRG_posX);
	}

	protected void setPosX(float posX) throws IllegalArgumentException, IllegalAccessException {
		ObfuscationReflectionHelper.setPrivateValue(FontRenderer.class, this.font, posX, SRG_posX);
	}

	protected float getPosY() throws IllegalArgumentException, IllegalAccessException {
		return ObfuscationReflectionHelper.getPrivateValue(FontRenderer.class, this.font, SRG_posY);
	}

	protected void setPosY(float posY) throws IllegalArgumentException, IllegalAccessException {
		ObfuscationReflectionHelper.setPrivateValue(FontRenderer.class, this.font, posY, SRG_posY);
	}
	
	protected boolean getBoldStyle() throws IllegalArgumentException, IllegalAccessException {
		return ObfuscationReflectionHelper.getPrivateValue(FontRenderer.class, this.font, SRG_boldStyle);
	}

	protected void setBoldStyle(boolean yesNo) throws IllegalArgumentException, IllegalAccessException {
		ObfuscationReflectionHelper.setPrivateValue(FontRenderer.class, this.font, yesNo, SRG_boldStyle);
	}
	
	protected boolean getUnderlineStyle() throws IllegalArgumentException, IllegalAccessException {
		return ObfuscationReflectionHelper.getPrivateValue(FontRenderer.class, this.font, SRG_underlineStyle);
	}

	protected void setUnderlineStyle(boolean yesNo) throws IllegalArgumentException, IllegalAccessException {
		ObfuscationReflectionHelper.setPrivateValue(FontRenderer.class, this.font, yesNo, SRG_underlineStyle);
	}
	
	protected boolean getStrikethroughStyle() throws IllegalArgumentException, IllegalAccessException {
		return ObfuscationReflectionHelper.getPrivateValue(FontRenderer.class, this.font, SRG_strikethroughStyle);
	}

	protected void setStrikethroughStyle(boolean yesNo) throws IllegalArgumentException, IllegalAccessException {
		ObfuscationReflectionHelper.setPrivateValue(FontRenderer.class, this.font, yesNo, SRG_strikethroughStyle);
	}
	
	protected boolean getItalicStyle() throws IllegalArgumentException, IllegalAccessException {
		return ObfuscationReflectionHelper.getPrivateValue(FontRenderer.class, this.font, SRG_italicStyle);
	}

	protected void setItalicStyle(boolean yesNo) throws IllegalArgumentException, IllegalAccessException {
		ObfuscationReflectionHelper.setPrivateValue(FontRenderer.class, this.font, yesNo, SRG_italicStyle);
	}
	
	protected boolean getRandomStyle() throws IllegalArgumentException, IllegalAccessException {
		return ObfuscationReflectionHelper.getPrivateValue(FontRenderer.class, this.font, SRG_randomStyle);
	}

	protected void setRandomStyle(boolean yesNo) throws IllegalArgumentException, IllegalAccessException {
		ObfuscationReflectionHelper.setPrivateValue(FontRenderer.class, this.font, yesNo, SRG_randomStyle);
	}
	
	protected int[] getColorCode() throws IllegalArgumentException, IllegalAccessException {
		return ObfuscationReflectionHelper.getPrivateValue(FontRenderer.class, this.font, SRG_colorCode);
	}

	protected void setColorCode(int[] yesNo) throws IllegalArgumentException, IllegalAccessException {
		ObfuscationReflectionHelper.setPrivateValue(FontRenderer.class, this.font, yesNo, SRG_colorCode);
	}

	protected void setColor(float r, float g, float b, float a) {
		GlStateManager.color(r, g, b, a);
	}
	
    protected void enableAlpha() {
        GlStateManager.enableAlpha();
    }

	/**
	 * Draws the specified string with a shadow.
	 */
	public int drawStringWithShadow(String text, float x, float y, int color) {
		return this.drawString(text, x, y, color, true);
	}

	/**
	 * Draws the specified string.
	 */
	public int drawString(String text, int x, int y, int color) {
		return this.drawString(text, (float)x, (float)y, color, false);
	}

	/**
	 * Returns the width of this string. Equivalent of FontMetrics.stringWidth(String s).
	 */
	public int getStringWidth(String text) {
		return this.font.getStringWidth(text);
	}

	/**
	 * Returns the width of this character as rendered.
	 */
	public int getCharWidth(char character) {
		return this.font.getCharWidth(character);
	}

	/**
	 * Trims a string to fit a specified Width.
	 */
	public String trimStringToWidth(String text, int width) {
		return this.font.trimStringToWidth(text, width);
	}

	/**
	 * Trims a string to a specified width, optionally starting from the end and working backwards.
	 * <h3>Samples:</h3>
	 * (Assuming that {@link #getCharWidth(char)} returns <code>6</code> for all of the characters in
	 * <code>0123456789</code> on the current resource pack)
	 * <table>
	 * <tr><th>Input</th><th>Returns</th></tr>
	 * <tr><td><code>trimStringToWidth("0123456789", 1, false)</code></td><td><samp>""</samp></td></tr>
	 * <tr><td><code>trimStringToWidth("0123456789", 6, false)</code></td><td><samp>"0"</samp></td></tr>
	 * <tr><td><code>trimStringToWidth("0123456789", 29, false)</code></td><td><samp>"0123"</samp></td></tr>
	 * <tr><td><code>trimStringToWidth("0123456789", 30, false)</code></td><td><samp>"01234"</samp></td></tr>
	 * <tr><td><code>trimStringToWidth("0123456789", 9001, false)</code></td><td><samp>"0123456789"</samp></td></tr>
	 * <tr><td><code>trimStringToWidth("0123456789", 1, true)</code></td><td><samp>""</samp></td></tr>
	 * <tr><td><code>trimStringToWidth("0123456789", 6, true)</code></td><td><samp>"9"</samp></td></tr>
	 * <tr><td><code>trimStringToWidth("0123456789", 29, true)</code></td><td><samp>"6789"</samp></td></tr>
	 * <tr><td><code>trimStringToWidth("0123456789", 30, true)</code></td><td><samp>"56789"</samp></td></tr>
	 * <tr><td><code>trimStringToWidth("0123456789", 9001, true)</code></td><td><samp>"0123456789"</samp></td></tr>
	 * </table>
	 */
	public String trimStringToWidth(String text, int width, boolean reverse) {
		return this.font.trimStringToWidth(text, width);
	}

	/**
	 * Returns the height (in pixels) of the given string if it is wordwrapped to the given max width.
	 */
	public int getWordWrappedHeight(String str, int maxLength) {
		return this.font.getWordWrappedHeight(str, maxLength);
	}

	/**
	 * Set unicodeFlag controlling whether strings should be rendered with Unicode fonts instead of the default.png
	 * font.
	 */
	public void setUnicodeFlag(boolean unicodeFlagIn) {
		this.font.setUnicodeFlag(unicodeFlagIn);
	}

	/**
	 * Get unicodeFlag controlling whether strings should be rendered with Unicode fonts instead of the default.png
	 * font.
	 */
	public boolean getUnicodeFlag() {
		return this.font.getUnicodeFlag();
	}

	/**
	 * Set bidiFlag to control if the Unicode Bidirectional Algorithm should be run before rendering any string.
	 */
	public void setBidiFlag(boolean bidiFlagIn) {
		this.font.setBidiFlag(bidiFlagIn);
	}

	/**
	 * Breaks a string into a list of pieces where the width of each line is always less than or equal to the provided
	 * width. Formatting codes will be preserved between lines.
	 */
	public List<String> listFormattedStringToWidth(String str, int wrapWidth) {
		return this.font.listFormattedStringToWidth(str, wrapWidth);
	}

}

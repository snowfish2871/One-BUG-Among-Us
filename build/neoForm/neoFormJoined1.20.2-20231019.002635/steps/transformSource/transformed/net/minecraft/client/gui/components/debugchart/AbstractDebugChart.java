package net.minecraft.client.gui.components.debugchart;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.SampleLogger;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractDebugChart {
   protected static final int COLOR_GREY = 14737632;
   protected static final int CHART_HEIGHT = 60;
   protected static final int LINE_WIDTH = 1;
   protected final Font font;
   protected final SampleLogger logger;

   protected AbstractDebugChart(Font p_299029_, SampleLogger p_298721_) {
      this.font = p_299029_;
      this.logger = p_298721_;
   }

   public int getWidth(int p_298843_) {
      return Math.min(this.logger.capacity() + 2, p_298843_);
   }

   public void drawChart(GuiGraphics p_298960_, int p_298986_, int p_298301_) {
      int i = p_298960_.guiHeight();
      p_298960_.fill(RenderType.guiOverlay(), p_298986_, i - 60, p_298986_ + p_298301_, i, -1873784752);
      long j = 0L;
      long k = 2147483647L;
      long l = -2147483648L;
      int i1 = Math.max(0, this.logger.capacity() - (p_298301_ - 2));
      int j1 = this.logger.size() - i1;

      for(int k1 = 0; k1 < j1; ++k1) {
         int l1 = p_298986_ + k1 + 1;
         long i2 = this.logger.get(i1 + k1);
         k = Math.min(k, i2);
         l = Math.max(l, i2);
         j += i2;
         int j2 = this.getSampleHeight((double)i2);
         int k2 = this.getSampleColor(i2);
         p_298960_.fill(RenderType.guiOverlay(), l1, i - j2, l1 + 1, i, k2);
      }

      p_298960_.hLine(RenderType.guiOverlay(), p_298986_, p_298986_ + p_298301_ - 1, i - 60, -1);
      p_298960_.hLine(RenderType.guiOverlay(), p_298986_, p_298986_ + p_298301_ - 1, i - 1, -1);
      p_298960_.vLine(RenderType.guiOverlay(), p_298986_, i - 60, i, -1);
      p_298960_.vLine(RenderType.guiOverlay(), p_298986_ + p_298301_ - 1, i - 60, i, -1);
      if (j1 > 0) {
         String s = this.toDisplayString((double)k) + " min";
         String s1 = this.toDisplayString((double)j / (double)j1) + " avg";
         String s2 = this.toDisplayString((double)l) + " max";
         p_298960_.drawString(this.font, s, p_298986_ + 2, i - 60 - 9, 14737632);
         p_298960_.drawCenteredString(this.font, s1, p_298986_ + p_298301_ / 2, i - 60 - 9, 14737632);
         p_298960_.drawString(this.font, s2, p_298986_ + p_298301_ - this.font.width(s2) - 2, i - 60 - 9, 14737632);
      }

      this.renderAdditionalLinesAndLabels(p_298960_, p_298986_, p_298301_, i);
   }

   protected void renderAdditionalLinesAndLabels(GuiGraphics p_298895_, int p_298979_, int p_298732_, int p_299176_) {
   }

   protected void drawStringWithShade(GuiGraphics p_298386_, String p_298809_, int p_298657_, int p_298698_) {
      p_298386_.fill(RenderType.guiOverlay(), p_298657_, p_298698_, p_298657_ + this.font.width(p_298809_) + 1, p_298698_ + 9, -1873784752);
      p_298386_.drawString(this.font, p_298809_, p_298657_ + 1, p_298698_ + 1, 14737632, false);
   }

   protected abstract String toDisplayString(double p_298241_);

   protected abstract int getSampleHeight(double p_298971_);

   protected abstract int getSampleColor(long p_299300_);

   protected int getSampleColor(double p_298217_, double p_298257_, int p_298676_, double p_299233_, int p_298930_, double p_299140_, int p_298542_) {
      p_298217_ = Mth.clamp(p_298217_, p_298257_, p_299140_);
      return p_298217_ < p_299233_
         ? FastColor.ARGB32.lerp((float)(p_298217_ / (p_299233_ - p_298257_)), p_298676_, p_298930_)
         : FastColor.ARGB32.lerp((float)((p_298217_ - p_299233_) / (p_299140_ - p_299233_)), p_298930_, p_298542_);
   }
}

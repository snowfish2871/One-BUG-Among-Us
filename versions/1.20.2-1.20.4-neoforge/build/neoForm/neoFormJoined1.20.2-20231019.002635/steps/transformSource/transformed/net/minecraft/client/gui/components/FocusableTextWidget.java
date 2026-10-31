package net.minecraft.client.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FocusableTextWidget extends MultiLineTextWidget {
   private static final int BACKGROUND_COLOR = 1426063360;
   private static final int PADDING = 4;
   private final boolean alwaysShowBorder;

   public FocusableTextWidget(int p_295441_, Component p_296440_, Font p_296307_) {
      this(p_295441_, p_296440_, p_296307_, true);
   }

   public FocusableTextWidget(int p_295671_, Component p_295867_, Font p_294548_, boolean p_295676_) {
      super(p_295867_, p_294548_);
      this.setMaxWidth(p_295671_);
      this.setCentered(true);
      this.active = true;
      this.alwaysShowBorder = p_295676_;
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput p_295798_) {
      p_295798_.add(NarratedElementType.TITLE, this.getMessage());
   }

   @Override
   public void renderWidget(GuiGraphics p_296375_, int p_295686_, int p_295354_, float p_295563_) {
      if (this.isFocused() || this.alwaysShowBorder) {
         int i = this.getX() - 4;
         int j = this.getY() - 4;
         int k = this.getWidth() + 8;
         int l = this.getHeight() + 8;
         int i1 = this.alwaysShowBorder ? (this.isFocused() ? -1 : -6250336) : -1;
         p_296375_.fill(i + 1, j, i + k, j + l, 1426063360);
         p_296375_.renderOutline(i, j, k, l, i1);
      }

      super.renderWidget(p_296375_, p_295686_, p_295354_, p_295563_);
   }

   @Override
   public void playDownSound(SoundManager p_295576_) {
   }
}

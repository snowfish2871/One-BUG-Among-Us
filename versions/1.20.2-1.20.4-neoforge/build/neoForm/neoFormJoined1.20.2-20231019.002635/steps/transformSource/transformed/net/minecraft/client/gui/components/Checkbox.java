package net.minecraft.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Checkbox extends AbstractButton {
   private static final ResourceLocation CHECKBOX_SELECTED_HIGHLIGHTED_SPRITE = new ResourceLocation("widget/checkbox_selected_highlighted");
   private static final ResourceLocation CHECKBOX_SELECTED_SPRITE = new ResourceLocation("widget/checkbox_selected");
   private static final ResourceLocation CHECKBOX_HIGHLIGHTED_SPRITE = new ResourceLocation("widget/checkbox_highlighted");
   private static final ResourceLocation CHECKBOX_SPRITE = new ResourceLocation("widget/checkbox");
   private static final int TEXT_COLOR = 14737632;
   private boolean selected;
   private final boolean showLabel;

   public Checkbox(int p_93826_, int p_93827_, int p_93828_, int p_93829_, Component p_93830_, boolean p_93831_) {
      this(p_93826_, p_93827_, p_93828_, p_93829_, p_93830_, p_93831_, true);
   }

   public Checkbox(int p_93833_, int p_93834_, int p_93835_, int p_93836_, Component p_93837_, boolean p_93838_, boolean p_93839_) {
      super(p_93833_, p_93834_, p_93835_, p_93836_, p_93837_);
      this.selected = p_93838_;
      this.showLabel = p_93839_;
   }

   @Override
   public void onPress() {
      this.selected = !this.selected;
   }

   public boolean selected() {
      return this.selected;
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput p_260253_) {
      p_260253_.add(NarratedElementType.TITLE, this.createNarrationMessage());
      if (this.active) {
         if (this.isFocused()) {
            p_260253_.add(NarratedElementType.USAGE, Component.translatable("narration.checkbox.usage.focused"));
         } else {
            p_260253_.add(NarratedElementType.USAGE, Component.translatable("narration.checkbox.usage.hovered"));
         }
      }
   }

   @Override
   public void renderWidget(GuiGraphics p_283124_, int p_282925_, int p_282705_, float p_282612_) {
      Minecraft minecraft = Minecraft.getInstance();
      RenderSystem.enableDepthTest();
      Font font = minecraft.font;
      p_283124_.setColor(1.0F, 1.0F, 1.0F, this.alpha);
      RenderSystem.enableBlend();
      ResourceLocation resourcelocation;
      if (this.selected) {
         resourcelocation = this.isFocused() ? CHECKBOX_SELECTED_HIGHLIGHTED_SPRITE : CHECKBOX_SELECTED_SPRITE;
      } else {
         resourcelocation = this.isFocused() ? CHECKBOX_HIGHLIGHTED_SPRITE : CHECKBOX_SPRITE;
      }

      p_283124_.blitSprite(resourcelocation, this.getX(), this.getY(), 20, this.height);
      p_283124_.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      if (this.showLabel) {
         p_283124_.drawString(font, this.getMessage(), this.getX() + 24, this.getY() + (this.height - 8) / 2, 14737632 | Mth.ceil(this.alpha * 255.0F) << 24);
      }
   }
}

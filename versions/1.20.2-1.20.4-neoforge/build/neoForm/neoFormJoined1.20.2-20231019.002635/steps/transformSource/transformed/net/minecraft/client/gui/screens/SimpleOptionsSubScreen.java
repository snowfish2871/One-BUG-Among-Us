package net.minecraft.client.gui.screens;

import javax.annotation.Nullable;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class SimpleOptionsSubScreen extends OptionsSubScreen {
   protected final OptionInstance<?>[] smallOptions;
   @Nullable
   private AbstractWidget narratorButton;
   protected OptionsList list;

   public SimpleOptionsSubScreen(Screen p_232763_, Options p_232764_, Component p_232765_, OptionInstance<?>[] p_232766_) {
      super(p_232763_, p_232764_, p_232765_);
      this.smallOptions = p_232766_;
   }

   @Override
   protected void init() {
      this.list = new OptionsList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);
      this.list.addSmall(this.smallOptions);
      this.addWidget(this.list);
      this.createFooter();
      this.narratorButton = this.list.findOption(this.options.narrator());
      if (this.narratorButton != null) {
         this.narratorButton.active = this.minecraft.getNarrator().isActive();
      }
   }

   protected void createFooter() {
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, p_280827_ -> this.minecraft.setScreen(this.lastScreen))
            .bounds(this.width / 2 - 100, this.height - 27, 200, 20)
            .build()
      );
   }

   @Override
   public void render(GuiGraphics p_283632_, int p_283304_, int p_283302_, float p_282245_) {
      this.basicListRender(p_283632_, this.list, p_283304_, p_283302_, p_282245_);
   }

   @Override
   public void renderBackground(GuiGraphics p_295362_, int p_295382_, int p_294951_, float p_295275_) {
      this.renderDirtBackground(p_295362_);
   }

   public void updateNarratorButton() {
      if (this.narratorButton instanceof CycleButton) {
         ((CycleButton)this.narratorButton).setValue(this.options.narrator().get());
      }
   }
}

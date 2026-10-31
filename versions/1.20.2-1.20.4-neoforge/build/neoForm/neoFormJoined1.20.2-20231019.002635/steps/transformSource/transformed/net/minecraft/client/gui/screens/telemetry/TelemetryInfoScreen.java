package net.minecraft.client.gui.screens.telemetry;

import java.nio.file.Path;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TelemetryInfoScreen extends Screen {
   private static final int PADDING = 8;
   private static final Component TITLE = Component.translatable("telemetry_info.screen.title");
   private static final Component DESCRIPTION = Component.translatable("telemetry_info.screen.description").withStyle(ChatFormatting.GRAY);
   private static final Component BUTTON_PRIVACY_STATEMENT = Component.translatable("telemetry_info.button.privacy_statement");
   private static final Component BUTTON_GIVE_FEEDBACK = Component.translatable("telemetry_info.button.give_feedback");
   private static final Component BUTTON_SHOW_DATA = Component.translatable("telemetry_info.button.show_data");
   private final Screen lastScreen;
   private final Options options;
   private TelemetryEventWidget telemetryEventWidget;
   private double savedScroll;

   public TelemetryInfoScreen(Screen p_261720_, Options p_262019_) {
      super(TITLE);
      this.lastScreen = p_261720_;
      this.options = p_262019_;
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), DESCRIPTION);
   }

   @Override
   protected void init() {
      FrameLayout framelayout = new FrameLayout();
      framelayout.defaultChildLayoutSetting().padding(8);
      framelayout.setMinHeight(this.height);
      LinearLayout linearlayout = framelayout.addChild(LinearLayout.vertical(), framelayout.newChildLayoutSettings().align(0.5F, 0.0F));
      linearlayout.defaultCellSetting().alignHorizontallyCenter().paddingBottom(8);
      linearlayout.addChild(new StringWidget(this.getTitle(), this.font));
      linearlayout.addChild(new MultiLineTextWidget(DESCRIPTION, this.font).setMaxWidth(this.width - 16).setCentered(true));
      Button button = Button.builder(BUTTON_PRIVACY_STATEMENT, this::openPrivacyStatementLink).build();
      linearlayout.addChild(button);
      GridLayout gridlayout = this.twoButtonContainer(
         Button.builder(BUTTON_GIVE_FEEDBACK, this::openFeedbackLink).build(), Button.builder(BUTTON_SHOW_DATA, this::openDataFolder).build()
      );
      linearlayout.addChild(gridlayout);
      GridLayout gridlayout1 = this.twoButtonContainer(this.createTelemetryButton(), Button.builder(CommonComponents.GUI_DONE, this::openLastScreen).build());
      framelayout.addChild(gridlayout1, framelayout.newChildLayoutSettings().align(0.5F, 1.0F));
      framelayout.arrangeElements();
      this.telemetryEventWidget = new TelemetryEventWidget(
         0, 0, this.width - 40, gridlayout1.getY() - (gridlayout.getY() + gridlayout.getHeight()) - 16, this.minecraft.font
      );
      this.telemetryEventWidget.setScrollAmount(this.savedScroll);
      this.telemetryEventWidget.setOnScrolledListener(p_262168_ -> this.savedScroll = p_262168_);
      this.setInitialFocus(this.telemetryEventWidget);
      linearlayout.addChild(this.telemetryEventWidget);
      framelayout.arrangeElements();
      FrameLayout.alignInRectangle(framelayout, 0, 0, this.width, this.height, 0.5F, 0.0F);
      framelayout.visitWidgets(this::addRenderableWidget);
   }

   private AbstractWidget createTelemetryButton() {
      AbstractWidget abstractwidget = this.options
         .telemetryOptInExtra()
         .createButton(this.options, 0, 0, 150, p_261857_ -> this.telemetryEventWidget.onOptInChanged(p_261857_));
      abstractwidget.active = this.minecraft.extraTelemetryAvailable();
      return abstractwidget;
   }

   private void openLastScreen(Button p_261672_) {
      this.minecraft.setScreen(this.lastScreen);
   }

   private void openPrivacyStatementLink(Button p_295613_) {
      this.minecraft.setScreen(new ConfirmLinkScreen(p_293609_ -> {
         if (p_293609_) {
            Util.getPlatform().openUri("http://go.microsoft.com/fwlink/?LinkId=521839");
         }

         this.minecraft.setScreen(this);
      }, "http://go.microsoft.com/fwlink/?LinkId=521839", true));
   }

   private void openFeedbackLink(Button p_261531_) {
      this.minecraft.setScreen(new ConfirmLinkScreen(p_280897_ -> {
         if (p_280897_) {
            Util.getPlatform().openUri("https://aka.ms/javafeedback?ref=game");
         }

         this.minecraft.setScreen(this);
      }, "https://aka.ms/javafeedback?ref=game", true));
   }

   private void openDataFolder(Button p_261840_) {
      Path path = this.minecraft.getTelemetryManager().getLogDirectory();
      Util.getPlatform().openUri(path.toUri());
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   @Override
   public void renderBackground(GuiGraphics p_296285_, int p_294649_, int p_294609_, float p_294669_) {
      this.renderDirtBackground(p_296285_);
   }

   private GridLayout twoButtonContainer(AbstractWidget p_265763_, AbstractWidget p_265710_) {
      GridLayout gridlayout = new GridLayout();
      gridlayout.defaultCellSetting().alignHorizontallyCenter().paddingHorizontal(4);
      gridlayout.addChild(p_265763_, 0, 0);
      gridlayout.addChild(p_265710_, 0, 1);
      return gridlayout;
   }
}

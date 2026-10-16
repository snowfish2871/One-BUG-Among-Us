package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.exception.RealmsDefaultUncaughtExceptionHandler;
import com.mojang.realmsclient.util.task.LongRunningTask;
import java.time.Duration;
import javax.annotation.Nullable;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.realms.RepeatedNarrator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsLongRunningMcoTaskScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final RepeatedNarrator REPEATED_NARRATOR = new RepeatedNarrator(Duration.ofSeconds(5L));
   private LongRunningTask task;
   private final Screen lastScreen;
   private volatile Component title = CommonComponents.EMPTY;
   private final LinearLayout layout = LinearLayout.vertical();
   @Nullable
   private LoadingDotsWidget loadingDotsWidget;

   public RealmsLongRunningMcoTaskScreen(Screen p_88777_, LongRunningTask p_88778_) {
      super(GameNarrator.NO_TITLE);
      this.lastScreen = p_88777_;
      this.task = p_88778_;
      this.setTitle(p_88778_.getTitle());
      Thread thread = new Thread(p_88778_, "Realms-long-running-task");
      thread.setUncaughtExceptionHandler(new RealmsDefaultUncaughtExceptionHandler(LOGGER));
      thread.start();
   }

   @Override
   public void tick() {
      super.tick();
      REPEATED_NARRATOR.narrate(this.minecraft.getNarrator(), this.loadingDotsWidget.getMessage());
   }

   @Override
   public boolean keyPressed(int p_88781_, int p_88782_, int p_88783_) {
      if (p_88781_ == 256) {
         this.cancel();
         return true;
      } else {
         return super.keyPressed(p_88781_, p_88782_, p_88783_);
      }
   }

   @Override
   public void init() {
      this.layout.defaultCellSetting().alignHorizontallyCenter();
      this.loadingDotsWidget = new LoadingDotsWidget(this.font, this.title);
      this.layout.addChild(this.loadingDotsWidget, p_300631_ -> p_300631_.paddingBottom(30));
      this.layout.addChild(Button.builder(CommonComponents.GUI_CANCEL, p_300632_ -> this.cancel()).build());
      this.layout.visitWidgets(this::addRenderableWidget);
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      FrameLayout.centerInRectangle(this.layout, this.getRectangle());
   }

   protected void cancel() {
      this.task.abortTask();
      this.minecraft.setScreen(this.lastScreen);
   }

   public void setTitle(Component p_88797_) {
      if (this.loadingDotsWidget != null) {
         this.loadingDotsWidget.setMessage(p_88797_);
      }

      this.title = p_88797_;
   }
}

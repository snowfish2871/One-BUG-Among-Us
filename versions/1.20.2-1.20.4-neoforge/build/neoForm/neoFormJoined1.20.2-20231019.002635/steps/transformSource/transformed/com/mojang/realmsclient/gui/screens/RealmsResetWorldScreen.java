package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.dto.WorldTemplatePaginatedList;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.WorldGenerationInfo;
import com.mojang.realmsclient.util.task.LongRunningTask;
import com.mojang.realmsclient.util.task.ResettingGeneratedWorldTask;
import com.mojang.realmsclient.util.task.ResettingTemplateWorldTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsResetWorldScreen extends RealmsScreen {
   static final Logger LOGGER = LogUtils.getLogger();
   static final ResourceLocation SLOT_FRAME_SPRITE = new ResourceLocation("widget/slot_frame");
   private static final Component CREATE_REALM_TITLE = Component.translatable("mco.selectServer.create");
   private static final Component CREATE_REALM_SUBTITLE = Component.translatable("mco.selectServer.create.subtitle");
   private static final Component CREATE_WORLD_TITLE = Component.translatable("mco.configure.world.switch.slot");
   private static final Component CREATE_WORLD_SUBTITLE = Component.translatable("mco.configure.world.switch.slot.subtitle");
   private static final Component RESET_WORLD_TITLE = Component.translatable("mco.reset.world.title");
   private static final Component RESET_WORLD_SUBTITLE = Component.translatable("mco.reset.world.warning");
   public static final Component CREATE_WORLD_RESET_TASK_TITLE = Component.translatable("mco.create.world.reset.title");
   private static final Component RESET_WORLD_RESET_TASK_TITLE = Component.translatable("mco.reset.world.resetting.screen.title");
   private static final Component WORLD_TEMPLATES_TITLE = Component.translatable("mco.reset.world.template");
   private static final Component ADVENTURES_TITLE = Component.translatable("mco.reset.world.adventure");
   private static final Component EXPERIENCES_TITLE = Component.translatable("mco.reset.world.experience");
   private static final Component INSPIRATION_TITLE = Component.translatable("mco.reset.world.inspiration");
   private final Screen lastScreen;
   private final RealmsServer serverData;
   private final Component subtitle;
   private final int subtitleColor;
   private final Component resetTaskTitle;
   private static final ResourceLocation UPLOAD_LOCATION = new ResourceLocation("textures/gui/realms/upload.png");
   private static final ResourceLocation ADVENTURE_MAP_LOCATION = new ResourceLocation("textures/gui/realms/adventure.png");
   private static final ResourceLocation SURVIVAL_SPAWN_LOCATION = new ResourceLocation("textures/gui/realms/survival_spawn.png");
   private static final ResourceLocation NEW_WORLD_LOCATION = new ResourceLocation("textures/gui/realms/new_world.png");
   private static final ResourceLocation EXPERIENCE_LOCATION = new ResourceLocation("textures/gui/realms/experience.png");
   private static final ResourceLocation INSPIRATION_LOCATION = new ResourceLocation("textures/gui/realms/inspiration.png");
   WorldTemplatePaginatedList templates;
   WorldTemplatePaginatedList adventuremaps;
   WorldTemplatePaginatedList experiences;
   WorldTemplatePaginatedList inspirations;
   public final int slot;
   private final Runnable resetWorldRunnable;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

   private RealmsResetWorldScreen(
      Screen p_89329_, RealmsServer p_89330_, int p_300857_, Component p_301121_, Component p_300859_, int p_300978_, Component p_300942_, Runnable p_89331_
   ) {
      super(p_301121_);
      this.lastScreen = p_89329_;
      this.serverData = p_89330_;
      this.slot = p_300857_;
      this.subtitle = p_300859_;
      this.subtitleColor = p_300978_;
      this.resetTaskTitle = p_300942_;
      this.resetWorldRunnable = p_89331_;
   }

   public static RealmsResetWorldScreen forNewRealm(Screen p_300881_, RealmsServer p_301228_, Runnable p_300926_) {
      return new RealmsResetWorldScreen(
         p_300881_, p_301228_, p_301228_.activeSlot, CREATE_REALM_TITLE, CREATE_REALM_SUBTITLE, -6250336, CREATE_WORLD_RESET_TASK_TITLE, p_300926_
      );
   }

   public static RealmsResetWorldScreen forEmptySlot(Screen p_300943_, int p_301275_, RealmsServer p_301312_, Runnable p_301297_) {
      return new RealmsResetWorldScreen(
         p_300943_, p_301312_, p_301275_, CREATE_WORLD_TITLE, CREATE_WORLD_SUBTITLE, -6250336, CREATE_WORLD_RESET_TASK_TITLE, p_301297_
      );
   }

   public static RealmsResetWorldScreen forResetSlot(Screen p_301039_, RealmsServer p_300914_, Runnable p_301017_) {
      return new RealmsResetWorldScreen(
         p_301039_, p_300914_, p_300914_.activeSlot, RESET_WORLD_TITLE, RESET_WORLD_SUBTITLE, -65536, RESET_WORLD_RESET_TASK_TITLE, p_301017_
      );
   }

   @Override
   public void init() {
      LinearLayout linearlayout = LinearLayout.vertical();
      linearlayout.addChild(new StringWidget(this.title, this.font), LayoutSettings::alignHorizontallyCenter);
      linearlayout.addChild(SpacerElement.height(3));
      linearlayout.addChild(new StringWidget(this.subtitle, this.font).setColor(this.subtitleColor), LayoutSettings::alignHorizontallyCenter);
      this.layout.addToHeader(linearlayout);
      (new Thread("Realms-reset-world-fetcher") {
         @Override
         public void run() {
            RealmsClient realmsclient = RealmsClient.create();

            try {
               WorldTemplatePaginatedList worldtemplatepaginatedlist = realmsclient.fetchWorldTemplates(1, 10, RealmsServer.WorldType.NORMAL);
               WorldTemplatePaginatedList worldtemplatepaginatedlist1 = realmsclient.fetchWorldTemplates(1, 10, RealmsServer.WorldType.ADVENTUREMAP);
               WorldTemplatePaginatedList worldtemplatepaginatedlist2 = realmsclient.fetchWorldTemplates(1, 10, RealmsServer.WorldType.EXPERIENCE);
               WorldTemplatePaginatedList worldtemplatepaginatedlist3 = realmsclient.fetchWorldTemplates(1, 10, RealmsServer.WorldType.INSPIRATION);
               RealmsResetWorldScreen.this.minecraft.execute(() -> {
                  RealmsResetWorldScreen.this.templates = worldtemplatepaginatedlist;
                  RealmsResetWorldScreen.this.adventuremaps = worldtemplatepaginatedlist1;
                  RealmsResetWorldScreen.this.experiences = worldtemplatepaginatedlist2;
                  RealmsResetWorldScreen.this.inspirations = worldtemplatepaginatedlist3;
               });
            } catch (RealmsServiceException realmsserviceexception) {
               RealmsResetWorldScreen.LOGGER.error("Couldn't fetch templates in reset world", (Throwable)realmsserviceexception);
            }
         }
      }).start();
      this.addRenderableWidget(
         new RealmsResetWorldScreen.FrameButton(
            this.frame(1),
            row(0) + 10,
            RealmsResetNormalWorldScreen.TITLE,
            NEW_WORLD_LOCATION,
            p_280746_ -> this.minecraft.setScreen(new RealmsResetNormalWorldScreen(this::generationSelectionCallback, this.title))
         )
      );
      this.addRenderableWidget(
         new RealmsResetWorldScreen.FrameButton(
            this.frame(2),
            row(0) + 10,
            RealmsSelectFileToUploadScreen.TITLE,
            UPLOAD_LOCATION,
            p_300636_ -> this.minecraft.setScreen(new RealmsSelectFileToUploadScreen(this.serverData.id, this.slot, this))
         )
      );
      this.addRenderableWidget(
         new RealmsResetWorldScreen.FrameButton(
            this.frame(3),
            row(0) + 10,
            WORLD_TEMPLATES_TITLE,
            SURVIVAL_SPAWN_LOCATION,
            p_300639_ -> this.minecraft
                  .setScreen(
                     new RealmsSelectWorldTemplateScreen(WORLD_TEMPLATES_TITLE, this::templateSelectionCallback, RealmsServer.WorldType.NORMAL, this.templates)
                  )
         )
      );
      this.addRenderableWidget(
         new RealmsResetWorldScreen.FrameButton(
            this.frame(1),
            row(6) + 20,
            ADVENTURES_TITLE,
            ADVENTURE_MAP_LOCATION,
            p_300637_ -> this.minecraft
                  .setScreen(
                     new RealmsSelectWorldTemplateScreen(
                        ADVENTURES_TITLE, this::templateSelectionCallback, RealmsServer.WorldType.ADVENTUREMAP, this.adventuremaps
                     )
                  )
         )
      );
      this.addRenderableWidget(
         new RealmsResetWorldScreen.FrameButton(
            this.frame(2),
            row(6) + 20,
            EXPERIENCES_TITLE,
            EXPERIENCE_LOCATION,
            p_300638_ -> this.minecraft
                  .setScreen(
                     new RealmsSelectWorldTemplateScreen(
                        EXPERIENCES_TITLE, this::templateSelectionCallback, RealmsServer.WorldType.EXPERIENCE, this.experiences
                     )
                  )
         )
      );
      this.addRenderableWidget(
         new RealmsResetWorldScreen.FrameButton(
            this.frame(3),
            row(6) + 20,
            INSPIRATION_TITLE,
            INSPIRATION_LOCATION,
            p_300640_ -> this.minecraft
                  .setScreen(
                     new RealmsSelectWorldTemplateScreen(
                        INSPIRATION_TITLE, this::templateSelectionCallback, RealmsServer.WorldType.INSPIRATION, this.inspirations
                     )
                  )
         )
      );
      this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, p_300644_ -> this.onClose()).build());
      this.layout.visitWidgets(this::addRenderableWidget);
      this.layout.arrangeElements();
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(this.getTitle(), this.subtitle);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   private int frame(int p_89393_) {
      return this.width / 2 - 130 + (p_89393_ - 1) * 100;
   }

   private void startTask(LongRunningTask p_167458_) {
      this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, p_167458_));
   }

   public void switchSlot(Runnable p_89383_) {
      this.startTask(new SwitchSlotTask(this.serverData.id, this.slot, () -> this.minecraft.execute(p_89383_)));
   }

   private void templateSelectionCallback(@Nullable WorldTemplate p_167454_) {
      this.minecraft.setScreen(this);
      if (p_167454_ != null) {
         this.resetWorld(() -> this.startTask(new ResettingTemplateWorldTask(p_167454_, this.serverData.id, this.resetTaskTitle, this.resetWorldRunnable)));
      }
   }

   private void generationSelectionCallback(@Nullable WorldGenerationInfo p_167456_) {
      this.minecraft.setScreen(this);
      if (p_167456_ != null) {
         this.resetWorld(() -> this.startTask(new ResettingGeneratedWorldTask(p_167456_, this.serverData.id, this.resetTaskTitle, this.resetWorldRunnable)));
      }
   }

   private void resetWorld(Runnable p_167465_) {
      if (this.slot == -1) {
         p_167465_.run();
      } else {
         this.switchSlot(p_167465_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class FrameButton extends Button {
      private static final int WIDTH = 60;
      private static final int HEIGHT = 72;
      private static final int IMAGE_SIZE = 56;
      private final ResourceLocation image;

      FrameButton(int p_89439_, int p_89440_, Component p_89441_, ResourceLocation p_89442_, Button.OnPress p_89443_) {
         super(p_89439_, p_89440_, 60, 72, p_89441_, p_89443_, DEFAULT_NARRATION);
         this.image = p_89442_;
      }

      @Override
      public void renderWidget(GuiGraphics p_282595_, int p_282741_, int p_283560_, float p_281923_) {
         boolean flag = this.isHoveredOrFocused();
         if (flag) {
            p_282595_.setColor(0.56F, 0.56F, 0.56F, 1.0F);
         }

         int i = this.getX();
         int j = this.getY();
         p_282595_.blit(this.image, i + 2, j + 14, 0.0F, 0.0F, 56, 56, 56, 56);
         p_282595_.blitSprite(RealmsResetWorldScreen.SLOT_FRAME_SPRITE, i, j + 12, 60, 60);
         p_282595_.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         int k = flag ? -6250336 : -1;
         p_282595_.drawCenteredString(RealmsResetWorldScreen.this.font, this.getMessage(), i + 30, j, k);
      }
   }
}

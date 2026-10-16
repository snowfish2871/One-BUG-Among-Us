package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.util.task.WorldCreationTask;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsCreateRealmScreen extends RealmsScreen {
   private static final Component NAME_LABEL = Component.translatable("mco.configure.world.name");
   private static final Component DESCRIPTION_LABEL = Component.translatable("mco.configure.world.description");
   private static final int BUTTON_SPACING = 10;
   private static final int CONTENT_WIDTH = 210;
   private final RealmsServer server;
   private final RealmsMainScreen lastScreen;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private EditBox nameBox;
   private EditBox descriptionBox;

   public RealmsCreateRealmScreen(RealmsServer p_88574_, RealmsMainScreen p_88575_) {
      super(Component.translatable("mco.selectServer.create"));
      this.server = p_88574_;
      this.lastScreen = p_88575_;
   }

   @Override
   public void init() {
      this.layout.addToHeader(new StringWidget(this.title, this.font));
      LinearLayout linearlayout = this.layout.addToContents(LinearLayout.vertical()).spacing(10);
      Button button = Button.builder(Component.translatable("mco.create.world"), p_88592_ -> this.createWorld()).build();
      button.active = false;
      this.nameBox = new EditBox(this.font, 210, 20, Component.translatable("mco.configure.world.name"));
      this.nameBox.setResponder(p_293568_ -> button.active = !Util.isBlank(p_293568_));
      this.descriptionBox = new EditBox(this.font, 210, 20, Component.translatable("mco.configure.world.description"));
      linearlayout.addChild(CommonLayouts.labeledElement(this.font, this.nameBox, NAME_LABEL));
      linearlayout.addChild(CommonLayouts.labeledElement(this.font, this.descriptionBox, DESCRIPTION_LABEL));
      LinearLayout linearlayout1 = this.layout.addToFooter(LinearLayout.horizontal().spacing(10));
      linearlayout1.addChild(button);
      linearlayout1.addChild(Button.builder(CommonComponents.GUI_CANCEL, p_293570_ -> this.onClose()).build());
      this.layout.visitWidgets(this::addRenderableWidget);
      this.repositionElements();
      this.setInitialFocus(this.nameBox);
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
   }

   private void createWorld() {
      RealmsResetWorldScreen realmsresetworldscreen = RealmsResetWorldScreen.forNewRealm(this.lastScreen, this.server, () -> this.minecraft.execute(() -> {
            this.lastScreen.refreshServerList();
            this.minecraft.setScreen(this.lastScreen);
         }));
      this.minecraft
         .setScreen(
            new RealmsLongRunningMcoTaskScreen(
               this.lastScreen, new WorldCreationTask(this.server.id, this.nameBox.getValue(), this.descriptionBox.getValue(), realmsresetworldscreen)
            )
         );
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }
}

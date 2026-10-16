package com.mojang.realmsclient;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.mojang.realmsclient.client.Ping;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.PingResult;
import com.mojang.realmsclient.dto.RealmsNews;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RegionPingResult;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.RealmsServerList;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsCreateRealmScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongConfirmationScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPendingInvitesScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopupScreen;
import com.mojang.realmsclient.gui.task.DataFetcher;
import com.mojang.realmsclient.util.RealmsPersistence;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsMainScreen extends RealmsScreen {
   static final ResourceLocation INFO_SPRITE = new ResourceLocation("icon/info");
   static final ResourceLocation NEW_REALM_SPRITE = new ResourceLocation("icon/new_realm");
   static final ResourceLocation EXPIRED_SPRITE = new ResourceLocation("realm_status/expired");
   static final ResourceLocation EXPIRES_SOON_SPRITE = new ResourceLocation("realm_status/expires_soon");
   static final ResourceLocation OPEN_SPRITE = new ResourceLocation("realm_status/open");
   static final ResourceLocation CLOSED_SPRITE = new ResourceLocation("realm_status/closed");
   private static final ResourceLocation INVITE_SPRITE = new ResourceLocation("icon/invite");
   private static final ResourceLocation NEWS_SPRITE = new ResourceLocation("icon/news");
   static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation LOGO_LOCATION = new ResourceLocation("textures/gui/title/realms.png");
   private static final ResourceLocation NO_REALMS_LOCATION = new ResourceLocation("textures/gui/realms/no_realms.png");
   private static final Component TITLE = Component.translatable("menu.online");
   private static final Component LOADING_TEXT = Component.translatable("mco.selectServer.loading");
   static final Component SERVER_UNITIALIZED_TEXT = Component.translatable("mco.selectServer.uninitialized").withStyle(ChatFormatting.GREEN);
   static final Component SUBSCRIPTION_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredList");
   private static final Component SUBSCRIPTION_RENEW_TEXT = Component.translatable("mco.selectServer.expiredRenew");
   static final Component TRIAL_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredTrial");
   static final Component SELECT_MINIGAME_PREFIX = Component.translatable("mco.selectServer.minigame").append(CommonComponents.SPACE);
   private static final Component PLAY_TEXT = Component.translatable("mco.selectServer.play");
   private static final Component LEAVE_SERVER_TEXT = Component.translatable("mco.selectServer.leave");
   private static final Component CONFIGURE_SERVER_TEXT = Component.translatable("mco.selectServer.configure");
   static final Component SERVER_EXPIRED_TOOLTIP = Component.translatable("mco.selectServer.expired");
   static final Component SERVER_EXPIRES_SOON_TOOLTIP = Component.translatable("mco.selectServer.expires.soon");
   static final Component SERVER_EXPIRES_IN_DAY_TOOLTIP = Component.translatable("mco.selectServer.expires.day");
   static final Component SERVER_OPEN_TOOLTIP = Component.translatable("mco.selectServer.open");
   static final Component SERVER_CLOSED_TOOLTIP = Component.translatable("mco.selectServer.closed");
   static final Component UNITIALIZED_WORLD_NARRATION = Component.translatable("gui.narrate.button", SERVER_UNITIALIZED_TEXT);
   private static final Component NO_REALMS_TEXT = Component.translatable("mco.selectServer.noRealms");
   private static final Tooltip NO_PENDING_INVITES = Tooltip.create(Component.translatable("mco.invites.nopending"));
   private static final Tooltip PENDING_INVITES = Tooltip.create(Component.translatable("mco.invites.pending"));
   private static final int BUTTON_WIDTH = 100;
   private static final int BUTTON_COLUMNS = 3;
   private static final int BUTTON_SPACING = 4;
   private static final int CONTENT_WIDTH = 308;
   private static final int LOGO_WIDTH = 128;
   private static final int LOGO_HEIGHT = 34;
   private static final int LOGO_TEXTURE_WIDTH = 128;
   private static final int LOGO_TEXTURE_HEIGHT = 64;
   private static final int LOGO_PADDING = 5;
   private static final int HEADER_HEIGHT = 44;
   private static final int FOOTER_PADDING = 10;
   private static final int ENTRY_WIDTH = 216;
   private static final int ITEM_HEIGHT = 36;
   private final CompletableFuture<RealmsAvailability.Result> availability = RealmsAvailability.get();
   @Nullable
   private DataFetcher.Subscription dataSubscription;
   private final Set<UUID> handledSeenNotifications = new HashSet<>();
   private static boolean regionsPinged;
   private final RateLimiter inviteNarrationLimiter;
   private final Screen lastScreen;
   private Button playButton;
   private Button backButton;
   private Button renewButton;
   private Button configureButton;
   private Button leaveButton;
   private RealmsMainScreen.RealmSelectionList realmSelectionList;
   private RealmsServerList serverList;
   private volatile boolean trialsAvailable;
   @Nullable
   private volatile String newsLink;
   long lastClickTime;
   private final List<RealmsNotification> notifications = new ArrayList<>();
   private Button addRealmButton;
   private RealmsMainScreen.NotificationButton pendingInvitesButton;
   private RealmsMainScreen.NotificationButton newsButton;
   private RealmsMainScreen.LayoutState activeLayoutState;
   @Nullable
   private HeaderAndFooterLayout layout;

   public RealmsMainScreen(Screen p_86315_) {
      super(TITLE);
      this.lastScreen = p_86315_;
      this.inviteNarrationLimiter = RateLimiter.create(0.016666668F);
   }

   @Override
   public void init() {
      this.serverList = new RealmsServerList(this.minecraft);
      this.realmSelectionList = this.addRenderableWidget(new RealmsMainScreen.RealmSelectionList());
      Component component = Component.translatable("mco.invites.title");
      this.pendingInvitesButton = new RealmsMainScreen.NotificationButton(
         component, INVITE_SPRITE, p_293547_ -> this.minecraft.setScreen(new RealmsPendingInvitesScreen(this, component))
      );
      Component component1 = Component.translatable("mco.news");
      this.newsButton = new RealmsMainScreen.NotificationButton(component1, NEWS_SPRITE, p_300623_ -> {
         if (this.newsLink != null) {
            ConfirmLinkScreen.confirmLinkNow(this.newsLink, this, true);
            if (this.newsButton.notificationCount() != 0) {
               RealmsPersistence.RealmsPersistenceData realmspersistence$realmspersistencedata = RealmsPersistence.readFile();
               realmspersistence$realmspersistencedata.hasUnreadNews = false;
               RealmsPersistence.writeFile(realmspersistence$realmspersistencedata);
               this.newsButton.setNotificationCount(0);
            }
         }
      });
      this.newsButton.setTooltip(Tooltip.create(component1));
      this.playButton = Button.builder(PLAY_TEXT, p_302303_ -> play(this.getSelectedServer(), this)).width(100).build();
      this.configureButton = Button.builder(CONFIGURE_SERVER_TEXT, p_86672_ -> this.configureClicked(this.getSelectedServer())).width(100).build();
      this.renewButton = Button.builder(SUBSCRIPTION_RENEW_TEXT, p_86622_ -> this.onRenew(this.getSelectedServer())).width(100).build();
      this.leaveButton = Button.builder(LEAVE_SERVER_TEXT, p_86679_ -> this.leaveClicked(this.getSelectedServer())).width(100).build();
      this.addRealmButton = Button.builder(Component.translatable("mco.selectServer.purchase"), p_300620_ -> this.openTrialAvailablePopup())
         .size(100, 20)
         .build();
      this.backButton = Button.builder(CommonComponents.GUI_BACK, p_293552_ -> this.minecraft.setScreen(this.lastScreen)).width(100).build();
      this.updateLayout(RealmsMainScreen.LayoutState.LOADING);
      this.updateButtonStates();
      this.availability.thenAcceptAsync(p_293549_ -> {
         Screen screen = p_293549_.createErrorScreen(this.lastScreen);
         if (screen == null) {
            this.dataSubscription = this.initDataFetcher(this.minecraft.realmsDataFetcher());
         } else {
            this.minecraft.setScreen(screen);
         }
      }, this.screenExecutor);
   }

   @Override
   protected void repositionElements() {
      if (this.layout != null) {
         this.realmSelectionList.updateSize(this.width, this.height, this.layout.getHeaderHeight(), this.height - this.layout.getFooterHeight());
         this.layout.arrangeElements();
      }
   }

   private void updateLayout(RealmsMainScreen.LayoutState p_294474_) {
      if (this.activeLayoutState != p_294474_) {
         if (this.layout != null) {
            this.layout.visitWidgets(p_293554_ -> this.removeWidget(p_293554_));
         }

         this.layout = this.createLayout(p_294474_);
         this.activeLayoutState = p_294474_;
         this.layout.visitWidgets(this::addRenderableWidget);
         this.repositionElements();
      }
   }

   private HeaderAndFooterLayout createLayout(RealmsMainScreen.LayoutState p_295052_) {
      HeaderAndFooterLayout headerandfooterlayout = new HeaderAndFooterLayout(this);
      headerandfooterlayout.setHeaderHeight(44);
      headerandfooterlayout.addToHeader(this.createHeader());
      Layout layout = this.createFooter(p_295052_);
      layout.arrangeElements();
      headerandfooterlayout.setFooterHeight(layout.getHeight() + 20);
      headerandfooterlayout.addToFooter(layout);
      switch(p_295052_) {
         case LOADING:
            headerandfooterlayout.addToContents(new LoadingDotsWidget(this.font, LOADING_TEXT));
            break;
         case NO_REALMS:
            headerandfooterlayout.addToContents(this.createNoRealmsContent());
      }

      return headerandfooterlayout;
   }

   private Layout createHeader() {
      int i = 90;
      LinearLayout linearlayout = LinearLayout.horizontal().spacing(4);
      linearlayout.defaultCellSetting().alignVerticallyMiddle();
      linearlayout.addChild(this.pendingInvitesButton);
      linearlayout.addChild(this.newsButton);
      LinearLayout linearlayout1 = LinearLayout.horizontal();
      linearlayout1.defaultCellSetting().alignVerticallyMiddle();
      linearlayout1.addChild(SpacerElement.width(90));
      linearlayout1.addChild(ImageWidget.texture(128, 34, LOGO_LOCATION, 128, 64), LayoutSettings::alignHorizontallyCenter);
      linearlayout1.addChild(new FrameLayout(90, 44)).addChild(linearlayout, LayoutSettings::alignHorizontallyRight);
      return linearlayout1;
   }

   private Layout createFooter(RealmsMainScreen.LayoutState p_294561_) {
      GridLayout gridlayout = new GridLayout().spacing(4);
      GridLayout.RowHelper gridlayout$rowhelper = gridlayout.createRowHelper(3);
      if (p_294561_ == RealmsMainScreen.LayoutState.LIST) {
         gridlayout$rowhelper.addChild(this.playButton);
         gridlayout$rowhelper.addChild(this.configureButton);
         gridlayout$rowhelper.addChild(this.renewButton);
         gridlayout$rowhelper.addChild(this.leaveButton);
      }

      gridlayout$rowhelper.addChild(this.addRealmButton);
      gridlayout$rowhelper.addChild(this.backButton);
      return gridlayout;
   }

   private LinearLayout createNoRealmsContent() {
      LinearLayout linearlayout = LinearLayout.vertical().spacing(10);
      linearlayout.defaultCellSetting().alignHorizontallyCenter();
      linearlayout.addChild(ImageWidget.texture(130, 64, NO_REALMS_LOCATION, 130, 64));
      FocusableTextWidget focusabletextwidget = new FocusableTextWidget(308, NO_REALMS_TEXT, this.font, false);
      linearlayout.addChild(focusabletextwidget);
      return linearlayout;
   }

   void updateButtonStates() {
      RealmsServer realmsserver = this.getSelectedServer();
      this.addRealmButton.active = this.activeLayoutState != RealmsMainScreen.LayoutState.LOADING;
      this.playButton.active = this.shouldPlayButtonBeActive(realmsserver);
      this.renewButton.active = this.shouldRenewButtonBeActive(realmsserver);
      this.leaveButton.active = this.shouldLeaveButtonBeActive(realmsserver);
      this.configureButton.active = this.shouldConfigureButtonBeActive(realmsserver);
   }

   boolean shouldPlayButtonBeActive(@Nullable RealmsServer p_86563_) {
      return p_86563_ != null && !p_86563_.expired && p_86563_.state == RealmsServer.State.OPEN;
   }

   private boolean shouldRenewButtonBeActive(@Nullable RealmsServer p_86595_) {
      return p_86595_ != null && p_86595_.expired && this.isSelfOwnedServer(p_86595_);
   }

   private boolean shouldConfigureButtonBeActive(@Nullable RealmsServer p_86620_) {
      return p_86620_ != null && this.isSelfOwnedServer(p_86620_);
   }

   private boolean shouldLeaveButtonBeActive(@Nullable RealmsServer p_86645_) {
      return p_86645_ != null && !this.isSelfOwnedServer(p_86645_);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.dataSubscription != null) {
         this.dataSubscription.tick();
      }
   }

   public static void refreshPendingInvites() {
      Minecraft.getInstance().realmsDataFetcher().pendingInvitesTask.reset();
   }

   public void refreshServerList() {
      Minecraft.getInstance().realmsDataFetcher().serverListUpdateTask.reset();
   }

   private DataFetcher.Subscription initDataFetcher(RealmsDataFetcher p_238836_) {
      DataFetcher.Subscription datafetcher$subscription = p_238836_.dataFetcher.createSubscription();
      datafetcher$subscription.subscribe(
         p_238836_.serverListUpdateTask,
         p_302304_ -> {
            this.serverList.updateServersList(p_302304_);
            this.updateLayout(
               this.serverList.isEmpty() && this.notifications.isEmpty() ? RealmsMainScreen.LayoutState.NO_REALMS : RealmsMainScreen.LayoutState.LIST
            );
            this.refreshRealmsSelectionList();
            boolean flag = false;
   
            for(RealmsServer realmsserver : this.serverList) {
               if (this.isSelfOwnedNonExpiredServer(realmsserver)) {
                  flag = true;
               }
            }
   
            if (!regionsPinged && flag) {
               regionsPinged = true;
               this.pingRegions();
            }
         }
      );
      callRealmsClient(RealmsClient::getNotifications, p_302305_ -> {
         this.notifications.clear();
         this.notifications.addAll(p_302305_);
         if (!this.notifications.isEmpty() && this.activeLayoutState != RealmsMainScreen.LayoutState.LOADING) {
            this.updateLayout(RealmsMainScreen.LayoutState.LIST);
            this.refreshRealmsSelectionList();
         }
      });
      datafetcher$subscription.subscribe(p_238836_.pendingInvitesTask, p_300619_ -> {
         this.pendingInvitesButton.setNotificationCount(p_300619_);
         this.pendingInvitesButton.setTooltip(p_300619_ == 0 ? NO_PENDING_INVITES : PENDING_INVITES);
         if (p_300619_ > 0 && this.inviteNarrationLimiter.tryAcquire(1)) {
            this.minecraft.getNarrator().sayNow(Component.translatable("mco.configure.world.invite.narration", p_300619_));
         }
      });
      datafetcher$subscription.subscribe(p_238836_.trialAvailabilityTask, p_293548_ -> this.trialsAvailable = p_293548_);
      datafetcher$subscription.subscribe(p_238836_.newsTask, p_300622_ -> {
         p_238836_.newsManager.updateUnreadNews(p_300622_);
         this.newsLink = p_238836_.newsManager.newsLink();
         this.newsButton.setNotificationCount(p_238836_.newsManager.hasUnreadNews() ? Integer.MAX_VALUE : 0);
      });
      return datafetcher$subscription;
   }

   private static <T> void callRealmsClient(RealmsMainScreen.RealmsCall<T> p_275561_, Consumer<T> p_275686_) {
      Minecraft minecraft = Minecraft.getInstance();
      CompletableFuture.<T>supplyAsync(() -> {
         try {
            return p_275561_.request(RealmsClient.create(minecraft));
         } catch (RealmsServiceException realmsserviceexception) {
            throw new RuntimeException(realmsserviceexception);
         }
      }).thenAcceptAsync(p_275686_, minecraft).exceptionally(p_274626_ -> {
         LOGGER.error("Failed to execute call to Realms Service", p_274626_);
         return null;
      });
   }

   private void refreshRealmsSelectionList() {
      RealmsServer realmsserver = this.getSelectedServer();
      this.realmSelectionList.clear();
      List<UUID> list = new ArrayList<>();

      for(RealmsNotification realmsnotification : this.notifications) {
         this.addEntriesForNotification(this.realmSelectionList, realmsnotification);
         if (!realmsnotification.seen() && !this.handledSeenNotifications.contains(realmsnotification.uuid())) {
            list.add(realmsnotification.uuid());
         }
      }

      if (!list.isEmpty()) {
         callRealmsClient(p_274625_ -> {
            p_274625_.notificationsSeen(list);
            return null;
         }, p_274630_ -> this.handledSeenNotifications.addAll(list));
      }

      for(RealmsServer realmsserver1 : this.serverList) {
         RealmsMainScreen.ServerEntry realmsmainscreen$serverentry = new RealmsMainScreen.ServerEntry(realmsserver1);
         this.realmSelectionList.addEntry(realmsmainscreen$serverentry);
         if (realmsserver != null && realmsserver.id == realmsserver1.id) {
            this.realmSelectionList.setSelected((RealmsMainScreen.Entry)realmsmainscreen$serverentry);
         }
      }

      this.updateButtonStates();
   }

   private void addEntriesForNotification(RealmsMainScreen.RealmSelectionList p_275392_, RealmsNotification p_275492_) {
      if (p_275492_ instanceof RealmsNotification.VisitUrl realmsnotification$visiturl) {
         Component component = realmsnotification$visiturl.getMessage();
         int i = this.font.wordWrapHeight(component, 216);
         int j = Mth.positiveCeilDiv(i + 7, 36) - 1;
         p_275392_.addEntry(new RealmsMainScreen.NotificationMessageEntry(component, j + 2, realmsnotification$visiturl));

         for(int k = 0; k < j; ++k) {
            p_275392_.addEntry(new RealmsMainScreen.EmptyEntry());
         }

         p_275392_.addEntry(new RealmsMainScreen.ButtonEntry(realmsnotification$visiturl.buildOpenLinkButton(this)));
      }
   }

   private void pingRegions() {
      new Thread(() -> {
         List<RegionPingResult> list = Ping.pingAllRegions();
         RealmsClient realmsclient = RealmsClient.create();
         PingResult pingresult = new PingResult();
         pingresult.pingResults = list;
         pingresult.worldIds = this.getOwnedNonExpiredWorldIds();

         try {
            realmsclient.sendPingResults(pingresult);
         } catch (Throwable throwable) {
            LOGGER.warn("Could not send ping result to Realms: ", throwable);
         }
      }).start();
   }

   private List<Long> getOwnedNonExpiredWorldIds() {
      List<Long> list = Lists.newArrayList();

      for(RealmsServer realmsserver : this.serverList) {
         if (this.isSelfOwnedNonExpiredServer(realmsserver)) {
            list.add(realmsserver.id);
         }
      }

      return list;
   }

   private void onRenew(@Nullable RealmsServer p_193500_) {
      if (p_193500_ != null) {
         String s = CommonLinks.extendRealms(p_193500_.remoteSubscriptionId, this.minecraft.getUser().getProfileId(), p_193500_.expiredTrial);
         this.minecraft.keyboardHandler.setClipboard(s);
         Util.getPlatform().openUri(s);
      }
   }

   private void configureClicked(@Nullable RealmsServer p_86657_) {
      if (p_86657_ != null && this.minecraft.isLocalPlayer(p_86657_.ownerUUID)) {
         this.minecraft.setScreen(new RealmsConfigureWorldScreen(this, p_86657_.id));
      }
   }

   private void leaveClicked(@Nullable RealmsServer p_86670_) {
      if (p_86670_ != null && !this.minecraft.isLocalPlayer(p_86670_.ownerUUID)) {
         Component component = Component.translatable("mco.configure.world.leave.question.line1");
         Component component1 = Component.translatable("mco.configure.world.leave.question.line2");
         this.minecraft
            .setScreen(
               new RealmsLongConfirmationScreen(
                  p_231253_ -> this.leaveServer(p_231253_, p_86670_), RealmsLongConfirmationScreen.Type.INFO, component, component1, true
               )
            );
      }
   }

   @Nullable
   private RealmsServer getSelectedServer() {
      RealmsMainScreen.Entry realmsmainscreen$entry = this.realmSelectionList.getSelected();
      return realmsmainscreen$entry != null ? realmsmainscreen$entry.getServer() : null;
   }

   private void leaveServer(boolean p_193494_, final RealmsServer p_193495_) {
      if (p_193494_) {
         (new Thread("Realms-leave-server") {
               @Override
               public void run() {
                  try {
                     RealmsClient realmsclient = RealmsClient.create();
                     realmsclient.uninviteMyselfFrom(p_193495_.id);
                     RealmsMainScreen.this.minecraft.execute(() -> RealmsMainScreen.this.removeServer(p_193495_));
                  } catch (RealmsServiceException realmsserviceexception) {
                     RealmsMainScreen.LOGGER.error("Couldn't configure world", (Throwable)realmsserviceexception);
                     RealmsMainScreen.this.minecraft
                        .execute(() -> RealmsMainScreen.this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, RealmsMainScreen.this)));
                  }
               }
            })
            .start();
      }

      this.minecraft.setScreen(this);
   }

   void removeServer(RealmsServer p_86677_) {
      this.serverList.removeItem(p_86677_);
      this.realmSelectionList.children().removeIf(p_231250_ -> {
         RealmsServer realmsserver = p_231250_.getServer();
         return realmsserver != null && realmsserver.id == p_86677_.id;
      });
      this.realmSelectionList.setSelected(null);
      this.updateButtonStates();
   }

   void dismissNotification(UUID p_275349_) {
      callRealmsClient(p_274628_ -> {
         p_274628_.notificationsDismiss(List.of(p_275349_));
         return null;
      }, p_274632_ -> {
         this.notifications.removeIf(p_274621_ -> p_274621_.dismissable() && p_275349_.equals(p_274621_.uuid()));
         this.refreshRealmsSelectionList();
      });
   }

   public void resetScreen() {
      this.realmSelectionList.setSelected(null);
   }

   @Override
   public Component getNarrationMessage() {
      return (Component)(switch(this.activeLayoutState) {
         case LOADING -> CommonComponents.joinForNarration(super.getNarrationMessage(), LOADING_TEXT);
         case NO_REALMS -> CommonComponents.joinForNarration(super.getNarrationMessage(), NO_REALMS_TEXT);
         case LIST -> super.getNarrationMessage();
      });
   }

   @Override
   public void render(GuiGraphics p_282736_, int p_283347_, int p_282480_, float p_283485_) {
      super.render(p_282736_, p_283347_, p_282480_, p_283485_);
      if (this.trialsAvailable && this.addRealmButton.active) {
         RealmsPopupScreen.renderDiamond(p_282736_, this.addRealmButton);
      }

      switch(RealmsClient.ENVIRONMENT) {
         case STAGE:
            this.renderEnvironment(p_282736_, "STAGE!", -256);
            break;
         case LOCAL:
            this.renderEnvironment(p_282736_, "LOCAL!", 8388479);
      }
   }

   private void openTrialAvailablePopup() {
      this.minecraft.setScreen(new RealmsPopupScreen(this, this.trialsAvailable));
   }

   public static void play(@Nullable RealmsServer p_86516_, Screen p_86517_) {
      if (p_86516_ != null) {
         Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(p_86517_, new GetServerDetailsTask(p_86517_, p_86516_)));
      }
   }

   boolean isSelfOwnedServer(RealmsServer p_86684_) {
      return this.minecraft.isLocalPlayer(p_86684_.ownerUUID);
   }

   private boolean isSelfOwnedNonExpiredServer(RealmsServer p_86689_) {
      return this.isSelfOwnedServer(p_86689_) && !p_86689_.expired;
   }

   private void renderEnvironment(GuiGraphics p_294591_, String p_295050_, int p_294351_) {
      p_294591_.pose().pushPose();
      p_294591_.pose().translate((float)(this.width / 2 - 25), 20.0F, 0.0F);
      p_294591_.pose().mulPose(Axis.ZP.rotationDegrees(-20.0F));
      p_294591_.pose().scale(1.5F, 1.5F, 1.5F);
      p_294591_.drawString(this.font, p_295050_, 0, 0, p_294351_, false);
      p_294591_.pose().popPose();
   }

   @OnlyIn(Dist.CLIENT)
   class ButtonEntry extends RealmsMainScreen.Entry {
      private final Button button;

      public ButtonEntry(Button p_275726_) {
         this.button = p_275726_;
      }

      @Override
      public boolean mouseClicked(double p_275240_, double p_275616_, int p_275528_) {
         this.button.mouseClicked(p_275240_, p_275616_, p_275528_);
         return true;
      }

      @Override
      public boolean keyPressed(int p_275630_, int p_275328_, int p_275519_) {
         return this.button.keyPressed(p_275630_, p_275328_, p_275519_) ? true : super.keyPressed(p_275630_, p_275328_, p_275519_);
      }

      @Override
      public void render(
         GuiGraphics p_283542_,
         int p_282029_,
         int p_281480_,
         int p_281377_,
         int p_283160_,
         int p_281920_,
         int p_283267_,
         int p_281282_,
         boolean p_281269_,
         float p_282372_
      ) {
         this.button.setPosition(RealmsMainScreen.this.width / 2 - 75, p_281480_ + 4);
         this.button.render(p_283542_, p_283267_, p_281282_, p_282372_);
      }

      @Override
      public Component getNarration() {
         return this.button.getMessage();
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class CrossButton extends ImageButton {
      private static final WidgetSprites SPRITES = new WidgetSprites(
         new ResourceLocation("widget/cross_button"), new ResourceLocation("widget/cross_button_highlighted")
      );

      protected CrossButton(Button.OnPress p_275420_, Component p_275193_) {
         super(0, 0, 14, 14, SPRITES, p_275420_);
         this.setTooltip(Tooltip.create(p_275193_));
      }
   }

   @OnlyIn(Dist.CLIENT)
   class EmptyEntry extends RealmsMainScreen.Entry {
      @Override
      public void render(
         GuiGraphics p_302489_,
         int p_302486_,
         int p_302498_,
         int p_302485_,
         int p_302492_,
         int p_302487_,
         int p_302488_,
         int p_302496_,
         boolean p_302491_,
         float p_302497_
      ) {
      }

      @Override
      public Component getNarration() {
         return Component.empty();
      }
   }

   @OnlyIn(Dist.CLIENT)
   abstract class Entry extends ObjectSelectionList.Entry<RealmsMainScreen.Entry> {
      @Nullable
      public RealmsServer getServer() {
         return null;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static enum LayoutState {
      LOADING,
      NO_REALMS,
      LIST;
   }

   @OnlyIn(Dist.CLIENT)
   static class NotificationButton extends SpriteIconButton.CenteredIcon {
      private static final ResourceLocation[] NOTIFICATION_ICONS = new ResourceLocation[]{
         new ResourceLocation("notification/1"),
         new ResourceLocation("notification/2"),
         new ResourceLocation("notification/3"),
         new ResourceLocation("notification/4"),
         new ResourceLocation("notification/5"),
         new ResourceLocation("notification/more")
      };
      private static final int UNKNOWN_COUNT = Integer.MAX_VALUE;
      private static final int SIZE = 20;
      private static final int SPRITE_SIZE = 14;
      private int notificationCount;

      public NotificationButton(Component p_296209_, ResourceLocation p_295363_, Button.OnPress p_294154_) {
         super(20, 20, p_296209_, 14, 14, p_295363_, p_294154_);
      }

      int notificationCount() {
         return this.notificationCount;
      }

      public void setNotificationCount(int p_295599_) {
         this.notificationCount = p_295599_;
      }

      @Override
      public void renderWidget(GuiGraphics p_295969_, int p_294986_, int p_294741_, float p_295116_) {
         super.renderWidget(p_295969_, p_294986_, p_294741_, p_295116_);
         if (this.active && this.notificationCount != 0) {
            this.drawNotificationCounter(p_295969_);
         }
      }

      private void drawNotificationCounter(GuiGraphics p_295995_) {
         p_295995_.blitSprite(NOTIFICATION_ICONS[Math.min(this.notificationCount, 6) - 1], this.getX() + this.getWidth() - 5, this.getY() - 3, 8, 8);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class NotificationMessageEntry extends RealmsMainScreen.Entry {
      private static final int SIDE_MARGINS = 40;
      private static final int OUTLINE_COLOR = -12303292;
      private final Component text;
      private final int frameItemHeight;
      private final List<AbstractWidget> children = new ArrayList<>();
      @Nullable
      private final RealmsMainScreen.CrossButton dismissButton;
      private final MultiLineTextWidget textWidget;
      private final GridLayout gridLayout;
      private final FrameLayout textFrame;
      private int lastEntryWidth = -1;

      public NotificationMessageEntry(Component p_275215_, int p_302495_, RealmsNotification p_275494_) {
         this.text = p_275215_;
         this.frameItemHeight = p_302495_;
         this.gridLayout = new GridLayout();
         int i = 7;
         this.gridLayout.addChild(ImageWidget.sprite(20, 20, RealmsMainScreen.INFO_SPRITE), 0, 0, this.gridLayout.newCellSettings().padding(7, 7, 0, 0));
         this.gridLayout.addChild(SpacerElement.width(40), 0, 0);
         this.textFrame = this.gridLayout.addChild(new FrameLayout(0, 9 * 3 * (p_302495_ - 1)), 0, 1, this.gridLayout.newCellSettings().paddingTop(7));
         this.textWidget = this.textFrame
            .addChild(
               new MultiLineTextWidget(p_275215_, RealmsMainScreen.this.font).setCentered(true),
               this.textFrame.newChildLayoutSettings().alignHorizontallyCenter().alignVerticallyTop()
            );
         this.gridLayout.addChild(SpacerElement.width(40), 0, 2);
         if (p_275494_.dismissable()) {
            this.dismissButton = this.gridLayout
               .addChild(
                  new RealmsMainScreen.CrossButton(
                     p_275478_ -> RealmsMainScreen.this.dismissNotification(p_275494_.uuid()), Component.translatable("mco.notification.dismiss")
                  ),
                  0,
                  2,
                  this.gridLayout.newCellSettings().alignHorizontallyRight().padding(0, 7, 7, 0)
               );
         } else {
            this.dismissButton = null;
         }

         this.gridLayout.visitWidgets(this.children::add);
      }

      @Override
      public boolean keyPressed(int p_275646_, int p_275453_, int p_275621_) {
         return this.dismissButton != null && this.dismissButton.keyPressed(p_275646_, p_275453_, p_275621_)
            ? true
            : super.keyPressed(p_275646_, p_275453_, p_275621_);
      }

      private void updateEntryWidth(int p_275670_) {
         if (this.lastEntryWidth != p_275670_) {
            this.refreshLayout(p_275670_);
            this.lastEntryWidth = p_275670_;
         }
      }

      private void refreshLayout(int p_275267_) {
         int i = p_275267_ - 80;
         this.textFrame.setMinWidth(i);
         this.textWidget.setMaxWidth(i);
         this.gridLayout.arrangeElements();
      }

      @Override
      public void renderBack(
         GuiGraphics p_281374_,
         int p_282622_,
         int p_283656_,
         int p_281830_,
         int p_281651_,
         int p_283685_,
         int p_281784_,
         int p_282510_,
         boolean p_283146_,
         float p_283324_
      ) {
         super.renderBack(p_281374_, p_282622_, p_283656_, p_281830_, p_281651_, p_283685_, p_281784_, p_282510_, p_283146_, p_283324_);
         p_281374_.renderOutline(p_281830_ - 2, p_283656_ - 2, p_281651_, 36 * this.frameItemHeight - 2, -12303292);
      }

      @Override
      public void render(
         GuiGraphics p_281768_,
         int p_275375_,
         int p_275358_,
         int p_275447_,
         int p_275694_,
         int p_275477_,
         int p_275710_,
         int p_275677_,
         boolean p_275542_,
         float p_275323_
      ) {
         this.gridLayout.setPosition(p_275447_, p_275358_);
         this.updateEntryWidth(p_275694_ - 4);
         this.children.forEach(p_280688_ -> p_280688_.render(p_281768_, p_275710_, p_275677_, p_275323_));
      }

      @Override
      public boolean mouseClicked(double p_275209_, double p_275338_, int p_275560_) {
         if (this.dismissButton != null) {
            this.dismissButton.mouseClicked(p_275209_, p_275338_, p_275560_);
         }

         return true;
      }

      @Override
      public Component getNarration() {
         return this.text;
      }
   }

   @OnlyIn(Dist.CLIENT)
   class RealmSelectionList extends RealmsObjectSelectionList<RealmsMainScreen.Entry> {
      public RealmSelectionList() {
         super(RealmsMainScreen.this.width, RealmsMainScreen.this.height, 0, RealmsMainScreen.this.height, 36);
      }

      public void setSelected(@Nullable RealmsMainScreen.Entry p_86849_) {
         super.setSelected(p_86849_);
         RealmsMainScreen.this.updateButtonStates();
      }

      @Override
      public int getMaxPosition() {
         return this.getItemCount() * 36;
      }

      @Override
      public int getRowWidth() {
         return 300;
      }
   }

   @OnlyIn(Dist.CLIENT)
   interface RealmsCall<T> {
      T request(RealmsClient p_275639_) throws RealmsServiceException;
   }

   @OnlyIn(Dist.CLIENT)
   class ServerEntry extends RealmsMainScreen.Entry {
      private static final int SKIN_HEAD_LARGE_WIDTH = 36;
      private final RealmsServer serverData;

      public ServerEntry(RealmsServer p_86856_) {
         this.serverData = p_86856_;
      }

      @Override
      public void render(
         GuiGraphics p_283093_,
         int p_281645_,
         int p_283047_,
         int p_283525_,
         int p_282321_,
         int p_282391_,
         int p_281913_,
         int p_282475_,
         boolean p_282378_,
         float p_282843_
      ) {
         if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
            p_283093_.blitSprite(RealmsMainScreen.NEW_REALM_SPRITE, p_283525_ + 36 + 10, p_283047_ + 6, 40, 20);
            int i1 = p_283525_ + 36 + 10 + 40 + 10;
            p_283093_.drawString(RealmsMainScreen.this.font, RealmsMainScreen.SERVER_UNITIALIZED_TEXT, i1, p_283047_ + 12, -1);
         } else {
            int i = 225;
            int j = 2;
            this.renderStatusLights(this.serverData, p_283093_, p_283525_ + 36, p_283047_, p_281913_, p_282475_, 225, 2);
            if (RealmsMainScreen.this.isSelfOwnedServer(this.serverData) && this.serverData.expired) {
               Component component = this.serverData.expiredTrial ? RealmsMainScreen.TRIAL_EXPIRED_TEXT : RealmsMainScreen.SUBSCRIPTION_EXPIRED_TEXT;
               int j1 = p_283047_ + 11 + 5;
               p_283093_.drawString(RealmsMainScreen.this.font, component, p_283525_ + 36 + 2, j1 + 1, 15553363, false);
            } else {
               if (this.serverData.worldType == RealmsServer.WorldType.MINIGAME) {
                  int k = 13413468;
                  int l = RealmsMainScreen.this.font.width(RealmsMainScreen.SELECT_MINIGAME_PREFIX);
                  p_283093_.drawString(RealmsMainScreen.this.font, RealmsMainScreen.SELECT_MINIGAME_PREFIX, p_283525_ + 36 + 2, p_283047_ + 12, 13413468, false);
                  p_283093_.drawString(RealmsMainScreen.this.font, this.serverData.getMinigameName(), p_283525_ + 36 + 2 + l, p_283047_ + 12, 7105644, false);
               } else {
                  p_283093_.drawString(RealmsMainScreen.this.font, this.serverData.getDescription(), p_283525_ + 36 + 2, p_283047_ + 12, 7105644, false);
               }

               if (!RealmsMainScreen.this.isSelfOwnedServer(this.serverData)) {
                  p_283093_.drawString(RealmsMainScreen.this.font, this.serverData.owner, p_283525_ + 36 + 2, p_283047_ + 12 + 11, 5000268, false);
               }
            }

            p_283093_.drawString(RealmsMainScreen.this.font, this.serverData.getName(), p_283525_ + 36 + 2, p_283047_ + 1, -1, false);
            RealmsUtil.renderPlayerFace(p_283093_, p_283525_ + 36 - 36, p_283047_, 32, this.serverData.ownerUUID);
         }
      }

      private void playRealm() {
         RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         RealmsMainScreen.play(this.serverData, RealmsMainScreen.this);
      }

      private void createUnitializedRealm() {
         RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         RealmsCreateRealmScreen realmscreaterealmscreen = new RealmsCreateRealmScreen(this.serverData, RealmsMainScreen.this);
         RealmsMainScreen.this.minecraft.setScreen(realmscreaterealmscreen);
      }

      @Override
      public boolean mouseClicked(double p_86858_, double p_86859_, int p_86860_) {
         if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
            this.createUnitializedRealm();
         } else if (RealmsMainScreen.this.shouldPlayButtonBeActive(this.serverData)) {
            if (Util.getMillis() - RealmsMainScreen.this.lastClickTime < 250L && this.isFocused()) {
               this.playRealm();
            }

            RealmsMainScreen.this.lastClickTime = Util.getMillis();
         }

         return true;
      }

      @Override
      public boolean keyPressed(int p_279120_, int p_279121_, int p_279296_) {
         if (CommonInputs.selected(p_279120_)) {
            if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
               this.createUnitializedRealm();
               return true;
            }

            if (RealmsMainScreen.this.shouldPlayButtonBeActive(this.serverData)) {
               this.playRealm();
               return true;
            }
         }

         return super.keyPressed(p_279120_, p_279121_, p_279296_);
      }

      private void renderStatusLights(
         RealmsServer p_272798_, GuiGraphics p_283451_, int p_273706_, int p_272591_, int p_273561_, int p_273468_, int p_273073_, int p_273187_
      ) {
         int i = p_273706_ + p_273073_ + 22;
         if (p_272798_.expired) {
            this.drawRealmStatus(
               p_283451_, i, p_272591_ + p_273187_, p_273561_, p_273468_, RealmsMainScreen.EXPIRED_SPRITE, () -> RealmsMainScreen.SERVER_EXPIRED_TOOLTIP
            );
         } else if (p_272798_.state == RealmsServer.State.CLOSED) {
            this.drawRealmStatus(
               p_283451_, i, p_272591_ + p_273187_, p_273561_, p_273468_, RealmsMainScreen.CLOSED_SPRITE, () -> RealmsMainScreen.SERVER_CLOSED_TOOLTIP
            );
         } else if (RealmsMainScreen.this.isSelfOwnedServer(p_272798_) && p_272798_.daysLeft < 7) {
            this.drawRealmStatus(
               p_283451_,
               i,
               p_272591_ + p_273187_,
               p_273561_,
               p_273468_,
               RealmsMainScreen.EXPIRES_SOON_SPRITE,
               () -> {
                  if (p_272798_.daysLeft <= 0) {
                     return RealmsMainScreen.SERVER_EXPIRES_SOON_TOOLTIP;
                  } else {
                     return (Component)(p_272798_.daysLeft == 1
                        ? RealmsMainScreen.SERVER_EXPIRES_IN_DAY_TOOLTIP
                        : Component.translatable("mco.selectServer.expires.days", p_272798_.daysLeft));
                  }
               }
            );
         } else if (p_272798_.state == RealmsServer.State.OPEN) {
            this.drawRealmStatus(
               p_283451_, i, p_272591_ + p_273187_, p_273561_, p_273468_, RealmsMainScreen.OPEN_SPRITE, () -> RealmsMainScreen.SERVER_OPEN_TOOLTIP
            );
         }
      }

      private void drawRealmStatus(
         GuiGraphics p_299156_, int p_298202_, int p_298578_, int p_298658_, int p_298312_, ResourceLocation p_298898_, Supplier<Component> p_298650_
      ) {
         p_299156_.blitSprite(p_298898_, p_298202_, p_298578_, 10, 28);
         if (p_298658_ >= p_298202_
            && p_298658_ <= p_298202_ + 9
            && p_298312_ >= p_298578_
            && p_298312_ <= p_298578_ + 27
            && p_298312_ < RealmsMainScreen.this.height - 40
            && p_298312_ > 32) {
            RealmsMainScreen.this.setTooltipForNextRenderPass(p_298650_.get());
         }
      }

      @Override
      public Component getNarration() {
         return (Component)(this.serverData.state == RealmsServer.State.UNINITIALIZED
            ? RealmsMainScreen.UNITIALIZED_WORLD_NARRATION
            : Component.translatable("narrator.select", this.serverData.name));
      }

      @Nullable
      @Override
      public RealmsServer getServer() {
         return this.serverData;
      }
   }
}

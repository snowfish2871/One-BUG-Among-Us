package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerAddress;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.RealmsBrokenWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongConfirmationScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTickTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsTermsScreen;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.net.URL;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GetServerDetailsTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.connect.connecting");
   private final RealmsServer server;
   private final Screen lastScreen;

   public GetServerDetailsTask(Screen p_90333_, RealmsServer p_90334_) {
      this.lastScreen = p_90333_;
      this.server = p_90334_;
   }

   @Override
   public void run() {
      RealmsServerAddress realmsserveraddress;
      try {
         realmsserveraddress = this.fetchServerAddress();
      } catch (CancellationException cancellationexception) {
         LOGGER.info("User aborted connecting to realms");
         return;
      } catch (RealmsServiceException realmsserviceexception) {
         switch(realmsserviceexception.realmsError.errorCode()) {
            case 6002:
               setScreen(new RealmsTermsScreen(this.lastScreen, this.server));
               return;
            case 6006:
               boolean flag1 = Minecraft.getInstance().isLocalPlayer(this.server.ownerUUID);
               setScreen(
                  (Screen)(flag1
                     ? new RealmsBrokenWorldScreen(this.lastScreen, this.server.id, this.server.worldType == RealmsServer.WorldType.MINIGAME)
                     : new RealmsGenericErrorScreen(
                        Component.translatable("mco.brokenworld.nonowner.title"), Component.translatable("mco.brokenworld.nonowner.error"), this.lastScreen
                     ))
               );
               return;
            default:
               this.error(realmsserviceexception);
               LOGGER.error("Couldn't connect to world", (Throwable)realmsserviceexception);
               return;
         }
      } catch (TimeoutException timeoutexception) {
         this.error(Component.translatable("mco.errorMessage.connectionFailure"));
         return;
      } catch (Exception exception) {
         LOGGER.error("Couldn't connect to world", (Throwable)exception);
         this.error(exception);
         return;
      }

      boolean flag = realmsserveraddress.resourcePackUrl != null && realmsserveraddress.resourcePackHash != null;
      Screen screen = (Screen)(flag
         ? this.resourcePackDownloadConfirmationScreen(realmsserveraddress, this::connectScreen)
         : this.connectScreen(realmsserveraddress));
      setScreen(screen);
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }

   private RealmsServerAddress fetchServerAddress() throws RealmsServiceException, TimeoutException, CancellationException {
      RealmsClient realmsclient = RealmsClient.create();

      for(int i = 0; i < 40; ++i) {
         if (this.aborted()) {
            throw new CancellationException();
         }

         try {
            return realmsclient.join(this.server.id);
         } catch (RetryCallException retrycallexception) {
            pause((long)retrycallexception.delaySeconds);
         }
      }

      throw new TimeoutException();
   }

   public RealmsLongRunningMcoTaskScreen connectScreen(RealmsServerAddress p_167638_) {
      return new RealmsLongRunningMcoTickTaskScreen(this.lastScreen, new ConnectTask(this.lastScreen, this.server, p_167638_));
   }

   private RealmsLongConfirmationScreen resourcePackDownloadConfirmationScreen(RealmsServerAddress p_167640_, Function<RealmsServerAddress, Screen> p_167641_) {
      BooleanConsumer booleanconsumer = p_302309_ -> {
         if (!p_302309_) {
            setScreen(this.lastScreen);
         } else {
            this.scheduleResourcePackDownload(p_167640_).thenRun(() -> setScreen(p_167641_.apply(p_167640_))).exceptionally(p_287306_ -> {
               Minecraft.getInstance().getDownloadedPackSource().clearServerPack();
               LOGGER.error("Failed to download resource pack from {}", p_167640_, p_287306_);
               setScreen(new RealmsGenericErrorScreen(Component.translatable("mco.download.resourcePack.fail"), this.lastScreen));
               return null;
            });
         }
      };
      return new RealmsLongConfirmationScreen(
         booleanconsumer,
         RealmsLongConfirmationScreen.Type.INFO,
         Component.translatable("mco.configure.world.resourcepack.question.line1"),
         Component.translatable("mco.configure.world.resourcepack.question.line2"),
         true
      );
   }

   private CompletableFuture<?> scheduleResourcePackDownload(RealmsServerAddress p_167652_) {
      try {
         return Minecraft.getInstance()
            .getDownloadedPackSource()
            .downloadAndSelectResourcePack(new URL(p_167652_.resourcePackUrl), p_167652_.resourcePackHash, false);
      } catch (Exception exception) {
         CompletableFuture<Void> completablefuture = new CompletableFuture<>();
         completablefuture.completeExceptionally(exception);
         return completablefuture;
      }
   }
}

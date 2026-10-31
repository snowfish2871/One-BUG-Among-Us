package com.mojang.realmsclient;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsClientOutdatedScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsParentalConsentScreen;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsAvailability {
   private static final Logger LOGGER = LogUtils.getLogger();
   @Nullable
   private static CompletableFuture<RealmsAvailability.Result> future;

   public static CompletableFuture<RealmsAvailability.Result> get() {
      if (future == null || shouldRefresh(future)) {
         future = check();
      }

      return future;
   }

   private static boolean shouldRefresh(CompletableFuture<RealmsAvailability.Result> p_295124_) {
      RealmsAvailability.Result realmsavailability$result = p_295124_.getNow(null);
      return realmsavailability$result != null && realmsavailability$result.exception() != null;
   }

   private static CompletableFuture<RealmsAvailability.Result> check() {
      return CompletableFuture.supplyAsync(
         () -> {
            if (!net.neoforged.fml.loading.FMLLoader.isProduction() && net.minecraft.client.Minecraft.getInstance().getUser().getAccessToken().equals("0")) {
               // Neo: we use '0' in dev, so short-circuit to avoid exception
               LOGGER.trace("User access token is '0'. Skipping realms availability check.");
               return new RealmsAvailability.Result(Type.AUTHENTICATION_ERROR);
            }

            RealmsClient realmsclient = RealmsClient.create();
   
            try {
               if (realmsclient.clientCompatible() != RealmsClient.CompatibleVersionResponse.COMPATIBLE) {
                  return new RealmsAvailability.Result(RealmsAvailability.Type.INCOMPATIBLE_CLIENT);
               } else {
                  return !realmsclient.hasParentalConsent()
                     ? new RealmsAvailability.Result(RealmsAvailability.Type.NEEDS_PARENTAL_CONSENT)
                     : new RealmsAvailability.Result(RealmsAvailability.Type.SUCCESS);
               }
            } catch (RealmsServiceException realmsserviceexception) {
               LOGGER.error("Couldn't connect to realms", (Throwable)realmsserviceexception);
               return realmsserviceexception.realmsError.errorCode() == 401
                  ? new RealmsAvailability.Result(RealmsAvailability.Type.AUTHENTICATION_ERROR)
                  : new RealmsAvailability.Result(realmsserviceexception);
            }
         },
         Util.ioPool()
      );
   }

   @OnlyIn(Dist.CLIENT)
   public static record Result(RealmsAvailability.Type type, @Nullable RealmsServiceException exception) {
      public Result(RealmsAvailability.Type p_294456_) {
         this(p_294456_, null);
      }

      public Result(RealmsServiceException p_294364_) {
         this(RealmsAvailability.Type.UNEXPECTED_ERROR, p_294364_);
      }

      @Nullable
      public Screen createErrorScreen(Screen p_296406_) {
         return (Screen)(switch(this.type) {
            case SUCCESS -> null;
            case INCOMPATIBLE_CLIENT -> new RealmsClientOutdatedScreen(p_296406_);
            case NEEDS_PARENTAL_CONSENT -> new RealmsParentalConsentScreen(p_296406_);
            case AUTHENTICATION_ERROR -> new RealmsGenericErrorScreen(
            Component.translatable("mco.error.invalid.session.title"), Component.translatable("mco.error.invalid.session.message"), p_296406_
         );
            case UNEXPECTED_ERROR -> new RealmsGenericErrorScreen(Objects.requireNonNull(this.exception), p_296406_);
         });
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Type {
      SUCCESS,
      INCOMPATIBLE_CLIENT,
      NEEDS_PARENTAL_CONSENT,
      AUTHENTICATION_ERROR,
      UNEXPECTED_ERROR;
   }
}

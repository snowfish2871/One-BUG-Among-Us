package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.ForcedUsernameChangeException;
import com.mojang.authlib.exceptions.InsufficientPrivilegesException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.UserBannedException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.logging.LogUtils;
import java.math.BigInteger;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ClientboundGameProfilePacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.realms.DisconnectedRealmsScreen;
import net.minecraft.util.Crypt;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientHandshakePacketListenerImpl implements ClientLoginPacketListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Minecraft minecraft;
   @Nullable
   private final ServerData serverData;
   @Nullable
   private final Screen parent;
   private final Consumer<Component> updateStatus;
   private final Connection connection;
   private final boolean newWorld;
   @Nullable
   private final Duration worldLoadDuration;
   @Nullable
   private String minigameName;
   private final AtomicReference<ClientHandshakePacketListenerImpl.State> state = new AtomicReference<>(ClientHandshakePacketListenerImpl.State.CONNECTING);

   public ClientHandshakePacketListenerImpl(
      Connection p_261697_,
      Minecraft p_261835_,
      @Nullable ServerData p_261938_,
      @Nullable Screen p_261783_,
      boolean p_261562_,
      @Nullable Duration p_261673_,
      Consumer<Component> p_261945_
   ) {
      this.connection = p_261697_;
      this.minecraft = p_261835_;
      this.serverData = p_261938_;
      this.parent = p_261783_;
      this.updateStatus = p_261945_;
      this.newWorld = p_261562_;
      this.worldLoadDuration = p_261673_;
   }

   private void switchState(ClientHandshakePacketListenerImpl.State p_302230_) {
      ClientHandshakePacketListenerImpl.State clienthandshakepacketlistenerimpl$state = this.state.updateAndGet(p_302151_ -> {
         if (!p_302230_.fromStates.contains(p_302151_)) {
            throw new IllegalStateException("Tried to switch to " + p_302230_ + " from " + p_302151_ + ", but expected one of " + p_302230_.fromStates);
         } else {
            return p_302230_;
         }
      });
      this.updateStatus.accept(clienthandshakepacketlistenerimpl$state.message);
   }

   @Override
   public void handleHello(ClientboundHelloPacket p_104549_) {
      this.switchState(ClientHandshakePacketListenerImpl.State.AUTHORIZING);

      Cipher cipher;
      Cipher cipher1;
      String s;
      ServerboundKeyPacket serverboundkeypacket;
      try {
         SecretKey secretkey = Crypt.generateSecretKey();
         PublicKey publickey = p_104549_.getPublicKey();
         s = new BigInteger(Crypt.digestData(p_104549_.getServerId(), publickey, secretkey)).toString(16);
         cipher = Crypt.getCipher(2, secretkey);
         cipher1 = Crypt.getCipher(1, secretkey);
         byte[] abyte = p_104549_.getChallenge();
         serverboundkeypacket = new ServerboundKeyPacket(secretkey, publickey, abyte);
      } catch (Exception exception) {
         throw new IllegalStateException("Protocol error", exception);
      }

      HttpUtil.DOWNLOAD_EXECUTOR.submit(() -> {
         Component component = this.authenticateServer(s);
         if (component != null) {
            if (this.serverData == null || !this.serverData.isLan()) {
               this.connection.disconnect(component);
               return;
            }

            LOGGER.warn(component.getString());
         }

         this.switchState(ClientHandshakePacketListenerImpl.State.ENCRYPTING);
         this.connection.send(serverboundkeypacket, PacketSendListener.thenRun(() -> this.connection.setEncryptionKey(cipher, cipher1)));
      });
   }

   @Nullable
   private Component authenticateServer(String p_104532_) {
      try {
         this.getMinecraftSessionService().joinServer(this.minecraft.getUser().getProfileId(), this.minecraft.getUser().getAccessToken(), p_104532_);
         return null;
      } catch (AuthenticationUnavailableException authenticationunavailableexception) {
         return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.serversUnavailable"));
      } catch (InvalidCredentialsException invalidcredentialsexception) {
         return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.invalidSession"));
      } catch (InsufficientPrivilegesException insufficientprivilegesexception) {
         return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.insufficientPrivileges"));
      } catch (ForcedUsernameChangeException | UserBannedException userbannedexception) {
         return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.userBanned"));
      } catch (AuthenticationException authenticationexception) {
         return Component.translatable("disconnect.loginFailedInfo", authenticationexception.getMessage());
      }
   }

   private MinecraftSessionService getMinecraftSessionService() {
      return this.minecraft.getMinecraftSessionService();
   }

   @Override
   public void handleGameProfile(ClientboundGameProfilePacket p_104547_) {
      this.switchState(ClientHandshakePacketListenerImpl.State.JOINING);
      GameProfile gameprofile = p_104547_.getGameProfile();
      this.connection.send(new ServerboundLoginAcknowledgedPacket());
      this.connection
         .setListener(
            new ClientConfigurationPacketListenerImpl(
               this.minecraft,
               this.connection,
               new CommonListenerCookie(
                  gameprofile,
                  this.minecraft.getTelemetryManager().createWorldSessionManager(this.newWorld, this.worldLoadDuration, this.minigameName),
                  ClientRegistryLayer.createRegistryAccess().compositeAccess(),
                  FeatureFlags.DEFAULT_FLAGS,
                  null,
                  this.serverData,
                  this.parent
               )
            )
         );
      this.connection.send(new ServerboundCustomPayloadPacket(new BrandPayload(ClientBrandRetriever.getClientModName())));
      this.connection.send(new ServerboundClientInformationPacket(this.minecraft.options.buildPlayerInformation()));
   }

   @Override
   public void onDisconnect(Component p_104543_) {
      if (this.serverData != null && this.serverData.isRealm()) {
         this.minecraft.setScreen(new DisconnectedRealmsScreen(this.parent, CommonComponents.CONNECT_FAILED, p_104543_));
      } else {
         this.minecraft.setScreen(net.neoforged.neoforge.network.NetworkHooks.getModMismatchData(connection) != null ? new net.neoforged.neoforge.client.gui.ModMismatchDisconnectedScreen(this.parent, CommonComponents.CONNECT_FAILED, p_104543_, net.neoforged.neoforge.network.NetworkHooks.getModMismatchData(connection)) : new DisconnectedScreen(this.parent, CommonComponents.CONNECT_FAILED, p_104543_));
      }
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }

   @Override
   public void handleDisconnect(ClientboundLoginDisconnectPacket p_104553_) {
      this.connection.disconnect(p_104553_.getReason());
   }

   @Override
   public void handleCompression(ClientboundLoginCompressionPacket p_104551_) {
      if (!this.connection.isMemoryConnection()) {
         this.connection.setupCompression(p_104551_.getCompressionThreshold(), false);
      }
   }

   @Override
   public void handleCustomQuery(ClientboundCustomQueryPacket p_104545_) {
      if (p_104545_.payload() instanceof net.neoforged.neoforge.network.ICustomQueryPayloadWithBuffer payload && net.neoforged.neoforge.network.NetworkHooks.onCustomQuery(p_104545_, payload, this.connection)) return;
      this.updateStatus.accept(Component.translatable("connect.negotiating"));
      this.connection.send(new ServerboundCustomQueryAnswerPacket(p_104545_.transactionId(), null));
   }

   public void setMinigameName(String p_286653_) {
      this.minigameName = p_286653_;
   }

   @OnlyIn(Dist.CLIENT)
   static enum State {
      CONNECTING(Component.translatable("connect.connecting"), Set.of()),
      AUTHORIZING(Component.translatable("connect.authorizing"), Set.of(CONNECTING)),
      ENCRYPTING(Component.translatable("connect.encrypting"), Set.of(AUTHORIZING)),
      JOINING(Component.translatable("connect.joining"), Set.of(ENCRYPTING, CONNECTING));

      final Component message;
      final Set<ClientHandshakePacketListenerImpl.State> fromStates;

      private State(Component p_302239_, Set<ClientHandshakePacketListenerImpl.State> p_302236_) {
         this.message = p_302239_;
         this.fromStates = p_302236_;
      }
   }
}

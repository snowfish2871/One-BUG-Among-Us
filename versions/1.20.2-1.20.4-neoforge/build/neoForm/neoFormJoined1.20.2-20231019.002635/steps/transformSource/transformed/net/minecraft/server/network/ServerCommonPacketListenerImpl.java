package net.minecraft.server.network;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.util.VisibleForDebug;
import org.slf4j.Logger;

public abstract class ServerCommonPacketListenerImpl implements ServerCommonPacketListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int LATENCY_CHECK_INTERVAL = 15000;
   private static final Component TIMEOUT_DISCONNECTION_MESSAGE = Component.translatable("disconnect.timeout");
   protected final MinecraftServer server;
   public final Connection connection;
   private long keepAliveTime;
   private boolean keepAlivePending;
   private long keepAliveChallenge;
   private int latency;
   private volatile boolean suspendFlushingOnServerThread = false;

   public ServerCommonPacketListenerImpl(MinecraftServer p_295057_, Connection p_294822_, CommonListenerCookie p_301980_) {
      this.server = p_295057_;
      this.connection = p_294822_;
      this.keepAliveTime = Util.getMillis();
      this.latency = p_301980_.latency();
   }

   @Override
   public void onDisconnect(Component p_295648_) {
      if (this.isSingleplayerOwner()) {
         LOGGER.info("Stopping singleplayer server as player logged out");
         this.server.halt(false);
      }
   }

   @Override
   public void handleKeepAlive(ServerboundKeepAlivePacket p_294627_) {
      if (this.keepAlivePending && p_294627_.getId() == this.keepAliveChallenge) {
         int i = (int)(Util.getMillis() - this.keepAliveTime);
         this.latency = (this.latency * 3 + i) / 4;
         this.keepAlivePending = false;
      } else if (!this.isSingleplayerOwner()) {
         this.disconnect(TIMEOUT_DISCONNECTION_MESSAGE);
      }
   }

   @Override
   public void handlePong(ServerboundPongPacket p_295142_) {
   }

   @Override
   public void handleCustomPayload(ServerboundCustomPayloadPacket p_294276_) {
      if (p_294276_.payload() instanceof net.neoforged.neoforge.network.custom.payload.SimplePayload simplePayload && net.neoforged.neoforge.network.NetworkHooks.onCustomPayload(p_294276_, simplePayload, connection)) {
         return;
      }
      
      LOGGER.warn("Unknown custom packet payload: {}", p_294276_.payload().id());
   }

   @Override
   public void handleResourcePackResponse(ServerboundResourcePackPacket p_295695_) {
      PacketUtils.ensureRunningOnSameThread(p_295695_, this, this.server);
      if (p_295695_.getAction() == ServerboundResourcePackPacket.Action.DECLINED && this.server.isResourcePackRequired()) {
         LOGGER.info("Disconnecting {} due to resource pack rejection", this.playerProfile().getName());
         this.disconnect(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
      }
   }

   protected void keepConnectionAlive() {
      this.server.getProfiler().push("keepAlive");
      long i = Util.getMillis();
      if (i - this.keepAliveTime >= 15000L) {
         if (this.keepAlivePending) {
            this.disconnect(TIMEOUT_DISCONNECTION_MESSAGE);
         } else {
            this.keepAlivePending = true;
            this.keepAliveTime = i;
            this.keepAliveChallenge = i;
            this.send(new ClientboundKeepAlivePacket(this.keepAliveChallenge));
         }
      }

      this.server.getProfiler().pop();
   }

   public void suspendFlushing() {
      this.suspendFlushingOnServerThread = true;
   }

   public void resumeFlushing() {
      this.suspendFlushingOnServerThread = false;
      this.connection.flushChannel();
   }

   public void send(Packet<?> p_294278_) {
      this.send(p_294278_, null);
   }

   public void send(Packet<?> p_295099_, @Nullable PacketSendListener p_296321_) {
      boolean flag = !this.suspendFlushingOnServerThread || !this.server.isSameThread();

      try {
         this.connection.send(p_295099_, p_296321_, flag);
      } catch (Throwable throwable) {
         CrashReport crashreport = CrashReport.forThrowable(throwable, "Sending packet");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Packet being sent");
         crashreportcategory.setDetail("Packet class", () -> p_295099_.getClass().getCanonicalName());
         throw new ReportedException(crashreport);
      }
   }

   public void disconnect(Component p_294116_) {
      this.connection.send(new ClientboundDisconnectPacket(p_294116_), PacketSendListener.thenRun(() -> this.connection.disconnect(p_294116_)));
      this.connection.setReadOnly();
      this.server.executeBlocking(this.connection::handleDisconnection);
   }

   protected boolean isSingleplayerOwner() {
      return this.server.isSingleplayerOwner(this.playerProfile());
   }

   protected abstract GameProfile playerProfile();

   @VisibleForDebug
   public GameProfile getOwner() {
      return this.playerProfile();
   }

   public int latency() {
      return this.latency;
   }

   protected CommonListenerCookie createCookie(ClientInformation p_301973_) {
      return new CommonListenerCookie(this.playerProfile(), this.latency, p_301973_);
   }
}

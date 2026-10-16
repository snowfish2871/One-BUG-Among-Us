package net.minecraft.client.multiplayer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.ServerboundPacketListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.realms.DisconnectedRealmsScreen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagNetworkSerialization;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public abstract class ClientCommonPacketListenerImpl implements ClientCommonPacketListener {
   private static final Component GENERIC_DISCONNECT_MESSAGE = Component.translatable("disconnect.lost");
   private static final Logger LOGGER = LogUtils.getLogger();
   protected final Minecraft minecraft;
   public final Connection connection;
   @Nullable
   protected final ServerData serverData;
   @Nullable
   protected String serverBrand;
   protected final WorldSessionTelemetryManager telemetryManager;
   @Nullable
   protected final Screen postDisconnectScreen;
   private final List<ClientCommonPacketListenerImpl.DeferredPacket> deferredPackets = new ArrayList<>();

   protected ClientCommonPacketListenerImpl(Minecraft p_295454_, Connection p_294773_, CommonListenerCookie p_294647_) {
      this.minecraft = p_295454_;
      this.connection = p_294773_;
      this.serverData = p_294647_.serverData();
      this.serverBrand = p_294647_.serverBrand();
      this.telemetryManager = p_294647_.telemetryManager();
      this.postDisconnectScreen = p_294647_.postDisconnectScreen();
   }

   @Override
   public void handleKeepAlive(ClientboundKeepAlivePacket p_295361_) {
      this.sendWhen(new ServerboundKeepAlivePacket(p_295361_.getId()), () -> !RenderSystem.isFrozenAtPollEvents(), Duration.ofMinutes(1L));
   }

   @Override
   public void handlePing(ClientboundPingPacket p_295594_) {
      PacketUtils.ensureRunningOnSameThread(p_295594_, this, this.minecraft);
      this.send(new ServerboundPongPacket(p_295594_.getId()));
   }

   @Override
   public void handleCustomPayload(ClientboundCustomPayloadPacket p_295727_) {
      CustomPacketPayload custompacketpayload = p_295727_.payload();
      if (!(custompacketpayload instanceof DiscardedPayload)) {
         PacketUtils.ensureRunningOnSameThread(p_295727_, this, this.minecraft);
         if (custompacketpayload instanceof BrandPayload brandpayload) {
            this.serverBrand = brandpayload.brand();
            this.telemetryManager.onServerBrandReceived(brandpayload.brand());
         } else {
            this.handleCustomPayload(p_295727_, custompacketpayload);
         }
      }
   }

   protected abstract void handleCustomPayload(ClientboundCustomPayloadPacket p_295727_, CustomPacketPayload p_295776_);

   protected abstract RegistryAccess.Frozen registryAccess();

   @Override
   public void handleResourcePack(ClientboundResourcePackPacket p_294620_) {
      URL url = parseResourcePackUrl(p_294620_.getUrl());
      if (url == null) {
         this.send(ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD);
      } else {
         String s = p_294620_.getHash();
         boolean flag = p_294620_.isRequired();
         if (this.serverData != null && this.serverData.getResourcePackStatus() == ServerData.ServerPackStatus.ENABLED) {
            this.send(ServerboundResourcePackPacket.Action.ACCEPTED);
            this.packApplicationCallback(this.minecraft.getDownloadedPackSource().downloadAndSelectResourcePack(url, s, true));
         } else if (this.serverData != null
            && this.serverData.getResourcePackStatus() != ServerData.ServerPackStatus.PROMPT
            && (!flag || this.serverData.getResourcePackStatus() != ServerData.ServerPackStatus.DISABLED)) {
            this.send(ServerboundResourcePackPacket.Action.DECLINED);
            if (flag) {
               this.connection.disconnect(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
            }
         } else {
            this.minecraft.execute(() -> this.showServerPackPrompt(url, s, flag, p_294620_.getPrompt()));
         }
      }
   }

   private void showServerPackPrompt(URL p_294328_, String p_295567_, boolean p_294890_, @Nullable Component p_295229_) {
      Screen screen = this.minecraft.screen;
      this.minecraft
         .setScreen(
            new ConfirmScreen(
               p_295450_ -> {
                  this.minecraft.setScreen(screen);
                  if (p_295450_) {
                     if (this.serverData != null) {
                        this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
                     }
         
                     this.send(ServerboundResourcePackPacket.Action.ACCEPTED);
                     this.packApplicationCallback(this.minecraft.getDownloadedPackSource().downloadAndSelectResourcePack(p_294328_, p_295567_, true));
                  } else {
                     this.send(ServerboundResourcePackPacket.Action.DECLINED);
                     if (p_294890_) {
                        this.connection.disconnect(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
                     } else if (this.serverData != null) {
                        this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.DISABLED);
                     }
                  }
         
                  if (this.serverData != null) {
                     ServerList.saveSingleServer(this.serverData);
                  }
               },
               p_294890_ ? Component.translatable("multiplayer.requiredTexturePrompt.line1") : Component.translatable("multiplayer.texturePrompt.line1"),
               preparePackPrompt(
                  p_294890_
                     ? Component.translatable("multiplayer.requiredTexturePrompt.line2").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                     : Component.translatable("multiplayer.texturePrompt.line2"),
                  p_295229_
               ),
               p_294890_ ? CommonComponents.GUI_PROCEED : CommonComponents.GUI_YES,
               (Component)(p_294890_ ? Component.translatable("menu.disconnect") : CommonComponents.GUI_NO)
            )
         );
   }

   private static Component preparePackPrompt(Component p_296200_, @Nullable Component p_295584_) {
      return (Component)(p_295584_ == null ? p_296200_ : Component.translatable("multiplayer.texturePrompt.serverPrompt", p_296200_, p_295584_));
   }

   @Nullable
   private static URL parseResourcePackUrl(String p_295495_) {
      try {
         URL url = new URL(p_295495_);
         String s = url.getProtocol();
         return !"http".equals(s) && !"https".equals(s) ? null : url;
      } catch (MalformedURLException malformedurlexception) {
         return null;
      }
   }

   private void packApplicationCallback(CompletableFuture<?> p_294183_) {
      p_294183_.thenRun(() -> this.send(ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED)).exceptionally(p_294374_ -> {
         this.send(ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD);
         return null;
      });
   }

   @Override
   public void handleUpdateTags(ClientboundUpdateTagsPacket p_294605_) {
      PacketUtils.ensureRunningOnSameThread(p_294605_, this, this.minecraft);
      p_294605_.getTags().forEach(this::updateTagsForRegistry);
   }

   private <T> void updateTagsForRegistry(ResourceKey<? extends Registry<? extends T>> p_294128_, TagNetworkSerialization.NetworkPayload p_294666_) {
      if (!p_294666_.isEmpty()) {
         Registry<T> registry = this.registryAccess().<T>registry(p_294128_).orElseThrow(() -> new IllegalStateException("Unknown registry " + p_294128_));
         Map<TagKey<T>, List<Holder<T>>> map = new HashMap<>();
         TagNetworkSerialization.deserializeTagsFromNetwork((ResourceKey<? extends Registry<T>>)p_294128_, registry, p_294666_, map::put);
         registry.bindTags(map);
      }
   }

   private void send(ServerboundResourcePackPacket.Action p_296064_) {
      this.connection.send(new ServerboundResourcePackPacket(p_296064_));
   }

   @Override
   public void handleDisconnect(ClientboundDisconnectPacket p_296159_) {
      this.connection.disconnect(p_296159_.getReason());
   }

   protected void sendDeferredPackets() {
      Iterator<ClientCommonPacketListenerImpl.DeferredPacket> iterator = this.deferredPackets.iterator();

      while(iterator.hasNext()) {
         ClientCommonPacketListenerImpl.DeferredPacket clientcommonpacketlistenerimpl$deferredpacket = iterator.next();
         if (clientcommonpacketlistenerimpl$deferredpacket.sendCondition().getAsBoolean()) {
            this.send(clientcommonpacketlistenerimpl$deferredpacket.packet);
            iterator.remove();
         } else if (clientcommonpacketlistenerimpl$deferredpacket.expirationTime() <= Util.getMillis()) {
            iterator.remove();
         }
      }
   }

   public void send(Packet<?> p_295097_) {
      this.connection.send(p_295097_);
   }

   @Override
   public void onDisconnect(Component p_295485_) {
      this.telemetryManager.onDisconnect();
      this.minecraft.disconnect(this.createDisconnectScreen(p_295485_));
      LOGGER.warn("Client disconnected with reason: {}", p_295485_.getString());
   }

   protected Screen createDisconnectScreen(Component p_296470_) {
      Screen screen = Objects.requireNonNullElseGet(this.postDisconnectScreen, () -> new JoinMultiplayerScreen(new TitleScreen()));
      return (Screen)(this.serverData != null && this.serverData.isRealm()
         ? new DisconnectedRealmsScreen(screen, GENERIC_DISCONNECT_MESSAGE, p_296470_)
         : new DisconnectedScreen(screen, GENERIC_DISCONNECT_MESSAGE, p_296470_));
   }

   @Nullable
   public String serverBrand() {
      return this.serverBrand;
   }

   private void sendWhen(Packet<? extends ServerboundPacketListener> p_296259_, BooleanSupplier p_296086_, Duration p_294812_) {
      if (p_296086_.getAsBoolean()) {
         this.send(p_296259_);
      } else {
         this.deferredPackets.add(new ClientCommonPacketListenerImpl.DeferredPacket(p_296259_, p_296086_, Util.getMillis() + p_294812_.toMillis()));
      }
   }

   @OnlyIn(Dist.CLIENT)
   static record DeferredPacket(Packet<? extends ServerboundPacketListener> packet, BooleanSupplier sendCondition, long expirationTime) {
   }
}

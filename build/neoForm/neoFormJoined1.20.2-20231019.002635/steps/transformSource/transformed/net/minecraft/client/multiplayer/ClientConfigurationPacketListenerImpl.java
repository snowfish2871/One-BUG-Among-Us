package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.network.protocol.configuration.ClientboundUpdateEnabledFeaturesPacket;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientConfigurationPacketListenerImpl extends ClientCommonPacketListenerImpl implements TickablePacketListener, ClientConfigurationPacketListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final GameProfile localGameProfile;
   private RegistryAccess.Frozen receivedRegistries;
   private FeatureFlagSet enabledFeatures;

   public ClientConfigurationPacketListenerImpl(Minecraft p_295262_, Connection p_296339_, CommonListenerCookie p_294706_) {
      super(p_295262_, p_296339_, p_294706_);
      this.localGameProfile = p_294706_.localGameProfile();
      this.receivedRegistries = p_294706_.receivedRegistries();
      this.enabledFeatures = p_294706_.enabledFeatures();
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }

   @Override
   protected RegistryAccess.Frozen registryAccess() {
      return this.receivedRegistries;
   }

   @Override
   protected void handleCustomPayload(net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket p_295727_, CustomPacketPayload p_295411_) {
      this.handleUnknownCustomPayload(p_295727_, p_295411_);
   }

   private void handleUnknownCustomPayload(net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket p_295727_, CustomPacketPayload p_296412_) {
      if (p_296412_ instanceof net.neoforged.neoforge.network.custom.payload.SimplePayload simplePayload && net.neoforged.neoforge.network.NetworkHooks.onCustomPayload(p_295727_, simplePayload, connection)) {
         return;
      }
      
      LOGGER.warn("Unknown custom packet payload: {}", p_296412_.id());
   }

   @Override
   public void handleRegistryData(ClientboundRegistryDataPacket p_295757_) {
      PacketUtils.ensureRunningOnSameThread(p_295757_, this, this.minecraft);
      RegistryAccess.Frozen registryaccess$frozen = ClientRegistryLayer.createRegistryAccess()
         .replaceFrom(ClientRegistryLayer.REMOTE, p_295757_.registryHolder())
         .compositeAccess();
      if (!this.connection.isMemoryConnection()) {
         registryaccess$frozen.registries().forEach(p_296478_ -> p_296478_.value().resetTags());
      }

      this.receivedRegistries = registryaccess$frozen;
   }

   @Override
   public void handleEnabledFeatures(ClientboundUpdateEnabledFeaturesPacket p_294410_) {
      this.enabledFeatures = FeatureFlags.REGISTRY.fromNames(p_294410_.features());
   }

   @Override
   public void handleConfigurationFinished(ClientboundFinishConfigurationPacket p_294585_) {
      this.connection.suspendInboundAfterProtocolChange();
      PacketUtils.ensureRunningOnSameThread(p_294585_, this, this.minecraft);
      this.connection
         .setListener(
            new ClientPacketListener(
               this.minecraft,
               this.connection,
               new CommonListenerCookie(
                  this.localGameProfile,
                  this.telemetryManager,
                  this.receivedRegistries,
                  this.enabledFeatures,
                  this.serverBrand,
                  this.serverData,
                  this.postDisconnectScreen
               )
            )
         );
      this.connection.resumeInboundAfterProtocolChange();
      this.connection.send(new ServerboundFinishConfigurationPacket());
      net.neoforged.neoforge.network.NetworkHooks.handleClientLoginSuccess(this.connection);
   }

   @Override
   public void tick() {
      this.sendDeferredPackets();
   }
}

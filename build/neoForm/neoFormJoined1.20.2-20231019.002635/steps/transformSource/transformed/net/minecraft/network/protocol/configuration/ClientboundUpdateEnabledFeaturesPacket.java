package net.minecraft.network.protocol.configuration;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;

public record ClientboundUpdateEnabledFeaturesPacket(Set<ResourceLocation> features) implements Packet<ClientConfigurationPacketListener> {
   public ClientboundUpdateEnabledFeaturesPacket(FriendlyByteBuf p_295459_) {
      this(p_295459_.<ResourceLocation, Set<ResourceLocation>>readCollection(HashSet::new, FriendlyByteBuf::readResourceLocation));
   }

   @Override
   public void write(FriendlyByteBuf p_294891_) {
      p_294891_.writeCollection(this.features, FriendlyByteBuf::writeResourceLocation);
   }

   public void handle(ClientConfigurationPacketListener p_294170_) {
      p_294170_.handleEnabledFeatures(this);
   }
}

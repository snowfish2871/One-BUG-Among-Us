package net.minecraft.network.protocol.configuration;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientboundFinishConfigurationPacket() implements Packet<ClientConfigurationPacketListener> {
   public ClientboundFinishConfigurationPacket(FriendlyByteBuf p_296427_) {
      this();
   }

   @Override
   public void write(FriendlyByteBuf p_294478_) {
   }

   public void handle(ClientConfigurationPacketListener p_294157_) {
      p_294157_.handleConfigurationFinished(this);
   }

   @Override
   public ConnectionProtocol nextProtocol() {
      return ConnectionProtocol.PLAY;
   }
}

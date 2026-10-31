package net.minecraft.network.protocol.game;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientboundStartConfigurationPacket() implements Packet<ClientGamePacketListener> {
   public ClientboundStartConfigurationPacket(FriendlyByteBuf p_294371_) {
      this();
   }

   @Override
   public void write(FriendlyByteBuf p_295895_) {
   }

   public void handle(ClientGamePacketListener p_294539_) {
      p_294539_.handleConfigurationStart(this);
   }

   @Override
   public ConnectionProtocol nextProtocol() {
      return ConnectionProtocol.CONFIGURATION;
   }
}

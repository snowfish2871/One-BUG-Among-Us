package net.minecraft.network.protocol.game;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ServerboundConfigurationAcknowledgedPacket() implements Packet<ServerGamePacketListener> {
   public ServerboundConfigurationAcknowledgedPacket(FriendlyByteBuf p_294397_) {
      this();
   }

   @Override
   public void write(FriendlyByteBuf p_294932_) {
   }

   public void handle(ServerGamePacketListener p_295823_) {
      p_295823_.handleConfigurationAcknowledged(this);
   }

   @Override
   public ConnectionProtocol nextProtocol() {
      return ConnectionProtocol.CONFIGURATION;
   }
}

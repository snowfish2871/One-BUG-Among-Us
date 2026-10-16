package net.minecraft.network.protocol.configuration;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ServerboundFinishConfigurationPacket() implements Packet<ServerConfigurationPacketListener> {
   public ServerboundFinishConfigurationPacket(FriendlyByteBuf p_294908_) {
      this();
   }

   @Override
   public void write(FriendlyByteBuf p_294657_) {
   }

   public void handle(ServerConfigurationPacketListener p_295379_) {
      p_295379_.handleConfigurationFinished(this);
   }

   @Override
   public ConnectionProtocol nextProtocol() {
      return ConnectionProtocol.PLAY;
   }
}

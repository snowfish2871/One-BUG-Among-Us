package net.minecraft.network.protocol.login;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ServerboundLoginAcknowledgedPacket() implements Packet<ServerLoginPacketListener> {
   public ServerboundLoginAcknowledgedPacket(FriendlyByteBuf p_295418_) {
      this();
   }

   @Override
   public void write(FriendlyByteBuf p_295478_) {
   }

   public void handle(ServerLoginPacketListener p_296298_) {
      p_296298_.handleLoginAcknowledgement(this);
   }

   @Override
   public ConnectionProtocol nextProtocol() {
      return ConnectionProtocol.CONFIGURATION;
   }
}

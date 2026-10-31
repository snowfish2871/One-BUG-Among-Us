package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public class ClientboundDisconnectPacket implements Packet<ClientCommonPacketListener> {
   private final Component reason;

   public ClientboundDisconnectPacket(Component p_295102_) {
      this.reason = p_295102_;
   }

   public ClientboundDisconnectPacket(FriendlyByteBuf p_295309_) {
      this.reason = p_295309_.readComponent();
   }

   @Override
   public void write(FriendlyByteBuf p_295069_) {
      p_295069_.writeComponent(this.reason);
   }

   public void handle(ClientCommonPacketListener p_295529_) {
      p_295529_.handleDisconnect(this);
   }

   public Component getReason() {
      return this.reason;
   }
}

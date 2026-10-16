package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundKeepAlivePacket implements Packet<ServerCommonPacketListener> {
   private final long id;

   public ServerboundKeepAlivePacket(long p_294123_) {
      this.id = p_294123_;
   }

   public void handle(ServerCommonPacketListener p_295223_) {
      p_295223_.handleKeepAlive(this);
   }

   public ServerboundKeepAlivePacket(FriendlyByteBuf p_294566_) {
      this.id = p_294566_.readLong();
   }

   @Override
   public void write(FriendlyByteBuf p_295504_) {
      p_295504_.writeLong(this.id);
   }

   public long getId() {
      return this.id;
   }
}

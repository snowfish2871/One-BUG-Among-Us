package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundPongPacket implements Packet<ServerCommonPacketListener> {
   private final int id;

   public ServerboundPongPacket(int p_295752_) {
      this.id = p_295752_;
   }

   public ServerboundPongPacket(FriendlyByteBuf p_295215_) {
      this.id = p_295215_.readInt();
   }

   @Override
   public void write(FriendlyByteBuf p_295843_) {
      p_295843_.writeInt(this.id);
   }

   public void handle(ServerCommonPacketListener p_295714_) {
      p_295714_.handlePong(this);
   }

   public int getId() {
      return this.id;
   }
}

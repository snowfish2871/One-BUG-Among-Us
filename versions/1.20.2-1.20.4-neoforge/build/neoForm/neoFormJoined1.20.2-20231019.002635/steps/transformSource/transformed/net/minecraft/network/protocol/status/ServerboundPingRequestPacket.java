package net.minecraft.network.protocol.status;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerPingPacketListener;

public class ServerboundPingRequestPacket implements Packet<ServerPingPacketListener> {
   private final long time;

   public ServerboundPingRequestPacket(long p_134991_) {
      this.time = p_134991_;
   }

   public ServerboundPingRequestPacket(FriendlyByteBuf p_179838_) {
      this.time = p_179838_.readLong();
   }

   @Override
   public void write(FriendlyByteBuf p_135000_) {
      p_135000_.writeLong(this.time);
   }

   public void handle(ServerPingPacketListener p_299264_) {
      p_299264_.handlePingRequest(this);
   }

   public long getTime() {
      return this.time;
   }
}

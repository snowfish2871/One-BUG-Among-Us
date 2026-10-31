package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundPingPacket implements Packet<ClientCommonPacketListener> {
   private final int id;

   public ClientboundPingPacket(int p_294778_) {
      this.id = p_294778_;
   }

   public ClientboundPingPacket(FriendlyByteBuf p_294565_) {
      this.id = p_294565_.readInt();
   }

   @Override
   public void write(FriendlyByteBuf p_295570_) {
      p_295570_.writeInt(this.id);
   }

   public void handle(ClientCommonPacketListener p_295189_) {
      p_295189_.handlePing(this);
   }

   public int getId() {
      return this.id;
   }
}

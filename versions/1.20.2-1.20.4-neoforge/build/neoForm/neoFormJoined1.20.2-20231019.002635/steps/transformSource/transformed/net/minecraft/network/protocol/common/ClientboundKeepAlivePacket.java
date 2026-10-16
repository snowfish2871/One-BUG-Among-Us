package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundKeepAlivePacket implements Packet<ClientCommonPacketListener> {
   private final long id;

   public ClientboundKeepAlivePacket(long p_296274_) {
      this.id = p_296274_;
   }

   public ClientboundKeepAlivePacket(FriendlyByteBuf p_296088_) {
      this.id = p_296088_.readLong();
   }

   @Override
   public void write(FriendlyByteBuf p_295294_) {
      p_295294_.writeLong(this.id);
   }

   public void handle(ClientCommonPacketListener p_296005_) {
      p_296005_.handleKeepAlive(this);
   }

   public long getId() {
      return this.id;
   }
}

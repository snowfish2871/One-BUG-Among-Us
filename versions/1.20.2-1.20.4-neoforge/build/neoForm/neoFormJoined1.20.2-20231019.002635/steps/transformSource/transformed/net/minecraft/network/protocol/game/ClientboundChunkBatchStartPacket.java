package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientboundChunkBatchStartPacket() implements Packet<ClientGamePacketListener> {
   public ClientboundChunkBatchStartPacket(FriendlyByteBuf p_295580_) {
      this();
   }

   @Override
   public void write(FriendlyByteBuf p_295744_) {
   }

   public void handle(ClientGamePacketListener p_295685_) {
      p_295685_.handleChunkBatchStart(this);
   }
}

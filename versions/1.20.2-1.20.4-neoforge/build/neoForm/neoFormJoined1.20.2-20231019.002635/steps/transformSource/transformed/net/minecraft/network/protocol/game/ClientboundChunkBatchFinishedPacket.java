package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientboundChunkBatchFinishedPacket(int batchSize) implements Packet<ClientGamePacketListener> {
   public ClientboundChunkBatchFinishedPacket(FriendlyByteBuf p_296389_) {
      this(p_296389_.readVarInt());
   }

   @Override
   public void write(FriendlyByteBuf p_294533_) {
      p_294533_.writeVarInt(this.batchSize);
   }

   public void handle(ClientGamePacketListener p_296221_) {
      p_296221_.handleChunkBatchFinished(this);
   }
}

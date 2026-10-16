package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ServerboundChunkBatchReceivedPacket(float desiredChunksPerTick) implements Packet<ServerGamePacketListener> {
   public ServerboundChunkBatchReceivedPacket(FriendlyByteBuf p_294171_) {
      this(p_294171_.readFloat());
   }

   @Override
   public void write(FriendlyByteBuf p_294492_) {
      p_294492_.writeFloat(this.desiredChunksPerTick);
   }

   public void handle(ServerGamePacketListener p_294926_) {
      p_294926_.handleChunkBatchReceived(this);
   }
}

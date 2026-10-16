package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.ChunkPos;

public record ClientboundForgetLevelChunkPacket(ChunkPos pos) implements Packet<ClientGamePacketListener> {
   public ClientboundForgetLevelChunkPacket(FriendlyByteBuf p_178858_) {
      this(p_178858_.readChunkPos());
   }

   @Override
   public void write(FriendlyByteBuf p_132151_) {
      p_132151_.writeChunkPos(this.pos);
   }

   public void handle(ClientGamePacketListener p_132148_) {
      p_132148_.handleForgetLevelChunk(this);
   }
}

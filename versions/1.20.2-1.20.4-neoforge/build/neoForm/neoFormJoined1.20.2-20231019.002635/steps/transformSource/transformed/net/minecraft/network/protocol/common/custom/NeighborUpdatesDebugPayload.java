package net.minecraft.network.protocol.common.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record NeighborUpdatesDebugPayload(long time, BlockPos pos) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/neighbors_update");

   public NeighborUpdatesDebugPayload(FriendlyByteBuf p_295153_) {
      this(p_295153_.readVarLong(), p_295153_.readBlockPos());
   }

   @Override
   public void write(FriendlyByteBuf p_296340_) {
      p_296340_.writeVarLong(this.time);
      p_296340_.writeBlockPos(this.pos);
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }
}

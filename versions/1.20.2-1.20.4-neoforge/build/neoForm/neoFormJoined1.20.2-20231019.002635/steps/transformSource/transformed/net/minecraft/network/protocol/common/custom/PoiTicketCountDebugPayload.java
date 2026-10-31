package net.minecraft.network.protocol.common.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PoiTicketCountDebugPayload(BlockPos pos, int freeTicketCount) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/poi_ticket_count");

   public PoiTicketCountDebugPayload(FriendlyByteBuf p_295976_) {
      this(p_295976_.readBlockPos(), p_295976_.readInt());
   }

   @Override
   public void write(FriendlyByteBuf p_295476_) {
      p_295476_.writeBlockPos(this.pos);
      p_295476_.writeInt(this.freeTicketCount);
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }
}

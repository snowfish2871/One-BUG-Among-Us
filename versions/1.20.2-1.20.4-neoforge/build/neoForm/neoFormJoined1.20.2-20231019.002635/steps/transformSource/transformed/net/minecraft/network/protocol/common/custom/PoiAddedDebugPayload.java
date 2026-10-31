package net.minecraft.network.protocol.common.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PoiAddedDebugPayload(BlockPos pos, String type, int freeTicketCount) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/poi_added");

   public PoiAddedDebugPayload(FriendlyByteBuf p_294710_) {
      this(p_294710_.readBlockPos(), p_294710_.readUtf(), p_294710_.readInt());
   }

   @Override
   public void write(FriendlyByteBuf p_295635_) {
      p_295635_.writeBlockPos(this.pos);
      p_295635_.writeUtf(this.type);
      p_295635_.writeInt(this.freeTicketCount);
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }
}

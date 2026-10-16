package net.minecraft.network.protocol.common.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PoiRemovedDebugPayload(BlockPos pos) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/poi_removed");

   public PoiRemovedDebugPayload(FriendlyByteBuf p_295381_) {
      this(p_295381_.readBlockPos());
   }

   @Override
   public void write(FriendlyByteBuf p_295470_) {
      p_295470_.writeBlockPos(this.pos);
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }
}

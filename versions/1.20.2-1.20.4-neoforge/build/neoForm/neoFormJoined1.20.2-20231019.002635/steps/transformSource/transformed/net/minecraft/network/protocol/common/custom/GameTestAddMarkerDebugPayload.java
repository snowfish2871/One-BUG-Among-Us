package net.minecraft.network.protocol.common.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record GameTestAddMarkerDebugPayload(BlockPos pos, int color, String text, int durationMs) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/game_test_add_marker");

   public GameTestAddMarkerDebugPayload(FriendlyByteBuf p_294353_) {
      this(p_294353_.readBlockPos(), p_294353_.readInt(), p_294353_.readUtf(), p_294353_.readInt());
   }

   @Override
   public void write(FriendlyByteBuf p_294856_) {
      p_294856_.writeBlockPos(this.pos);
      p_294856_.writeInt(this.color);
      p_294856_.writeUtf(this.text);
      p_294856_.writeInt(this.durationMs);
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }
}

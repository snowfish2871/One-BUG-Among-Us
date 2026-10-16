package net.minecraft.network.protocol.common.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record WorldGenAttemptDebugPayload(BlockPos pos, float scale, float red, float green, float blue, float alpha) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/worldgen_attempt");

   public WorldGenAttemptDebugPayload(FriendlyByteBuf p_295574_) {
      this(p_295574_.readBlockPos(), p_295574_.readFloat(), p_295574_.readFloat(), p_295574_.readFloat(), p_295574_.readFloat(), p_295574_.readFloat());
   }

   @Override
   public void write(FriendlyByteBuf p_295822_) {
      p_295822_.writeBlockPos(this.pos);
      p_295822_.writeFloat(this.scale);
      p_295822_.writeFloat(this.red);
      p_295822_.writeFloat(this.green);
      p_295822_.writeFloat(this.blue);
      p_295822_.writeFloat(this.alpha);
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }
}

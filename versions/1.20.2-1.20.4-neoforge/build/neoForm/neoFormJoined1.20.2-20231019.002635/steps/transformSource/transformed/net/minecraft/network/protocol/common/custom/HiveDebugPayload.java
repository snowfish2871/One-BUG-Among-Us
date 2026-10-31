package net.minecraft.network.protocol.common.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record HiveDebugPayload(HiveDebugPayload.HiveInfo hiveInfo) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/hive");

   public HiveDebugPayload(FriendlyByteBuf p_296486_) {
      this(new HiveDebugPayload.HiveInfo(p_296486_));
   }

   @Override
   public void write(FriendlyByteBuf p_294516_) {
      this.hiveInfo.write(p_294516_);
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }

   public static record HiveInfo(BlockPos pos, String hiveType, int occupantCount, int honeyLevel, boolean sedated) {
      public HiveInfo(FriendlyByteBuf p_295182_) {
         this(p_295182_.readBlockPos(), p_295182_.readUtf(), p_295182_.readInt(), p_295182_.readInt(), p_295182_.readBoolean());
      }

      public void write(FriendlyByteBuf p_295315_) {
         p_295315_.writeBlockPos(this.pos);
         p_295315_.writeUtf(this.hiveType);
         p_295315_.writeInt(this.occupantCount);
         p_295315_.writeInt(this.honeyLevel);
         p_295315_.writeBoolean(this.sedated);
      }
   }
}

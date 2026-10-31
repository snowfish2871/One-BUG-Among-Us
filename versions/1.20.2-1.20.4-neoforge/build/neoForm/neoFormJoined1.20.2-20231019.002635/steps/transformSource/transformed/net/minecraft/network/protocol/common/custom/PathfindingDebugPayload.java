package net.minecraft.network.protocol.common.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.pathfinder.Path;

public record PathfindingDebugPayload(int entityId, Path path, float maxNodeDistance) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/path");

   public PathfindingDebugPayload(FriendlyByteBuf p_296445_) {
      this(p_296445_.readInt(), Path.createFromStream(p_296445_), p_296445_.readFloat());
   }

   @Override
   public void write(FriendlyByteBuf p_295342_) {
      p_295342_.writeInt(this.entityId);
      this.path.writeToStream(p_295342_);
      p_295342_.writeFloat(this.maxNodeDistance);
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }
}

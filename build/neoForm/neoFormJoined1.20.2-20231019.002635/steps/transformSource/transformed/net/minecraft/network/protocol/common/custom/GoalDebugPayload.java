package net.minecraft.network.protocol.common.custom;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record GoalDebugPayload(int entityId, BlockPos pos, List<GoalDebugPayload.DebugGoal> goals) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/goal_selector");

   public GoalDebugPayload(FriendlyByteBuf p_295257_) {
      this(p_295257_.readInt(), p_295257_.readBlockPos(), p_295257_.readList(GoalDebugPayload.DebugGoal::new));
   }

   @Override
   public void write(FriendlyByteBuf p_295543_) {
      p_295543_.writeInt(this.entityId);
      p_295543_.writeBlockPos(this.pos);
      p_295543_.writeCollection(this.goals, (p_294168_, p_295766_) -> p_295766_.write(p_294168_));
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }

   public static record DebugGoal(int priority, boolean isRunning, String name) {
      public DebugGoal(FriendlyByteBuf p_294803_) {
         this(p_294803_.readInt(), p_294803_.readBoolean(), p_294803_.readUtf(255));
      }

      public void write(FriendlyByteBuf p_294258_) {
         p_294258_.writeInt(this.priority);
         p_294258_.writeBoolean(this.isRunning);
         p_294258_.writeUtf(this.name);
      }
   }
}

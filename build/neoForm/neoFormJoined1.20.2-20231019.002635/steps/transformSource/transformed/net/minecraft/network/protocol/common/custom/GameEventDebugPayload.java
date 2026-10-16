package net.minecraft.network.protocol.common.custom;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public record GameEventDebugPayload(ResourceKey<GameEvent> type, Vec3 pos) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/game_event");

   public GameEventDebugPayload(FriendlyByteBuf p_294345_) {
      this(p_294345_.readResourceKey(Registries.GAME_EVENT), p_294345_.readVec3());
   }

   @Override
   public void write(FriendlyByteBuf p_295680_) {
      p_295680_.writeResourceKey(this.type);
      p_295680_.writeVec3(this.pos);
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }
}

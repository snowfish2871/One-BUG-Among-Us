package net.minecraft.network.protocol.common.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.PositionSourceType;

public record GameEventListenerDebugPayload(PositionSource listenerPos, int listenerRange) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/game_event_listeners");

   public GameEventListenerDebugPayload(FriendlyByteBuf p_294634_) {
      this(PositionSourceType.fromNetwork(p_294634_), p_294634_.readVarInt());
   }

   @Override
   public void write(FriendlyByteBuf p_295260_) {
      PositionSourceType.toNetwork(this.listenerPos, p_295260_);
      p_295260_.writeVarInt(this.listenerRange);
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }
}

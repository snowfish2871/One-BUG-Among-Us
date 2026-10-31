package net.minecraft.network.protocol.game;

import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record ClientboundLoginPacket(
   int playerId,
   boolean hardcore,
   Set<ResourceKey<Level>> levels,
   int maxPlayers,
   int chunkRadius,
   int simulationDistance,
   boolean reducedDebugInfo,
   boolean showDeathScreen,
   boolean doLimitedCrafting,
   CommonPlayerSpawnInfo commonPlayerSpawnInfo
) implements Packet<ClientGamePacketListener> {
   public ClientboundLoginPacket(FriendlyByteBuf p_178960_) {
      this(
         p_178960_.readInt(),
         p_178960_.readBoolean(),
         p_178960_.readCollection(Sets::newHashSetWithExpectedSize, p_258210_ -> p_258210_.readResourceKey(Registries.DIMENSION)),
         p_178960_.readVarInt(),
         p_178960_.readVarInt(),
         p_178960_.readVarInt(),
         p_178960_.readBoolean(),
         p_178960_.readBoolean(),
         p_178960_.readBoolean(),
         new CommonPlayerSpawnInfo(p_178960_)
      );
   }

   @Override
   public void write(FriendlyByteBuf p_132400_) {
      p_132400_.writeInt(this.playerId);
      p_132400_.writeBoolean(this.hardcore);
      p_132400_.writeCollection(this.levels, FriendlyByteBuf::writeResourceKey);
      p_132400_.writeVarInt(this.maxPlayers);
      p_132400_.writeVarInt(this.chunkRadius);
      p_132400_.writeVarInt(this.simulationDistance);
      p_132400_.writeBoolean(this.reducedDebugInfo);
      p_132400_.writeBoolean(this.showDeathScreen);
      p_132400_.writeBoolean(this.doLimitedCrafting);
      this.commonPlayerSpawnInfo.write(p_132400_);
   }

   public void handle(ClientGamePacketListener p_132397_) {
      p_132397_.handleLogin(this);
   }
}

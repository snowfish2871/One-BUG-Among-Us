package net.minecraft.network.protocol.game;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public record CommonPlayerSpawnInfo(
   ResourceKey<DimensionType> dimensionType,
   ResourceKey<Level> dimension,
   long seed,
   GameType gameType,
   @Nullable GameType previousGameType,
   boolean isDebug,
   boolean isFlat,
   Optional<GlobalPos> lastDeathLocation,
   int portalCooldown
) {
   public CommonPlayerSpawnInfo(FriendlyByteBuf p_294338_) {
      this(
         p_294338_.readResourceKey(Registries.DIMENSION_TYPE),
         p_294338_.readResourceKey(Registries.DIMENSION),
         p_294338_.readLong(),
         GameType.byId(p_294338_.readByte()),
         GameType.byNullableId(p_294338_.readByte()),
         p_294338_.readBoolean(),
         p_294338_.readBoolean(),
         p_294338_.readOptional(FriendlyByteBuf::readGlobalPos),
         p_294338_.readVarInt()
      );
   }

   public void write(FriendlyByteBuf p_294161_) {
      p_294161_.writeResourceKey(this.dimensionType);
      p_294161_.writeResourceKey(this.dimension);
      p_294161_.writeLong(this.seed);
      p_294161_.writeByte(this.gameType.getId());
      p_294161_.writeByte(GameType.getNullableId(this.previousGameType));
      p_294161_.writeBoolean(this.isDebug);
      p_294161_.writeBoolean(this.isFlat);
      p_294161_.writeOptional(this.lastDeathLocation, FriendlyByteBuf::writeGlobalPos);
      p_294161_.writeVarInt(this.portalCooldown);
   }
}

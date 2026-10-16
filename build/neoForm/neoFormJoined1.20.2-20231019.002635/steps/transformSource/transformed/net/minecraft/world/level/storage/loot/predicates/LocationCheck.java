package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public record LocationCheck(Optional<LocationPredicate> predicate, BlockPos offset) implements LootItemCondition {
   private static final MapCodec<BlockPos> OFFSET_CODEC = RecordCodecBuilder.mapCodec(
      p_298180_ -> p_298180_.group(
               ExtraCodecs.strictOptionalField(Codec.INT, "offsetX", 0).forGetter(Vec3i::getX),
               ExtraCodecs.strictOptionalField(Codec.INT, "offsetY", 0).forGetter(Vec3i::getY),
               ExtraCodecs.strictOptionalField(Codec.INT, "offsetZ", 0).forGetter(Vec3i::getZ)
            )
            .apply(p_298180_, BlockPos::new)
   );
   public static final Codec<LocationCheck> CODEC = RecordCodecBuilder.create(
      p_298182_ -> p_298182_.group(
               ExtraCodecs.strictOptionalField(LocationPredicate.CODEC, "predicate").forGetter(LocationCheck::predicate),
               OFFSET_CODEC.forGetter(LocationCheck::offset)
            )
            .apply(p_298182_, LocationCheck::new)
   );

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.LOCATION_CHECK;
   }

   public boolean test(LootContext p_81731_) {
      Vec3 vec3 = p_81731_.getParamOrNull(LootContextParams.ORIGIN);
      return vec3 != null
         && (
            this.predicate.isEmpty()
               || this.predicate
                  .get()
                  .matches(
                     p_81731_.getLevel(), vec3.x() + (double)this.offset.getX(), vec3.y() + (double)this.offset.getY(), vec3.z() + (double)this.offset.getZ()
                  )
         );
   }

   public static LootItemCondition.Builder checkLocation(LocationPredicate.Builder p_81726_) {
      return () -> new LocationCheck(Optional.of(p_81726_.build()), BlockPos.ZERO);
   }

   public static LootItemCondition.Builder checkLocation(LocationPredicate.Builder p_81728_, BlockPos p_81729_) {
      return () -> new LocationCheck(Optional.of(p_81728_.build()), p_81729_);
   }
}

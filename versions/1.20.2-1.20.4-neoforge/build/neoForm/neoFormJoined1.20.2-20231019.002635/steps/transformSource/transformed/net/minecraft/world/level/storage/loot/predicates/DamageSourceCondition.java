package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.critereon.DamageSourcePredicate;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public record DamageSourceCondition(Optional<DamageSourcePredicate> predicate) implements LootItemCondition {
   public static final Codec<DamageSourceCondition> CODEC = RecordCodecBuilder.create(
      p_298174_ -> p_298174_.group(ExtraCodecs.strictOptionalField(DamageSourcePredicate.CODEC, "predicate").forGetter(DamageSourceCondition::predicate))
            .apply(p_298174_, DamageSourceCondition::new)
   );

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.DAMAGE_SOURCE_PROPERTIES;
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return ImmutableSet.of(LootContextParams.ORIGIN, LootContextParams.DAMAGE_SOURCE);
   }

   public boolean test(LootContext p_81592_) {
      DamageSource damagesource = p_81592_.getParamOrNull(LootContextParams.DAMAGE_SOURCE);
      Vec3 vec3 = p_81592_.getParamOrNull(LootContextParams.ORIGIN);
      if (vec3 != null && damagesource != null) {
         return this.predicate.isEmpty() || this.predicate.get().matches(p_81592_.getLevel(), vec3, damagesource);
      } else {
         return false;
      }
   }

   public static LootItemCondition.Builder hasDamageSource(DamageSourcePredicate.Builder p_81590_) {
      return () -> new DamageSourceCondition(Optional.of(p_81590_.build()));
   }
}

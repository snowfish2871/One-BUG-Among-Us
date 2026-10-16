package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import java.util.List;

public class AnyOfCondition extends CompositeLootItemCondition {
   public static final Codec<AnyOfCondition> CODEC = createCodec(AnyOfCondition::new);

   AnyOfCondition(List<LootItemCondition> p_299191_) {
      super(p_299191_, LootItemConditions.orConditions(p_299191_));
   }

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.ANY_OF;
   }

   public static AnyOfCondition.Builder anyOf(LootItemCondition.Builder... p_286239_) {
      return new AnyOfCondition.Builder(p_286239_);
   }

   public static class Builder extends CompositeLootItemCondition.Builder {
      public Builder(LootItemCondition.Builder... p_286497_) {
         super(p_286497_);
      }

      @Override
      public AnyOfCondition.Builder or(LootItemCondition.Builder p_286344_) {
         this.addTerm(p_286344_);
         return this;
      }

      @Override
      protected LootItemCondition create(List<LootItemCondition> p_298816_) {
         return new AnyOfCondition(p_298816_);
      }
   }
}

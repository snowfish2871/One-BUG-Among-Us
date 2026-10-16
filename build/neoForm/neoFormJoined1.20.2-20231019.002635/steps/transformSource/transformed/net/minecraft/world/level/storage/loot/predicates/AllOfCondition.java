package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import java.util.List;

public class AllOfCondition extends CompositeLootItemCondition {
   public static final Codec<AllOfCondition> CODEC = createCodec(AllOfCondition::new);
   public static final Codec<AllOfCondition> INLINE_CODEC = createInlineCodec(AllOfCondition::new);

   AllOfCondition(List<LootItemCondition> p_299216_) {
      super(p_299216_, LootItemConditions.andConditions(p_299216_));
   }

   public static AllOfCondition allOf(List<LootItemCondition> p_298400_) {
      return new AllOfCondition(List.copyOf(p_298400_));
   }

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.ALL_OF;
   }

   public static AllOfCondition.Builder allOf(LootItemCondition.Builder... p_286873_) {
      return new AllOfCondition.Builder(p_286873_);
   }

   public static class Builder extends CompositeLootItemCondition.Builder {
      public Builder(LootItemCondition.Builder... p_286842_) {
         super(p_286842_);
      }

      @Override
      public AllOfCondition.Builder and(LootItemCondition.Builder p_286760_) {
         this.addTerm(p_286760_);
         return this;
      }

      @Override
      protected LootItemCondition create(List<LootItemCondition> p_298491_) {
         return new AllOfCondition(p_298491_);
      }
   }
}

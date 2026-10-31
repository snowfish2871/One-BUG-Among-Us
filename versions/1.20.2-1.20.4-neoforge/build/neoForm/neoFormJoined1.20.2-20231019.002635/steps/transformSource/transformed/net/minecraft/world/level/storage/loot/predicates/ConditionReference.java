package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.ValidationContext;
import org.slf4j.Logger;

public record ConditionReference(ResourceLocation name) implements LootItemCondition {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<ConditionReference> CODEC = RecordCodecBuilder.create(
      p_298173_ -> p_298173_.group(ResourceLocation.CODEC.fieldOf("name").forGetter(ConditionReference::name)).apply(p_298173_, ConditionReference::new)
   );

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.REFERENCE;
   }

   @Override
   public void validate(ValidationContext p_81560_) {
      LootDataId<LootItemCondition> lootdataid = new LootDataId<>(LootDataType.PREDICATE, this.name);
      if (p_81560_.hasVisitedElement(lootdataid)) {
         p_81560_.reportProblem("Condition " + this.name + " is recursively called");
      } else {
         LootItemCondition.super.validate(p_81560_);
         p_81560_.resolver()
            .getElementOptional(lootdataid)
            .ifPresentOrElse(
               p_279085_ -> p_279085_.validate(p_81560_.enterElement(".{" + this.name + "}", lootdataid)),
               () -> p_81560_.reportProblem("Unknown condition table called " + this.name)
            );
      }
   }

   public boolean test(LootContext p_81558_) {
      LootItemCondition lootitemcondition = p_81558_.getResolver().getElement(LootDataType.PREDICATE, this.name);
      if (lootitemcondition == null) {
         LOGGER.warn("Tried using unknown condition table called {}", this.name);
         return false;
      } else {
         LootContext.VisitedEntry<?> visitedentry = LootContext.createVisitedEntry(lootitemcondition);
         if (p_81558_.pushVisitedElement(visitedentry)) {
            boolean flag;
            try {
               flag = lootitemcondition.test(p_81558_);
            } finally {
               p_81558_.popVisitedElement(visitedentry);
            }

            return flag;
         } else {
            LOGGER.warn("Detected infinite loop in loot tables");
            return false;
         }
      }
   }

   public static LootItemCondition.Builder conditionReference(ResourceLocation p_165481_) {
      return () -> new ConditionReference(p_165481_);
   }
}

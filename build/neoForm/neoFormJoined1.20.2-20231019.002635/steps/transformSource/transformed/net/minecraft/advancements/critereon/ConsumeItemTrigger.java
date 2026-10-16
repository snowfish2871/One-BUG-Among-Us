package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class ConsumeItemTrigger extends SimpleCriterionTrigger<ConsumeItemTrigger.TriggerInstance> {
   public ConsumeItemTrigger.TriggerInstance createInstance(JsonObject p_286724_, Optional<ContextAwarePredicate> p_298695_, DeserializationContext p_286887_) {
      return new ConsumeItemTrigger.TriggerInstance(p_298695_, ItemPredicate.fromJson(p_286724_.get("item")));
   }

   public void trigger(ServerPlayer p_23683_, ItemStack p_23684_) {
      this.trigger(p_23683_, p_23687_ -> p_23687_.matches(p_23684_));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ItemPredicate> item;

      public TriggerInstance(Optional<ContextAwarePredicate> p_299052_, Optional<ItemPredicate> p_298675_) {
         super(p_299052_);
         this.item = p_298675_;
      }

      public static Criterion<ConsumeItemTrigger.TriggerInstance> usedItem() {
         return CriteriaTriggers.CONSUME_ITEM.createCriterion(new ConsumeItemTrigger.TriggerInstance(Optional.empty(), Optional.empty()));
      }

      public static Criterion<ConsumeItemTrigger.TriggerInstance> usedItem(ItemLike p_301200_) {
         return usedItem(ItemPredicate.Builder.item().of(p_301200_.asItem()));
      }

      public static Criterion<ConsumeItemTrigger.TriggerInstance> usedItem(ItemPredicate.Builder p_300855_) {
         return CriteriaTriggers.CONSUME_ITEM.createCriterion(new ConsumeItemTrigger.TriggerInstance(Optional.empty(), Optional.of(p_300855_.build())));
      }

      public boolean matches(ItemStack p_23702_) {
         return this.item.isEmpty() || this.item.get().matches(p_23702_);
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.item.ifPresent(p_298865_ -> jsonobject.add("item", p_298865_.serializeToJson()));
         return jsonobject;
      }
   }
}

package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class FilledBucketTrigger extends SimpleCriterionTrigger<FilledBucketTrigger.TriggerInstance> {
   public FilledBucketTrigger.TriggerInstance createInstance(JsonObject p_286783_, Optional<ContextAwarePredicate> p_298781_, DeserializationContext p_286812_) {
      Optional<ItemPredicate> optional = ItemPredicate.fromJson(p_286783_.get("item"));
      return new FilledBucketTrigger.TriggerInstance(p_298781_, optional);
   }

   public void trigger(ServerPlayer p_38773_, ItemStack p_38774_) {
      this.trigger(p_38773_, p_38777_ -> p_38777_.matches(p_38774_));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ItemPredicate> item;

      public TriggerInstance(Optional<ContextAwarePredicate> p_299006_, Optional<ItemPredicate> p_299027_) {
         super(p_299006_);
         this.item = p_299027_;
      }

      public static Criterion<FilledBucketTrigger.TriggerInstance> filledBucket(ItemPredicate.Builder p_300965_) {
         return CriteriaTriggers.FILLED_BUCKET.createCriterion(new FilledBucketTrigger.TriggerInstance(Optional.empty(), Optional.of(p_300965_.build())));
      }

      public boolean matches(ItemStack p_38792_) {
         return !this.item.isPresent() || this.item.get().matches(p_38792_);
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.item.ifPresent(p_299279_ -> jsonobject.add("item", p_299279_.serializeToJson()));
         return jsonobject;
      }
   }
}

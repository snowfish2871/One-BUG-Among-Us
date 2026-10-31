package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class UsingItemTrigger extends SimpleCriterionTrigger<UsingItemTrigger.TriggerInstance> {
   public UsingItemTrigger.TriggerInstance createInstance(JsonObject p_286642_, Optional<ContextAwarePredicate> p_299230_, DeserializationContext p_286897_) {
      Optional<ItemPredicate> optional = ItemPredicate.fromJson(p_286642_.get("item"));
      return new UsingItemTrigger.TriggerInstance(p_299230_, optional);
   }

   public void trigger(ServerPlayer p_163866_, ItemStack p_163867_) {
      this.trigger(p_163866_, p_163870_ -> p_163870_.matches(p_163867_));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ItemPredicate> item;

      public TriggerInstance(Optional<ContextAwarePredicate> p_298803_, Optional<ItemPredicate> p_298750_) {
         super(p_298803_);
         this.item = p_298750_;
      }

      public static Criterion<UsingItemTrigger.TriggerInstance> lookingAt(EntityPredicate.Builder p_163884_, ItemPredicate.Builder p_163885_) {
         return CriteriaTriggers.USING_ITEM
            .createCriterion(new UsingItemTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(p_163884_)), Optional.of(p_163885_.build())));
      }

      public boolean matches(ItemStack p_163887_) {
         return !this.item.isPresent() || this.item.get().matches(p_163887_);
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.item.ifPresent(p_299087_ -> jsonobject.add("item", p_299087_.serializeToJson()));
         return jsonobject;
      }
   }
}

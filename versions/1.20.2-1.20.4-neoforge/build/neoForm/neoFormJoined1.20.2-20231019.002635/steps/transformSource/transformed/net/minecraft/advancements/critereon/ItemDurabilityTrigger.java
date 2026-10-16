package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ItemDurabilityTrigger extends SimpleCriterionTrigger<ItemDurabilityTrigger.TriggerInstance> {
   public ItemDurabilityTrigger.TriggerInstance createInstance(
      JsonObject p_286693_, Optional<ContextAwarePredicate> p_298839_, DeserializationContext p_286352_
   ) {
      Optional<ItemPredicate> optional = ItemPredicate.fromJson(p_286693_.get("item"));
      MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromJson(p_286693_.get("durability"));
      MinMaxBounds.Ints minmaxbounds$ints1 = MinMaxBounds.Ints.fromJson(p_286693_.get("delta"));
      return new ItemDurabilityTrigger.TriggerInstance(p_298839_, optional, minmaxbounds$ints, minmaxbounds$ints1);
   }

   public void trigger(ServerPlayer p_43670_, ItemStack p_43671_, int p_43672_) {
      this.trigger(p_43670_, p_43676_ -> p_43676_.matches(p_43671_, p_43672_));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ItemPredicate> item;
      private final MinMaxBounds.Ints durability;
      private final MinMaxBounds.Ints delta;

      public TriggerInstance(
         Optional<ContextAwarePredicate> p_298655_, Optional<ItemPredicate> p_298552_, MinMaxBounds.Ints p_286431_, MinMaxBounds.Ints p_286460_
      ) {
         super(p_298655_);
         this.item = p_298552_;
         this.durability = p_286431_;
         this.delta = p_286460_;
      }

      public static Criterion<ItemDurabilityTrigger.TriggerInstance> changedDurability(Optional<ItemPredicate> p_299020_, MinMaxBounds.Ints p_151288_) {
         return changedDurability(Optional.empty(), p_299020_, p_151288_);
      }

      public static Criterion<ItemDurabilityTrigger.TriggerInstance> changedDurability(
         Optional<ContextAwarePredicate> p_299196_, Optional<ItemPredicate> p_299039_, MinMaxBounds.Ints p_286730_
      ) {
         return CriteriaTriggers.ITEM_DURABILITY_CHANGED
            .createCriterion(new ItemDurabilityTrigger.TriggerInstance(p_299196_, p_299039_, p_286730_, MinMaxBounds.Ints.ANY));
      }

      public boolean matches(ItemStack p_43699_, int p_43700_) {
         if (this.item.isPresent() && !this.item.get().matches(p_43699_)) {
            return false;
         } else if (!this.durability.matches(p_43699_.getMaxDamage() - p_43700_)) {
            return false;
         } else {
            return this.delta.matches(p_43699_.getDamageValue() - p_43700_);
         }
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.item.ifPresent(p_298213_ -> jsonobject.add("item", p_298213_.serializeToJson()));
         jsonobject.add("durability", this.durability.serializeToJson());
         jsonobject.add("delta", this.delta.serializeToJson());
         return jsonobject;
      }
   }
}

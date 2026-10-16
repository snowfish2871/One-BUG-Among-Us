package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class EnchantedItemTrigger extends SimpleCriterionTrigger<EnchantedItemTrigger.TriggerInstance> {
   public EnchantedItemTrigger.TriggerInstance createInstance(JsonObject p_286360_, Optional<ContextAwarePredicate> p_298531_, DeserializationContext p_286441_) {
      Optional<ItemPredicate> optional = ItemPredicate.fromJson(p_286360_.get("item"));
      MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromJson(p_286360_.get("levels"));
      return new EnchantedItemTrigger.TriggerInstance(p_298531_, optional, minmaxbounds$ints);
   }

   public void trigger(ServerPlayer p_27669_, ItemStack p_27670_, int p_27671_) {
      this.trigger(p_27669_, p_27675_ -> p_27675_.matches(p_27670_, p_27671_));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ItemPredicate> item;
      private final MinMaxBounds.Ints levels;

      public TriggerInstance(Optional<ContextAwarePredicate> p_298281_, Optional<ItemPredicate> p_299065_, MinMaxBounds.Ints p_286367_) {
         super(p_298281_);
         this.item = p_299065_;
         this.levels = p_286367_;
      }

      public static Criterion<EnchantedItemTrigger.TriggerInstance> enchantedItem() {
         return CriteriaTriggers.ENCHANTED_ITEM
            .createCriterion(new EnchantedItemTrigger.TriggerInstance(Optional.empty(), Optional.empty(), MinMaxBounds.Ints.ANY));
      }

      public boolean matches(ItemStack p_27692_, int p_27693_) {
         if (this.item.isPresent() && !this.item.get().matches(p_27692_)) {
            return false;
         } else {
            return this.levels.matches(p_27693_);
         }
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.item.ifPresent(p_298366_ -> jsonobject.add("item", p_298366_.serializeToJson()));
         jsonobject.add("levels", this.levels.serializeToJson());
         return jsonobject;
      }
   }
}

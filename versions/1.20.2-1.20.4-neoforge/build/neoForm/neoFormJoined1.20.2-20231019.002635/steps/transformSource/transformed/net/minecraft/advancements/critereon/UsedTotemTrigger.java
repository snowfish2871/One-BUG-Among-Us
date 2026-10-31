package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class UsedTotemTrigger extends SimpleCriterionTrigger<UsedTotemTrigger.TriggerInstance> {
   public UsedTotemTrigger.TriggerInstance createInstance(JsonObject p_286841_, Optional<ContextAwarePredicate> p_299247_, DeserializationContext p_286414_) {
      Optional<ItemPredicate> optional = ItemPredicate.fromJson(p_286841_.get("item"));
      return new UsedTotemTrigger.TriggerInstance(p_299247_, optional);
   }

   public void trigger(ServerPlayer p_74432_, ItemStack p_74433_) {
      this.trigger(p_74432_, p_74436_ -> p_74436_.matches(p_74433_));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ItemPredicate> item;

      public TriggerInstance(Optional<ContextAwarePredicate> p_298823_, Optional<ItemPredicate> p_298900_) {
         super(p_298823_);
         this.item = p_298900_;
      }

      public static Criterion<UsedTotemTrigger.TriggerInstance> usedTotem(ItemPredicate p_163725_) {
         return CriteriaTriggers.USED_TOTEM.createCriterion(new UsedTotemTrigger.TriggerInstance(Optional.empty(), Optional.of(p_163725_)));
      }

      public static Criterion<UsedTotemTrigger.TriggerInstance> usedTotem(ItemLike p_74453_) {
         return CriteriaTriggers.USED_TOTEM
            .createCriterion(new UsedTotemTrigger.TriggerInstance(Optional.empty(), Optional.of(ItemPredicate.Builder.item().of(p_74453_).build())));
      }

      public boolean matches(ItemStack p_74451_) {
         return this.item.isEmpty() || this.item.get().matches(p_74451_);
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.item.ifPresent(p_299098_ -> jsonobject.add("item", p_299098_.serializeToJson()));
         return jsonobject;
      }
   }
}

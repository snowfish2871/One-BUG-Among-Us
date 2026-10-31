package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class PickedUpItemTrigger extends SimpleCriterionTrigger<PickedUpItemTrigger.TriggerInstance> {
   protected PickedUpItemTrigger.TriggerInstance createInstance(
      JsonObject p_286503_, Optional<ContextAwarePredicate> p_298630_, DeserializationContext p_286658_
   ) {
      Optional<ItemPredicate> optional = ItemPredicate.fromJson(p_286503_.get("item"));
      Optional<ContextAwarePredicate> optional1 = EntityPredicate.fromJson(p_286503_, "entity", p_286658_);
      return new PickedUpItemTrigger.TriggerInstance(p_298630_, optional, optional1);
   }

   public void trigger(ServerPlayer p_221299_, ItemStack p_221300_, @Nullable Entity p_221301_) {
      LootContext lootcontext = EntityPredicate.createContext(p_221299_, p_221301_);
      this.trigger(p_221299_, p_221306_ -> p_221306_.matches(p_221299_, p_221300_, lootcontext));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ItemPredicate> item;
      private final Optional<ContextAwarePredicate> entity;

      public TriggerInstance(Optional<ContextAwarePredicate> p_298259_, Optional<ItemPredicate> p_298561_, Optional<ContextAwarePredicate> p_298652_) {
         super(p_298259_);
         this.item = p_298561_;
         this.entity = p_298652_;
      }

      public static Criterion<PickedUpItemTrigger.TriggerInstance> thrownItemPickedUpByEntity(
         ContextAwarePredicate p_286865_, Optional<ItemPredicate> p_299099_, Optional<ContextAwarePredicate> p_299117_
      ) {
         return CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_ENTITY
            .createCriterion(new PickedUpItemTrigger.TriggerInstance(Optional.of(p_286865_), p_299099_, p_299117_));
      }

      public static Criterion<PickedUpItemTrigger.TriggerInstance> thrownItemPickedUpByPlayer(
         Optional<ContextAwarePredicate> p_298917_, Optional<ItemPredicate> p_298224_, Optional<ContextAwarePredicate> p_298405_
      ) {
         return CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_PLAYER.createCriterion(new PickedUpItemTrigger.TriggerInstance(p_298917_, p_298224_, p_298405_));
      }

      public boolean matches(ServerPlayer p_221323_, ItemStack p_221324_, LootContext p_221325_) {
         if (this.item.isPresent() && !this.item.get().matches(p_221324_)) {
            return false;
         } else {
            return !this.entity.isPresent() || this.entity.get().matches(p_221325_);
         }
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.item.ifPresent(p_298370_ -> jsonobject.add("item", p_298370_.serializeToJson()));
         this.entity.ifPresent(p_298722_ -> jsonobject.add("entity", p_298722_.toJson()));
         return jsonobject;
      }
   }
}

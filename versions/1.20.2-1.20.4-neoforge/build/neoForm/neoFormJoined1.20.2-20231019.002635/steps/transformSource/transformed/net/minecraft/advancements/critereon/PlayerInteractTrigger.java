package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class PlayerInteractTrigger extends SimpleCriterionTrigger<PlayerInteractTrigger.TriggerInstance> {
   protected PlayerInteractTrigger.TriggerInstance createInstance(
      JsonObject p_286758_, Optional<ContextAwarePredicate> p_298524_, DeserializationContext p_286859_
   ) {
      Optional<ItemPredicate> optional = ItemPredicate.fromJson(p_286758_.get("item"));
      Optional<ContextAwarePredicate> optional1 = EntityPredicate.fromJson(p_286758_, "entity", p_286859_);
      return new PlayerInteractTrigger.TriggerInstance(p_298524_, optional, optional1);
   }

   public void trigger(ServerPlayer p_61495_, ItemStack p_61496_, Entity p_61497_) {
      LootContext lootcontext = EntityPredicate.createContext(p_61495_, p_61497_);
      this.trigger(p_61495_, p_61501_ -> p_61501_.matches(p_61496_, lootcontext));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ItemPredicate> item;
      private final Optional<ContextAwarePredicate> entity;

      public TriggerInstance(Optional<ContextAwarePredicate> p_298364_, Optional<ItemPredicate> p_298687_, Optional<ContextAwarePredicate> p_298827_) {
         super(p_298364_);
         this.item = p_298687_;
         this.entity = p_298827_;
      }

      public static Criterion<PlayerInteractTrigger.TriggerInstance> itemUsedOnEntity(
         Optional<ContextAwarePredicate> p_299056_, ItemPredicate.Builder p_286289_, Optional<ContextAwarePredicate> p_301218_
      ) {
         return CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY
            .createCriterion(new PlayerInteractTrigger.TriggerInstance(p_299056_, Optional.of(p_286289_.build()), p_301218_));
      }

      public static Criterion<PlayerInteractTrigger.TriggerInstance> itemUsedOnEntity(
         ItemPredicate.Builder p_286235_, Optional<ContextAwarePredicate> p_298893_
      ) {
         return itemUsedOnEntity(Optional.empty(), p_286235_, p_298893_);
      }

      public boolean matches(ItemStack p_61522_, LootContext p_61523_) {
         if (this.item.isPresent() && !this.item.get().matches(p_61522_)) {
            return false;
         } else {
            return this.entity.isEmpty() || this.entity.get().matches(p_61523_);
         }
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.item.ifPresent(p_298198_ -> jsonobject.add("item", p_298198_.serializeToJson()));
         this.entity.ifPresent(p_298319_ -> jsonobject.add("entity", p_298319_.toJson()));
         return jsonobject;
      }
   }
}

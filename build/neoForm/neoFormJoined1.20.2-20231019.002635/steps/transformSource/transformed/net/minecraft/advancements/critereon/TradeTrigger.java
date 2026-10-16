package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class TradeTrigger extends SimpleCriterionTrigger<TradeTrigger.TriggerInstance> {
   public TradeTrigger.TriggerInstance createInstance(JsonObject p_286317_, Optional<ContextAwarePredicate> p_299121_, DeserializationContext p_286251_) {
      Optional<ContextAwarePredicate> optional = EntityPredicate.fromJson(p_286317_, "villager", p_286251_);
      Optional<ItemPredicate> optional1 = ItemPredicate.fromJson(p_286317_.get("item"));
      return new TradeTrigger.TriggerInstance(p_299121_, optional, optional1);
   }

   public void trigger(ServerPlayer p_70960_, AbstractVillager p_70961_, ItemStack p_70962_) {
      LootContext lootcontext = EntityPredicate.createContext(p_70960_, p_70961_);
      this.trigger(p_70960_, p_70970_ -> p_70970_.matches(lootcontext, p_70962_));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ContextAwarePredicate> villager;
      private final Optional<ItemPredicate> item;

      public TriggerInstance(Optional<ContextAwarePredicate> p_298800_, Optional<ContextAwarePredicate> p_299138_, Optional<ItemPredicate> p_298795_) {
         super(p_298800_);
         this.villager = p_299138_;
         this.item = p_298795_;
      }

      public static Criterion<TradeTrigger.TriggerInstance> tradedWithVillager() {
         return CriteriaTriggers.TRADE.createCriterion(new TradeTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<TradeTrigger.TriggerInstance> tradedWithVillager(EntityPredicate.Builder p_191437_) {
         return CriteriaTriggers.TRADE
            .createCriterion(new TradeTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(p_191437_)), Optional.empty(), Optional.empty()));
      }

      public boolean matches(LootContext p_70985_, ItemStack p_70986_) {
         if (this.villager.isPresent() && !this.villager.get().matches(p_70985_)) {
            return false;
         } else {
            return !this.item.isPresent() || this.item.get().matches(p_70986_);
         }
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.item.ifPresent(p_298503_ -> jsonobject.add("item", p_298503_.serializeToJson()));
         this.villager.ifPresent(p_299018_ -> jsonobject.add("villager", p_299018_.toJson()));
         return jsonobject;
      }
   }
}

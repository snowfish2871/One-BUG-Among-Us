package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.storage.loot.LootContext;

public class LightningStrikeTrigger extends SimpleCriterionTrigger<LightningStrikeTrigger.TriggerInstance> {
   public LightningStrikeTrigger.TriggerInstance createInstance(
      JsonObject p_286546_, Optional<ContextAwarePredicate> p_299074_, DeserializationContext p_286393_
   ) {
      Optional<ContextAwarePredicate> optional = EntityPredicate.fromJson(p_286546_, "lightning", p_286393_);
      Optional<ContextAwarePredicate> optional1 = EntityPredicate.fromJson(p_286546_, "bystander", p_286393_);
      return new LightningStrikeTrigger.TriggerInstance(p_299074_, optional, optional1);
   }

   public void trigger(ServerPlayer p_153392_, LightningBolt p_153393_, List<Entity> p_153394_) {
      List<LootContext> list = p_153394_.stream().map(p_153390_ -> EntityPredicate.createContext(p_153392_, p_153390_)).collect(Collectors.toList());
      LootContext lootcontext = EntityPredicate.createContext(p_153392_, p_153393_);
      this.trigger(p_153392_, p_153402_ -> p_153402_.matches(lootcontext, list));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<ContextAwarePredicate> lightning;
      private final Optional<ContextAwarePredicate> bystander;

      public TriggerInstance(Optional<ContextAwarePredicate> p_298707_, Optional<ContextAwarePredicate> p_299016_, Optional<ContextAwarePredicate> p_298635_) {
         super(p_298707_);
         this.lightning = p_299016_;
         this.bystander = p_298635_;
      }

      public static Criterion<LightningStrikeTrigger.TriggerInstance> lightningStrike(Optional<EntityPredicate> p_301078_, Optional<EntityPredicate> p_301001_) {
         return CriteriaTriggers.LIGHTNING_STRIKE
            .createCriterion(new LightningStrikeTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(p_301078_), EntityPredicate.wrap(p_301001_)));
      }

      public boolean matches(LootContext p_153419_, List<LootContext> p_153420_) {
         if (this.lightning.isPresent() && !this.lightning.get().matches(p_153419_)) {
            return false;
         } else {
            return !this.bystander.isPresent() || !p_153420_.stream().noneMatch(this.bystander.get()::matches);
         }
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.lightning.ifPresent(p_298203_ -> jsonobject.add("lightning", p_298203_.toJson()));
         this.bystander.ifPresent(p_299232_ -> jsonobject.add("bystander", p_299232_.toJson()));
         return jsonobject;
      }
   }
}

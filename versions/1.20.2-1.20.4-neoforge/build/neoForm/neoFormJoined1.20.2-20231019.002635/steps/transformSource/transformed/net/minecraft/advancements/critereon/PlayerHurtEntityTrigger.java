package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class PlayerHurtEntityTrigger extends SimpleCriterionTrigger<PlayerHurtEntityTrigger.TriggerInstance> {
   public PlayerHurtEntityTrigger.TriggerInstance createInstance(
      JsonObject p_286442_, Optional<ContextAwarePredicate> p_298694_, DeserializationContext p_286750_
   ) {
      Optional<DamagePredicate> optional = DamagePredicate.fromJson(p_286442_.get("damage"));
      Optional<ContextAwarePredicate> optional1 = EntityPredicate.fromJson(p_286442_, "entity", p_286750_);
      return new PlayerHurtEntityTrigger.TriggerInstance(p_298694_, optional, optional1);
   }

   public void trigger(ServerPlayer p_60113_, Entity p_60114_, DamageSource p_60115_, float p_60116_, float p_60117_, boolean p_60118_) {
      LootContext lootcontext = EntityPredicate.createContext(p_60113_, p_60114_);
      this.trigger(p_60113_, p_60126_ -> p_60126_.matches(p_60113_, lootcontext, p_60115_, p_60116_, p_60117_, p_60118_));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<DamagePredicate> damage;
      private final Optional<ContextAwarePredicate> entity;

      public TriggerInstance(Optional<ContextAwarePredicate> p_299157_, Optional<DamagePredicate> p_298533_, Optional<ContextAwarePredicate> p_298308_) {
         super(p_299157_);
         this.damage = p_298533_;
         this.entity = p_298308_;
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntity() {
         return CriteriaTriggers.PLAYER_HURT_ENTITY
            .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntityWithDamage(Optional<DamagePredicate> p_300880_) {
         return CriteriaTriggers.PLAYER_HURT_ENTITY.createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), p_300880_, Optional.empty()));
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntityWithDamage(DamagePredicate.Builder p_300924_) {
         return CriteriaTriggers.PLAYER_HURT_ENTITY
            .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), Optional.of(p_300924_.build()), Optional.empty()));
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntity(Optional<EntityPredicate> p_298252_) {
         return CriteriaTriggers.PLAYER_HURT_ENTITY
            .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), Optional.empty(), EntityPredicate.wrap(p_298252_)));
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntity(
         Optional<DamagePredicate> p_301287_, Optional<EntityPredicate> p_300951_
      ) {
         return CriteriaTriggers.PLAYER_HURT_ENTITY
            .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), p_301287_, EntityPredicate.wrap(p_300951_)));
      }

      public static Criterion<PlayerHurtEntityTrigger.TriggerInstance> playerHurtEntity(DamagePredicate.Builder p_301281_, Optional<EntityPredicate> p_298362_) {
         return CriteriaTriggers.PLAYER_HURT_ENTITY
            .createCriterion(new PlayerHurtEntityTrigger.TriggerInstance(Optional.empty(), Optional.of(p_301281_.build()), EntityPredicate.wrap(p_298362_)));
      }

      public boolean matches(ServerPlayer p_60143_, LootContext p_60144_, DamageSource p_60145_, float p_60146_, float p_60147_, boolean p_60148_) {
         if (this.damage.isPresent() && !this.damage.get().matches(p_60143_, p_60145_, p_60146_, p_60147_, p_60148_)) {
            return false;
         } else {
            return !this.entity.isPresent() || this.entity.get().matches(p_60144_);
         }
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.damage.ifPresent(p_298419_ -> jsonobject.add("damage", p_298419_.serializeToJson()));
         this.entity.ifPresent(p_298603_ -> jsonobject.add("entity", p_298603_.toJson()));
         return jsonobject;
      }
   }
}

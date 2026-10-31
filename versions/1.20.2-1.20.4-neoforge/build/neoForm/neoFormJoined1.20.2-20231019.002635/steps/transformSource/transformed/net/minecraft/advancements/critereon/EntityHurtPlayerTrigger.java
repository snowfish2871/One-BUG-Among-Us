package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public class EntityHurtPlayerTrigger extends SimpleCriterionTrigger<EntityHurtPlayerTrigger.TriggerInstance> {
   public EntityHurtPlayerTrigger.TriggerInstance createInstance(
      JsonObject p_286430_, Optional<ContextAwarePredicate> p_298313_, DeserializationContext p_286648_
   ) {
      Optional<DamagePredicate> optional = DamagePredicate.fromJson(p_286430_.get("damage"));
      return new EntityHurtPlayerTrigger.TriggerInstance(p_298313_, optional);
   }

   public void trigger(ServerPlayer p_35175_, DamageSource p_35176_, float p_35177_, float p_35178_, boolean p_35179_) {
      this.trigger(p_35175_, p_35186_ -> p_35186_.matches(p_35175_, p_35176_, p_35177_, p_35178_, p_35179_));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<DamagePredicate> damage;

      public TriggerInstance(Optional<ContextAwarePredicate> p_298990_, Optional<DamagePredicate> p_299150_) {
         super(p_298990_);
         this.damage = p_299150_;
      }

      public static Criterion<EntityHurtPlayerTrigger.TriggerInstance> entityHurtPlayer() {
         return CriteriaTriggers.ENTITY_HURT_PLAYER.createCriterion(new EntityHurtPlayerTrigger.TriggerInstance(Optional.empty(), Optional.empty()));
      }

      public static Criterion<EntityHurtPlayerTrigger.TriggerInstance> entityHurtPlayer(DamagePredicate p_300967_) {
         return CriteriaTriggers.ENTITY_HURT_PLAYER.createCriterion(new EntityHurtPlayerTrigger.TriggerInstance(Optional.empty(), Optional.of(p_300967_)));
      }

      public static Criterion<EntityHurtPlayerTrigger.TriggerInstance> entityHurtPlayer(DamagePredicate.Builder p_301174_) {
         return CriteriaTriggers.ENTITY_HURT_PLAYER
            .createCriterion(new EntityHurtPlayerTrigger.TriggerInstance(Optional.empty(), Optional.of(p_301174_.build())));
      }

      public boolean matches(ServerPlayer p_35201_, DamageSource p_35202_, float p_35203_, float p_35204_, boolean p_35205_) {
         return !this.damage.isPresent() || this.damage.get().matches(p_35201_, p_35202_, p_35203_, p_35204_, p_35205_);
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.damage.ifPresent(p_298904_ -> jsonobject.add("damage", p_298904_.serializeToJson()));
         return jsonobject;
      }
   }
}

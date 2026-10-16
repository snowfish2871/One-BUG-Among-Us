package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.Vec3;

public class TargetBlockTrigger extends SimpleCriterionTrigger<TargetBlockTrigger.TriggerInstance> {
   public TargetBlockTrigger.TriggerInstance createInstance(JsonObject p_286796_, Optional<ContextAwarePredicate> p_299244_, DeserializationContext p_286418_) {
      MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromJson(p_286796_.get("signal_strength"));
      Optional<ContextAwarePredicate> optional = EntityPredicate.fromJson(p_286796_, "projectile", p_286418_);
      return new TargetBlockTrigger.TriggerInstance(p_299244_, minmaxbounds$ints, optional);
   }

   public void trigger(ServerPlayer p_70212_, Entity p_70213_, Vec3 p_70214_, int p_70215_) {
      LootContext lootcontext = EntityPredicate.createContext(p_70212_, p_70213_);
      this.trigger(p_70212_, p_70224_ -> p_70224_.matches(lootcontext, p_70214_, p_70215_));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final MinMaxBounds.Ints signalStrength;
      private final Optional<ContextAwarePredicate> projectile;

      public TriggerInstance(Optional<ContextAwarePredicate> p_299077_, MinMaxBounds.Ints p_286505_, Optional<ContextAwarePredicate> p_299328_) {
         super(p_299077_);
         this.signalStrength = p_286505_;
         this.projectile = p_299328_;
      }

      public static Criterion<TargetBlockTrigger.TriggerInstance> targetHit(MinMaxBounds.Ints p_286700_, Optional<ContextAwarePredicate> p_298563_) {
         return CriteriaTriggers.TARGET_BLOCK_HIT.createCriterion(new TargetBlockTrigger.TriggerInstance(Optional.empty(), p_286700_, p_298563_));
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         jsonobject.add("signal_strength", this.signalStrength.serializeToJson());
         this.projectile.ifPresent(p_298984_ -> jsonobject.add("projectile", p_298984_.toJson()));
         return jsonobject;
      }

      public boolean matches(LootContext p_70242_, Vec3 p_70243_, int p_70244_) {
         if (!this.signalStrength.matches(p_70244_)) {
            return false;
         } else {
            return !this.projectile.isPresent() || this.projectile.get().matches(p_70242_);
         }
      }
   }
}

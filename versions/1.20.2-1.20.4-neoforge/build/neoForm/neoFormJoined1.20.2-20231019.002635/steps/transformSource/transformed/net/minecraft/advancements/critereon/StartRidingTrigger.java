package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;

public class StartRidingTrigger extends SimpleCriterionTrigger<StartRidingTrigger.TriggerInstance> {
   public StartRidingTrigger.TriggerInstance createInstance(JsonObject p_286869_, Optional<ContextAwarePredicate> p_298367_, DeserializationContext p_286559_) {
      return new StartRidingTrigger.TriggerInstance(p_298367_);
   }

   public void trigger(ServerPlayer p_160388_) {
      this.trigger(p_160388_, p_160394_ -> true);
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      public TriggerInstance(Optional<ContextAwarePredicate> p_299165_) {
         super(p_299165_);
      }

      public static Criterion<StartRidingTrigger.TriggerInstance> playerStartsRiding(EntityPredicate.Builder p_160402_) {
         return CriteriaTriggers.START_RIDING_TRIGGER.createCriterion(new StartRidingTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(p_160402_))));
      }
   }
}

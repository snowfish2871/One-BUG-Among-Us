package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;

public class ConstructBeaconTrigger extends SimpleCriterionTrigger<ConstructBeaconTrigger.TriggerInstance> {
   public ConstructBeaconTrigger.TriggerInstance createInstance(
      JsonObject p_286576_, Optional<ContextAwarePredicate> p_298375_, DeserializationContext p_286224_
   ) {
      MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromJson(p_286576_.get("level"));
      return new ConstructBeaconTrigger.TriggerInstance(p_298375_, minmaxbounds$ints);
   }

   public void trigger(ServerPlayer p_148030_, int p_148031_) {
      this.trigger(p_148030_, p_148028_ -> p_148028_.matches(p_148031_));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final MinMaxBounds.Ints level;

      public TriggerInstance(Optional<ContextAwarePredicate> p_298591_, MinMaxBounds.Ints p_286272_) {
         super(p_298591_);
         this.level = p_286272_;
      }

      public static Criterion<ConstructBeaconTrigger.TriggerInstance> constructedBeacon() {
         return CriteriaTriggers.CONSTRUCT_BEACON.createCriterion(new ConstructBeaconTrigger.TriggerInstance(Optional.empty(), MinMaxBounds.Ints.ANY));
      }

      public static Criterion<ConstructBeaconTrigger.TriggerInstance> constructedBeacon(MinMaxBounds.Ints p_301138_) {
         return CriteriaTriggers.CONSTRUCT_BEACON.createCriterion(new ConstructBeaconTrigger.TriggerInstance(Optional.empty(), p_301138_));
      }

      public boolean matches(int p_148033_) {
         return this.level.matches(p_148033_);
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         jsonobject.add("level", this.level.serializeToJson());
         return jsonobject;
      }
   }
}

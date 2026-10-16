package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class LevitationTrigger extends SimpleCriterionTrigger<LevitationTrigger.TriggerInstance> {
   public LevitationTrigger.TriggerInstance createInstance(JsonObject p_286359_, Optional<ContextAwarePredicate> p_298830_, DeserializationContext p_286241_) {
      Optional<DistancePredicate> optional = DistancePredicate.fromJson(p_286359_.get("distance"));
      MinMaxBounds.Ints minmaxbounds$ints = MinMaxBounds.Ints.fromJson(p_286359_.get("duration"));
      return new LevitationTrigger.TriggerInstance(p_298830_, optional, minmaxbounds$ints);
   }

   public void trigger(ServerPlayer p_49117_, Vec3 p_49118_, int p_49119_) {
      this.trigger(p_49117_, p_49124_ -> p_49124_.matches(p_49117_, p_49118_, p_49119_));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final Optional<DistancePredicate> distance;
      private final MinMaxBounds.Ints duration;

      public TriggerInstance(Optional<ContextAwarePredicate> p_299278_, Optional<DistancePredicate> p_299004_, MinMaxBounds.Ints p_286676_) {
         super(p_299278_);
         this.distance = p_299004_;
         this.duration = p_286676_;
      }

      public static Criterion<LevitationTrigger.TriggerInstance> levitated(DistancePredicate p_49145_) {
         return CriteriaTriggers.LEVITATION
            .createCriterion(new LevitationTrigger.TriggerInstance(Optional.empty(), Optional.of(p_49145_), MinMaxBounds.Ints.ANY));
      }

      public boolean matches(ServerPlayer p_49141_, Vec3 p_49142_, int p_49143_) {
         if (this.distance.isPresent() && !this.distance.get().matches(p_49142_.x, p_49142_.y, p_49142_.z, p_49141_.getX(), p_49141_.getY(), p_49141_.getZ())) {
            return false;
         } else {
            return this.duration.matches(p_49143_);
         }
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         this.distance.ifPresent(p_298982_ -> jsonobject.add("distance", p_298982_.serializeToJson()));
         jsonobject.add("duration", this.duration.serializeToJson());
         return jsonobject;
      }
   }
}

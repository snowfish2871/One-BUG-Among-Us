package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;

public abstract class AbstractCriterionTriggerInstance implements SimpleCriterionTrigger.SimpleInstance {
   private final Optional<ContextAwarePredicate> player;

   public AbstractCriterionTriggerInstance(Optional<ContextAwarePredicate> p_298258_) {
      this.player = p_298258_;
   }

   @Override
   public Optional<ContextAwarePredicate> playerPredicate() {
      return this.player;
   }

   @Override
   public JsonObject serializeToJson() {
      JsonObject jsonobject = new JsonObject();
      this.player.ifPresent(p_299322_ -> jsonobject.add("player", p_299322_.toJson()));
      return jsonobject;
   }
}

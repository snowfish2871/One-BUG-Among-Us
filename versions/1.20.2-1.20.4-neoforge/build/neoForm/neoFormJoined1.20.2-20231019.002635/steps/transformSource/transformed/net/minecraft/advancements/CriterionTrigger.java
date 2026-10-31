package net.minecraft.advancements;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.server.PlayerAdvancements;

public interface CriterionTrigger<T extends CriterionTriggerInstance> {
   void addPlayerListener(PlayerAdvancements p_13674_, CriterionTrigger.Listener<T> p_13675_);

   void removePlayerListener(PlayerAdvancements p_13676_, CriterionTrigger.Listener<T> p_13677_);

   void removePlayerListeners(PlayerAdvancements p_13673_);

   T createInstance(JsonObject p_13671_, DeserializationContext p_13672_);

   default Criterion<T> createCriterion(T p_301092_) {
      return new Criterion<>(this, p_301092_);
   }

   public static record Listener<T extends CriterionTriggerInstance>(T trigger, AdvancementHolder advancement, String criterion) {
      public void run(PlayerAdvancements p_13687_) {
         p_13687_.award(this.advancement, this.criterion);
      }
   }
}

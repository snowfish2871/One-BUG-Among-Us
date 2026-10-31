package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

public class LootTableTrigger extends SimpleCriterionTrigger<LootTableTrigger.TriggerInstance> {
   protected LootTableTrigger.TriggerInstance createInstance(JsonObject p_286915_, Optional<ContextAwarePredicate> p_298881_, DeserializationContext p_286891_) {
      ResourceLocation resourcelocation = new ResourceLocation(GsonHelper.getAsString(p_286915_, "loot_table"));
      return new LootTableTrigger.TriggerInstance(p_298881_, resourcelocation);
   }

   public void trigger(ServerPlayer p_54598_, ResourceLocation p_54599_) {
      this.trigger(p_54598_, p_54606_ -> p_54606_.matches(p_54599_));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final ResourceLocation lootTable;

      public TriggerInstance(Optional<ContextAwarePredicate> p_298850_, ResourceLocation p_286434_) {
         super(p_298850_);
         this.lootTable = p_286434_;
      }

      public static Criterion<LootTableTrigger.TriggerInstance> lootTableUsed(ResourceLocation p_54619_) {
         return CriteriaTriggers.GENERATE_LOOT.createCriterion(new LootTableTrigger.TriggerInstance(Optional.empty(), p_54619_));
      }

      public boolean matches(ResourceLocation p_54621_) {
         return this.lootTable.equals(p_54621_);
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         jsonobject.addProperty("loot_table", this.lootTable.toString());
         return jsonobject;
      }
   }
}

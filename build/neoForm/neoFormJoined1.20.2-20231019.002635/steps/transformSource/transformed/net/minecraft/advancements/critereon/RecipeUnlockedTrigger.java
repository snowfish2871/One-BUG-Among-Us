package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeHolder;

public class RecipeUnlockedTrigger extends SimpleCriterionTrigger<RecipeUnlockedTrigger.TriggerInstance> {
   public RecipeUnlockedTrigger.TriggerInstance createInstance(
      JsonObject p_286387_, Optional<ContextAwarePredicate> p_298778_, DeserializationContext p_286649_
   ) {
      ResourceLocation resourcelocation = new ResourceLocation(GsonHelper.getAsString(p_286387_, "recipe"));
      return new RecipeUnlockedTrigger.TriggerInstance(p_298778_, resourcelocation);
   }

   public void trigger(ServerPlayer p_63719_, RecipeHolder<?> p_301160_) {
      this.trigger(p_63719_, p_300666_ -> p_300666_.matches(p_301160_));
   }

   public static Criterion<RecipeUnlockedTrigger.TriggerInstance> unlocked(ResourceLocation p_63729_) {
      return CriteriaTriggers.RECIPE_UNLOCKED.createCriterion(new RecipeUnlockedTrigger.TriggerInstance(Optional.empty(), p_63729_));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final ResourceLocation recipe;

      public TriggerInstance(Optional<ContextAwarePredicate> p_298891_, ResourceLocation p_286775_) {
         super(p_298891_);
         this.recipe = p_286775_;
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         jsonobject.addProperty("recipe", this.recipe.toString());
         return jsonobject;
      }

      public boolean matches(RecipeHolder<?> p_301295_) {
         return this.recipe.equals(p_301295_.id());
      }
   }
}

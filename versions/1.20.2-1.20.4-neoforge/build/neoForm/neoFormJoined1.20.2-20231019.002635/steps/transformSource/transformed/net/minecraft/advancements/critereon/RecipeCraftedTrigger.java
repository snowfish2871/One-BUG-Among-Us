package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

public class RecipeCraftedTrigger extends SimpleCriterionTrigger<RecipeCraftedTrigger.TriggerInstance> {
   protected RecipeCraftedTrigger.TriggerInstance createInstance(
      JsonObject p_286751_, Optional<ContextAwarePredicate> p_298438_, DeserializationContext p_286668_
   ) {
      ResourceLocation resourcelocation = new ResourceLocation(GsonHelper.getAsString(p_286751_, "recipe_id"));
      List<ItemPredicate> list = ItemPredicate.fromJsonArray(p_286751_.get("ingredients"));
      return new RecipeCraftedTrigger.TriggerInstance(p_298438_, resourcelocation, list);
   }

   public void trigger(ServerPlayer p_281468_, ResourceLocation p_282903_, List<ItemStack> p_282070_) {
      this.trigger(p_281468_, p_282798_ -> p_282798_.matches(p_282903_, p_282070_));
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final ResourceLocation recipeId;
      private final List<ItemPredicate> predicates;

      public TriggerInstance(Optional<ContextAwarePredicate> p_298295_, ResourceLocation p_286906_, List<ItemPredicate> p_286302_) {
         super(p_298295_);
         this.recipeId = p_286906_;
         this.predicates = p_286302_;
      }

      public static Criterion<RecipeCraftedTrigger.TriggerInstance> craftedItem(ResourceLocation p_282794_, List<ItemPredicate.Builder> p_281369_) {
         return CriteriaTriggers.RECIPE_CRAFTED
            .createCriterion(
               new RecipeCraftedTrigger.TriggerInstance(Optional.empty(), p_282794_, p_281369_.stream().map(ItemPredicate.Builder::build).toList())
            );
      }

      public static Criterion<RecipeCraftedTrigger.TriggerInstance> craftedItem(ResourceLocation p_283538_) {
         return CriteriaTriggers.RECIPE_CRAFTED.createCriterion(new RecipeCraftedTrigger.TriggerInstance(Optional.empty(), p_283538_, List.of()));
      }

      boolean matches(ResourceLocation p_283528_, List<ItemStack> p_283698_) {
         if (!p_283528_.equals(this.recipeId)) {
            return false;
         } else {
            List<ItemStack> list = new ArrayList<>(p_283698_);

            for(ItemPredicate itempredicate : this.predicates) {
               boolean flag = false;
               Iterator<ItemStack> iterator = list.iterator();

               while(iterator.hasNext()) {
                  if (itempredicate.matches(iterator.next())) {
                     iterator.remove();
                     flag = true;
                     break;
                  }
               }

               if (!flag) {
                  return false;
               }
            }

            return true;
         }
      }

      @Override
      public JsonObject serializeToJson() {
         JsonObject jsonobject = super.serializeToJson();
         jsonobject.addProperty("recipe_id", this.recipeId.toString());
         if (!this.predicates.isEmpty()) {
            jsonobject.add("ingredients", ItemPredicate.serializeToJsonArray(this.predicates));
         }

         return jsonobject;
      }
   }
}

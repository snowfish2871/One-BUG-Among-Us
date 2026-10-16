package net.minecraft.data.recipes;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class SmithingTransformRecipeBuilder {
   private final Ingredient template;
   private final Ingredient base;
   private final Ingredient addition;
   private final RecipeCategory category;
   private final Item result;
   private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
   private final RecipeSerializer<?> type;

   public SmithingTransformRecipeBuilder(
      RecipeSerializer<?> p_266683_, Ingredient p_266973_, Ingredient p_267047_, Ingredient p_267009_, RecipeCategory p_266694_, Item p_267183_
   ) {
      this.category = p_266694_;
      this.type = p_266683_;
      this.template = p_266973_;
      this.base = p_267047_;
      this.addition = p_267009_;
      this.result = p_267183_;
   }

   public static SmithingTransformRecipeBuilder smithing(
      Ingredient p_267071_, Ingredient p_266959_, Ingredient p_266803_, RecipeCategory p_266757_, Item p_267256_
   ) {
      return new SmithingTransformRecipeBuilder(RecipeSerializer.SMITHING_TRANSFORM, p_267071_, p_266959_, p_266803_, p_266757_, p_267256_);
   }

   public SmithingTransformRecipeBuilder unlocks(String p_266919_, Criterion<?> p_300923_) {
      this.criteria.put(p_266919_, p_300923_);
      return this;
   }

   public void save(RecipeOutput p_301163_, String p_300906_) {
      this.save(p_301163_, new ResourceLocation(p_300906_));
   }

   public void save(RecipeOutput p_301291_, ResourceLocation p_300873_) {
      this.ensureValid(p_300873_);
      Advancement.Builder advancement$builder = p_301291_.advancement()
         .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(p_300873_))
         .rewards(AdvancementRewards.Builder.recipe(p_300873_))
         .requirements(AdvancementRequirements.Strategy.OR);
      this.criteria.forEach(advancement$builder::addCriterion);
      p_301291_.accept(
         new SmithingTransformRecipeBuilder.Result(
            p_300873_,
            this.type,
            this.template,
            this.base,
            this.addition,
            this.result,
            advancement$builder.build(p_300873_.withPrefix("recipes/" + this.category.getFolderName() + "/"))
         )
      );
   }

   private void ensureValid(ResourceLocation p_267259_) {
      if (this.criteria.isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + p_267259_);
      }
   }

   public static record Result(
      ResourceLocation id, RecipeSerializer<?> type, Ingredient template, Ingredient base, Ingredient addition, Item result, AdvancementHolder advancement
   ) implements FinishedRecipe {
      @Override
      public void serializeRecipeData(JsonObject p_266713_) {
         p_266713_.add("template", this.template.toJson(true));
         p_266713_.add("base", this.base.toJson(true));
         p_266713_.add("addition", this.addition.toJson(true));
         JsonObject jsonobject = new JsonObject();
         jsonobject.addProperty("item", BuiltInRegistries.ITEM.getKey(this.result).toString());
         p_266713_.add("result", jsonobject);
      }
   }
}

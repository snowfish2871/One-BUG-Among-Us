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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class SmithingTrimRecipeBuilder {
   private final RecipeCategory category;
   private final Ingredient template;
   private final Ingredient base;
   private final Ingredient addition;
   private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
   private final RecipeSerializer<?> type;

   public SmithingTrimRecipeBuilder(RecipeSerializer<?> p_267085_, RecipeCategory p_267007_, Ingredient p_266712_, Ingredient p_267018_, Ingredient p_267264_) {
      this.category = p_267007_;
      this.type = p_267085_;
      this.template = p_266712_;
      this.base = p_267018_;
      this.addition = p_267264_;
   }

   public static SmithingTrimRecipeBuilder smithingTrim(Ingredient p_266812_, Ingredient p_266843_, Ingredient p_267309_, RecipeCategory p_267269_) {
      return new SmithingTrimRecipeBuilder(RecipeSerializer.SMITHING_TRIM, p_267269_, p_266812_, p_266843_, p_267309_);
   }

   public SmithingTrimRecipeBuilder unlocks(String p_266882_, Criterion<?> p_301261_) {
      this.criteria.put(p_266882_, p_301261_);
      return this;
   }

   public void save(RecipeOutput p_301110_, ResourceLocation p_266718_) {
      this.ensureValid(p_266718_);
      Advancement.Builder advancement$builder = p_301110_.advancement()
         .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(p_266718_))
         .rewards(AdvancementRewards.Builder.recipe(p_266718_))
         .requirements(AdvancementRequirements.Strategy.OR);
      this.criteria.forEach(advancement$builder::addCriterion);
      p_301110_.accept(
         new SmithingTrimRecipeBuilder.Result(
            p_266718_,
            this.type,
            this.template,
            this.base,
            this.addition,
            advancement$builder.build(p_266718_.withPrefix("recipes/" + this.category.getFolderName() + "/"))
         )
      );
   }

   private void ensureValid(ResourceLocation p_267040_) {
      if (this.criteria.isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + p_267040_);
      }
   }

   public static record Result(
      ResourceLocation id, RecipeSerializer<?> type, Ingredient template, Ingredient base, Ingredient addition, AdvancementHolder advancement
   ) implements FinishedRecipe {
      @Override
      public void serializeRecipeData(JsonObject p_267008_) {
         p_267008_.add("template", this.template.toJson(true));
         p_267008_.add("base", this.base.toJson(true));
         p_267008_.add("addition", this.addition.toJson(true));
      }
   }
}

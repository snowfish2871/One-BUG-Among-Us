package net.minecraft.data.recipes;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
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
import net.minecraft.world.level.ItemLike;

public class SingleItemRecipeBuilder implements RecipeBuilder {
   private final RecipeCategory category;
   private final Item result;
   private final Ingredient ingredient;
   private final int count;
   private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
   @Nullable
   private String group;
   private final RecipeSerializer<?> type;

   public SingleItemRecipeBuilder(RecipeCategory p_251425_, RecipeSerializer<?> p_249762_, Ingredient p_251221_, ItemLike p_251302_, int p_250964_) {
      this.category = p_251425_;
      this.type = p_249762_;
      this.result = p_251302_.asItem();
      this.ingredient = p_251221_;
      this.count = p_250964_;
   }

   public static SingleItemRecipeBuilder stonecutting(Ingredient p_248596_, RecipeCategory p_250503_, ItemLike p_250269_) {
      return new SingleItemRecipeBuilder(p_250503_, RecipeSerializer.STONECUTTER, p_248596_, p_250269_, 1);
   }

   public static SingleItemRecipeBuilder stonecutting(Ingredient p_251375_, RecipeCategory p_248984_, ItemLike p_250105_, int p_249506_) {
      return new SingleItemRecipeBuilder(p_248984_, RecipeSerializer.STONECUTTER, p_251375_, p_250105_, p_249506_);
   }

   public SingleItemRecipeBuilder unlockedBy(String p_176810_, Criterion<?> p_301267_) {
      this.criteria.put(p_176810_, p_301267_);
      return this;
   }

   public SingleItemRecipeBuilder group(@Nullable String p_176808_) {
      this.group = p_176808_;
      return this;
   }

   @Override
   public Item getResult() {
      return this.result;
   }

   @Override
   public void save(RecipeOutput p_301137_, ResourceLocation p_126328_) {
      this.ensureValid(p_126328_);
      Advancement.Builder advancement$builder = p_301137_.advancement()
         .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(p_126328_))
         .rewards(AdvancementRewards.Builder.recipe(p_126328_))
         .requirements(AdvancementRequirements.Strategy.OR);
      this.criteria.forEach(advancement$builder::addCriterion);
      p_301137_.accept(
         new SingleItemRecipeBuilder.Result(
            p_126328_,
            this.type,
            this.group == null ? "" : this.group,
            this.ingredient,
            this.result,
            this.count,
            advancement$builder.build(p_126328_.withPrefix("recipes/" + this.category.getFolderName() + "/"))
         )
      );
   }

   private void ensureValid(ResourceLocation p_126330_) {
      if (this.criteria.isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + p_126330_);
      }
   }

   public static record Result(
      ResourceLocation id, RecipeSerializer<?> type, String group, Ingredient ingredient, Item result, int count, AdvancementHolder advancement
   ) implements FinishedRecipe {
      @Override
      public void serializeRecipeData(JsonObject p_126349_) {
         if (!this.group.isEmpty()) {
            p_126349_.addProperty("group", this.group);
         }

         p_126349_.add("ingredient", this.ingredient.toJson(false));
         p_126349_.addProperty("result", BuiltInRegistries.ITEM.getKey(this.result).toString());
         p_126349_.addProperty("count", this.count);
      }
   }
}

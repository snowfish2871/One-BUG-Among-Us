package net.minecraft.data.recipes;

import javax.annotation.Nullable;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class SpecialRecipeBuilder extends CraftingRecipeBuilder {
   final RecipeSerializer<?> serializer;

   public SpecialRecipeBuilder(RecipeSerializer<?> p_250173_) {
      this.serializer = p_250173_;
   }

   public static SpecialRecipeBuilder special(RecipeSerializer<? extends CraftingRecipe> p_249458_) {
      return new SpecialRecipeBuilder(p_249458_);
   }

   public void save(RecipeOutput p_301307_, String p_126361_) {
      this.save(p_301307_, new ResourceLocation(p_126361_));
   }

   public void save(RecipeOutput p_301123_, final ResourceLocation p_301214_) {
      p_301123_.accept(new CraftingRecipeBuilder.CraftingResult(CraftingBookCategory.MISC) {
         @Override
         public RecipeSerializer<?> type() {
            return SpecialRecipeBuilder.this.serializer;
         }

         @Override
         public ResourceLocation id() {
            return p_301214_;
         }

         @Nullable
         @Override
         public AdvancementHolder advancement() {
            return null;
         }
      });
   }
}

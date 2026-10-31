package net.minecraft.data.recipes;

import com.google.gson.JsonObject;
import net.minecraft.world.item.crafting.CraftingBookCategory;

public abstract class CraftingRecipeBuilder {
   protected static CraftingBookCategory determineBookCategory(RecipeCategory p_250736_) {
      return switch(p_250736_) {
         case BUILDING_BLOCKS -> CraftingBookCategory.BUILDING;
         case TOOLS, COMBAT -> CraftingBookCategory.EQUIPMENT;
         case REDSTONE -> CraftingBookCategory.REDSTONE;
         default -> CraftingBookCategory.MISC;
      };
   }

   protected abstract static class CraftingResult implements FinishedRecipe {
      private final CraftingBookCategory category;

      protected CraftingResult(CraftingBookCategory p_250313_) {
         this.category = p_250313_;
      }

      @Override
      public void serializeRecipeData(JsonObject p_250456_) {
         p_250456_.addProperty("category", this.category.getSerializedName());
      }
   }
}

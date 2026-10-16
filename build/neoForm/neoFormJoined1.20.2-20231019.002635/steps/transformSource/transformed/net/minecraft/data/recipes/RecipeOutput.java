package net.minecraft.data.recipes;

import net.minecraft.advancements.Advancement;

public interface RecipeOutput extends net.neoforged.neoforge.common.extensions.IRecipeOutputExtension {
   default void accept(FinishedRecipe p_301033_) {
      accept(p_301033_, new net.neoforged.neoforge.common.conditions.ICondition[0]);
   }

   Advancement.Builder advancement();
}

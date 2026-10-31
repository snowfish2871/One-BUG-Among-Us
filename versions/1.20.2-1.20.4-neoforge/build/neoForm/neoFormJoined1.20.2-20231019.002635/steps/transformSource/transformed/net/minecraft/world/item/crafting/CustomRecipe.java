package net.minecraft.world.item.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

public abstract class CustomRecipe implements CraftingRecipe {
   private final CraftingBookCategory category;

   public CustomRecipe(CraftingBookCategory p_249010_) {
      this.category = p_249010_;
   }

   @Override
   public boolean isSpecial() {
      return true;
   }

   @Override
   public ItemStack getResultItem(RegistryAccess p_267025_) {
      return ItemStack.EMPTY;
   }

   @Override
   public CraftingBookCategory category() {
      return this.category;
   }
}

package net.minecraft.world.item.crafting;

import net.minecraft.resources.ResourceLocation;

public record RecipeHolder<T extends Recipe<?>>(ResourceLocation id, T value) {
   @Override
   public boolean equals(Object p_301091_) {
      if (this == p_301091_) {
         return true;
      } else {
         if (p_301091_ instanceof RecipeHolder recipeholder && this.id.equals(recipeholder.id)) {
            return true;
         }

         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.id.hashCode();
   }

   @Override
   public String toString() {
      return this.id.toString();
   }
}

package net.minecraft.world.ticks;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public interface ContainerSingleItem extends Container {
   @Override
   default int getContainerSize() {
      return 1;
   }

   @Override
   default boolean isEmpty() {
      return this.getFirstItem().isEmpty();
   }

   @Override
   default void clearContent() {
      this.removeFirstItem();
   }

   default ItemStack getFirstItem() {
      return this.getItem(0);
   }

   default ItemStack removeFirstItem() {
      return this.removeItemNoUpdate(0);
   }

   default void setFirstItem(ItemStack p_273635_) {
      this.setItem(0, p_273635_);
   }

   @Override
   default ItemStack removeItemNoUpdate(int p_273409_) {
      return this.removeItem(p_273409_, this.getMaxStackSize());
   }
}

package net.minecraft.world.item;

public class SimpleFoiledItem extends Item {
   public SimpleFoiledItem(Item.Properties p_43136_) {
      super(p_43136_);
   }

   @Override
   public boolean isFoil(ItemStack p_43138_) {
      return true;
   }
}

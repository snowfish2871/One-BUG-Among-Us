package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EquipmentSlot;

public class SwiftSneakEnchantment extends Enchantment {
   public SwiftSneakEnchantment(Enchantment.Rarity p_220306_, EquipmentSlot... p_220307_) {
      super(p_220306_, EnchantmentCategory.ARMOR_LEGS, p_220307_);
   }

   @Override
   public int getMinCost(int p_220310_) {
      return p_220310_ * 25;
   }

   @Override
   public int getMaxCost(int p_220313_) {
      return this.getMinCost(p_220313_) + 50;
   }

   @Override
   public boolean isTreasureOnly() {
      return true;
   }

   @Override
   public boolean isTradeable() {
      return false;
   }

   @Override
   public boolean isDiscoverable() {
      return false;
   }

   @Override
   public int getMaxLevel() {
      return 3;
   }
}

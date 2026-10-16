package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EquipmentSlot;

public class SoulSpeedEnchantment extends Enchantment {
   public SoulSpeedEnchantment(Enchantment.Rarity p_45175_, EquipmentSlot... p_45176_) {
      super(p_45175_, EnchantmentCategory.ARMOR_FEET, p_45176_);
   }

   @Override
   public int getMinCost(int p_45179_) {
      return p_45179_ * 10;
   }

   @Override
   public int getMaxCost(int p_45182_) {
      return this.getMinCost(p_45182_) + 15;
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

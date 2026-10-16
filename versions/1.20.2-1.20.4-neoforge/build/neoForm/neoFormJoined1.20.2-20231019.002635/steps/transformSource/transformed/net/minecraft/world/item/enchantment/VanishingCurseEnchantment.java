package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EquipmentSlot;

public class VanishingCurseEnchantment extends Enchantment {
   public VanishingCurseEnchantment(Enchantment.Rarity p_45270_, EquipmentSlot... p_45271_) {
      super(p_45270_, EnchantmentCategory.VANISHABLE, p_45271_);
   }

   @Override
   public int getMinCost(int p_45274_) {
      return 25;
   }

   @Override
   public int getMaxCost(int p_45277_) {
      return 50;
   }

   @Override
   public boolean isTreasureOnly() {
      return true;
   }

   @Override
   public boolean isCurse() {
      return true;
   }
}

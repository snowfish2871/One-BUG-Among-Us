package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EquipmentSlot;

public class KnockbackEnchantment extends Enchantment {
   protected KnockbackEnchantment(Enchantment.Rarity p_45079_, EquipmentSlot... p_45080_) {
      super(p_45079_, EnchantmentCategory.WEAPON, p_45080_);
   }

   @Override
   public int getMinCost(int p_45083_) {
      return 5 + 20 * (p_45083_ - 1);
   }

   @Override
   public int getMaxCost(int p_45085_) {
      return super.getMinCost(p_45085_) + 50;
   }

   @Override
   public int getMaxLevel() {
      return 2;
   }
}

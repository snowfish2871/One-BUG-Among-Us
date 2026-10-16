package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EquipmentSlot;

public class MendingEnchantment extends Enchantment {
   public MendingEnchantment(Enchantment.Rarity p_45098_, EquipmentSlot... p_45099_) {
      super(p_45098_, EnchantmentCategory.BREAKABLE, p_45099_);
   }

   @Override
   public int getMinCost(int p_45102_) {
      return p_45102_ * 25;
   }

   @Override
   public int getMaxCost(int p_45105_) {
      return this.getMinCost(p_45105_) + 50;
   }

   @Override
   public boolean isTreasureOnly() {
      return true;
   }
}

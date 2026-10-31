package net.minecraft.core.dispenser;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public abstract class AbstractProjectileDispenseBehavior extends DefaultDispenseItemBehavior {
   @Override
   public ItemStack execute(BlockSource p_302427_, ItemStack p_123367_) {
      Level level = p_302427_.level();
      Position position = DispenserBlock.getDispensePosition(p_302427_);
      Direction direction = p_302427_.state().getValue(DispenserBlock.FACING);
      Projectile projectile = this.getProjectile(level, position, p_123367_);
      projectile.shoot(
         (double)direction.getStepX(), (double)((float)direction.getStepY() + 0.1F), (double)direction.getStepZ(), this.getPower(), this.getUncertainty()
      );
      level.addFreshEntity(projectile);
      p_123367_.shrink(1);
      return p_123367_;
   }

   @Override
   protected void playSound(BlockSource p_302441_) {
      p_302441_.level().levelEvent(1002, p_302441_.pos(), 0);
   }

   protected abstract Projectile getProjectile(Level p_123360_, Position p_123361_, ItemStack p_123362_);

   protected float getUncertainty() {
      return 6.0F;
   }

   protected float getPower() {
      return 1.1F;
   }
}

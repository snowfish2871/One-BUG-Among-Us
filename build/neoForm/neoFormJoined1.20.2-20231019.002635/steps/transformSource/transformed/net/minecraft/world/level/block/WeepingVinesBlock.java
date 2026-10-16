package net.minecraft.world.level.block;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WeepingVinesBlock extends GrowingPlantHeadBlock {
   protected static final VoxelShape SHAPE = Block.box(4.0, 9.0, 4.0, 12.0, 16.0, 12.0);

   public WeepingVinesBlock(BlockBehaviour.Properties p_154966_) {
      super(p_154966_, Direction.DOWN, SHAPE, false, 0.1);
   }

   @Override
   protected int getBlocksToGrowWhenBonemealed(RandomSource p_222680_) {
      return NetherVines.getBlocksToGrowWhenBonemealed(p_222680_);
   }

   @Override
   protected Block getBodyBlock() {
      return Blocks.WEEPING_VINES_PLANT;
   }

   @Override
   protected boolean canGrowInto(BlockState p_154971_) {
      return NetherVines.isValidGrowthState(p_154971_);
   }
}

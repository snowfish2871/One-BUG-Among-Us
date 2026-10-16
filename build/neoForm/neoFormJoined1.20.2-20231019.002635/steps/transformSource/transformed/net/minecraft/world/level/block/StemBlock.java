package net.minecraft.world.level.block;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StemBlock extends BushBlock implements BonemealableBlock {
   public static final int MAX_AGE = 7;
   public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
   protected static final float AABB_OFFSET = 1.0F;
   protected static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
      Block.box(7.0, 0.0, 7.0, 9.0, 2.0, 9.0),
      Block.box(7.0, 0.0, 7.0, 9.0, 4.0, 9.0),
      Block.box(7.0, 0.0, 7.0, 9.0, 6.0, 9.0),
      Block.box(7.0, 0.0, 7.0, 9.0, 8.0, 9.0),
      Block.box(7.0, 0.0, 7.0, 9.0, 10.0, 9.0),
      Block.box(7.0, 0.0, 7.0, 9.0, 12.0, 9.0),
      Block.box(7.0, 0.0, 7.0, 9.0, 14.0, 9.0),
      Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0)
   };
   private final StemGrownBlock fruit;
   private final Supplier<Item> seedSupplier;

   public StemBlock(StemGrownBlock p_154728_, Supplier<Item> p_154729_, BlockBehaviour.Properties p_154730_) {
      super(p_154730_);
      this.fruit = p_154728_;
      this.seedSupplier = p_154729_;
      this.registerDefaultState(this.stateDefinition.any().setValue(AGE, Integer.valueOf(0)));
   }

   @Override
   public VoxelShape getShape(BlockState p_57047_, BlockGetter p_57048_, BlockPos p_57049_, CollisionContext p_57050_) {
      return SHAPE_BY_AGE[p_57047_.getValue(AGE)];
   }

   @Override
   protected boolean mayPlaceOn(BlockState p_57053_, BlockGetter p_57054_, BlockPos p_57055_) {
      return p_57053_.is(Blocks.FARMLAND);
   }

   @Override
   public void randomTick(BlockState p_222538_, ServerLevel p_222539_, BlockPos p_222540_, RandomSource p_222541_) {
      if (!p_222539_.isAreaLoaded(p_222540_, 1)) return; // Forge: prevent loading unloaded chunks when checking neighbor's light
      if (p_222539_.getRawBrightness(p_222540_, 0) >= 9) {
         float f = CropBlock.getGrowthSpeed(this, p_222539_, p_222540_);
         if (net.neoforged.neoforge.common.CommonHooks.onCropsGrowPre(p_222539_, p_222540_, p_222538_, p_222541_.nextInt((int)(25.0F / f) + 1) == 0)) {
            int i = p_222538_.getValue(AGE);
            if (i < 7) {
               p_222539_.setBlock(p_222540_, p_222538_.setValue(AGE, Integer.valueOf(i + 1)), 2);
            } else {
               Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(p_222541_);
               BlockPos blockpos = p_222540_.relative(direction);
               BlockState blockstate = p_222539_.getBlockState(blockpos.below());
               if (p_222539_.isEmptyBlock(blockpos) && (blockstate.canSustainPlant(p_222539_, blockpos.below(), Direction.UP, this.fruit) || blockstate.is(Blocks.FARMLAND) || blockstate.is(BlockTags.DIRT))) {
                  p_222539_.setBlockAndUpdate(blockpos, this.fruit.defaultBlockState());
                  p_222539_.setBlockAndUpdate(
                     p_222540_, this.fruit.getAttachedStem().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, direction)
                  );
               }
            }
            net.neoforged.neoforge.common.CommonHooks.onCropsGrowPost(p_222539_, p_222540_, p_222538_);
         }
      }
   }

   @Override
   public ItemStack getCloneItemStack(BlockGetter p_57026_, BlockPos p_57027_, BlockState p_57028_) {
      return new ItemStack(this.seedSupplier.get());
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_255699_, BlockPos p_57031_, BlockState p_57032_) {
      return p_57032_.getValue(AGE) != 7;
   }

   @Override
   public boolean isBonemealSuccess(Level p_222533_, RandomSource p_222534_, BlockPos p_222535_, BlockState p_222536_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_222528_, RandomSource p_222529_, BlockPos p_222530_, BlockState p_222531_) {
      int i = Math.min(7, p_222531_.getValue(AGE) + Mth.nextInt(p_222528_.random, 2, 5));
      BlockState blockstate = p_222531_.setValue(AGE, Integer.valueOf(i));
      p_222528_.setBlock(p_222530_, blockstate, 2);
      if (i == 7) {
         blockstate.randomTick(p_222528_, p_222530_, p_222528_.random);
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_57040_) {
      p_57040_.add(AGE);
   }

   public StemGrownBlock getFruit() {
      return this.fruit;
   }

   //FORGE START
   @Override
   public net.neoforged.neoforge.common.PlantType getPlantType(BlockGetter world, BlockPos pos) {
      return net.neoforged.neoforge.common.PlantType.CROP;
   }
}

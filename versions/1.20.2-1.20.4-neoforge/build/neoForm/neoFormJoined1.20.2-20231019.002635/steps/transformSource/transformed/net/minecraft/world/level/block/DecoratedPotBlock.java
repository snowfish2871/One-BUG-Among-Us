package net.minecraft.world.level.block;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DecoratedPotBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
   public static final ResourceLocation SHERDS_DYNAMIC_DROP_ID = new ResourceLocation("sherds");
   private static final VoxelShape BOUNDING_BOX = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);
   private static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
   private static final BooleanProperty CRACKED = BlockStateProperties.CRACKED;
   private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

   public DecoratedPotBlock(BlockBehaviour.Properties p_273064_) {
      super(p_273064_);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(HORIZONTAL_FACING, Direction.NORTH)
            .setValue(WATERLOGGED, Boolean.valueOf(false))
            .setValue(CRACKED, Boolean.valueOf(false))
      );
   }

   @Override
   public BlockState updateShape(
      BlockState p_276307_, Direction p_276322_, BlockState p_276280_, LevelAccessor p_276320_, BlockPos p_276270_, BlockPos p_276312_
   ) {
      if (p_276307_.getValue(WATERLOGGED)) {
         p_276320_.scheduleTick(p_276270_, Fluids.WATER, Fluids.WATER.getTickDelay(p_276320_));
      }

      return super.updateShape(p_276307_, p_276322_, p_276280_, p_276320_, p_276270_, p_276312_);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_272711_) {
      FluidState fluidstate = p_272711_.getLevel().getFluidState(p_272711_.getClickedPos());
      return this.defaultBlockState()
         .setValue(HORIZONTAL_FACING, p_272711_.getHorizontalDirection())
         .setValue(WATERLOGGED, Boolean.valueOf(fluidstate.getType() == Fluids.WATER))
         .setValue(CRACKED, Boolean.valueOf(false));
   }

   @Override
   public void setPlacedBy(Level p_295174_, BlockPos p_295227_, BlockState p_294576_, @Nullable LivingEntity p_295902_, ItemStack p_294817_) {
      if (p_295174_.isClientSide) {
         p_295174_.getBlockEntity(p_295227_, BlockEntityType.DECORATED_POT).ifPresent(p_294066_ -> p_294066_.setFromItem(p_294817_));
      }
   }

   @Override
   public boolean isPathfindable(BlockState p_276295_, BlockGetter p_276308_, BlockPos p_276313_, PathComputationType p_276303_) {
      return false;
   }

   @Override
   public VoxelShape getShape(BlockState p_273112_, BlockGetter p_273055_, BlockPos p_273137_, CollisionContext p_273151_) {
      return BOUNDING_BOX;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_273169_) {
      p_273169_.add(HORIZONTAL_FACING, WATERLOGGED, CRACKED);
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos p_273396_, BlockState p_272674_) {
      return new DecoratedPotBlockEntity(p_273396_, p_272674_);
   }

   @Override
   public List<ItemStack> getDrops(BlockState p_287683_, LootParams.Builder p_287582_) {
      BlockEntity blockentity = p_287582_.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
      if (blockentity instanceof DecoratedPotBlockEntity decoratedpotblockentity) {
         p_287582_.withDynamicDrop(
            SHERDS_DYNAMIC_DROP_ID, p_284876_ -> decoratedpotblockentity.getDecorations().sorted().map(Item::getDefaultInstance).forEach(p_284876_)
         );
      }

      return super.getDrops(p_287683_, p_287582_);
   }

   @Override
   public void playerWillDestroy(Level p_273590_, BlockPos p_273343_, BlockState p_272869_, Player p_273002_) {
      ItemStack itemstack = p_273002_.getMainHandItem();
      BlockState blockstate = p_272869_;
      if (itemstack.is(ItemTags.BREAKS_DECORATED_POTS) && !EnchantmentHelper.hasSilkTouch(itemstack)) {
         blockstate = p_272869_.setValue(CRACKED, Boolean.valueOf(true));
         p_273590_.setBlock(p_273343_, blockstate, 4);
      }

      super.playerWillDestroy(p_273590_, p_273343_, blockstate, p_273002_);
   }

   @Override
   public FluidState getFluidState(BlockState p_272593_) {
      return p_272593_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_272593_);
   }

   @Override
   public SoundType getSoundType(BlockState p_277561_) {
      return p_277561_.getValue(CRACKED) ? SoundType.DECORATED_POT_CRACKED : SoundType.DECORATED_POT;
   }

   @Override
   public void appendHoverText(ItemStack p_285238_, @Nullable BlockGetter p_285450_, List<Component> p_285448_, TooltipFlag p_284997_) {
      super.appendHoverText(p_285238_, p_285450_, p_285448_, p_284997_);
      DecoratedPotBlockEntity.Decorations decoratedpotblockentity$decorations = DecoratedPotBlockEntity.Decorations.load(
         BlockItem.getBlockEntityData(p_285238_)
      );
      if (!decoratedpotblockentity$decorations.equals(DecoratedPotBlockEntity.Decorations.EMPTY)) {
         p_285448_.add(CommonComponents.EMPTY);
         Stream.of(
               decoratedpotblockentity$decorations.front(),
               decoratedpotblockentity$decorations.left(),
               decoratedpotblockentity$decorations.right(),
               decoratedpotblockentity$decorations.back()
            )
            .forEach(p_284873_ -> p_285448_.add(new ItemStack(p_284873_, 1).getHoverName().plainCopy().withStyle(ChatFormatting.GRAY)));
      }
   }

   @Override
   public ItemStack getCloneItemStack(BlockGetter p_295955_, BlockPos p_294412_, BlockState p_294723_) {
      BlockEntity blockentity = p_295955_.getBlockEntity(p_294412_);
      return blockentity instanceof DecoratedPotBlockEntity decoratedpotblockentity
         ? decoratedpotblockentity.getItem()
         : super.getCloneItemStack(p_295955_, p_294412_, p_294723_);
   }
}

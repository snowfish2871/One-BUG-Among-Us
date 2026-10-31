package net.minecraft.world.level.block.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

public class BeaconBlockEntity extends BlockEntity implements MenuProvider, Nameable {
   private static final int MAX_LEVELS = 4;
   public static final MobEffect[][] BEACON_EFFECTS = new MobEffect[][]{
      {MobEffects.MOVEMENT_SPEED, MobEffects.DIG_SPEED}, {MobEffects.DAMAGE_RESISTANCE, MobEffects.JUMP}, {MobEffects.DAMAGE_BOOST}, {MobEffects.REGENERATION}
   };
   private static final Set<MobEffect> VALID_EFFECTS = Arrays.stream(BEACON_EFFECTS).flatMap(Arrays::stream).collect(Collectors.toSet());
   public static final int DATA_LEVELS = 0;
   public static final int DATA_PRIMARY = 1;
   public static final int DATA_SECONDARY = 2;
   public static final int NUM_DATA_VALUES = 3;
   private static final int BLOCKS_CHECK_PER_TICK = 10;
   private static final Component DEFAULT_NAME = Component.translatable("container.beacon");
   private static final String TAG_PRIMARY = "primary_effect";
   private static final String TAG_SECONDARY = "secondary_effect";
   List<BeaconBlockEntity.BeaconBeamSection> beamSections = Lists.newArrayList();
   private List<BeaconBlockEntity.BeaconBeamSection> checkingBeamSections = Lists.newArrayList();
   int levels;
   private int lastCheckY;
   @Nullable
   MobEffect primaryPower;
   @Nullable
   MobEffect secondaryPower;
   @Nullable
   private Component name;
   private LockCode lockKey = LockCode.NO_LOCK;
   private final ContainerData dataAccess = new ContainerData() {
      @Override
      public int get(int p_58711_) {
         return switch(p_58711_) {
            case 0 -> BeaconBlockEntity.this.levels;
            case 1 -> BeaconMenu.encodeEffect(BeaconBlockEntity.this.primaryPower);
            case 2 -> BeaconMenu.encodeEffect(BeaconBlockEntity.this.secondaryPower);
            default -> 0;
         };
      }

      @Override
      public void set(int p_58713_, int p_58714_) {
         switch(p_58713_) {
            case 0:
               BeaconBlockEntity.this.levels = p_58714_;
               break;
            case 1:
               if (!BeaconBlockEntity.this.level.isClientSide && !BeaconBlockEntity.this.beamSections.isEmpty()) {
                  BeaconBlockEntity.playSound(BeaconBlockEntity.this.level, BeaconBlockEntity.this.worldPosition, SoundEvents.BEACON_POWER_SELECT);
               }

               BeaconBlockEntity.this.primaryPower = BeaconBlockEntity.filterEffect(BeaconMenu.decodeEffect(p_58714_));
               break;
            case 2:
               BeaconBlockEntity.this.secondaryPower = BeaconBlockEntity.filterEffect(BeaconMenu.decodeEffect(p_58714_));
         }
      }

      @Override
      public int getCount() {
         return 3;
      }
   };

   @Nullable
   static MobEffect filterEffect(@Nullable MobEffect p_298440_) {
      return VALID_EFFECTS.contains(p_298440_) ? p_298440_ : null;
   }

   public BeaconBlockEntity(BlockPos p_155088_, BlockState p_155089_) {
      super(BlockEntityType.BEACON, p_155088_, p_155089_);
   }

   public static void tick(Level p_155108_, BlockPos p_155109_, BlockState p_155110_, BeaconBlockEntity p_155111_) {
      int i = p_155109_.getX();
      int j = p_155109_.getY();
      int k = p_155109_.getZ();
      BlockPos blockpos;
      if (p_155111_.lastCheckY < j) {
         blockpos = p_155109_;
         p_155111_.checkingBeamSections = Lists.newArrayList();
         p_155111_.lastCheckY = p_155109_.getY() - 1;
      } else {
         blockpos = new BlockPos(i, p_155111_.lastCheckY + 1, k);
      }

      BeaconBlockEntity.BeaconBeamSection beaconblockentity$beaconbeamsection = p_155111_.checkingBeamSections.isEmpty()
         ? null
         : p_155111_.checkingBeamSections.get(p_155111_.checkingBeamSections.size() - 1);
      int l = p_155108_.getHeight(Heightmap.Types.WORLD_SURFACE, i, k);

      for(int i1 = 0; i1 < 10 && blockpos.getY() <= l; ++i1) {
         BlockState blockstate = p_155108_.getBlockState(blockpos);
         Block block = blockstate.getBlock();
         float[] afloat = blockstate.getBeaconColorMultiplier(p_155108_, blockpos, p_155109_);
         if (afloat != null) {
            if (p_155111_.checkingBeamSections.size() <= 1) {
               beaconblockentity$beaconbeamsection = new BeaconBlockEntity.BeaconBeamSection(afloat);
               p_155111_.checkingBeamSections.add(beaconblockentity$beaconbeamsection);
            } else if (beaconblockentity$beaconbeamsection != null) {
               if (Arrays.equals(afloat, beaconblockentity$beaconbeamsection.color)) {
                  beaconblockentity$beaconbeamsection.increaseHeight();
               } else {
                  beaconblockentity$beaconbeamsection = new BeaconBlockEntity.BeaconBeamSection(
                     new float[]{
                        (beaconblockentity$beaconbeamsection.color[0] + afloat[0]) / 2.0F,
                        (beaconblockentity$beaconbeamsection.color[1] + afloat[1]) / 2.0F,
                        (beaconblockentity$beaconbeamsection.color[2] + afloat[2]) / 2.0F
                     }
                  );
                  p_155111_.checkingBeamSections.add(beaconblockentity$beaconbeamsection);
               }
            }
         } else {
            if (beaconblockentity$beaconbeamsection == null || blockstate.getLightBlock(p_155108_, blockpos) >= 15 && !blockstate.is(Blocks.BEDROCK)) {
               p_155111_.checkingBeamSections.clear();
               p_155111_.lastCheckY = l;
               break;
            }

            beaconblockentity$beaconbeamsection.increaseHeight();
         }

         blockpos = blockpos.above();
         ++p_155111_.lastCheckY;
      }

      int j1 = p_155111_.levels;
      if (p_155108_.getGameTime() % 80L == 0L) {
         if (!p_155111_.beamSections.isEmpty()) {
            p_155111_.levels = updateBase(p_155108_, i, j, k);
         }

         if (p_155111_.levels > 0 && !p_155111_.beamSections.isEmpty()) {
            applyEffects(p_155108_, p_155109_, p_155111_.levels, p_155111_.primaryPower, p_155111_.secondaryPower);
            playSound(p_155108_, p_155109_, SoundEvents.BEACON_AMBIENT);
         }
      }

      if (p_155111_.lastCheckY >= l) {
         p_155111_.lastCheckY = p_155108_.getMinBuildHeight() - 1;
         boolean flag = j1 > 0;
         p_155111_.beamSections = p_155111_.checkingBeamSections;
         if (!p_155108_.isClientSide) {
            boolean flag1 = p_155111_.levels > 0;
            if (!flag && flag1) {
               playSound(p_155108_, p_155109_, SoundEvents.BEACON_ACTIVATE);

               for(ServerPlayer serverplayer : p_155108_.getEntitiesOfClass(
                  ServerPlayer.class, new AABB((double)i, (double)j, (double)k, (double)i, (double)(j - 4), (double)k).inflate(10.0, 5.0, 10.0)
               )) {
                  CriteriaTriggers.CONSTRUCT_BEACON.trigger(serverplayer, p_155111_.levels);
               }
            } else if (flag && !flag1) {
               playSound(p_155108_, p_155109_, SoundEvents.BEACON_DEACTIVATE);
            }
         }
      }
   }

   private static int updateBase(Level p_155093_, int p_155094_, int p_155095_, int p_155096_) {
      int i = 0;

      for(int j = 1; j <= 4; i = j++) {
         int k = p_155095_ - j;
         if (k < p_155093_.getMinBuildHeight()) {
            break;
         }

         boolean flag = true;

         for(int l = p_155094_ - j; l <= p_155094_ + j && flag; ++l) {
            for(int i1 = p_155096_ - j; i1 <= p_155096_ + j; ++i1) {
               if (!p_155093_.getBlockState(new BlockPos(l, k, i1)).is(BlockTags.BEACON_BASE_BLOCKS)) {
                  flag = false;
                  break;
               }
            }
         }

         if (!flag) {
            break;
         }
      }

      return i;
   }

   @Override
   public void setRemoved() {
      playSound(this.level, this.worldPosition, SoundEvents.BEACON_DEACTIVATE);
      super.setRemoved();
   }

   private static void applyEffects(Level p_155098_, BlockPos p_155099_, int p_155100_, @Nullable MobEffect p_155101_, @Nullable MobEffect p_155102_) {
      if (!p_155098_.isClientSide && p_155101_ != null) {
         double d0 = (double)(p_155100_ * 10 + 10);
         int i = 0;
         if (p_155100_ >= 4 && p_155101_ == p_155102_) {
            i = 1;
         }

         int j = (9 + p_155100_ * 2) * 20;
         AABB aabb = new AABB(p_155099_).inflate(d0).expandTowards(0.0, (double)p_155098_.getHeight(), 0.0);
         List<Player> list = p_155098_.getEntitiesOfClass(Player.class, aabb);

         for(Player player : list) {
            player.addEffect(new MobEffectInstance(p_155101_, j, i, true, true));
         }

         if (p_155100_ >= 4 && p_155101_ != p_155102_ && p_155102_ != null) {
            for(Player player1 : list) {
               player1.addEffect(new MobEffectInstance(p_155102_, j, 0, true, true));
            }
         }
      }
   }

   public static void playSound(Level p_155104_, BlockPos p_155105_, SoundEvent p_155106_) {
      p_155104_.playSound(null, p_155105_, p_155106_, SoundSource.BLOCKS, 1.0F, 1.0F);
   }

   public List<BeaconBlockEntity.BeaconBeamSection> getBeamSections() {
      return (List<BeaconBlockEntity.BeaconBeamSection>)(this.levels == 0 ? ImmutableList.of() : this.beamSections);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag() {
      return this.saveWithoutMetadata();
   }

   private static void storeEffect(CompoundTag p_298214_, String p_298983_, @Nullable MobEffect p_299071_) {
      if (p_299071_ != null) {
         ResourceLocation resourcelocation = BuiltInRegistries.MOB_EFFECT.getKey(p_299071_);
         if (resourcelocation != null) {
            p_298214_.putString(p_298983_, resourcelocation.toString());
         }
      }
   }

   @Nullable
   private static MobEffect loadEffect(CompoundTag p_298570_, String p_299310_) {
      if (p_298570_.contains(p_299310_, 8)) {
         ResourceLocation resourcelocation = ResourceLocation.tryParse(p_298570_.getString(p_299310_));
         return filterEffect(BuiltInRegistries.MOB_EFFECT.get(resourcelocation));
      } else {
         return null;
      }
   }

   @Override
   public void load(CompoundTag p_155113_) {
      super.load(p_155113_);
      this.primaryPower = loadEffect(p_155113_, "primary_effect");
      this.secondaryPower = loadEffect(p_155113_, "secondary_effect");
      if (p_155113_.contains("CustomName", 8)) {
         this.name = Component.Serializer.fromJson(p_155113_.getString("CustomName"));
      }

      this.lockKey = LockCode.fromTag(p_155113_);
   }

   @Override
   protected void saveAdditional(CompoundTag p_187463_) {
      super.saveAdditional(p_187463_);
      storeEffect(p_187463_, "primary_effect", this.primaryPower);
      storeEffect(p_187463_, "secondary_effect", this.secondaryPower);
      p_187463_.putInt("Levels", this.levels);
      if (this.name != null) {
         p_187463_.putString("CustomName", Component.Serializer.toJson(this.name));
      }

      this.lockKey.addToTag(p_187463_);
   }

   public void setCustomName(@Nullable Component p_58682_) {
      this.name = p_58682_;
   }

   @Nullable
   @Override
   public Component getCustomName() {
      return this.name;
   }

   @Nullable
   @Override
   public AbstractContainerMenu createMenu(int p_58696_, Inventory p_58697_, Player p_58698_) {
      return BaseContainerBlockEntity.canUnlock(p_58698_, this.lockKey, this.getDisplayName())
         ? new BeaconMenu(p_58696_, p_58697_, this.dataAccess, ContainerLevelAccess.create(this.level, this.getBlockPos()))
         : null;
   }

   @Override
   public Component getDisplayName() {
      return this.getName();
   }

   @Override
   public Component getName() {
      return this.name != null ? this.name : DEFAULT_NAME;
   }

   @Override
   public void setLevel(Level p_155091_) {
      super.setLevel(p_155091_);
      this.lastCheckY = p_155091_.getMinBuildHeight() - 1;
   }

   public static class BeaconBeamSection {
      final float[] color;
      private int height;

      public BeaconBeamSection(float[] p_58718_) {
         this.color = p_58718_;
         this.height = 1;
      }

      protected void increaseHeight() {
         ++this.height;
      }

      public float[] getColor() {
         return this.color;
      }

      public int getHeight() {
         return this.height;
      }
   }
}

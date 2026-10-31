package net.minecraft.world.level.block.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Services;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SkullBlockEntity extends BlockEntity {
   public static final String TAG_SKULL_OWNER = "SkullOwner";
   public static final String TAG_NOTE_BLOCK_SOUND = "note_block_sound";
   @Nullable
   private static GameProfileCache profileCache;
   @Nullable
   private static MinecraftSessionService sessionService;
   @Nullable
   private static Executor mainThreadExecutor;
   private static final Executor CHECKED_MAIN_THREAD_EXECUTOR = p_294078_ -> {
      Executor executor = mainThreadExecutor;
      if (executor != null) {
         executor.execute(p_294078_);
      }
   };
   @Nullable
   private GameProfile owner;
   @Nullable
   private ResourceLocation noteBlockSound;
   private int animationTickCount;
   private boolean isAnimating;

   public SkullBlockEntity(BlockPos p_155731_, BlockState p_155732_) {
      super(BlockEntityType.SKULL, p_155731_, p_155732_);
   }

   public static void setup(Services p_222886_, Executor p_222887_) {
      profileCache = p_222886_.profileCache();
      sessionService = p_222886_.sessionService();
      mainThreadExecutor = p_222887_;
   }

   public static void clear() {
      profileCache = null;
      sessionService = null;
      mainThreadExecutor = null;
   }

   @Override
   protected void saveAdditional(CompoundTag p_187518_) {
      super.saveAdditional(p_187518_);
      if (this.owner != null) {
         CompoundTag compoundtag = new CompoundTag();
         NbtUtils.writeGameProfile(compoundtag, this.owner);
         p_187518_.put("SkullOwner", compoundtag);
      }

      if (this.noteBlockSound != null) {
         p_187518_.putString("note_block_sound", this.noteBlockSound.toString());
      }
   }

   @Override
   public void load(CompoundTag p_155745_) {
      super.load(p_155745_);
      if (p_155745_.contains("SkullOwner", 10)) {
         this.setOwner(NbtUtils.readGameProfile(p_155745_.getCompound("SkullOwner")));
      } else if (p_155745_.contains("ExtraType", 8)) {
         String s = p_155745_.getString("ExtraType");
         if (!StringUtil.isNullOrEmpty(s)) {
            this.setOwner(new GameProfile(Util.NIL_UUID, s));
         }
      }

      if (p_155745_.contains("note_block_sound", 8)) {
         this.noteBlockSound = ResourceLocation.tryParse(p_155745_.getString("note_block_sound"));
      }
   }

   public static void animation(Level p_261710_, BlockPos p_262153_, BlockState p_262021_, SkullBlockEntity p_261594_) {
      if (p_262021_.hasProperty(SkullBlock.POWERED) && p_262021_.getValue(SkullBlock.POWERED)) {
         p_261594_.isAnimating = true;
         ++p_261594_.animationTickCount;
      } else {
         p_261594_.isAnimating = false;
      }
   }

   public float getAnimation(float p_262053_) {
      return this.isAnimating ? (float)this.animationTickCount + p_262053_ : (float)this.animationTickCount;
   }

   @Nullable
   public GameProfile getOwnerProfile() {
      return this.owner;
   }

   @Nullable
   public ResourceLocation getNoteBlockSound() {
      return this.noteBlockSound;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag() {
      return this.saveWithoutMetadata();
   }

   public void setOwner(@Nullable GameProfile p_59770_) {
      synchronized(this) {
         this.owner = p_59770_;
      }

      this.updateOwnerProfile();
   }

   private void updateOwnerProfile() {
      if (this.owner != null && !Util.isBlank(this.owner.getName()) && !hasTextures(this.owner)) {
         fetchGameProfile(this.owner.getName()).thenAcceptAsync(p_294081_ -> {
            this.owner = p_294081_.orElse(this.owner);
            this.setChanged();
         }, CHECKED_MAIN_THREAD_EXECUTOR);
      } else {
         this.setChanged();
      }
   }

   @Nullable
   public static GameProfile getOrResolveGameProfile(CompoundTag p_296019_) {
      if (p_296019_.contains("SkullOwner", 10)) {
         return NbtUtils.readGameProfile(p_296019_.getCompound("SkullOwner"));
      } else {
         if (p_296019_.contains("SkullOwner", 8)) {
            String s = p_296019_.getString("SkullOwner");
            if (!Util.isBlank(s)) {
               p_296019_.remove("SkullOwner");
               resolveGameProfile(p_296019_, s);
            }
         }

         return null;
      }
   }

   public static void resolveGameProfile(CompoundTag p_294797_) {
      String s = p_294797_.getString("SkullOwner");
      if (!Util.isBlank(s)) {
         resolveGameProfile(p_294797_, s);
      }
   }

   private static void resolveGameProfile(CompoundTag p_294887_, String p_296231_) {
      fetchGameProfile(p_296231_)
         .thenAccept(
            p_294077_ -> p_294887_.put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), p_294077_.orElse(new GameProfile(Util.NIL_UUID, p_296231_))))
         );
   }

   private static CompletableFuture<Optional<GameProfile>> fetchGameProfile(String p_295932_) {
      GameProfileCache gameprofilecache = profileCache;
      return gameprofilecache == null
         ? CompletableFuture.completedFuture(Optional.empty())
         : gameprofilecache.getAsync(p_295932_)
            .thenCompose(p_294079_ -> p_294079_.isPresent() ? fillProfileTextures(p_294079_.get()) : CompletableFuture.completedFuture(Optional.empty()))
            .thenApplyAsync((p_294080_ -> {
               GameProfileCache gameprofilecache1 = profileCache;
               if (gameprofilecache1 != null) {
                  p_294080_.ifPresent(gameprofilecache1::add);
                  return p_294080_;
               } else {
                  return Optional.empty();
               }
            }), CHECKED_MAIN_THREAD_EXECUTOR);
   }

   private static CompletableFuture<Optional<GameProfile>> fillProfileTextures(GameProfile p_295873_) {
      return hasTextures(p_295873_) ? CompletableFuture.completedFuture(Optional.of(p_295873_)) : CompletableFuture.supplyAsync(() -> {
         MinecraftSessionService minecraftsessionservice = sessionService;
         if (minecraftsessionservice != null) {
            ProfileResult profileresult = minecraftsessionservice.fetchProfile(p_295873_.getId(), true);
            return profileresult == null ? Optional.of(p_295873_) : Optional.of(profileresult.profile());
         } else {
            return Optional.empty();
         }
      }, Util.backgroundExecutor());
   }

   private static boolean hasTextures(GameProfile p_295602_) {
      return p_295602_.getProperties().containsKey("textures");
   }
}

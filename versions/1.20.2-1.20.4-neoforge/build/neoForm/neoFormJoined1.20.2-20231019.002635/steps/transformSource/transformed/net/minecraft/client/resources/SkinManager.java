package net.minecraft.client.resources;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.hash.Hashing;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.properties.Property;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Optionull;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkinManager {
   private static final String PROPERTY_TEXTURES = "textures";
   private final LoadingCache<SkinManager.CacheKey, CompletableFuture<PlayerSkin>> skinCache;
   private final SkinManager.TextureCache skinTextures;
   private final SkinManager.TextureCache capeTextures;
   private final SkinManager.TextureCache elytraTextures;

   public SkinManager(TextureManager p_118812_, Path p_294690_, final MinecraftSessionService p_118814_, final Executor p_294105_) {
      this.skinTextures = new SkinManager.TextureCache(p_118812_, p_294690_, Type.SKIN);
      this.capeTextures = new SkinManager.TextureCache(p_118812_, p_294690_, Type.CAPE);
      this.elytraTextures = new SkinManager.TextureCache(p_118812_, p_294690_, Type.ELYTRA);
      this.skinCache = CacheBuilder.newBuilder()
         .expireAfterAccess(Duration.ofSeconds(15L))
         .build(new CacheLoader<SkinManager.CacheKey, CompletableFuture<PlayerSkin>>() {
            public CompletableFuture<PlayerSkin> load(SkinManager.CacheKey p_296373_) {
               GameProfile gameprofile = p_296373_.profile();
               return CompletableFuture.<SkinManager.TextureInfo>supplyAsync(() -> {
                  try {
                     try {
                        return SkinManager.TextureInfo.unpack(p_118814_.getTextures(gameprofile, true), true);
                     } catch (InsecurePublicKeyException insecurepublickeyexception) {
                        return SkinManager.TextureInfo.unpack(p_118814_.getTextures(gameprofile, false), false);
                     }
                  } catch (Throwable throwable) {
                     return SkinManager.TextureInfo.EMPTY;
                  }
               }, Util.backgroundExecutor()).thenComposeAsync(p_296179_ -> SkinManager.this.registerTextures(gameprofile, p_296179_), p_294105_);
            }
         });
   }

   public Supplier<PlayerSkin> lookupInsecure(GameProfile p_295432_) {
      CompletableFuture<PlayerSkin> completablefuture = this.getOrLoad(p_295432_);
      PlayerSkin playerskin = DefaultPlayerSkin.get(p_295432_);
      return () -> completablefuture.getNow(playerskin);
   }

   public PlayerSkin getInsecureSkin(GameProfile p_294261_) {
      PlayerSkin playerskin = this.getOrLoad(p_294261_).getNow(null);
      return playerskin != null ? playerskin : DefaultPlayerSkin.get(p_294261_);
   }

   public CompletableFuture<PlayerSkin> getOrLoad(GameProfile p_294865_) {
      return this.skinCache.getUnchecked(new SkinManager.CacheKey(p_294865_));
   }

   CompletableFuture<PlayerSkin> registerTextures(GameProfile p_295204_, SkinManager.TextureInfo p_296177_) {
      MinecraftProfileTexture minecraftprofiletexture = p_296177_.skin();
      CompletableFuture<ResourceLocation> completablefuture;
      PlayerSkin.Model playerskin$model;
      if (minecraftprofiletexture != null) {
         completablefuture = this.skinTextures.getOrLoad(minecraftprofiletexture);
         playerskin$model = PlayerSkin.Model.byName(minecraftprofiletexture.getMetadata("model"));
      } else {
         PlayerSkin playerskin = DefaultPlayerSkin.get(p_295204_);
         completablefuture = CompletableFuture.completedFuture(playerskin.texture());
         playerskin$model = playerskin.model();
      }

      String s = Optionull.map(minecraftprofiletexture, MinecraftProfileTexture::getUrl);
      MinecraftProfileTexture minecraftprofiletexture1 = p_296177_.cape();
      CompletableFuture<ResourceLocation> completablefuture1 = minecraftprofiletexture1 != null
         ? this.capeTextures.getOrLoad(minecraftprofiletexture1)
         : CompletableFuture.completedFuture(null);
      MinecraftProfileTexture minecraftprofiletexture2 = p_296177_.elytra();
      CompletableFuture<ResourceLocation> completablefuture2 = minecraftprofiletexture2 != null
         ? this.elytraTextures.getOrLoad(minecraftprofiletexture2)
         : CompletableFuture.completedFuture(null);
      return CompletableFuture.allOf(completablefuture, completablefuture1, completablefuture2)
         .thenApply(
            p_299814_ -> new PlayerSkin(completablefuture.join(), s, completablefuture1.join(), completablefuture2.join(), playerskin$model, p_296177_.secure())
         );
   }

   @Nullable
   static Property getTextureProperty(GameProfile p_295149_) {
      return Iterables.getFirst(p_295149_.getProperties().get("textures"), null);
   }

   @OnlyIn(Dist.CLIENT)
   static record CacheKey(GameProfile profile) {
      @Override
      public boolean equals(Object p_296051_) {
         if (!(p_296051_ instanceof SkinManager.CacheKey)) {
            return false;
         } else {
            SkinManager.CacheKey skinmanager$cachekey = (SkinManager.CacheKey)p_296051_;
            return this.profile.getId().equals(skinmanager$cachekey.profile.getId())
               && Objects.equals(this.texturesData(), skinmanager$cachekey.texturesData());
         }
      }

      @Override
      public int hashCode() {
         return this.profile.getId().hashCode() + Objects.hashCode(this.texturesData()) * 31;
      }

      @Nullable
      private String texturesData() {
         Property property = SkinManager.getTextureProperty(this.profile);
         return property != null ? property.value() : null;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class TextureCache {
      private final TextureManager textureManager;
      private final Path root;
      private final Type type;
      private final Map<String, CompletableFuture<ResourceLocation>> textures = new Object2ObjectOpenHashMap<>();

      TextureCache(TextureManager p_295278_, Path p_294453_, Type p_294220_) {
         this.textureManager = p_295278_;
         this.root = p_294453_;
         this.type = p_294220_;
      }

      public CompletableFuture<ResourceLocation> getOrLoad(MinecraftProfileTexture p_294862_) {
         String s = p_294862_.getHash();
         CompletableFuture<ResourceLocation> completablefuture = this.textures.get(s);
         if (completablefuture == null) {
            completablefuture = this.registerTexture(p_294862_);
            this.textures.put(s, completablefuture);
         }

         return completablefuture;
      }

      private CompletableFuture<ResourceLocation> registerTexture(MinecraftProfileTexture p_295647_) {
         String s = Hashing.sha1().hashUnencodedChars(p_295647_.getHash()).toString();
         ResourceLocation resourcelocation = this.getTextureLocation(s);
         Path path = this.root.resolve(s.length() > 2 ? s.substring(0, 2) : "xx").resolve(s);
         CompletableFuture<ResourceLocation> completablefuture = new CompletableFuture<>();
         HttpTexture httptexture = new HttpTexture(
            path.toFile(),
            p_295647_.getUrl(),
            DefaultPlayerSkin.getDefaultTexture(),
            this.type == Type.SKIN,
            () -> completablefuture.complete(resourcelocation)
         );
         this.textureManager.register(resourcelocation, httptexture);
         return completablefuture;
      }

      private ResourceLocation getTextureLocation(String p_295006_) {
         String s = switch(this.type) {
            case SKIN -> "skins";
            case CAPE -> "capes";
            case ELYTRA -> "elytra";
         };
         return new ResourceLocation(s + "/" + p_295006_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   static record TextureInfo(
      @Nullable MinecraftProfileTexture skin, @Nullable MinecraftProfileTexture cape, @Nullable MinecraftProfileTexture elytra, boolean secure
   ) {
      public static final SkinManager.TextureInfo EMPTY = new SkinManager.TextureInfo(null, null, null, true);

      public static SkinManager.TextureInfo unpack(Map<Type, MinecraftProfileTexture> p_296229_, boolean p_296459_) {
         return p_296229_.isEmpty()
            ? EMPTY
            : new SkinManager.TextureInfo(p_296229_.get(Type.SKIN), p_296229_.get(Type.CAPE), p_296229_.get(Type.ELYTRA), p_296459_);
      }
   }
}

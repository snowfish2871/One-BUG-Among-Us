package net.minecraft.network.protocol.common.custom;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public record BrainDebugPayload(BrainDebugPayload.BrainDump brainDump) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/brain");

   public BrainDebugPayload(FriendlyByteBuf p_295683_) {
      this(new BrainDebugPayload.BrainDump(p_295683_));
   }

   @Override
   public void write(FriendlyByteBuf p_294240_) {
      this.brainDump.write(p_294240_);
   }

   @Override
   public ResourceLocation id() {
      return ID;
   }

   public static record BrainDump(
      UUID uuid,
      int id,
      String name,
      String profession,
      int xp,
      float health,
      float maxHealth,
      Vec3 pos,
      String inventory,
      @Nullable Path path,
      boolean wantsGolem,
      int angerLevel,
      List<String> activities,
      List<String> behaviors,
      List<String> memories,
      List<String> gossips,
      Set<BlockPos> pois,
      Set<BlockPos> potentialPois
   ) {
      public BrainDump(FriendlyByteBuf p_294290_) {
         this(
            p_294290_.readUUID(),
            p_294290_.readInt(),
            p_294290_.readUtf(),
            p_294290_.readUtf(),
            p_294290_.readInt(),
            p_294290_.readFloat(),
            p_294290_.readFloat(),
            p_294290_.readVec3(),
            p_294290_.readUtf(),
            p_294290_.readNullable(Path::createFromStream),
            p_294290_.readBoolean(),
            p_294290_.readInt(),
            p_294290_.readList(FriendlyByteBuf::readUtf),
            p_294290_.readList(FriendlyByteBuf::readUtf),
            p_294290_.readList(FriendlyByteBuf::readUtf),
            p_294290_.readList(FriendlyByteBuf::readUtf),
            p_294290_.readCollection(HashSet::new, FriendlyByteBuf::readBlockPos),
            p_294290_.readCollection(HashSet::new, FriendlyByteBuf::readBlockPos)
         );
      }

      public void write(FriendlyByteBuf p_296077_) {
         p_296077_.writeUUID(this.uuid);
         p_296077_.writeInt(this.id);
         p_296077_.writeUtf(this.name);
         p_296077_.writeUtf(this.profession);
         p_296077_.writeInt(this.xp);
         p_296077_.writeFloat(this.health);
         p_296077_.writeFloat(this.maxHealth);
         p_296077_.writeVec3(this.pos);
         p_296077_.writeUtf(this.inventory);
         p_296077_.writeNullable(this.path, (p_296121_, p_295181_) -> p_295181_.writeToStream(p_296121_));
         p_296077_.writeBoolean(this.wantsGolem);
         p_296077_.writeInt(this.angerLevel);
         p_296077_.writeCollection(this.activities, FriendlyByteBuf::writeUtf);
         p_296077_.writeCollection(this.behaviors, FriendlyByteBuf::writeUtf);
         p_296077_.writeCollection(this.memories, FriendlyByteBuf::writeUtf);
         p_296077_.writeCollection(this.gossips, FriendlyByteBuf::writeUtf);
         p_296077_.writeCollection(this.pois, FriendlyByteBuf::writeBlockPos);
         p_296077_.writeCollection(this.potentialPois, FriendlyByteBuf::writeBlockPos);
      }

      public boolean hasPoi(BlockPos p_294781_) {
         return this.pois.contains(p_294781_);
      }

      public boolean hasPotentialPoi(BlockPos p_295626_) {
         return this.potentialPois.contains(p_295626_);
      }
   }
}

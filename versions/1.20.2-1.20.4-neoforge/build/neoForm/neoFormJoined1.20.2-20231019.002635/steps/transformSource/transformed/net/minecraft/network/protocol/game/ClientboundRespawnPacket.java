package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientboundRespawnPacket(CommonPlayerSpawnInfo commonPlayerSpawnInfo, byte dataToKeep) implements Packet<ClientGamePacketListener> {
   public static final byte KEEP_ATTRIBUTES = 1;
   public static final byte KEEP_ENTITY_DATA = 2;
   public static final byte KEEP_ALL_DATA = 3;

   public ClientboundRespawnPacket(FriendlyByteBuf p_179191_) {
      this(new CommonPlayerSpawnInfo(p_179191_), p_179191_.readByte());
   }

   @Override
   public void write(FriendlyByteBuf p_132954_) {
      this.commonPlayerSpawnInfo.write(p_132954_);
      p_132954_.writeByte(this.dataToKeep);
   }

   public void handle(ClientGamePacketListener p_132951_) {
      p_132951_.handleRespawn(this);
   }

   public boolean shouldKeep(byte p_263573_) {
      return (this.dataToKeep & p_263573_) != 0;
   }
}

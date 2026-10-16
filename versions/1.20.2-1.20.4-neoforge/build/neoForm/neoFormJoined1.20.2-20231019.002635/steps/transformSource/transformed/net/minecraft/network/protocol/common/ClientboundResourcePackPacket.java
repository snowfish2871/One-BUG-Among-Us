package net.minecraft.network.protocol.common;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public class ClientboundResourcePackPacket implements Packet<ClientCommonPacketListener> {
   public static final int MAX_HASH_LENGTH = 40;
   private final String url;
   private final String hash;
   private final boolean required;
   @Nullable
   private final Component prompt;

   public ClientboundResourcePackPacket(String p_295013_, String p_295569_, boolean p_294544_, @Nullable Component p_294687_) {
      if (p_295569_.length() > 40) {
         throw new IllegalArgumentException("Hash is too long (max 40, was " + p_295569_.length() + ")");
      } else {
         this.url = p_295013_;
         this.hash = p_295569_;
         this.required = p_294544_;
         this.prompt = p_294687_;
      }
   }

   public ClientboundResourcePackPacket(FriendlyByteBuf p_294285_) {
      this.url = p_294285_.readUtf();
      this.hash = p_294285_.readUtf(40);
      this.required = p_294285_.readBoolean();
      this.prompt = p_294285_.readNullable(FriendlyByteBuf::readComponent);
   }

   @Override
   public void write(FriendlyByteBuf p_295106_) {
      p_295106_.writeUtf(this.url);
      p_295106_.writeUtf(this.hash);
      p_295106_.writeBoolean(this.required);
      p_295106_.writeNullable(this.prompt, FriendlyByteBuf::writeComponent);
   }

   public void handle(ClientCommonPacketListener p_294448_) {
      p_294448_.handleResourcePack(this);
   }

   public String getUrl() {
      return this.url;
   }

   public String getHash() {
      return this.hash;
   }

   public boolean isRequired() {
      return this.required;
   }

   @Nullable
   public Component getPrompt() {
      return this.prompt;
   }
}

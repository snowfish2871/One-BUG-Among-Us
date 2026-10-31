package net.minecraft.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.AttributeKey;
import java.util.List;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.Packet;

public class PacketBundleUnpacker extends MessageToMessageEncoder<Packet<?>> {
   private final AttributeKey<? extends BundlerInfo.Provider> bundlerAttributeKey;

   public PacketBundleUnpacker(AttributeKey<? extends BundlerInfo.Provider> p_294766_) {
      this.bundlerAttributeKey = p_294766_;
   }

   protected void encode(ChannelHandlerContext p_265691_, Packet<?> p_265038_, List<Object> p_265735_) throws Exception {
      BundlerInfo.Provider bundlerinfo$provider = p_265691_.channel().attr(this.bundlerAttributeKey).get();
      if (bundlerinfo$provider == null) {
         throw new EncoderException("Bundler not configured: " + p_265038_);
      } else {
         bundlerinfo$provider.bundlerInfo().unbundlePacket(p_265038_, p_265735_::add);
      }
   }
}

package net.minecraft.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import java.io.IOException;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import org.slf4j.Logger;

public class PacketEncoder extends MessageToByteEncoder<Packet<?>> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final AttributeKey<ConnectionProtocol.CodecData<?>> codecKey;

   public PacketEncoder(AttributeKey<ConnectionProtocol.CodecData<?>> p_294628_) {
      this.codecKey = p_294628_;
   }

   protected void encode(ChannelHandlerContext p_130545_, Packet<?> p_130546_, ByteBuf p_130547_) throws Exception {
      Attribute<ConnectionProtocol.CodecData<?>> attribute = p_130545_.channel().attr(this.codecKey);
      ConnectionProtocol.CodecData<?> codecdata = attribute.get();
      if (codecdata == null) {
         throw new RuntimeException("ConnectionProtocol unknown: " + p_130546_);
      } else {
         int i = codecdata.packetId(p_130546_);
         if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(Connection.PACKET_SENT_MARKER, "OUT: [{}:{}] {}", codecdata.protocol().id(), i, p_130546_.getClass().getName());
         }

         if (i == -1) {
            throw new IOException("Can't serialize unregistered packet");
         } else {
            FriendlyByteBuf friendlybytebuf = new FriendlyByteBuf(p_130547_);
            friendlybytebuf.writeVarInt(i);

            try {
               int j = friendlybytebuf.writerIndex();
               p_130546_.write(friendlybytebuf);
               int k = friendlybytebuf.writerIndex() - j;
               if (k > 8388608) {
                  throw new IllegalArgumentException("Packet too big (is " + k + ", should be less than 8388608): " + p_130546_);
               }

               JvmProfiler.INSTANCE.onPacketSent(codecdata.protocol(), i, p_130545_.channel().remoteAddress(), k);
            } catch (Throwable throwable) {
               LOGGER.error("Error receiving packet {}", i, throwable);
               if (p_130546_.isSkippable()) {
                  throw new SkipPacketException(throwable);
               }

               throw throwable;
            } finally {
               ProtocolSwapHandler.swapProtocolIfNeeded(attribute, p_130546_);
            }
         }
      }
   }
}

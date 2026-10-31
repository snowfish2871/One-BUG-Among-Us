package net.minecraft.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import java.io.IOException;
import java.util.List;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import org.slf4j.Logger;

public class PacketDecoder extends ByteToMessageDecoder implements ProtocolSwapHandler {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final AttributeKey<ConnectionProtocol.CodecData<?>> codecKey;

   public PacketDecoder(AttributeKey<ConnectionProtocol.CodecData<?>> p_296346_) {
      this.codecKey = p_296346_;
   }

   @Override
   protected void decode(ChannelHandlerContext p_130535_, ByteBuf p_130536_, List<Object> p_130537_) throws Exception {
      int i = p_130536_.readableBytes();
      if (i != 0) {
         Attribute<ConnectionProtocol.CodecData<?>> attribute = p_130535_.channel().attr(this.codecKey);
         ConnectionProtocol.CodecData<?> codecdata = attribute.get();
         FriendlyByteBuf friendlybytebuf = new FriendlyByteBuf(p_130536_);
         int j = friendlybytebuf.readVarInt();
         Packet<?> packet = codecdata.createPacket(j, friendlybytebuf);
         if (packet == null) {
            throw new IOException("Bad packet id " + j);
         } else {
            JvmProfiler.INSTANCE.onPacketReceived(codecdata.protocol(), j, p_130535_.channel().remoteAddress(), i);
            if (friendlybytebuf.readableBytes() > 0) {
               throw new IOException(
                  "Packet "
                     + codecdata.protocol().id()
                     + "/"
                     + j
                     + " ("
                     + packet.getClass().getSimpleName()
                     + ") was larger than I expected, found "
                     + friendlybytebuf.readableBytes()
                     + " bytes extra whilst reading packet "
                     + j
               );
            } else {
               p_130537_.add(packet);
               if (LOGGER.isDebugEnabled()) {
                  LOGGER.debug(Connection.PACKET_RECEIVED_MARKER, " IN: [{}:{}] {}", codecdata.protocol().id(), j, packet.getClass().getName());
               }

               ProtocolSwapHandler.swapProtocolIfNeeded(attribute, packet);
            }
         }
      }
   }
}

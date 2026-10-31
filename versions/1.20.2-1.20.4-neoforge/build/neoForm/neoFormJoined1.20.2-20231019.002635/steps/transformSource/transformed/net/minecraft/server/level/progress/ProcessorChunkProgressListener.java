package net.minecraft.server.level.progress;

import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;

public class ProcessorChunkProgressListener implements ChunkProgressListener {
   private final ChunkProgressListener delegate;
   private final ProcessorMailbox<Runnable> mailbox;

   private ProcessorChunkProgressListener(ChunkProgressListener p_9640_, Executor p_9641_) {
      this.delegate = p_9640_;
      this.mailbox = ProcessorMailbox.create(p_9641_, "progressListener");
   }

   public static ProcessorChunkProgressListener createStarted(ChunkProgressListener p_143584_, Executor p_143585_) {
      ProcessorChunkProgressListener processorchunkprogresslistener = new ProcessorChunkProgressListener(p_143584_, p_143585_);
      processorchunkprogresslistener.start();
      return processorchunkprogresslistener;
   }

   @Override
   public void updateSpawnPos(ChunkPos p_9643_) {
      this.mailbox.tell(() -> this.delegate.updateSpawnPos(p_9643_));
   }

   @Override
   public void onStatusChange(ChunkPos p_9645_, @Nullable ChunkStatus p_9646_) {
      this.mailbox.tell(() -> this.delegate.onStatusChange(p_9645_, p_9646_));
   }

   @Override
   public void start() {
      this.mailbox.tell(this.delegate::start);
   }

   @Override
   public void stop() {
      this.mailbox.tell(this.delegate::stop);
   }
}

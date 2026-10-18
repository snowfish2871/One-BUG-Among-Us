// 像 tail -f 一样跟着日志文件读；文件被轮转或截断就从头再来一遍。
package dev.oba.core;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class LogWatcher implements AutoCloseable {

    // 读到一行就往外发一个事件，顺便带上这一行的初步判断
    public record Event(long offset, long lineNumber, String line, Diagnosis.KnownIssue issue, boolean fatalHint) {
        public boolean isIssue() {
            return issue != null;
        }
    }

    private final Path file;
    private final Consumer<Event> sink;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final long pollMillis;
    private Thread thread;
    private long lineNumber;

    public LogWatcher(Path file, Consumer<Event> sink, long pollMillis) {
        this.file = file.toAbsolutePath().normalize();
        this.sink = sink;
        // 再急也别快过 50ms，不然就是白烧 CPU
        this.pollMillis = Math.max(50, pollMillis);
    }

    // 阻塞式主循环，自己丢到守护线程上跑
    public void runBlocking() {
        running.set(true);
        long offset = 0;
        long inode = -1;
        while (running.get()) {
            try {
                if (!Files.isRegularFile(file)) {
                    Thread.sleep(pollMillis);
                    continue;
                }
                long currentInode = fileKey();
                long size = Files.size(file);
                if (offset > size || (inode != -1 && currentInode != inode)) {
                    // 文件被轮转或截断了，当成新文件重新读
                    offset = 0;
                    lineNumber = 0;
                }
                inode = currentInode;
                if (size > offset) {
                    offset = readFrom(offset);
                } else {
                    Thread.sleep(pollMillis);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (IOException e) {
                // 文件正被游戏写着，偶发读失败很正常，歇一会儿再试
                try {
                    Thread.sleep(Math.max(250, pollMillis));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    // 起一个守护线程把行推给 sink
    public LogWatcher startDaemon(String threadName) {
        thread = new Thread(this::runBlocking, threadName);
        thread.setDaemon(true);
        thread.start();
        return this;
    }

    public boolean isRunning() {
        return running.get();
    }

    public long lastLineNumber() {
        return lineNumber;
    }

    public Path file() {
        return file;
    }

    // 从 offset 往后读，凑齐整行才发出去；返回新的 offset
    private long readFrom(long offset) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            raf.seek(offset);
            long pos = offset;
            // 按字节切行、切完整行之后才解码。这样既不会把多字节字符从中间劈开
            //（一块读 16KB，正好切在汉字中间就会出乱码），也能容忍 GBK 日志
            java.io.ByteArrayOutputStream pending = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = raf.read(buf)) > 0) {
                pos += n;
                for (int i = 0; i < n; i++) {
                    if (buf[i] == '\n') {
                        emit(decodeLine(pending.toByteArray()));
                        pending.reset();
                    } else {
                        pending.write(buf[i]);
                    }
                }
            }
            return pos;
        }
    }

    // 去掉行尾的 \r，再按内容猜编码
    private String decodeLine(byte[] raw) {
        int len = raw.length;
        if (len > 0 && raw[len - 1] == '\r') {
            len--;
        }
        return LogAnalyzer.decode(java.util.Arrays.copyOf(raw, len));
    }

    private void emit(String line) {
        lineNumber++;
        Diagnosis.KnownIssue issue = null;
        boolean fatalHint = false;
        if (!line.isBlank()) {
            Optional<Diagnosis.KnownIssue> found = LogAnalyzer.classifyLine(line);
            if (found.isPresent()) {
                issue = found.get();
                fatalHint = issue.severity() == Diagnosis.Severity.FATAL;
            } else if (looksLikeStackTraceStart(line)) {
                // 命不中规则但明显是异常开头，也提醒一下
                fatalHint = true;
            }
        }
        sink.accept(new Event(0, lineNumber, line, issue, fatalHint));
    }

    // 判断这一行是不是一段堆栈的开头：Caused by: ... 或 java.lang.XxxException: ...
    private static boolean looksLikeStackTraceStart(String line) {
        String t = line.trim();
        if (t.startsWith("Caused by:") || t.startsWith("Suppressed:")) {
            return true;
        }
        if (t.startsWith("java.lang.") || t.startsWith("java.util.")
                || t.startsWith("java.io.") || t.startsWith("net.minecraft.")
                || t.startsWith("net.neoforged.")) {
            return t.contains("Exception") || t.contains("Error") || t.contains("Throwable");
        }
        return false;
    }

    // 文件身份，用来发现"同名文件被换了一个"
    private long fileKey() {
        try {
            Object key = Files.readAttributes(file, java.nio.file.attribute.BasicFileAttributes.class).fileKey();
            return key == null ? 0L : key.hashCode();
        } catch (IOException e) {
            return -1L;
        }
    }

    // 固定长度的行缓冲，用来留住报错前后的上下文
    public static final class RingBuffer {
        private final String[] lines;
        private int index;
        private int size;

        public RingBuffer(int capacity) {
            this.lines = new String[Math.max(1, capacity)];
        }

        public synchronized void add(String line) {
            lines[index] = line;
            index = (index + 1) % lines.length;
            if (size < lines.length) {
                size++;
            }
        }

        // 按写入顺序取出当前内容
        public synchronized List<String> snapshot() {
            List<String> out = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                int at = (index - size + i + lines.length * 2) % lines.length;
                out.add(lines[at]);
            }
            return out;
        }

        public synchronized int size() {
            return size;
        }
    }

    @Override
    public void close() {
        running.set(false);
        if (thread != null) {
            thread.interrupt();
        }
    }
}

// 模组文件不一定是磁盘上的普通文件。
//
// NeoForge 会把「套在另一个 jar 里」的模组报成一个属于 zip 文件系统的路径，
// 这种 Path 调 toFile() 会直接抛 UnsupportedOperationException —— 它不属于默认文件系统。
// 这里把「按需随机访问」和「流式扫一遍」分开：普通文件继续用 JarFile，其余一律流式读。
package dev.oba.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ModFiles {

    private ModFiles() {
    }

    /** 这个路径是不是默认文件系统上的普通文件（只有这种情况才能 toFile）。 */
    public static boolean isPlainFile(Path path) {
        return path != null && path.getFileSystem() == FileSystems.getDefault();
    }

    /** 值得读进来看一眼的文本条目。 */
    private static boolean looksTextual(String lower) {
        return lower.endsWith(".json") || lower.endsWith(".mcmeta") || lower.endsWith(".txt");
    }

    /**
     * 流式扫一遍 jar（也能读套在别的 zip 里的 jar），返回「条目名 → 文本」。
     *
     * <p>不做随机访问，因此不需要 toFile()，任何文件系统都适用。
     */
    public static Map<String, String> smallTextEntries(Path path, int limit, long maxBytes)
            throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (InputStream raw = Files.newInputStream(path);
             ZipInputStream zin = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (!looksTextual(name.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                // 压缩条目的大小有时是 -1（未知），所以边读边数，超了就当没有
                byte[] data = readBounded(zin, maxBytes);
                if (data == null) {
                    continue;
                }
                out.put(name, new String(data, StandardCharsets.UTF_8));
                if (out.size() >= limit) {
                    break;
                }
            }
        }
        return out;
    }

    /** 这个 jar 里有没有某个条目（同样不要求默认文件系统）。 */
    public static boolean contains(Path path, String entryName) {        try (InputStream raw = Files.newInputStream(path);
             ZipInputStream zin = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (entryName.equals(entry.getName())) {
                    return true;
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // 读不了就按「没有」处理
        }
        return false;
    }

    /** 读一个条目的全部字节；读不到返回 null。同样不要求默认文件系统。 */
    public static byte[] readEntry(Path path, String entryName, long maxBytes) {
        try (InputStream raw = Files.newInputStream(path);
             ZipInputStream zin = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (entryName.equals(entry.getName())) {
                    return readBounded(zin, maxBytes);
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // 读不了就当没有
        }
        return null;
    }

    /** 有上限地读一个条目；超过上限返回 null，免得为了一点标记把整个大文件读进内存。 */
    private static byte[] readBounded(InputStream in, long maxBytes) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(chunk)) > 0) {
            total += n;
            if (total > maxBytes) {
                return null;
            }
            buffer.write(chunk, 0, n);
        }
        return buffer.toByteArray();
    }
}

// 加载器报出来的一条模组信息。各家接口的字段名不一样，
// 在这里抹平成共享代码能用的样子。
package dev.oba.loader;

import java.nio.file.Path;

public record LoadedMod(String modId, String displayName, String version, Path file) {
}

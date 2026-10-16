// 存当前轨道装进来的那一份加载器实现。构造函数开头调一次 bind 就行。
package dev.oba.loader;

public final class Loader {

    private static volatile LoaderEnv env;

    // 实现方在 init 里把自己交进来
    public static void bind(LoaderEnv value) {
        env = value;
    }

    // 拿当前实现。还没绑定时返回一个什么都不做的兜底实现，
    // 免得任何一句调用都要判空 —— 这是个诊断工具，它自己必须先活着
    public static LoaderEnv env() {
        LoaderEnv current = env;
        return current != null ? current : Fallback.INSTANCE;
    }

    // 已经绑定过真正的实现了吗
    public static boolean ready() {
        return env != null;
    }

    // 没绑定时用的空实现
    private static final class Fallback implements LoaderEnv {

        static final Fallback INSTANCE = new Fallback();

        @Override
        public String loaderName() {
            return "unknown";
        }

        @Override
        public java.util.List<LoadedMod> mods() {
            return java.util.List.of();
        }

        @Override
        public java.nio.file.Path gameDir() {
            return null;
        }

        @Override
        public Object modEventBus() {
            return null;
        }

        @Override
        public void init() {
            // 什么都不做
        }

        @Override
        public void onServerStopping(Runnable callback) {
            // 什么都不做
        }
    }

    private Loader() {
    }
}

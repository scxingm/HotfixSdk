package cn.scxingm.hotfix;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashSet;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * SDK 热更新类
 * 目前测试出该类不能引用其他类，否则将导致更新失败
 * Created by scxingm on 2018/2/6.
 */

public final class HotfixInstall {

    private static HashSet<File> loadedDex = new HashSet<>();
    private static final String DEX_SUFFIX = ".dex";
    private static final String APK_SUFFIX = ".apk";
    private static final String JAR_SUFFIX = ".jar";
    private static final String ZIP_SUFFIX = ".zip";
    private static final String OPTIMIZE_DEX_DIR = "optimize_dex";
    private static final String PATCH_NAME = "hotfix-sdk"; // 这是更新包文件名开头
    public static final String DEX_DIR = "sdk_patch"; // 这是更新包文件存放位置
    public static final String PATCH_END_STRING = "-patch.jar"; // 这是更新包文件名结束

    static {
        loadedDex.clear();
    }

    /**
     * 加载补丁，使用默认目录：data/data/包名/files/odex
     * @param context
     */
    public static void init(Context context) {
        init(context, null);
    }

    /**
     * 加载补丁
     * @param context       上下文
     * @param patchFilesDir 补丁所在目录
     */
    public static void init(Context context, File patchFilesDir) {
        if (context == null) return;
        try{
            // 遍历所有的修复dex
            File fileDir = patchFilesDir != null ?
                    patchFilesDir : new File(context.getFilesDir(), DEX_DIR);// data/data/包名/files/odex（这个可以任意位置）
            File[] listFiles = fileDir.listFiles();
            if (listFiles == null || listFiles.length == 0) return;
            Log.e("Aads", "---> | patch | find new patches.");
            for (File file : listFiles) {
                if (file.getName().endsWith(PATCH_END_STRING) &&
                        (file.getName().endsWith(DEX_SUFFIX)
                                || file.getName().endsWith(APK_SUFFIX)
                                || file.getName().endsWith(JAR_SUFFIX)
                                || file.getName().endsWith(ZIP_SUFFIX))) {
                    Log.w("Aads", "---> | patch | find patch "+file.getName());
                    loadedDex.add(file);// 存入集合
                } else {
                    Log.e("Aads", "---> | patch | no have aads patch.");
                }
            }

            if (loadedDex.size() > 0)
                // dex合并之前的dex
                doDexInject(context, loadedDex);
        }catch (Exception e){
            Log.e("Aads", "---> | patch | update the patch load failed.");
        }
    }

    /**
     * 合并dex
     * @param appContext
     * @param loadedDex
     */
    private static void doDexInject(Context appContext, HashSet<File> loadedDex) {
        String optimizeDir = appContext.getFilesDir().getAbsolutePath() + File.separator + OPTIMIZE_DEX_DIR;// data/data/包名/files/optimize_dex（这个必须是自己程序下的目录）
        File fopt = new File(optimizeDir);
        if (!fopt.exists()) {
            fopt.mkdirs();
        }
        try {
            // 1.加载应用程序的dex
            PathClassLoader pathLoader = (PathClassLoader) appContext.getClassLoader();
            for (File dex : loadedDex) {
                // 2.加载指定的修复的dex文件
                DexClassLoader dexLoader = new DexClassLoader(
                        dex.getAbsolutePath(),// 修复好的dex（补丁）所在目录
                        fopt.getAbsolutePath(),// 存放dex的解压目录（用于jar、zip、apk格式的补丁）
                        null,// 加载dex时需要的库
                        pathLoader// 父类加载器
                );
                // 3.合并
                Object dexPathList = getPathList(dexLoader);
                Object pathPathList = getPathList(pathLoader);
                Object leftDexElements = getDexElements(dexPathList);
                Object rightDexElements = getDexElements(pathPathList);
                // 合并完成
                Object dexElements = combineArray(leftDexElements, rightDexElements);
                // 重写给PathList里面的Element[] dexElements;赋值
                Object pathList = getPathList(pathLoader);// 一定要重新获取，不要用pathPathList，会报错
                setField(pathList, pathList.getClass(), "dexElements", dexElements);
            }
        } catch (Exception e) {
            Log.e("Aads", "---> | patch | update the patch merge failed.");
        }
    }

    /**
     * 反射给对象中的属性重新赋值
     */
    private static void setField(Object obj, Class<?> cl, String field, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field declaredField = cl.getDeclaredField(field);
        declaredField.setAccessible(true);
        declaredField.set(obj, value);
    }

    /**
     * 反射得到对象中的属性值
     */
    private static Object getField(Object obj, Class<?> cl, String field) throws NoSuchFieldException, IllegalAccessException {
        Field localField = cl.getDeclaredField(field);
        localField.setAccessible(true);
        return localField.get(obj);
    }


    /**
     * 反射得到类加载器中的pathList对象
     */
    private static Object getPathList(Object baseDexClassLoader) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        return getField(baseDexClassLoader, Class.forName("dalvik.system.BaseDexClassLoader"), "pathList");
    }

    /**
     * 反射得到pathList中的dexElements
     */
    private static Object getDexElements(Object pathList) throws NoSuchFieldException, IllegalAccessException {
        return getField(pathList, pathList.getClass(), "dexElements");
    }

    /**
     * 数组合并
     */
    private static Object combineArray(Object arrayLhs, Object arrayRhs) {
        Class<?> componentType = arrayLhs.getClass().getComponentType();
        int i = Array.getLength(arrayLhs);// 得到左数组长度（补丁数组）
        int j = Array.getLength(arrayRhs);// 得到原dex数组长度
        int k = i + j;// 得到总数组长度（补丁数组+原dex数组）
        Object result = Array.newInstance(componentType, k);// 创建一个类型为componentType，长度为k的新数组
        System.arraycopy(arrayLhs, 0, result, 0, i);
        System.arraycopy(arrayRhs, 0, result, i, j);
        return result;
    }
}

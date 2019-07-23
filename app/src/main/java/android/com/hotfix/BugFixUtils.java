package android.com.hotfix;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashSet;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class BugFixUtils {
    private static final String DEX_SUFFIX = ".dex";
    private static final String APK_SUFFIX = ".apk";
    private static final String ZIP_SUFFIX = ".zip";
    private static final String JAR_SUFFIX = ".jar";

    public static final String DEX_DIR = "odex";
    private static final String OPTIMIZE_DEX_DIR = "optimize_dex";
    private static HashSet<File> loadedDex = new HashSet<>();

    static {
        loadedDex.clear();
    }

    /*
     * 加载补丁 使用默认目录 data/data/packageName/files/odex
     * */
//    public static void loadFixedDex(Context context) {
//        loadFixedDex(context, null);
//    }

    public static void loadFixedDex(Context context,OnFixListener onFixListener) {
        doDexInject(context, onFixListener,loadedDex);
    }

    private static void doDexInject(Context context, OnFixListener onFixListener, HashSet<File> loadedDex) {
        String optimizeDir = context.getFilesDir().getAbsolutePath() + File.separator + OPTIMIZE_DEX_DIR;
        //data/data/包名/files/optimize_dex（这个必须是自己程序下的目录）
        File fopt = new File(optimizeDir);
        if (!fopt.exists()) {
            fopt.mkdirs();
        }

        try {
            //1.加载应用程序dex的loader
            PathClassLoader pathClassLoader = (PathClassLoader) context.getClassLoader();
            for (File dex :
                    loadedDex) {
                //2.加载指定的修复的dex文件的loader
                DexClassLoader dexClassLoader = new DexClassLoader(
                        dex.getAbsolutePath(),//修复好的dex（补丁）所在的目录
                        fopt.getAbsolutePath(),//存放dex的解压目录
                        null,//加载dex时需要的库
                        pathClassLoader//父类加载器
                );
                //3。开始合并
                //合并的目标 Element[] 重新赋值

                /*
                 * BaseDexClassLoader 中 有变量 DexPathList pathList
                 * DexPathList 中 有变量 Element[] dexElements
                 * 反射获取
                 * */

                //3。1准备好pathList的引用
                Object dexPathList = getPathList(dexClassLoader);
                Object pathPathList = getPathList(pathClassLoader);

                //3.2从pathList中反射出element集合
                Object dexElement = getElements(dexPathList);
                Object pathElement = getElements(pathPathList);

                //3.3合并两个dex数组
                Object dexElements = combinerArray(dexElement, pathElement);

                //给PathClassLoader中的pathList中的dexElements 赋值
                Object pathList = getPathList(pathClassLoader);
                setField(pathList, pathList.getClass(), "dexElements", dexElements);
            }
            onFixListener.onFixed();
            Toast.makeText(context, "修复完成", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setField(Object pathList, Class<?> aClass, String dexElements, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field declaredField = aClass.getDeclaredField(dexElements);
        declaredField.setAccessible(true);
        declaredField.set(pathList, value);
    }

    private static Object combinerArray(Object dexElement, Object pathElement) {
        Class<?> clazz = dexElement.getClass().getComponentType();
        int dexLength = Array.getLength(dexElement);
        int pathLength = Array.getLength(pathElement);
        int length = dexLength + pathLength;
        Object result = Array.newInstance(clazz, length);
        System.arraycopy(dexElement, 0, result, 0, dexLength);
        System.arraycopy(pathElement, 0, result, dexLength, pathLength);
        return result;
    }

    private static Object getElements(Object dexPathList)
            throws NoSuchFieldException, IllegalAccessException {
        return getField(dexPathList, dexPathList.getClass(), "dexElements");
    }

    private static Object getPathList(Object obj)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        return getField(obj, Class.forName("dalvik.system.BaseDexClassLoader"), "pathList");
    }

    private static Object getField(Object object, Class<?> forName, String field)
            throws NoSuchFieldException, IllegalAccessException {
        Field localField = forName.getDeclaredField(field);
        localField.setAccessible(true);
        return localField.get(object);
    }

    public static boolean isGoingToFix(@NonNull Context context) {
        boolean canFix = false;
        File externalStorageDirectory = Environment.getExternalStorageDirectory();

        // 遍历所有的修复dex , 因为可能是多个dex修复包
        File fileDir = externalStorageDirectory != null ?
                new File(externalStorageDirectory, "007") :
                new File(context.getFilesDir(), DEX_DIR);// data/data/包名/files/odex（这个可以任意位置）

        File[] listFiles = fileDir.listFiles();
        if (listFiles != null) {
            for (File file : listFiles) {
                Log.e("lzf_path_1",file.getName());
                String fileName=file.getName();
                if (fileName.startsWith("classes") &&
                        (fileName.endsWith(DEX_SUFFIX)
                                || fileName.endsWith(APK_SUFFIX)
                                || fileName.endsWith(JAR_SUFFIX)
                                || fileName.endsWith(ZIP_SUFFIX))) {

                    loadedDex.add(file);// 存入集合
                    //有目标dex文件, 需要修复
                    canFix = true;
                }
            }
        }
        return canFix;
    }
}

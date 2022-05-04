package com.husky.projection.push.util;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;

/**
 * 获取存储路径工具类
 * 总共分为四类存储路径：
 * 1、/data/data/包名/
 * 2、/sdcard/Android/data/包名/
 * 3、/sdcard/xxx
 * 4、扩展sd卡不考虑
 *
 * <p>
 * Created by luhailong on 2017/7/4.
 */
public class StorageUtil {

    private static final String TAG = "StorageUtil";

    /**
     * 检查外部存储是否可用
     *
     * @return
     */
    public static boolean isExitsSdcard() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) || !Environment.isExternalStorageRemovable())
            return true;
        else
            return false;
    }

    /**
     * 获取内部存储缓存路径(不需要读写权限(4.4及以上)也不需要判断外部存储是否可用)
     *
     * @param context 上下文
     * @param dirName 文件夹名称
     * @return
     */
    public static String getInternalCacheDir(Context context, String dirName) {
        if (!TextUtils.isEmpty(dirName)) {
            File dir = new File(context.getCacheDir(), dirName);

            if (dir.exists())
                return dir.getPath();

            if (dir.mkdirs()) {
                return dir.getPath();
            }
            Log.d(TAG, "mkdirs fail!-->" + dirName);
        }
        return context.getCacheDir().getPath();
    }

    /**
     * 获取内部存储文件路径(不需要读写权限(4.4及以上)也不需要判断外部存储是否可用)
     * 根据type获取对应的目录路径(type为空返回内部存储文件根目录路径)
     *
     * @param context 上下文
     * @param dirName 文件夹名称
     * @return
     */
    public static String getInternalFilesDir(Context context, String dirName) {
        if (!TextUtils.isEmpty(dirName)) {
            File dir = new File(context.getFilesDir(), dirName);

            if (dir.exists())
                return dir.getPath();

            if (dir.mkdirs()) {
                return dir.getPath();
            }
            Log.d(TAG, "mkdirs fail!-->" + dirName);
        }
        return context.getFilesDir().getPath();
    }

    /**
     * 获取外部存储缓存路径(不需要读写权限(4.4及以上))
     *
     * @param context 上下文
     * @param dirName 文件夹名称
     * @return
     */
    public static String getExternalCacheDir(Context context, String dirName) {
        if (isExitsSdcard()) {
            if (!TextUtils.isEmpty(dirName)) {
                File dir = new File(context.getExternalCacheDir(), dirName);

                if (dir.exists())
                    return dir.getPath();

                if (dir.mkdirs()) {
                    return dir.getPath();
                }
                Log.d(TAG, "mkdirs fail!-->" + dirName);
            }

            File cache = context.getExternalCacheDir();
            if (cache != null) {
                return cache.getPath();
            }
        }
        return null;
    }

    /**
     * 获取外部存储文件路径(不需要读写权限(4.4及以上))
     * 根据type获取对应的目录路径(type为空返回外部存储缓存路径)
     *
     * @param context 上下文
     * @param dirName 文件夹名称
     * @return
     */
    public static String getExternalFilesDir(Context context, String dirName) {
        if (isExitsSdcard()) {
            File file = context.getExternalFilesDir(dirName);
            if (file != null) {
                return file.getPath();
            }
        }
        return null;
    }

    /**
     * 获取外部存储共有路径(根据type获取对应的目录)(type为空返回外部存储共有根目录路径)
     *
     * @param type 公共目录类型：
     *             Environment.DIRECTORY_ALARMS
     *             Environment.DIRECTORY_DCIM
     *             ...
     *             传入自定义文件夹名称无法创建对应的目录
     * @return
     */
    public static String getExternalStoragePublicDirectory(String type) {
        return isExitsSdcard() ? TextUtils.isEmpty(type) ? Environment.getExternalStorageDirectory().getPath() : Environment.getExternalStoragePublicDirectory(type).getPath() : null;
    }
}

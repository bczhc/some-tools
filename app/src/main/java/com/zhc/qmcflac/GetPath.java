package com.zhc.qmcflac;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

@SuppressWarnings("JavaDoc")
class GetPath {
    /**
     * 根据Uri获取图片路径，专为Android4.4设计
     *
     * @param act
     * @param uri
     * @return
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    String getPathFromUriOnKitKat(Activity act, Uri uri) {
        /*
         * uri=content://com.android.providers.media.documents/document/image%3A293502  4.4以后
         * uri=file:///storage/emulated/0/temp_photo.jpg
         * uri=content://media/external/images/media/193968
         *
         * uri=content://media/external/images/media/13   4.4以前
         */
        String path = null;
        if (DocumentsContract.isDocumentUri(act, uri)) {
            // 如果是document类型的Uri，则通过document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1]; // 解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                path = getPathFromUri(act, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                path = getPathFromUri(act, contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // 如果是content类型的Uri，则使用普通方式处理
            path = getPathFromUri(act, uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            // 如果是file类型的Uri，直接获取图片路径即可
            path = uri.getPath();
        }
        return path;
    }
/*---------------------
    作者：CoderCF
    来源：CSDN
    原文：https://blog.csdn.net/chengfu116/article/details/74923161
    版权声明：本文为博主原创文章，转载请附上博文链接！*/

/*    *//*
     * 根据Uri获取图片路径，Android4.4以前
     *
     * @param act
     * @param uri
     * @return
     *//*
    public String getPathFromUriBeforeKitKat(Activity act, Uri uri) {
        return getPathFromUri(act, uri, null);
    }*/

    /**
     * 通过Uri和selection来获取真实的图片路径
     *
     * @param act
     * @param uri
     * @param selection
     * @return
     */
    private static String getPathFromUri(Activity act, Uri uri, String selection) {
        String path = null;
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = act.getContentResolver().query(uri, projection, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }
/*---------------------
    作者：CoderCF
    来源：CSDN
    原文：https://blog.csdn.net/chengfu116/article/details/74923161
    版权声明：本文为博主原创文章，转载请附上博文链接！*/
}

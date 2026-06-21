package com.atuy.yws1editor.shizuku;

import android.os.ParcelFileDescriptor;

interface IShizukuFileService {
    ParcelFileDescriptor openFileForRead(String path);
    void writeFileAtomically(String path, in ParcelFileDescriptor source, long expectedLength, String expectedSha256);
    void copyFile(String sourcePath, String targetPath);
    void createDirectories(String path);
    String[] listFileNames(String path);
    long lastModified(String path);
}

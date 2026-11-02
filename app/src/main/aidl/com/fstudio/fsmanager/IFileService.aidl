package com.fstudio.fsmanager;

interface IFileService {
    void destroy() = 16777114;
    String[] listDir(String path) = 1;
    boolean delete(String path) = 2;
}

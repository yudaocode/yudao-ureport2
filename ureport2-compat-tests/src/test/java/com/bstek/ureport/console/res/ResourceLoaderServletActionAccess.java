package com.bstek.ureport.console.res;

/**
 * 测试辅助：访问 ResourceLoaderServletAction 包内可见的路径校验方法
 *
 * @author 芋道源码
 */
public final class ResourceLoaderServletActionAccess {

    private ResourceLoaderServletActionAccess() {
    }

    public static String check(String path) {
        return ResourceLoaderServletAction.checkAssetPath(path);
    }

}

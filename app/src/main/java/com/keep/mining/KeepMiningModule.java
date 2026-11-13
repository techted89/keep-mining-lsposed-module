package com.keep.mining;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedBridge;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.callback.BeforeInvocation;
import io.github.libxposed.api.callback.BeforeHookCallback;
import io.github.libxposed.api.Hooker;
import java.lang.reflect.Method;

/**
 * LSPosed module to keep Bitcoin mining running in background.
 * Hooks the mining stop lifecycle functions to prevent the app from stopping mining
 * when the app goes to background, screen turns off, or app is closed.
 */
public class KeepMiningModule implements XposedModule {

    private XposedInterface framework;

    @Override
    public void attachFramework(XposedInterface framework) {
        this.framework = framework;
    }

    @Override
    public void onPackageLoaded(String packageName) {
        if (!packageName.equals("com.orangepi.bitcoin")) {
            return;
        }

        XposedBridge.log("KeepMiningModule: Hooking target package " + packageName);

        try {
            // Hook 1: didChangeAppLifecycleState - the main Flutter lifecycle callback
            Class<?> lifecycleServiceClass = framework.findClass("me.carda.awesome_notifications.LifecycleService");
            if (lifecycleServiceClass != null) {
                hookLifecycleMethods(lifecycleServiceClass);
            }

            // Hook 2: MiningService stop methods
            Class<?> miningServiceClass = framework.findClass("me.carda.awesome_notifications.mining.MiningService");
            if (miningServiceClass != null) {
                hookMiningServiceMethods(miningServiceClass);
            }

            // Hook 3: _stopMiningIfNeeded - the core stop decision logic
            Class<?> miningControllerClass = framework.findClass("com.orangepi.bitcoin.mining.MiningController");
            if (miningControllerClass != null) {
                hookStopMiningIfNeeded(miningControllerClass);
            }

            XposedBridge.log("KeepMiningModule: All hooks installed for " + packageName);

        } catch (Exception e) {
            XposedBridge.log("KeepMiningModule: Error installing hooks - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void hookLifecycleMethods(Class<?> lifecycleServiceClass) {
        try {
            Method method = framework.findMethod(lifecycleServiceClass, "didChangeAppLifecycleState", "int");
            if (method != null) {
                framework.hook(method, XposedHooker.class);
                XposedBridge.log("KeepMiningModule: Hooked didChangeAppLifecycleState");
            }

            try {
                Method method2 = framework.findMethod(lifecycleServiceClass, "handleAppLifecycleStateChanged", "int");
                if (method2 != null) {
                    framework.hook(method2, XposedHooker.class);
                    XposedBridge.log("KeepMiningModule: Hooked handleAppLifecycleStateChanged");
                }
            } catch (Exception ignored) {}
        } catch (Exception e) {
            XposedBridge.log("KeepMiningModule: Error hooking lifecycle methods - " + e.getMessage());
        }
    }

    private void hookMiningServiceMethods(Class<?> miningServiceClass) {
        try {
            Method stopMethod = framework.findMethod(miningServiceClass, "stopMining", "(Landroid/content/Context;)V");
            if (stopMethod != null) {
                framework.hook(stopMethod, XposedHooker.class);
                XposedBridge.log("KeepMiningModule: Hooked MiningService.stopMining(Context)");
            }

            try {
                Method miningStopMethod = framework.findMethod(miningServiceClass, "stopMining", "()V");
                if (miningStopMethod != null) {
                    framework.hook(miningStopMethod, XposedHooker.class);
                    XposedBridge.log("KeepMiningModule: Hooked MiningService.stopMining()");
                }
            } catch (Exception ignored) {}
        } catch (Exception e) {
            XposedBridge.log("KeepMiningModule: Error hooking MiningService methods - " + e.getMessage());
        }
    }

    private void hookStopMiningIfNeeded(Class<?> miningControllerClass) {
        try {
            Method stopMethod = framework.findMethod(miningControllerClass, "_stopMiningIfNeeded", "()V");
            if (stopMethod != null) {
                framework.hook(stopMethod, XposedHooker.class);
                XposedBridge.log("KeepMiningModule: Hooked MiningController._stopMiningIfNeeded");
            }

            try {
                for (Method m : miningControllerClass.getMethods()) {
                    String name = m.getName().toLowerCase();
                    if (name.contains("stopmining") || name.contains("stop_mining")) {
                        framework.hook(m, XposedHooker.class);
                        XposedBridge.log("KeepMiningModule: Hooked method: " + m.getName());
                    }
                }
            } catch (Exception ignored) {}
        } catch (Exception e) {
            XposedBridge.log("KeepMiningModule: Error hooking stop mining methods - " + e.getMessage());
        }
    }

    public static class XposedHooker {
        @BeforeInvocation
        public static void before(BeforeHookCallback callback) {
            callback.returnAndSkip(null);
        }
    }
}
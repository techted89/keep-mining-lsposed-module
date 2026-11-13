package com.keep.mining;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedBridge;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.callback.BeforeInvocation;
import io.github.libxposed.api.Hooker;
import io.github.libxposed.api.MethodUnhooker;

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
        // Only hook our target package
        if (!packageName.equals("com.orangepi.bitcoin")) {
            return;
        }

        XposedBridge.log("KeepMiningModule: Hooking target package " + packageName);

        try {
            // Hook 1: didChangeAppLifecycleState - the main Flutter lifecycle callback
            // This is called when app goes to background/inactive/paused/hidden
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

    /**
     * Hook Flutter LifecycleService lifecycle methods
     */
    private void hookLifecycleMethods(Class<?> lifecycleServiceClass) {
        try {
            // Hook didChangeAppLifecycleState method
            // This is the entry point called by WidgetsBindingObserver
            Method method = framework.findMethod(lifecycleServiceClass, "didChangeAppLifecycleState", "int");
            if (method != null) {
                Hooker hooker = new XposedHooker();
                MethodUnhooker unhooker = framework.hook(method, hooker.getClass());
                XposedBridge.log("KeepMiningModule: Hooked didChangeAppLifecycleState");
            }

            // Also hook handleAppLifecycleStateChanged if it exists
            try {
                Method method2 = framework.findMethod(lifecycleServiceClass, "handleAppLifecycleStateChanged", "int");
                if (method2 != null) {
                    Hooker hooker2 = new XposedHooker();
                    MethodUnhooker unhooker2 = framework.hook(method2, hooker2.getClass());
                    XposedBridge.log("KeepMiningModule: Hooked handleAppLifecycleStateChanged");
                }
            } catch (Exception e) {
                // Method may not exist
            }

        } catch (Exception e) {
            XposedBridge.log("KeepMiningModule: Error hooking lifecycle methods - " + e.getMessage());
        }
    }

    /**
     * Hook MiningService methods that control mining stop
     */
    private void hookMiningServiceMethods(Class<?> miningServiceClass) {
        try {
            // Hook the stopMining method
            Method stopMethod = framework.findMethod(miningServiceClass, "stopMining", "(Landroid/content/Context;)V");
            if (stopMethod != null) {
                Hooker hooker = new XposedHooker();
                MethodUnhooker unhooker = framework.hook(stopMethod, hooker.getClass());
                XposedBridge.log("KeepMiningModule: Hooked MiningService.stopMining");
            }

            // Hook any method containing "stopMining" 
            try {
                Method miningStopMethod = framework.findMethod(miningServiceClass, "stopMining", "()V");
                if (miningStopMethod != null) {
                    Hooker hooker = new XposedHooker();
                    MethodUnhooker unhooker = framework.hook(miningStopMethod, hooker.getClass());
                    XposedBridge.log("KeepMiningModule: Hooked MiningService.stopMining()");
                }
            } catch (Exception e) {
                // Method signature may vary
            }

        } catch (Exception e) {
            XposedBridge.log("KeepMiningModule: Error hooking MiningService methods - " + e.getMessage());
        }
    }

    /**
     * Hook _stopMiningIfNeeded logic in MiningController
     */
    private void hookStopMiningIfNeeded(Class<?> miningControllerClass) {
        try {
            // Hook _stopMiningIfNeeded method
            Method stopMethod = framework.findMethod(miningControllerClass, "_stopMiningIfNeeded", "()V");
            if (stopMethod != null) {
                Hooker hooker = new XposedHooker();
                MethodUnhooker unhooker = framework.hook(stopMethod, hooker.getClass());
                XposedBridge.log("KeepMiningModule: Hooked MiningController._stopMiningIfNeeded");
            }

            // Also hook any method with "stopMining" in name
            try {
                for (Method m : miningControllerClass.getMethods()) {
                    if (m.getName().toLowerCase().contains("stopmining") || m.getName().toLowerCase().contains("stop_mining")) {
                        Hooker hooker = new XposedHooker();
                        MethodUnhooker unhooker = framework.hook(m, hooker.getClass());
                        XposedBridge.log("KeepMiningModule: Hooked method: " + m.getName());
                    }
                }
            } catch (Exception e) {
                // Iteration may fail
            }

        } catch (Exception e) {
            XposedBridge.log("KeepMiningModule: Error hooking stop mining methods - " + e.getMessage());
        }
    }

    /**
     * Modern Hooker class annotated with @XposedHooker
     * Handles before-invocation callbacks to block mining stop
     */
    public static class XposedHooker implements Hooker {
        
        @BeforeInvocation
        public static void before(BeforeHookCallback callback) {
            // This callback runs before the hooked method
            // We can block the mining stop by returning and skipping
            // The callback is for the method being hooked
            // We set a flag to indicate we want to skip/block
            callback.setResult(null);
            // Don't call proceed() - this effectively blocks the original method
            // The framework will not execute the original method
        }

        @BeforeInvocation
        public static void beforeBlockMining(BeforeHookCallback callback) {
            // Specific callback for mining stop blocking
            // We want to completely block mining stop execution
            callback.returnAndSkip(null);
            // This tells the framework to skip the original method entirely
        }
    }
}
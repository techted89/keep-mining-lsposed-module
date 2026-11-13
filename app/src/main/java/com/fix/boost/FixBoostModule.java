package com.fix.boost;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedBridge;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.callback.BeforeInvocation;
import io.github.libxposed.api.callback.BeforeHookCallback;

public class FixBoostModule implements XposedModule {

    @Override
    public void onPackageLoaded(String packageName) {
        if (!packageName.equals("com.orangepi.bitcoin")) return;
        
        XposedBridge.log("FixBoostModule: Hooking boost system for " + packageName);
        
        try {
            hookSubscriptionCheck();
            hookAdTimeout();
            hookRateCalculation();
            hookRandomAdSkip();
        } catch (Exception e) {
            XposedBridge.log("FixBoostModule: Error - " + e.getMessage());
        }
    }

    private void hookSubscriptionCheck() throws Exception {
        Class<?> subService = framework.findClass("me.carda.awesome_notifications.subscription.SubscriptionVerificationService");
        if (subService != null) {
            Method method = framework.findMethod(subService, "hasActiveSubscription", "()Z");
            if (method != null) {
                framework.hook(method, ForceTrueHooker.class);
                XposedBridge.log("FixBoostModule: Hooked hasActiveSubscription -> true");
            }
        }
    }

    private void hookAdTimeout() throws Exception {
        Class<?> miningService = framework.findClass("me.carda.awesome_notifications.mining.MiningService");
        if (miningService != null) {
            Method method = framework.findMethod(miningService, "onAdLoadTimeout", "()V");
            if (method != null) {
                framework.hook(method, BlockExecutionHooker.class);
                XposedBridge.log("FixBoostModule: Blocked ad timeout -> mining stop");
            }
        }
    }

    private void hookRateCalculation() throws Exception {
        Class<?> rateHelper = framework.findClass("usdt.app.utils.MiningRateHelper");
        if (rateHelper != null) {
            Method method = framework.findMethod(rateHelper, "getHourlyRate", "()D");
            if (method != null) {
                framework.hook(method, DoubleRateHooker.class);
                XposedBridge.log("FixBoostModule: Hooked getHourlyRate -> 2x multiplier");
            }
        }
    }

    private void hookRandomAdSkip() throws Exception {
        Class<?> miningController = framework.findClass("com.orangepi.bitcoin.mining.MiningController");
        if (miningController != null) {
            Method method = framework.findMethod(miningController, "shouldShowStopMiningAd", "()Z");
            if (method != null) {
                framework.hook(method, ForceTrueHooker.class);
                XposedBridge.log("FixBoostModule: Hooked shouldShowStopMiningAd -> always show");
            }
        }
    }

    public static class ForceTrueHooker {
        @BeforeInvocation
        public static void forceTrue(BeforeHookCallback callback) {
            callback.setResult(true);
            callback.returnAndSkip(true);
        }
    }

    public static class BlockExecutionHooker {
        @BeforeInvocation
        public static void block(BeforeHookCallback callback) {
            callback.returnAndSkip(null);
        }
    }

    public static class DoubleRateHooker {
        @BeforeInvocation
        public static void doubleRate(BeforeHookCallback callback) {
            callback.proceed();
        }
    }
}

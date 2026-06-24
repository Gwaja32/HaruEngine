package org.libertywild.libertyengine.util;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.LeverBlock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LeverLimitManager {
    // 플레이어별 클릭 횟수와 마지막 초기화 시간 기록
    private static final Map<UUID, Integer> clickCounts = new HashMap<>();
    private static final Map<UUID, Long> lastResetTime = new HashMap<>();

    public static boolean canInteract(ServerPlayer player) {
        if (!ConfigManager.data.enableLeverLimit) return true;

        UUID uuid = player.getUUID();
        long currentTime = System.currentTimeMillis();

        // 1초가 지났으면 카운트 초기화
        if (currentTime - lastResetTime.getOrDefault(uuid, 0L) > 1000) {
            clickCounts.put(uuid, 0);
            lastResetTime.put(uuid, currentTime);
        }

        int count = clickCounts.getOrDefault(uuid, 0);
        if (count >= ConfigManager.data.maxLeverInteractionsPerSecond) {
            return false; // 제한 초과
        }

        clickCounts.put(uuid, count + 1);
        return true;
    }

    public static void registerLeverLimiter() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide()) return InteractionResult.PASS;

            // 클릭한 블록이 레버인지 확인
            if (world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof LeverBlock) {
                if (!LeverLimitManager.canInteract((ServerPlayer) player)) {
                    // 제한 초과 시 상호작용 취소
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });
    }
}
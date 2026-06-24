package org.libertywild.libertyengine.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public class DynamicDistanceManager {

    private static final int DEFAULT_DISTANCE = 10;
    private static final int OPTIMIZED_DISTANCE = 5;
    private static final int THRESHOLD_PLAYERS = 10;
    private static final int DELAY_TICKS = 30 * 20; // 30초 (20틱 * 30)

    private static int pendingDistance = -1;
    private static int countdown = -1;
    private static int currentDistance = DEFAULT_DISTANCE;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DynamicDistanceManager::onTick);
    }

    private static void onTick(MinecraftServer server) {
        if (!ConfigManager.data.enableDynamicDistance) return;

        if (server.getPlayerList().getSimulationDistance() != 5) {
            server.getPlayerList().setSimulationDistance(5);
        }
        int playerCount = server.getPlayerList().getPlayers().size();
        int targetDistance = (playerCount >= THRESHOLD_PLAYERS) ? OPTIMIZED_DISTANCE : DEFAULT_DISTANCE;

        // 1. 상태 변화 감지 (목표 거리가 현재 설정과 다를 때)
        if (targetDistance != currentDistance && pendingDistance != targetDistance) {
            pendingDistance = targetDistance;
            countdown = DELAY_TICKS;

            String message = (targetDistance == OPTIMIZED_DISTANCE)
                    ? "[LibertyWild] 30초 뒤, 최적화를 위해 렌더 거리가 하향 조정됩니다."
                    : "[LibertyWild] 30초 뒤, 렌더 거리가 다시 상향 조정됩니다.";
            broadcast(server, message, ChatFormatting.YELLOW);
        }

        // 2. 카운트다운 진행 중 조건 변화 체크 (취소 로직)
        if (countdown > 0) {
            // 카운트다운 도중에 다시 인원수가 바뀌어서 원래 거리로 돌아가야 하는 상황이면
            if (targetDistance == currentDistance) {
                String cancelMessage = (pendingDistance == OPTIMIZED_DISTANCE)
                        ? "[LibertyWild] 최적화를 위한 렌더 거리 하향 조정이 취소되었습니다."
                        : "[LibertyWild] 렌더 거리 상향 조정이 취소되었습니다.";
                broadcast(server, cancelMessage, ChatFormatting.RED);

                reset();
                return;
            }

            countdown--;

            // 3. 30초 경과 후 실제 적용
            if (countdown == 0) {
                applyDistance(server, pendingDistance);
                currentDistance = pendingDistance;
                reset();
            }
        }
    }

    private static void applyDistance(MinecraftServer server, int distance) {
        // 서버 렌더 거리 및 시뮬레이션 거리 설정
        server.getPlayerList().setViewDistance(distance);

        String completeMessage = "[LibertyWild] 서버의 렌더 거리가 " + distance + "으로 변경되었습니다.";
        broadcast(server, completeMessage, ChatFormatting.GREEN);
    }

    private static void reset() {
        pendingDistance = -1;
        countdown = -1;
    }

    private static void broadcast(MinecraftServer server, String message, ChatFormatting color) {
        server.getPlayerList().broadcastSystemMessage(
                Component.literal(message).withStyle(color),
                false
        );
    }
}
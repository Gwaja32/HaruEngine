package kr.haruserver.haruengine.api;

import net.minecraft.server.level.ServerPlayer;

public interface ChunkMapAccessor {
    void setViewDistanceForce(int distance);

    // Shadow한 메서드를 직접 실행해주는 통로 메서드
    void invokeUpdatePlayerStatus(ServerPlayer player, boolean flag);
}

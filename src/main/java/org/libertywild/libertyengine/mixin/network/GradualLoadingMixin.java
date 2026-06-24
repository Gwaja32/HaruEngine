package org.libertywild.libertyengine.mixin.network;

import org.libertywild.libertyengine.api.ChunkMapAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(ServerPlayer.class)
public abstract class GradualLoadingMixin {

    @Inject(method = "initInventoryMenu", at = @At("TAIL"))
    private void startGradualLoading(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        MinecraftServer server = player.level().getServer();
        ChunkMap chunkMap = player.level().getChunkSource().chunkMap;
        ChunkMapAccessor accessor = (ChunkMapAccessor) chunkMap;

        int targetDistance = server.getPlayerList().getViewDistance();

        // 별도의 스레드 시작
        new Thread(() -> {
            try {
                // 1. 처음엔 아주 좁게 로딩
                accessor.setViewDistanceForce(0);
                // 메인 서버 스레드에서 실행되도록 요청
                server.execute(() -> accessor.invokeUpdatePlayerStatus(player, true));

                for (int i = 1; i <= targetDistance; i++) {
                    Random random = new Random();
                    int r = random.nextInt(1500, 2001);
                    // 각 단계마다 2초(2000ms)씩 강제 대기
                    Thread.sleep(r);

                    final int currentDistance = i;

                    // 메인 서버 스레드에서 실행
                    server.execute(() -> {
                        if (player.hasDisconnected()) return;
                        accessor.setViewDistanceForce(currentDistance);
                        accessor.invokeUpdatePlayerStatus(player, true);
                    });
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}


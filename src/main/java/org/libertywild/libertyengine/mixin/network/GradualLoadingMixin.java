package org.libertywild.libertyengine.mixin.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.ProfilePublicKey;
import org.libertywild.libertyengine.api.ChunkMapAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ServerPlayer.class)
public abstract class GradualLoadingMixin {

    @Unique
    private static final Set<ServerPlayer> LOADING_PLAYERS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Shadow private int requestedViewDistance;

    @Inject(method = "initInventoryMenu", at = @At("TAIL"))
    private void startGradualLoading(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        if (!LOADING_PLAYERS.add(player)) {
            return;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        ServerLevel level = player.level();
        ChunkMap chunkMap = level.getChunkSource().chunkMap;
        ChunkMapAccessor accessor = (ChunkMapAccessor) chunkMap;

        int targetDistance = server.getPlayerList().getViewDistance();

        // 1. 진입 직후 시야각을 0으로 설정하여 초기 렉 방지
        server.execute(() -> {
            if (player.hasDisconnected() || player.connection == null) return;
            this.requestedViewDistance = 0;
            accessor.invokeUpdatePlayerStatus(player, true);
        });

        // 2. 점진적 로딩 스레드
        new Thread(() -> {
            try {
                Random random = new Random();
                for (int i = 2; i <= targetDistance; i++) {
                    int r = random.nextInt(1500, 2001);
                    Thread.sleep(r);

                    if (player.hasDisconnected()) break;

                    final int currentDistance = i;
                    server.execute(() -> {
                        if (player.hasDisconnected() || player.connection == null) return;

                        this.requestedViewDistance = currentDistance;
                        accessor.invokeUpdatePlayerStatus(player, true);
                    });
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "GradualChunkLoading-Thread").start();
    }

    @Inject(method = "disconnect", at = @At("HEAD"))
    private void onDisconnect(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        LOADING_PLAYERS.remove(player);
    }
}

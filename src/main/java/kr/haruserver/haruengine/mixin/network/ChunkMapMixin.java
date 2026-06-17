package kr.haruserver.haruengine.mixin.network;

import kr.haruserver.haruengine.api.ChunkMapAccessor;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin implements ChunkMapAccessor {

    @Shadow
    private int serverViewDistance;

    // 만약 컴파일 에러가 난다면, updatePlayerStatus의 난독화된 이름을 찾아야 합니다.
    @Shadow
    public abstract void updatePlayerStatus(ServerPlayer player, boolean flag);

    @Override
    @Accessor("serverViewDistance")
    public abstract void setViewDistanceForce(int distance);

    @Override
    public void invokeUpdatePlayerStatus(ServerPlayer player, boolean flag) {
        this.updatePlayerStatus(player, flag);
    }
}

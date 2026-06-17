package kr.haruserver.haruengine.api;

import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkMap.class)
public interface ChunkMapInterface extends ChunkMapAccessor {
    @Override
    @Accessor("serverViewDistance")
    void setViewDistanceForce(int distance);
}

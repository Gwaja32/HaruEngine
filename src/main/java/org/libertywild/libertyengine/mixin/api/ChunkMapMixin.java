package org.libertywild.libertyengine.mixin.api;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import org.libertywild.libertyengine.api.ChunkMapAccessor;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin implements ChunkMapAccessor {
    @Override
    @Accessor("serverViewDistance")
    public abstract void setViewDistanceForce(int distance);

    @Override
    @Invoker("updatePlayerStatus")
    public abstract void invokeUpdatePlayerStatus(ServerPlayer player, boolean flag);

    @Override
    @Accessor("visibleChunkMap")
    public abstract Long2ObjectLinkedOpenHashMap<ChunkHolder> getVisibleChunkMap();
}

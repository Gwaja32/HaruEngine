package kr.haruserver.haruengine.util;

import java.util.Set;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

public class EntityFilter {

    // 초기화 시에만 사용하는 내부용 셋
    private static final Set<EntityType<?>> OPT_SET = Set.of(
            EntityTypes.CHICKEN, EntityTypes.COW, EntityTypes.PIG, EntityTypes.RABBIT,
            EntityTypes.SHEEP, EntityTypes.MOOSHROOM, EntityTypes.TROPICAL_FISH,
            EntityTypes.SALMON, EntityTypes.COD, EntityTypes.BAT,
            EntityTypes.SQUID, EntityTypes.GLOW_SQUID,
            EntityTypes.HAPPY_GHAST, EntityTypes.PHANTOM
    );

    private static final Set<EntityType<?>> PHYS_SET = Set.of(
            EntityTypes.CHICKEN, EntityTypes.COW, EntityTypes.PIG, EntityTypes.RABBIT,
            EntityTypes.SHEEP, EntityTypes.MOOSHROOM, EntityTypes.TROPICAL_FISH,
            EntityTypes.SALMON, EntityTypes.COD,
            EntityTypes.SQUID, EntityTypes.GLOW_SQUID
    );

    private static final Set<EntityType<?>> PARA_SET = Set.of(
            EntityTypes.HAPPY_GHAST, EntityTypes.PHANTOM, EntityTypes.BAT
    );

    /**
     * 엔티티 생성 시 비트마스크를 계산하는 로직 (1회성)
     */
    public static byte calculateMask(EntityType<?> type) {
        byte mask = 0;
        if (OPT_SET.contains(type)) mask |= 1;  // 001
        if (PHYS_SET.contains(type)) mask |= 2; // 010
        if (PARA_SET.contains(type)) mask |= 4; // 100
        return mask;
    }

    /**
     * [Spark 대응] 이제 Set을 뒤지지 않고 엔티티 내부의 숫자 하나만 확인합니다.
     */
    public static boolean isOptimizationTarget(Entity entity) {
        return (((HaruEntityAccessor) entity).haru$getFilterMask() & 1) != 0;
    }

    public static boolean isNoPhysicsTarget(Entity entity) {
        return (((HaruEntityAccessor) entity).haru$getFilterMask() & 2) != 0;
    }

    public static boolean isParallelTarget(Entity entity) {
        return (((HaruEntityAccessor) entity).haru$getFilterMask() & 4) != 0;
    }
}
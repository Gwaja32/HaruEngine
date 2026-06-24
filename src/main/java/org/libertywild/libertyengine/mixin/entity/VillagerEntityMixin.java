package org.libertywild.libertyengine.mixin.entity;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;

@Mixin(Villager.class)
public abstract class VillagerEntityMixin extends AbstractVillager {

    @Shadow private long lastRestockGameTime;
    @Shadow private int numberOfRestocksToday;
    @Shadow private long lastRestockCheckDay;
    @Shadow protected abstract void updateDemand();
    @Shadow protected abstract void resendOffersToTradingPlayer();

    @Unique private boolean lobotomy$needsRestockAfterClosing = false;
    @Unique private int lobotomy$aiActiveTicks = 0;
    @Unique private boolean lobotomy$isGossipProcessed = false; // 소문 처리 플래그

    public VillagerEntityMixin(EntityType<? extends AbstractVillager> entityType, Level world) {
        super(entityType, world);
    }

    // [규칙 1 & 2] 공격받았을 때
    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        boolean damaged = super.hurtServer(world, source, amount);
        if (damaged && source.getEntity() instanceof Player) {
            // 때렸을 때만 소문이 갱신되도록 플래그 초기화
            this.lobotomy$isGossipProcessed = false;
            this.lobotomy$aiActiveTicks = 40;
        }
        return damaged;
    }

    // [핵심] 거래창 열 때 소문 계산 제어
    @Inject(method = "updateSpecialPrices", at = @At("HEAD"), cancellable = true)
    private void onPrepareOffersFor(Player player, CallbackInfo ci) {
        // 이미 소문이 가격에 반영된 상태라면, 바닐라의 중복 계산 로직을 막음
        if (this.lobotomy$isGossipProcessed) {
            this.resendOffersToTradingPlayer(); // 가격 갱신 없이 창만 표시
            ci.cancel();
            return;
        }
        // 처음 열 때는 바닐라 로직을 실행하게 두고, 플래그를 true로 변경
        this.lobotomy$isGossipProcessed = true;
    }

    @Inject(method = "rewardTradeXp", at = @At("HEAD"))
    private void onAfterUsing(MerchantOffer offer, CallbackInfo ci) {
        this.lobotomy$aiActiveTicks = 600;
    }

    @Override
    public void setTradingPlayer(@Nullable Player customer) {
        if (this.getTradingPlayer() != null && customer == null) {
            if (this.lobotomy$hasAnySoldOutItem()) {
                this.lobotomy$needsRestockAfterClosing = true;
            }
        }
        super.setTradingPlayer(customer);
    }

    @Inject(method = "updateDemand", at = @At("HEAD"), cancellable = true)
    private void onUpdateDemandBonus(CallbackInfo ci) {
        if (this.lobotomy$needsRestockAfterClosing) {
            ci.cancel();
        }
    }

    @Inject(method = "restock", at = @At("TAIL"))
    private void afterRestock(CallbackInfo ci) {
        if (this.lobotomy$needsRestockAfterClosing) {
            for (MerchantOffer offer : this.getOffers()) {
                try {
                    // 필드 캐싱 없이 리플렉션을 직접 쓸 경우 대비하여 Yarn/Intermediary 둘 다 대응
                    Field demandField;
                    try {
                        demandField = MerchantOffer.class.getDeclaredField("demandBonus");
                    } catch (NoSuchFieldException e) {
                        demandField = MerchantOffer.class.getDeclaredField("field_18668");
                    }
                    demandField.setAccessible(true);
                    demandField.setInt(offer, 0);
                } catch (Exception ignored) {}
            }
            this.lobotomy$needsRestockAfterClosing = false;
        }
    }

    @Unique
    private boolean lobotomy$hasAnySoldOutItem() {
        for (MerchantOffer offer : this.getOffers()) {
            if (offer.isOutOfStock()) return true;
        }
        return false;
    }

    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    private void handleMobTick(ServerLevel world, CallbackInfo ci) {
        if (this.lobotomy$aiActiveTicks > 0) this.lobotomy$aiActiveTicks--;
        this.lobotomy$originalDayResetLogic(world);

        Villager self = (Villager) (Object) this;
        boolean hasJob = !self.getVillagerData().profession().is(VillagerProfession.NONE);
        boolean isChunSik = this.hasCustomName() && "춘식이".equals(this.getCustomName().getString());

        // 춘식이거나 아기 주민이면 AI 제한을 건너뜀
        if (isChunSik) return;

        if (hasJob) {
            boolean hasExperience = self.getVillagerXp() > 0;
            if (!hasExperience) {
                Optional<GlobalPos> jobSite = self.getBrain().getMemory(MemoryModuleType.JOB_SITE);
                if (jobSite.isPresent()) {
                    GlobalPos pos = jobSite.get();
                    if (world.dimension() != pos.dimension() || !world.getPoiManager().exists(pos.pos(), (poiType) -> true)) {
                        self.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
                        return;
                    }
                } else {
                    return;
                }
            }

            if (this.isTrading() || this.lobotomy$aiActiveTicks > 0) return;

            ci.cancel();
        }
        else {
            int chunkX = this.blockPosition().getX() >> 4;
            int chunkZ = this.blockPosition().getZ() >> 4;

            if (!this.lobotomy$isPlayerInNearbyChunks(world, chunkX, chunkZ, 1)) {
                ci.cancel();
            }
        }
    }

    /**
     * [Spark/WorldThreader 최적화] 인접 1청크(3x3) 내 플레이어 존재 여부 확인
     * @param radius 1이면 상하좌우 및 대각선 1칸 포함 (총 3x3 영역)
     */
    @Unique
    private boolean lobotomy$isPlayerInNearbyChunks(ServerLevel world, int entityX, int entityZ, int radius) {
        // WorldThreader 환경에서 안전하게 플레이어 리스트 순회
        for (ServerPlayer player : world.players()) {
            int playerX = player.getBlockX() >> 4;
            int playerZ = player.getBlockZ() >> 4;

            // 정수 절대값 계산으로 3x3 영역 판정 (매우 빠름)
            if (Math.abs(playerX - entityX) <= radius && Math.abs(playerZ - entityZ) <= radius) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private void lobotomy$originalDayResetLogic(ServerLevel world) {
        long currentTime = world.getGameTime();
        long currentDay = world.getOverworldClockTime() / 24000L;
        if (currentTime > this.lastRestockGameTime + 12000L || (this.lastRestockCheckDay > 0L && currentDay > this.lastRestockCheckDay)) {
            this.lastRestockCheckDay = currentDay;
            this.lastRestockGameTime = currentTime;
            this.lobotomy$clearDailyRestockCount();
            this.lobotomy$needsRestockAfterClosing = false;
            // 날짜가 지나면 소문이 자연 감소했을 수 있으므로 플래그 초기화
            this.lobotomy$isGossipProcessed = false;
        }
    }

    @Unique
    private void lobotomy$clearDailyRestockCount() {
        int availableSlots = 2 - this.numberOfRestocksToday;
        if (availableSlots > 0) {
            for (MerchantOffer tradeOffer : this.getOffers()) tradeOffer.resetUses();
        }
        for (int j = 0; j < availableSlots; ++j) this.updateDemand();
        this.resendOffersToTradingPlayer();
        this.numberOfRestocksToday = 0;
    }
}

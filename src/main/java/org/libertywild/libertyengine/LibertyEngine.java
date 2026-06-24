package org.libertywild.libertyengine;

import org.libertywild.libertyengine.command.LobotomizeCommand;
import org.libertywild.libertyengine.util.CommandManager;
import org.libertywild.libertyengine.util.ConfigManager;
import org.libertywild.libertyengine.util.DynamicDistanceManager;
import org.libertywild.libertyengine.util.LeverLimitManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibertyEngine implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("LibertyEngine");

    @Override
    public void onInitialize() {
        // 모드 초기화 시점, 가장 먼저 설정 파일을 로드합니다.
        ConfigManager.load();
        LOGGER.info("[LibertyEngine] Config loaded successfully.");

        // 1. 명령어 등록
        LobotomizeCommand.register();
        CommandManager.registerCommand();

        // 2. 동적 거리 관리자 등록 (성능 최적화 핵심)
        DynamicDistanceManager.register();
        LeverLimitManager.registerLeverLimiter();

        LOGGER.info("[LibertyEngine] Optimization components registered successfully.");

        // [수정] ParallelEngine 예열 코드를 삭제했습니다.
        // 더 이상 병렬 스레드 풀을 메인 틱에서 관리하지 않아 오버헤드가 줄어듭니다.
    }
}
package kr.haruserver.haruengine;

import kr.haruserver.haruengine.command.LobotomizeCommand;
import kr.haruserver.haruengine.util.CommandManager;
import kr.haruserver.haruengine.util.DynamicDistanceManager;
// import kr.haruserver.haruengine.util.ParallelEngine; // 이제 사용하지 않으므로 제거 가능
import kr.haruserver.haruengine.util.LeverLimitManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HaruEngine implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("HaruEngine");

    @Override
    public void onInitialize() {
        // 1. 명령어 등록
        LobotomizeCommand.register();
        CommandManager.registerCommand();

        // 2. 동적 거리 관리자 등록 (성능 최적화 핵심)
        DynamicDistanceManager.register();
        LeverLimitManager.registerLeverLimiter();

        LOGGER.info("HaruEngine: Optimization components registered successfully.");

        // [수정] ParallelEngine 예열 코드를 삭제했습니다.
        // 더 이상 병렬 스레드 풀을 메인 틱에서 관리하지 않아 오버헤드가 줄어듭니다.
    }
}
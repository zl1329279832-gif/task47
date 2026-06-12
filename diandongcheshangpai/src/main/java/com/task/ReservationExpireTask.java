package com.task;

import com.service.XuanpaiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 预占过期定时释放任务
 */
@Component
public class ReservationExpireTask {
    private static final Logger logger = LoggerFactory.getLogger(ReservationExpireTask.class);

    @Autowired
    private XuanpaiService xuanpaiService;

    /**
     * 每60秒检查并释放过期的预占号牌
     */
    @Scheduled(fixedRate = 60000)
    public void releaseExpired() {
        try {
            int count = xuanpaiService.releaseExpiredReservations();
            if (count > 0) {
                logger.info("已释放{}个过期预占号牌", count);
            }
        } catch (Exception e) {
            logger.error("释放过期预占号牌异常", e);
        }
    }
}

package dev.comon.toss_watch.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 대시보드가 마지막으로 조회한 포트폴리오의 보유 종목 스냅샷 1행.
 *
 * 계좌별로 나누지 않고 "현재 표시 중인 포트폴리오" 전체를 통째로 교체하는
 * 방식으로 운용된다 — [dev.comon.toss_watch.core.database.dao.PortfolioStockDao.replaceAll] 참고.
 */
@Entity(tableName = "portfolio_stocks")
data class PortfolioStockEntity(
    @PrimaryKey val stockCode: String,
    val stockName: String,
)

package com.dayone.scheduler;

import com.dayone.model.Company;
import com.dayone.model.ScrapedResult;
import com.dayone.model.constants.CacheKey;
import com.dayone.persists.CompanyRepository;
import com.dayone.persists.DividendRepository;
import com.dayone.persists.entity.CompanyEntity;
import com.dayone.persists.entity.DividendEntity;
import com.dayone.scraper.Scraper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
@EnableCaching
public class ScraperScheduler {

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;
    private final Scraper yahooFinanceScraper;

    // 일정 주기마다 수행
    @CacheEvict(value = CacheKey.KEY_FINANCE , allEntries = true)
    @Scheduled(cron = "0 0 0 * * *")
    public void yahooFinanceScheduling(){
        log.info("scraping scheduler is started");
        // 저장된 회사 목록 조회
        List<CompanyEntity> companies =this.companyRepository.findAll();

        // 회사마다 배당금 정보를 새로 스크래핑
        for(var company : companies){
            log.info("scraping scheduler is started - > " + company.getName());
            ScrapedResult scrapedResult = this.yahooFinanceScraper.scrap(new Company(company.getTicker(), company.getName()));
            // 스크래핑한 대방금 정보 중 데이터베이스에 없는 값은 저장
            scrapedResult.getDividendEntities().stream()
                    // 디비든 모델을 디비든 엔티티로 매핑
                    .map(e -> new DividendEntity(company.getId(), e))
                    // 엘리먼트를 하나씩 디비든 레퍼지토리에 삽입
                    .forEach(e -> {
                        boolean exists = this.dividendRepository.existsByCompanyIdAndDate(e.getCompanyId(), e.getDate());
                        if(!exists){
                            this.dividendRepository.save(e);
                            log.info("insert new dividend -> "+e.toString());
                        }
                    });
            // 연속적으로 스크래핑 대상 사이트 서버에 요청을 날리지 않도록 일시정지
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

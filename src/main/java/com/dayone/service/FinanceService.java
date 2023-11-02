package com.dayone.service;

import com.dayone.exception.impl.NoCompanyException;
import com.dayone.model.Company;
import com.dayone.model.Dividend;
import com.dayone.model.ScrapedResult;
import com.dayone.model.constants.CacheKey;
import com.dayone.persists.CompanyRepository;
import com.dayone.persists.DividendRepository;
import com.dayone.persists.entity.CompanyEntity;
import com.dayone.persists.entity.DividendEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
@Slf4j
@Service
@AllArgsConstructor
public class FinanceService {

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;

    @Cacheable(key = "#companyName", value = CacheKey.KEY_FINANCE)
    public ScrapedResult getDividendByCompanyName(String companyName){
        log.info("search company : "+ companyName);
        // 1. 회사명을 기준으로 회사 정보를 조회
        CompanyEntity company = this.companyRepository.findByName(companyName)
                .orElseThrow(() -> new NoCompanyException());

        // 2. 조회된 회사 ID 로 배당금 정보 조회
        List<DividendEntity> dividendEntities = this.dividendRepository.findByCompanyId(company.getId());

        // 3. 결과 조홥 후 반환
//        List<Dividend> dividends = new ArrayList<>();
//        for(var entity : dividendEntities){
//            dividends.add(Dividend.builder()
//                            .date(entity.getDate())
//                            .dividend(entity.getDividend())
//                            .build());
//        }
        List<Dividend> dividends = dividendEntities.stream()
                .map(e -> new Dividend(e.getDate() , e.getDividend()))
                .collect(Collectors.toList());

        return new ScrapedResult(new Company(company.getTicker(), company.getName()),
                dividends);
    }
}

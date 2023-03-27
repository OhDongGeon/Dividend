package com.example.dividend.service;

import com.example.dividend.exception.impl.AlreadyExistsTickerException;
import com.example.dividend.exception.impl.NoCompanyException;
import com.example.dividend.repository.entity.CompanyEntity;
import com.example.dividend.repository.entity.DividendEntity;
import com.example.dividend.model.Company;
import com.example.dividend.model.ScrapedResult;
import com.example.dividend.repository.CompanyRepository;
import com.example.dividend.repository.DividendRepository;
import com.example.dividend.scraper.Scraper;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.Trie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CompanyService {
    private final Trie trie;
    private final Scraper yahooFinanceScraper;
    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;


    public Company save(String ticker) {
        boolean exists = this.companyRepository.existsByTicker(ticker);
        if(exists) {
            throw new AlreadyExistsTickerException();
        }
        return this.storeCompanyAndDividend(ticker);
    }


    public Page<CompanyEntity> getAllCompany(Pageable pageable) {
        return this.companyRepository.findAll(pageable);
    }


    private Company storeCompanyAndDividend(String ticker) {
        // ticker 기준 회사 스크래핑
        Company company = this.yahooFinanceScraper.scrapCompanyByTicker(ticker);

        if(ObjectUtils.isEmpty(company)) {
            throw new RuntimeException("failed to scrap ticker -> " + ticker);
        }

        // 해당 회사가 존재할 경우, 회사의 배당금 정보를 스크래핑
        ScrapedResult scrapedResult = this.yahooFinanceScraper.scrap(company);

        // 스크래핑 결과
        CompanyEntity companyEntity = this.companyRepository.save(new CompanyEntity(company));
        List<DividendEntity> dividendEntityList= scrapedResult.getDividends().stream()
                                                                                .map(e -> new DividendEntity(companyEntity.getId(), e))
                                                                                .collect(Collectors.toList());

        this.dividendRepository.saveAll(dividendEntityList);

        return company;
    }


    public List<String> getCompanyNamesByKeyword(String keyword) {
        // 10개씩 조회
        Pageable limit = PageRequest.of(0, 10);
        Page<CompanyEntity> companyEntities = this.companyRepository.findByNameStartingWithIgnoreCase(keyword, limit);
        return companyEntities.stream()
                        .map(e -> e.getName())
                        .collect(Collectors.toList());
    }


    // 회사명 자동완성 저장 (Trie)
    public void addAutocompleteKeyword(String keyword) {
        this.trie.put(keyword, null);
    }


    // 회사명 자동완성 조회
    public List<String> autocomplete(String keyword) {
        return (List<String>) this.trie.prefixMap(keyword).keySet()
                                                            .stream()
                                                            .collect(Collectors.toList());
    }


    // 회사명 자동완성 키워드 삭제
    public  void deleteAutocompleteKeyword(String keyword) {
        this.trie.remove(keyword);
    }


    public String deleteCompany(String ticker) {
        var company = this.companyRepository.findByTicker(ticker)
                                        .orElseThrow(() -> new NoCompanyException());

        this.dividendRepository.deleteAllByCompanyId(company.getId());
        this.companyRepository.delete(company);

        // 회사를 지우면 자동완성의 회사명도 삭제 해야하기 떄문
        this.deleteAutocompleteKeyword(company.getName());
        return company.getName();
    }
}

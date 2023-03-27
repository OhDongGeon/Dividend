package com.example.dividend.scraper;

import com.example.dividend.exception.impl.AlreadyExistsTickerException;
import com.example.dividend.exception.impl.NoCompanyException;
import com.example.dividend.model.Company;
import com.example.dividend.model.Dividend;
import com.example.dividend.model.ScrapedResult;
import com.example.dividend.model.constants.Month;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Component
public class YahooFinanceScraper implements Scraper {
    // 야후에서 배당금 조회할 수 있는 URL %s:회사명
    private static final String STATISTICS_URL = "https://finance.yahoo.com/quote/%s/history?period1=%d&period2=%d&interval=1mo";
    // 회사명 경로
    private static final String SUMMARY_URL = "https://finance.yahoo.com/quote/%s?p=%s";
    private static final long START_TIME = 86400; // 60초 * 60분 * 24시간


    @Override
    public ScrapedResult scrap(Company company) {
        var scrapResult = new ScrapedResult();
        scrapResult.setCompany(company);

        try {
            long now = System.currentTimeMillis() / 1000;
            String url = String.format(STATISTICS_URL, company.getTicker(), START_TIME, now);

            Connection connect = Jsoup.connect(url);
            Document document = connect.get();

            Elements parsingDivs = document.getElementsByAttributeValue("data-test", "historical-prices"); // 테이블의 속성
            Element tableEle = parsingDivs.get(0); // table 전체
            Element tbody = tableEle.children().get(1); // get(0) head, get(1) body, get(2) foot

            List<Dividend> dividends = new ArrayList<>();

            for (Element item : tbody.children()) {
                String txt = item.text();
                if (!txt.endsWith("Dividend")) {
                    continue;
                }

                String[] splits = txt.split(" ");
                int month = Month.strToNumber(splits[0]);
                int day = Integer.valueOf(splits[1].replace(",", ""));
                int year = Integer.valueOf(splits[2]);
                String dividend = splits[3];

                if (month < 0) {
                    throw new RuntimeException("Unexpected Month enum value ->" + splits[0]);
                }

                dividends.add(new Dividend(LocalDateTime.of(year, month, day, 0, 0), dividend));
            }
            scrapResult.setDividends(dividends);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return scrapResult;
    }


    @Override
    public Company scrapCompanyByTicker(String ticker) {
        String url = String.format(SUMMARY_URL, ticker, ticker);
        try {
            Document document = Jsoup.connect(url).get();

            if(document.getElementsByTag("h1").toString().equals("")) {
                throw new NoCompanyException();
            }

            Element titleEle = document.getElementsByTag("h1").get(0);

            if(titleEle.text().split(" - ").length < 2) {
                throw new NoCompanyException();
            }

            // ex) abc - def - ghi  = def 로 변경
            String title = titleEle.text().split(" - ")[1].trim();

            return new Company(ticker, title);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
































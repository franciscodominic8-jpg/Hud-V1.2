package com.julian.foldhud;


final class Config {
    // Each outlet: {name, address}. Several addresses for one outlet can be joined with | (the first that works is used).
    static final String[][] WORLD = {
        {"BBC", "https://feeds.bbci.co.uk/news/world/rss.xml"},
        {"AL JAZEERA", "https://www.aljazeera.com/xml/rss/all.xml"},
        {"NPR", "https://feeds.npr.org/1004/rss.xml"},
        {"DW", "https://rss.dw.com/rdf/rss-en-world"}
    };
    static final String[][] MYANMAR = {
        {"IRRAWADDY", "https://www.irrawaddy.com/feed"},
        {"IRRAWADDY MM", "https://burma.irrawaddy.com/feed"},
        {"MYANMAR NOW", "https://myanmar-now.org/en/feed/"},
        {"DVB", "https://english.dvb.no/rss/|https://english.dvb.no/feed/"},
        {"DVB BURMESE", "https://burmese.dvb.no/rss/|https://burmese.dvb.no/feed"},
        {"KHIT THIT", "https://yktnews.com/feed/"},
        {"MIZZIMA", "https://eng.mizzima.com/feed"},
        {"MIZZIMA MM", "https://bur.mizzima.com/feed"},
        {"FRONTIER", "https://www.frontiermyanmar.net/en/feed/"},
        {"BBC BURMESE", "https://feeds.bbci.co.uk/burmese/rss.xml"}
    };
    // Yahoo Finance symbols and the names shown on screen
    static final String[] STOCKS = {"^GSPC", "AAPL", "MSFT", "NVDA"};
    static final String[] STOCK_NAMES = {"S&P 500", "AAPL", "MSFT", "NVDA"};
    // How old data may get before it is refreshed (milliseconds). Longer = less battery.
    static final long NEWS_TTL = 20 * 60000L, STOCKS_TTL = 10 * 60000L, WEATHER_TTL = 45 * 60000L, LOC_TTL = 60 * 60000L;
    static final int STEP_GOAL = 10000;
}
